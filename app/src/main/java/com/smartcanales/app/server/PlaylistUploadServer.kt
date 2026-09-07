package com.smartcanales.app.server

import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mini servidor HTTP en la TV para subir playlists .m3u desde PC/móvil (misma LAN).
 *
 * GET  /        → página de subida
 * POST /upload  → cuerpo = texto M3U
 * GET  /status  → {"ok":true}
 */
class PlaylistUploadServer(
	private val port: Int = DEFAULT_PORT,
	private val onPlaylistReceived: (content: String) -> Unit
) {

	private val running = AtomicBoolean(false)
	private var serverSocket: ServerSocket? = null
	private val executor = Executors.newCachedThreadPool()

	val isRunning: Boolean
		get() = running.get()

	fun start() {
		if (!running.compareAndSet(false, true)) return
		executor.execute {
			try {
				val server = ServerSocket()
				server.reuseAddress = true
				server.bind(InetSocketAddress("0.0.0.0", port))
				serverSocket = server
				Log.i(TAG, "Upload server listening on 0.0.0.0:$port")
				while (running.get()) {
					try {
						val client = server.accept()
						executor.execute { handleClient(client) }
					} catch (_: SocketException) {
						break
					} catch (e: Exception) {
						Log.e(TAG, "Accept failed", e)
					}
				}
			} catch (e: Exception) {
				Log.e(TAG, "Server failed to start on port $port", e)
				running.set(false)
			} finally {
				try {
					serverSocket?.close()
				} catch (_: Exception) {
				}
				serverSocket = null
			}
		}
	}

	fun stop() {
		running.set(false)
		try {
			serverSocket?.close()
		} catch (_: Exception) {
		}
		serverSocket = null
	}

	private fun handleClient(socket: Socket) {
		socket.soTimeout = 30_000
		socket.use { client ->
			try {
				val raw = readRequest(client) ?: return
				val requestLine = raw.requestLine
				val parts = requestLine.split(" ")
				if (parts.size < 2) {
					writeResponse(client, 400, "text/plain; charset=utf-8", "Bad Request")
					return
				}
				val method = parts[0].uppercase()
				val path = parts[1].substringBefore('?')

				when {
					method == "OPTIONS" -> {
						writeResponse(client, 204, "text/plain; charset=utf-8", "")
					}
					method == "GET" && (path == "/" || path == "/index.html") -> {
						writeResponse(client, 200, "text/html; charset=utf-8", INDEX_HTML)
					}
					method == "GET" && path == "/status" -> {
						writeResponse(
							client,
							200,
							"application/json; charset=utf-8",
							"""{"ok":true,"service":"SmartCanales","port":$port}"""
						)
					}
					method == "POST" && path == "/upload" -> {
						val body = raw.body
						if (body.isBlank()) {
							writeResponse(client, 400, "text/plain; charset=utf-8", "Playlist vacia")
							return
						}
						if (body.length > MAX_BODY_CHARS) {
							writeResponse(client, 400, "text/plain; charset=utf-8", "Archivo demasiado grande")
							return
						}
						try {
							onPlaylistReceived(body)
							writeResponse(client, 200, "text/html; charset=utf-8", SUCCESS_HTML)
						} catch (e: Exception) {
							Log.e(TAG, "Import failed", e)
							writeResponse(
								client,
								500,
								"text/plain; charset=utf-8",
								"Error importando: ${e.message}"
							)
						}
					}
					else -> writeResponse(client, 404, "text/plain; charset=utf-8", "Not Found: $path")
				}
			} catch (e: Exception) {
				Log.e(TAG, "Client handling failed", e)
				try {
					writeResponse(client, 500, "text/plain; charset=utf-8", "Error: ${e.message}")
				} catch (_: Exception) {
				}
			}
		}
	}

	private data class HttpRequest(
		val requestLine: String,
		val headers: Map<String, String>,
		val body: String
	)

	private fun readRequest(socket: Socket): HttpRequest? {
		val input = socket.getInputStream()
		val headerBytes = ByteArrayOutputStream()
		var state = 0 // looking for \r\n\r\n
		while (state < 4) {
			val b = input.read()
			if (b < 0) return null
			headerBytes.write(b)
			when (state) {
				0 -> state = if (b == '\r'.code) 1 else 0
				1 -> state = if (b == '\n'.code) 2 else 0
				2 -> state = if (b == '\r'.code) 3 else 0
				3 -> state = if (b == '\n'.code) 4 else 0
			}
			if (headerBytes.size() > 64 * 1024) return null
		}

		val headerText = headerBytes.toString(Charsets.UTF_8.name())
		val lines = headerText.split("\r\n")
		if (lines.isEmpty()) return null
		val requestLine = lines[0]
		val headers = linkedMapOf<String, String>()
		for (i in 1 until lines.size) {
			val line = lines[i]
			if (line.isEmpty()) break
			val idx = line.indexOf(':')
			if (idx > 0) {
				headers[line.substring(0, idx).trim().lowercase()] =
					line.substring(idx + 1).trim()
			}
		}

		val length = headers["content-length"]?.toIntOrNull() ?: 0
		val body = if (length > 0) {
			val buf = ByteArray(length)
			var read = 0
			while (read < length) {
				val n = input.read(buf, read, length - read)
				if (n < 0) break
				read += n
			}
			String(buf, 0, read, Charsets.UTF_8)
		} else {
			""
		}

		return HttpRequest(requestLine, headers, body)
	}

	private fun writeResponse(
		socket: Socket,
		code: Int,
		contentType: String,
		body: String
	) {
		val bytes = body.toByteArray(Charsets.UTF_8)
		val status = when (code) {
			200 -> "OK"
			204 -> "No Content"
			400 -> "Bad Request"
			404 -> "Not Found"
			500 -> "Internal Server Error"
			else -> "OK"
		}
		val header = buildString {
			append("HTTP/1.1 $code $status\r\n")
			append("Content-Type: $contentType\r\n")
			append("Content-Length: ${bytes.size}\r\n")
			append("Connection: close\r\n")
			append("Access-Control-Allow-Origin: *\r\n")
			append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
			append("Access-Control-Allow-Headers: Content-Type\r\n")
			append("Cache-Control: no-store\r\n")
			append("\r\n")
		}.toByteArray(Charsets.UTF_8)

		val out = socket.getOutputStream()
		out.write(header)
		if (bytes.isNotEmpty()) {
			out.write(bytes)
		}
		out.flush()
	}

	companion object {
		private const val TAG = "PlaylistUploadServer"
		const val DEFAULT_PORT = 8765
		private const val MAX_BODY_CHARS = 8 * 1024 * 1024

		private val INDEX_HTML = """
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>SmartCanales - Subir M3U</title>
<style>
body{font-family:Arial,sans-serif;background:#0a1218;color:#e8f1f4;margin:0;padding:24px}
.card{max-width:520px;margin:40px auto;background:#12202a;padding:28px;border-radius:12px;border:1px solid #2a3f4d}
h1{color:#1b9aaa;font-size:22px;margin:0 0 10px}
p{opacity:.85;line-height:1.45}
label{display:block;margin-top:16px;font-weight:700}
input[type=file],textarea{width:100%;margin-top:8px;box-sizing:border-box}
textarea{min-height:160px;background:#0a1218;color:#e8f1f4;border:1px solid #2a3f4d;border-radius:8px;padding:10px}
button{margin-top:16px;background:#1b9aaa;color:#001418;border:0;padding:14px 18px;border-radius:8px;font-weight:700;cursor:pointer;width:100%;font-size:16px}
#msg{margin-top:14px;min-height:1.2em;font-weight:700}
.ok{color:#6dd3a0}.err{color:#ff8a80}
.hint{font-size:13px;opacity:.65}
</style>
</head>
<body>
<div class="card">
<h1>SmartCanales</h1>
<p>1) Elige tu archivo <b>.m3u</b><br>2) Pulsa <b>Subir playlist</b><br>3) Mira la TV: se cargaran los canales.</p>
<label for="file">Archivo M3U</label>
<input id="file" type="file" accept=".m3u,.m3u8,text/plain,*/*">
<label for="paste">O pega aqui el contenido del M3U</label>
<textarea id="paste" placeholder="#EXTM3U&#10;#EXTINF:-1,Canal ejemplo&#10;http://..."></textarea>
<button id="btn" type="button">Subir playlist</button>
<p id="msg"></p>
<p class="hint">La app SmartCanales debe estar abierta en la TV.</p>
</div>
<script>
const file=document.getElementById('file');
const paste=document.getElementById('paste');
const btn=document.getElementById('btn');
const msg=document.getElementById('msg');
btn.onclick=async function(){
  msg.className='';
  msg.textContent='Subiendo...';
  try{
    var text=paste.value.trim();
    if(!text){
      if(!file.files.length){ throw new Error('Elige un archivo o pega el M3U'); }
      text=await file.files[0].text();
    }
    if(!text || text.length<8){ throw new Error('El contenido esta vacio'); }
    var res=await fetch('/upload',{method:'POST',headers:{'Content-Type':'text/plain; charset=utf-8'},body:text});
    var body=await res.text();
    if(!res.ok) throw new Error(body||res.statusText);
    msg.className='ok';
    msg.textContent='OK: playlist importada. Revisa la TV.';
  }catch(e){
    msg.className='err';
    msg.textContent=(e && e.message)?e.message:String(e);
  }
};
</script>
</body>
</html>
		""".trimIndent()

		private val SUCCESS_HTML = """
<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8"><title>OK</title></head>
<body style="font-family:Arial,sans-serif;background:#0a1218;color:#e8f1f4;padding:40px">
<h1 style="color:#1b9aaa">Playlist importada</h1>
<p>Vuelve a la TV: los canales deberian actualizarse solos.</p>
<p><a href="/" style="color:#1b9aaa">Subir otra</a></p>
</body></html>
		""".trimIndent()
	}
}

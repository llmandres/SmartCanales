package com.smartcanales.app.data.repository

import android.content.Context
import com.smartcanales.app.data.local.Channel
import com.smartcanales.app.data.local.ChannelDao
import com.smartcanales.app.data.playlist.M3uParser
import com.smartcanales.app.data.playlist.StreamSource
import com.smartcanales.app.data.playlist.UsbPlaylistScanner
import com.smartcanales.app.data.remote.AceStreamApi
import com.smartcanales.app.engine.AceStreamEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ChannelRepository(
	private val context: Context,
	private val channelDao: ChannelDao,
	private val aceStreamApi: AceStreamApi,
	private val aceStreamEngine: AceStreamEngine
) {

	fun observeChannels(): Flow<List<Channel>> = channelDao.observeAll()

	suspend fun getChannel(id: Long): Channel? = channelDao.getById(id)

	suspend fun importM3u(content: String): Int {
		val parsed = M3uParser.parse(content)
		if (parsed.isEmpty()) {
			throw IllegalArgumentException("No se encontraron canales en el M3U")
		}
		channelDao.replaceAll(parsed)
		return parsed.size
	}

	suspend fun ensureDefaultChannels() = withContext(Dispatchers.IO) {
		if (channelDao.count() > 0) return@withContext
		val text = context.assets.open(DEFAULT_ASSET).bufferedReader().use { it.readText() }
		val parsed = M3uParser.parse(text)
		if (parsed.isNotEmpty()) {
			channelDao.replaceAll(parsed)
		}
	}

	suspend fun importFromUsb(): String = withContext(Dispatchers.IO) {
		val playlists = UsbPlaylistScanner.findPlaylists(context)
		if (playlists.isEmpty()) {
			throw IllegalStateException(
				"No hay .m3u en el USB. Ponlo en la raiz o en /SmartCanales/"
			)
		}
		val chosen = playlists.first()
		val content = UsbPlaylistScanner.readPlaylist(chosen.file)
		val count = importM3u(content)
		"USB: ${chosen.label} ($count canales)"
	}

	suspend fun importFromUri(uri: android.net.Uri): String = withContext(Dispatchers.IO) {
		val content = context.contentResolver.openInputStream(uri)?.use { stream ->
			stream.bufferedReader(Charsets.UTF_8).readText()
		} ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")
		val count = importM3u(content)
		val name = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
		"Archivo: $name ($count canales)"
	}

	suspend fun importFromFile(file: File): Int = withContext(Dispatchers.IO) {
		importM3u(UsbPlaylistScanner.readPlaylist(file))
	}

	suspend fun resolvePlaybackUrl(channel: Channel): String {
		return when (val source = StreamSource.from(channel.streamUrl)) {
			is StreamSource.DirectHttp -> source.url
			is StreamSource.AceContentId -> {
				val started = aceStreamEngine.startEngine()
				if (!started) {
					throw IllegalStateException(
						"No se pudo iniciar AceStream. ¿Está instalado org.acestream.media?"
					)
				}
				delay(ENGINE_WARMUP_MS)
				val apiResponse = aceStreamApi.getStream(contentId = source.contentId)
				if (!apiResponse.error.isNullOrBlank()) {
					throw IllegalStateException(apiResponse.error)
				}
				val playbackUrl = apiResponse.response?.playbackUrl
				if (playbackUrl.isNullOrBlank()) {
					throw IllegalStateException("AceStream no devolvió playback_url")
				}
				playbackUrl
			}
		}
	}

	companion object {
		private const val ENGINE_WARMUP_MS = 1_500L
		private const val DEFAULT_ASSET = "default_channels.m3u"
	}
}

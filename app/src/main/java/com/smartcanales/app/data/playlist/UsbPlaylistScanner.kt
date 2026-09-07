package com.smartcanales.app.data.playlist

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import java.io.File

/**
 * Busca archivos .m3u / .m3u8 en memorias USB / almacenamiento extraíble.
 */
object UsbPlaylistScanner {

	private const val TAG = "UsbPlaylistScanner"
	private val SKIP_DIRS = setOf("emulated", "self", "oem", "tmp", "sdcard")

	data class FoundPlaylist(
		val file: File,
		val label: String
	)

	fun findPlaylists(context: Context): List<FoundPlaylist> {
		val found = linkedMapOf<String, FoundPlaylist>()

		candidateRoots(context).forEach { root ->
			if (!root.exists() || !root.canRead()) return@forEach
			try {
				scanRoot(root).forEach { file ->
					found.putIfAbsent(
						file.absolutePath,
						FoundPlaylist(
							file = file,
							label = "${root.name}/${file.name}"
						)
					)
				}
			} catch (e: Exception) {
				Log.w(TAG, "No se pudo escanear ${root.absolutePath}", e)
			}
		}

		return found.values.sortedBy { it.label.lowercase() }
	}

	fun readPlaylist(file: File): String {
		return file.readText(Charsets.UTF_8)
	}

	private fun candidateRoots(context: Context): List<File> {
		val roots = mutableListOf<File>()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			val sm = context.getSystemService(StorageManager::class.java)
			sm?.storageVolumes?.forEach { volume ->
				val dir = volume.directory ?: return@forEach
				if (volume.isRemovable || !volume.isPrimary) {
					roots += dir
				}
			}
		}

		File("/storage").listFiles()?.forEach { child ->
			if (child.isDirectory && child.name !in SKIP_DIRS) {
				roots += child
			}
		}

		File("/mnt/media_rw").listFiles()?.forEach { child ->
			if (child.isDirectory) roots += child
		}

		context.getExternalFilesDirs(null)?.forEach { dir ->
			val root = dir?.parentFile?.parentFile?.parentFile
			if (root != null) roots += root
		}

		@Suppress("DEPRECATION")
		val ext = Environment.getExternalStorageDirectory()
		if (ext != null) roots += ext

		return roots.distinctBy { it.absolutePath }
	}

	private fun scanRoot(root: File): List<File> {
		val results = mutableListOf<File>()

		// Preferencia: carpeta SmartCanales en la raíz del USB
		val preferred = File(root, "SmartCanales")
		if (preferred.isDirectory) {
			preferred.listFiles()?.forEach { addIfPlaylist(it, results) }
		}

		// Raíz del volumen (pocos niveles)
		root.listFiles()?.forEach { child ->
			addIfPlaylist(child, results)
			if (child.isDirectory && child.name !in SKIP_DIRS && !child.name.startsWith('.')) {
				child.listFiles()?.forEach { nested ->
					addIfPlaylist(nested, results)
				}
			}
		}

		return results
	}

	private fun addIfPlaylist(file: File, out: MutableList<File>) {
		if (!file.isFile || !file.canRead()) return
		val ext = file.extension.lowercase()
		if (ext == "m3u" || ext == "m3u8") {
			out += file
		}
	}
}

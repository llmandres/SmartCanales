package com.smartcanales.app.data.playlist

import com.smartcanales.app.data.local.Channel

/**
 * Parser M3U / M3U8 estilo IPTV (#EXTINF + URL).
 * Compatible con playlists típicas (group-title, tvg-name, acestream://, http/https).
 */
object M3uParser {

	private val EXTINF = Regex(
		"""#EXTINF:[^,]*,(.*)""",
		RegexOption.IGNORE_CASE
	)
	private val GROUP_TITLE = Regex(
		"""group-title="([^"]*)"""",
		RegexOption.IGNORE_CASE
	)
	private val TVG_NAME = Regex(
		"""tvg-name="([^"]*)"""",
		RegexOption.IGNORE_CASE
	)

	fun parse(content: String): List<Channel> {
		val channels = mutableListOf<Channel>()
		var pendingName: String? = null
		var pendingGroup: String? = null

		content.lineSequence().forEach { raw ->
			val line = raw.trim()
			if (line.isEmpty() || line.startsWith("#EXTM3U", ignoreCase = true)) {
				return@forEach
			}

			if (line.startsWith("#EXTINF", ignoreCase = true)) {
				pendingGroup = GROUP_TITLE.find(line)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
				val tvgName = TVG_NAME.find(line)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
				val displayName = EXTINF.find(line)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
				pendingName = displayName ?: tvgName ?: "Canal"
				return@forEach
			}

			if (line.startsWith("#")) {
				return@forEach
			}

			val url = line
			val name = pendingName ?: guessNameFromUrl(url)
			channels += Channel(
				name = name,
				streamUrl = url,
				groupTitle = pendingGroup
			)
			pendingName = null
			pendingGroup = null
		}

		return channels
	}

	private fun guessNameFromUrl(url: String): String {
		val hash = StreamSource.extractAceHash(url)
		if (hash != null) return "AceStream ${hash.take(8)}"
		return url.substringAfterLast('/').substringBefore('?').ifBlank { "Canal" }
	}
}

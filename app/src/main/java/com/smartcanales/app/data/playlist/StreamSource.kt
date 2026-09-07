package com.smartcanales.app.data.playlist

/**
 * Clasifica la URL de un ítem M3U para decidir cómo reproducirla.
 */
sealed interface StreamSource {
	data class DirectHttp(val url: String) : StreamSource
	data class AceContentId(val contentId: String) : StreamSource

	companion object {
		private val ACE_HASH = Regex("""^[a-fA-F0-9]{40}$""")
		private val ACE_SCHEME = Regex("""^acestream://([a-fA-F0-9]{40})""", RegexOption.IGNORE_CASE)
		private val ACE_ID_QUERY = Regex("""[?&](?:id|infohash)=([a-fA-F0-9]{40})""", RegexOption.IGNORE_CASE)

		fun from(streamUrl: String): StreamSource {
			val trimmed = streamUrl.trim()

			ACE_SCHEME.find(trimmed)?.groupValues?.getOrNull(1)?.let {
				return AceContentId(it.lowercase())
			}

			if (ACE_HASH.matches(trimmed)) {
				return AceContentId(trimmed.lowercase())
			}

			ACE_ID_QUERY.find(trimmed)?.groupValues?.getOrNull(1)?.let {
				// URL del motor local: mejor resolver sesión JSON y usar playback_url
				return AceContentId(it.lowercase())
			}

			return DirectHttp(trimmed)
		}

		fun extractAceHash(streamUrl: String): String? {
			return when (val source = from(streamUrl)) {
				is AceContentId -> source.contentId
				is DirectHttp -> null
			}
		}
	}
}

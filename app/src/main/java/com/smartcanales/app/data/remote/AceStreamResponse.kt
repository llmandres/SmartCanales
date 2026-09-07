package com.smartcanales.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * AceStream Engine HTTP API response for:
 * GET /ace/getstream?id={hash}&format=json
 *
 * @see <a href="https://wiki.acestream.media/Engine_HTTP_API">Engine HTTP API</a>
 */
data class AceStreamApiResponse(
	@SerializedName("response")
	val response: AceStreamStreamResponse?,
	@SerializedName("error")
	val error: String?
)

data class AceStreamStreamResponse(
	@SerializedName("playback_url")
	val playbackUrl: String?,
	@SerializedName("stat_url")
	val statUrl: String?,
	@SerializedName("command_url")
	val commandUrl: String?,
	@SerializedName("event_url")
	val eventUrl: String?,
	@SerializedName("infohash")
	val infohash: String?,
	@SerializedName("playback_session_id")
	val playbackSessionId: String?,
	@SerializedName("is_live")
	val isLive: Int?
)

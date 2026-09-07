package com.smartcanales.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AceStreamApi {

	@GET("ace/getstream")
	suspend fun getStream(
		@Query("id") contentId: String,
		@Query("format") format: String = "json"
	): AceStreamApiResponse
}

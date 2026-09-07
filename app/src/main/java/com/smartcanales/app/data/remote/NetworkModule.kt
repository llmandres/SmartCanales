package com.smartcanales.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

	const val ACESTREAM_BASE_URL = "http://127.0.0.1:6878/"

	fun createAceStreamApi(
		baseUrl: String = ACESTREAM_BASE_URL
	): AceStreamApi {
		val logging = HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BASIC
		}

		val client = OkHttpClient.Builder()
			.connectTimeout(15, TimeUnit.SECONDS)
			.readTimeout(45, TimeUnit.SECONDS)
			.writeTimeout(15, TimeUnit.SECONDS)
			.addInterceptor(logging)
			.build()

		return Retrofit.Builder()
			.baseUrl(baseUrl)
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(AceStreamApi::class.java)
	}
}

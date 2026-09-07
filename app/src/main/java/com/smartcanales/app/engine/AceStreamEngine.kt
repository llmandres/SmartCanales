package com.smartcanales.app.engine

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Utility to start the AceStream engine in the background via explicit Intent.
 *
 * Action: org.acestream.engine.START_ENGINE
 * Package: org.acestream.media
 */
class AceStreamEngine(
	private val context: Context
) {

	fun startEngine(): Boolean {
		return try {
			val intent = Intent(ACTION_START_ENGINE).apply {
				setPackage(PACKAGE_ACESTREAM_MEDIA)
			}
			context.startService(intent)
			true
		} catch (e: Exception) {
			Log.e(TAG, "Failed to start AceStream engine", e)
			false
		}
	}

	companion object {
		private const val TAG = "AceStreamEngine"
		const val ACTION_START_ENGINE = "org.acestream.engine.START_ENGINE"
		const val PACKAGE_ACESTREAM_MEDIA = "org.acestream.media"
	}
}

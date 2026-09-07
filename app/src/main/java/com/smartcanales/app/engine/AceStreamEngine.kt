package com.smartcanales.app.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

/**
 * Arranca el motor AceStream en Android / Android TV y espera a que
 * la API HTTP local responda en 127.0.0.1:6878.
 *
 * Paquetes oficiales:
 * - org.acestream.media / org.acestream.media.atv
 * - org.acestream.core / org.acestream.core.atv
 */
class AceStreamEngine(
	private val context: Context
) {

	fun installedPackages(): List<String> {
		return CANDIDATE_PACKAGES.filter { isInstalled(it) }
	}

	fun isAnyInstalled(): Boolean = installedPackages().isNotEmpty()

	fun startEngine(): Boolean {
		val installed = installedPackages()
		if (installed.isEmpty()) {
			Log.e(TAG, "AceStream no instalado")
			return false
		}

		var started = false
		for (pkg in installed) {
			if (startServiceForPackage(pkg)) {
				started = true
			}
			launchApp(pkg)
		}
		return started || installed.isNotEmpty()
	}

	/**
	 * Polls the local HTTP API until ready or timeout.
	 */
	suspend fun waitUntilReady(
		timeoutMs: Long = READY_TIMEOUT_MS,
		pollMs: Long = READY_POLL_MS
	): Boolean {
		val deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			if (isHttpApiReady()) return true
			delay(pollMs)
		}
		return isHttpApiReady()
	}

	fun isHttpApiReady(): Boolean {
		return try {
			val url = URL(VERSION_URL)
			val conn = (url.openConnection() as HttpURLConnection).apply {
				connectTimeout = 1_500
				readTimeout = 1_500
				requestMethod = "GET"
			}
			conn.inputStream.use { it.readBytes() }
			val code = conn.responseCode
			conn.disconnect()
			code in 200..299
		} catch (e: Exception) {
			Log.d(TAG, "API aún no lista: ${e.message}")
			false
		}
	}

	private fun isInstalled(packageName: String): Boolean {
		return try {
			if (Build.VERSION.SDK_INT >= 33) {
				context.packageManager.getPackageInfo(
					packageName,
					PackageManager.PackageInfoFlags.of(0)
				)
			} else {
				@Suppress("DEPRECATION")
				context.packageManager.getPackageInfo(packageName, 0)
			}
			true
		} catch (_: Exception) {
			false
		}
	}

	private fun startServiceForPackage(packageName: String): Boolean {
		return try {
			val intent = Intent(ACTION_START_ENGINE).apply {
				setPackage(packageName)
			}
			context.startService(intent)
			true
		} catch (e: Exception) {
			Log.w(TAG, "startService falló para $packageName", e)
			false
		}
	}

	private fun launchApp(packageName: String) {
		try {
			val launch = context.packageManager.getLaunchIntentForPackage(packageName)
			if (launch != null) {
				launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(launch)
			}
		} catch (e: Exception) {
			Log.w(TAG, "No se pudo abrir UI de $packageName", e)
		}
	}

	companion object {
		private const val TAG = "AceStreamEngine"
		const val ACTION_START_ENGINE = "org.acestream.engine.START_ENGINE"
		private const val VERSION_URL =
			"http://127.0.0.1:6878/webui/api/service?method=get_version"
		private const val READY_TIMEOUT_MS = 20_000L
		private const val READY_POLL_MS = 700L

		val CANDIDATE_PACKAGES = listOf(
			"org.acestream.media.atv",
			"org.acestream.core.atv",
			"org.acestream.media",
			"org.acestream.core"
		)
	}
}

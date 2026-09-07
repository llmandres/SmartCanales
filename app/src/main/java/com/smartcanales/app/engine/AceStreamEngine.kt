package com.smartcanales.app.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

/**
 * Arranca el motor AceStream y espera la API HTTP en 127.0.0.1:6878.
 */
class AceStreamEngine(
	private val context: Context
) {

	fun installedPackages(): List<String> {
		val found = linkedSetOf<String>()

		CANDIDATE_PACKAGES.forEach { pkg ->
			if (isInstalled(pkg)) found += pkg
		}

		// Escaneo amplio (QUERY_ALL_PACKAGES / visibilidad)
		found += discoverAceStreamPackages()

		// Quien responde al Intent START_ENGINE
		found += resolvePackagesForStartEngine()

		// Quien maneja acestream://
		found += resolvePackagesForAceStreamScheme()

		return found.toList()
	}

	fun isAnyInstalled(): Boolean = installedPackages().isNotEmpty()

	fun startEngine(): Boolean {
		val installed = installedPackages()
		if (installed.isEmpty()) {
			Log.e(TAG, "AceStream no instalado / no visible")
			return false
		}

		// Solo arrancar el servicio/motor. No abrir la UI en cada play
		// (si no, AceStream roba el foco y puede pedir login a mitad).
		var started = startServiceGlobal()
		installed.forEach { pkg ->
			if (startServiceForPackage(pkg)) {
				started = true
			}
		}
		return started
	}

	/** Abrir la app AceStream una vez (para login / primer arranque). */
	fun openAceStreamUi(): Boolean {
		val installed = installedPackages()
		if (installed.isEmpty()) return false
		installed.forEach { launchApp(it) }
		return true
	}

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

	private fun discoverAceStreamPackages(): List<String> {
		return try {
			@Suppress("DEPRECATION")
			val packages = context.packageManager.getInstalledPackages(0)
			packages.mapNotNull { info ->
				val name = info.packageName ?: return@mapNotNull null
				if (name.contains("acestream", ignoreCase = true)) name else null
			}
		} catch (e: Exception) {
			Log.w(TAG, "No se pudo listar paquetes", e)
			emptyList()
		}
	}

	private fun resolvePackagesForStartEngine(): List<String> {
		return try {
			val intent = Intent(ACTION_START_ENGINE)
			@Suppress("DEPRECATION")
			val services = context.packageManager.queryIntentServices(intent, 0)
			services.mapNotNull { it.serviceInfo?.packageName }
		} catch (e: Exception) {
			Log.w(TAG, "queryIntentServices falló", e)
			emptyList()
		}
	}

	private fun resolvePackagesForAceStreamScheme(): List<String> {
		return try {
			val intent = Intent(Intent.ACTION_VIEW, Uri.parse("acestream://test"))
			@Suppress("DEPRECATION")
			val activities = if (Build.VERSION.SDK_INT >= 33) {
				context.packageManager.queryIntentActivities(
					intent,
					PackageManager.ResolveInfoFlags.of(0)
				)
			} else {
				context.packageManager.queryIntentActivities(intent, 0)
			}
			activities.mapNotNull { it.activityInfo?.packageName }
		} catch (e: Exception) {
			Log.w(TAG, "queryIntentActivities falló", e)
			emptyList()
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

	private fun startServiceGlobal(): Boolean {
		return try {
			context.startService(Intent(ACTION_START_ENGINE))
			true
		} catch (e: Exception) {
			Log.w(TAG, "startService global falló", e)
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
		private const val READY_TIMEOUT_MS = 25_000L
		private const val READY_POLL_MS = 700L

		val CANDIDATE_PACKAGES = listOf(
			"org.acestream.media.atv",
			"org.acestream.core.atv",
			"org.acestream.media",
			"org.acestream.core"
		)
	}
}

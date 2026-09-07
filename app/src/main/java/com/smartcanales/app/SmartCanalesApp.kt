package com.smartcanales.app

import android.app.Application
import com.smartcanales.app.data.local.AppDatabase
import com.smartcanales.app.data.remote.NetworkModule
import com.smartcanales.app.data.repository.ChannelRepository
import com.smartcanales.app.engine.AceStreamEngine
import com.smartcanales.app.server.PlaylistUploadServer
import com.smartcanales.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmartCanalesApp : Application() {

	private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	lateinit var repository: ChannelRepository
		private set

	lateinit var aceStreamEngine: AceStreamEngine
		private set

	private var uploadServer: PlaylistUploadServer? = null

	private val _uploadEndpoint = MutableStateFlow<String?>(null)
	val uploadEndpoint: StateFlow<String?> = _uploadEndpoint.asStateFlow()

	private val _lastImportMessage = MutableStateFlow<String?>(null)
	val lastImportMessage: StateFlow<String?> = _lastImportMessage.asStateFlow()

	override fun onCreate() {
		super.onCreate()

		val database = AppDatabase.getInstance(this)
		aceStreamEngine = AceStreamEngine(this)
		repository = ChannelRepository(
			context = this,
			channelDao = database.channelDao(),
			aceStreamApi = NetworkModule.createAceStreamApi(),
			aceStreamEngine = aceStreamEngine
		)

		appScope.launch {
			runCatching { repository.ensureDefaultChannels() }
		}

		startUploadServer()
	}

	fun setImportMessage(message: String?) {
		_lastImportMessage.value = message
	}

	private fun startUploadServer() {
		val server = PlaylistUploadServer { content ->
			val count = runBlocking { repository.importM3u(content) }
			_lastImportMessage.value = "Importados $count canales desde M3U"
		}
		uploadServer = server
		server.start()

		val ip = NetworkUtils.preferredLocalIp() ?: "IP_DE_LA_TV"
		_uploadEndpoint.value = "http://$ip:${PlaylistUploadServer.DEFAULT_PORT}"
	}

	fun clearImportMessage() {
		_lastImportMessage.value = null
	}

	override fun onTerminate() {
		uploadServer?.stop()
		super.onTerminate()
	}
}

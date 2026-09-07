package com.smartcanales.app.ui.channels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartcanales.app.SmartCanalesApp
import com.smartcanales.app.data.local.Channel
import com.smartcanales.app.data.playlist.StreamSource
import com.smartcanales.app.ui.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChannelViewModel(
	application: Application
) : AndroidViewModel(application) {

	private val app = application as SmartCanalesApp
	private val repository = app.repository

	val channels: StateFlow<List<Channel>> = repository
		.observeChannels()
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = emptyList()
		)

	val uploadEndpoint: StateFlow<String?> = app.uploadEndpoint
	val lastImportMessage: StateFlow<String?> = app.lastImportMessage

	private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
	val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

	private val _selectedChannel = MutableStateFlow<Channel?>(null)
	val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

	private val _usbBusy = MutableStateFlow(false)
	val usbBusy: StateFlow<Boolean> = _usbBusy.asStateFlow()

	fun selectChannel(channel: Channel) {
		_selectedChannel.value = channel
		viewModelScope.launch {
			_playbackState.value = PlaybackState.Loading
			try {
				val playbackUrl = repository.resolvePlaybackUrl(channel)
				_playbackState.value = PlaybackState.Playing(playbackUrl)
			} catch (e: Exception) {
				_playbackState.value = PlaybackState.Error(
					e.message ?: "Error al obtener el flujo"
				)
			}
		}
	}

	fun importFromUsb() {
		if (_usbBusy.value) return
		viewModelScope.launch {
			_usbBusy.value = true
			app.setImportMessage("Buscando .m3u en el USB…")
			try {
				val message = repository.importFromUsb()
				app.setImportMessage(message)
			} catch (e: Exception) {
				app.setImportMessage(e.message ?: "Error al leer el USB")
			} finally {
				_usbBusy.value = false
			}
		}
	}

	fun importFromUri(uri: android.net.Uri) {
		if (_usbBusy.value) return
		viewModelScope.launch {
			_usbBusy.value = true
			app.setImportMessage("Importando archivo…")
			try {
				val message = repository.importFromUri(uri)
				app.setImportMessage(message)
			} catch (e: Exception) {
				app.setImportMessage(e.message ?: "Error al importar el archivo")
			} finally {
				_usbBusy.value = false
			}
		}
	}

	fun resetPlayback() {
		_playbackState.value = PlaybackState.Idle
		_selectedChannel.value = null
	}

	fun clearImportMessage() {
		app.clearImportMessage()
	}

	fun streamHint(channel: Channel): String {
		return when (StreamSource.from(channel.streamUrl)) {
			is StreamSource.AceContentId -> "AceStream"
			is StreamSource.DirectHttp -> channel.groupTitle ?: "HTTP"
		}
	}
}

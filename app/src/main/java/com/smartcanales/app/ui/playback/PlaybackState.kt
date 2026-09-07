package com.smartcanales.app.ui.playback

sealed interface PlaybackState {
	data object Idle : PlaybackState
	data object Loading : PlaybackState
	data class Playing(val url: String) : PlaybackState
	data class Error(val message: String) : PlaybackState
}

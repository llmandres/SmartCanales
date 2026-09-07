package com.smartcanales.app.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.smartcanales.app.ui.channels.ChannelViewModel
import com.smartcanales.app.ui.playback.PlaybackState

@OptIn(ExperimentalTvMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
	viewModel: ChannelViewModel,
	onBack: () -> Unit
) {
	val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
	val selected by viewModel.selectedChannel.collectAsStateWithLifecycle()
	val context = LocalContext.current

	val exoPlayer = remember {
		ExoPlayer.Builder(context).build().apply {
			playWhenReady = true
			repeatMode = Player.REPEAT_MODE_OFF
		}
	}

	BackHandler {
		exoPlayer.stop()
		viewModel.resetPlayback()
		onBack()
	}

	DisposableEffect(Unit) {
		onDispose {
			exoPlayer.release()
		}
	}

	LaunchedEffect(playbackState) {
		when (val state = playbackState) {
			is PlaybackState.Playing -> {
				exoPlayer.setMediaItem(MediaItem.fromUri(state.url))
				exoPlayer.prepare()
				exoPlayer.play()
			}
			is PlaybackState.Idle, is PlaybackState.Error -> {
				exoPlayer.stop()
				exoPlayer.clearMediaItems()
			}
			is PlaybackState.Loading -> Unit
		}
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black)
	) {
		when (val state = playbackState) {
			is PlaybackState.Playing -> {
				AndroidView(
					factory = { ctx ->
						PlayerView(ctx).apply {
							player = exoPlayer
							useController = true
							resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
							layoutParams = FrameLayout.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.MATCH_PARENT
							)
						}
					},
					modifier = Modifier.fillMaxSize(),
					update = { view -> view.player = exoPlayer }
				)
			}
			is PlaybackState.Loading -> {
				StatusOverlay(
					title = selected?.name ?: "Canal",
					message = "Conectando con AceStream…"
				)
			}
			is PlaybackState.Error -> {
				StatusOverlay(
					title = "Error",
					message = state.message
				)
			}
			is PlaybackState.Idle -> {
				StatusOverlay(
					title = "Reproductor",
					message = "Sin reproducción activa"
				)
			}
		}

		Text(
			text = selected?.name.orEmpty(),
			style = MaterialTheme.typography.titleMedium,
			color = Color.White.copy(alpha = 0.85f),
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(32.dp)
		)
	}
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusOverlay(
	title: String,
	message: String
) {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				text = title,
				style = MaterialTheme.typography.headlineSmall,
				color = Color.White
			)
			Text(
				text = message,
				style = MaterialTheme.typography.bodyLarge,
				color = Color.White.copy(alpha = 0.7f),
				modifier = Modifier.padding(top = 12.dp)
			)
		}
	}
}

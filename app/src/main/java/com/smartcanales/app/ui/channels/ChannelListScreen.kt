package com.smartcanales.app.ui.channels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.smartcanales.app.data.local.Channel
import com.smartcanales.app.ui.playback.PlaybackState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelListScreen(
	viewModel: ChannelViewModel,
	onNavigateToPlayer: () -> Unit
) {
	val channels by viewModel.channels.collectAsStateWithLifecycle()
	val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
	val selected by viewModel.selectedChannel.collectAsStateWithLifecycle()
	val uploadEndpoint by viewModel.uploadEndpoint.collectAsStateWithLifecycle()
	val importMessage by viewModel.lastImportMessage.collectAsStateWithLifecycle()
	val usbBusy by viewModel.usbBusy.collectAsStateWithLifecycle()
	val firstItemFocus = FocusRequester()
	val browseFocus = FocusRequester()

	val openDocument = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri ->
		if (uri != null) {
			viewModel.importFromUri(uri)
		}
	}

	LaunchedEffect(playbackState) {
		if (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Loading) {
			onNavigateToPlayer()
		}
	}

	LaunchedEffect(channels) {
		if (channels.isNotEmpty()) {
			firstItemFocus.requestFocus()
		} else {
			browseFocus.requestFocus()
		}
	}

	Row(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 48.dp, vertical = 32.dp)
	) {
		Column(
			modifier = Modifier
				.width(400.dp)
				.fillMaxHeight()
		) {
			Text(
				text = "SmartCanales",
				style = MaterialTheme.typography.headlineMedium,
				color = MaterialTheme.colorScheme.primary
			)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = "Web: ${uploadEndpoint ?: "..."}",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
			)
			Spacer(modifier = Modifier.height(12.dp))

			Card(
				onClick = {
					openDocument.launch(
						arrayOf(
							"audio/x-mpegurl",
							"application/vnd.apple.mpegurl",
							"application/x-mpegurl",
							"text/plain",
							"*/*"
						)
					)
				},
				modifier = Modifier
					.fillMaxWidth()
					.focusRequester(browseFocus),
				colors = CardDefaults.colors(
					containerColor = MaterialTheme.colorScheme.surface,
					focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
				),
				border = CardDefaults.border(
					focusedBorder = Border(
						border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
					)
				),
				scale = CardDefaults.scale(focusedScale = 1.03f)
			) {
				Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
					Text(
						text = if (usbBusy) "Importando..." else "Elegir M3U (explorador)",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = "Abre el file browser del aparato / USB / descargas",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
					)
				}
			}

			Spacer(modifier = Modifier.height(10.dp))

			Card(
				onClick = { viewModel.importFromUsb() },
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.colors(
					containerColor = MaterialTheme.colorScheme.surface,
					focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
				),
				border = CardDefaults.border(
					focusedBorder = Border(
						border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
					)
				),
				scale = CardDefaults.scale(focusedScale = 1.03f)
			) {
				Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
					Text(
						text = if (usbBusy) "Leyendo USB..." else "Buscar M3U en USB (auto)",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = "Escanea raiz del pendrive o /SmartCanales/",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
					)
				}
			}

			Spacer(modifier = Modifier.height(10.dp))

			Card(
				onClick = { viewModel.openAceStream() },
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.colors(
					containerColor = MaterialTheme.colorScheme.surface,
					focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
				),
				border = CardDefaults.border(
					focusedBorder = Border(
						border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
					)
				),
				scale = CardDefaults.scale(focusedScale = 1.03f)
			) {
				Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
					Text(
						text = "Abrir AceStream (login)",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = "Inicia sesion una vez y vuelve a SmartCanales",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
					)
				}
			}

			if (!importMessage.isNullOrBlank()) {
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = importMessage.orEmpty(),
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.secondary
				)
			}
			Spacer(modifier = Modifier.height(16.dp))

			if (channels.isEmpty()) {
				Text(
					text = "Sin canales. Usa el explorador, USB o la URL web.",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface
				)
			} else {
				Text(
					text = "${channels.size} canales - D-Pad + OK",
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
				)
				Spacer(modifier = Modifier.height(12.dp))
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(vertical = 8.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					itemsIndexed(
						items = channels,
						key = { _, channel -> channel.id }
					) { index, channel ->
						ChannelRowItem(
							channel = channel,
							subtitle = viewModel.streamHint(channel),
							isSelected = selected?.id == channel.id,
							modifier = if (index == 0) {
								Modifier.focusRequester(firstItemFocus)
							} else {
								Modifier
							},
							onClick = { viewModel.selectChannel(channel) }
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.width(48.dp))

		PreviewPanel(
			modifier = Modifier
				.weight(1f)
				.fillMaxHeight(),
			selected = selected,
			playbackState = playbackState,
			uploadEndpoint = uploadEndpoint
		)
	}
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelRowItem(
	channel: Channel,
	subtitle: String,
	isSelected: Boolean,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Card(
		onClick = onClick,
		modifier = modifier.fillMaxWidth(),
		colors = CardDefaults.colors(
			containerColor = MaterialTheme.colorScheme.surface,
			focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
			pressedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
		),
		border = CardDefaults.border(
			focusedBorder = Border(
				border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
			),
			border = Border(
				border = BorderStroke(
					1.dp,
					if (isSelected) MaterialTheme.colorScheme.primary
					else MaterialTheme.colorScheme.border
				)
			)
		),
		glow = CardDefaults.glow(
			focusedGlow = Glow(
				elevationColor = MaterialTheme.colorScheme.primary,
				elevation = 8.dp
			)
		),
		scale = CardDefaults.scale(focusedScale = 1.04f)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 20.dp, vertical = 16.dp)
		) {
			Text(
				text = channel.name,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = subtitle,
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PreviewPanel(
	modifier: Modifier = Modifier,
	selected: Channel?,
	playbackState: PlaybackState,
	uploadEndpoint: String?
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				text = selected?.name ?: "Ningun canal seleccionado",
				style = MaterialTheme.typography.headlineSmall,
				color = MaterialTheme.colorScheme.onBackground
			)
			Spacer(modifier = Modifier.height(12.dp))
			Text(
				text = when (playbackState) {
					is PlaybackState.Idle -> "Pulsa OK para reproducir"
					is PlaybackState.Loading -> "Preparando reproduccion..."
					is PlaybackState.Playing -> "Reproduciendo"
					is PlaybackState.Error -> playbackState.message
				},
				style = MaterialTheme.typography.bodyLarge,
				color = if (playbackState is PlaybackState.Error) {
					MaterialTheme.colorScheme.error
				} else {
					MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
				}
			)
			if (selected == null) {
				Spacer(modifier = Modifier.height(24.dp))
				Text(
					text = "Explorador, USB o web ($uploadEndpoint)",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
				)
			}
		}
	}
}
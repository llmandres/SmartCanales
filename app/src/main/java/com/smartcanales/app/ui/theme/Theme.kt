package com.smartcanales.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val TvDarkColors = darkColorScheme(
	primary = Color(0xFF1B9AAA),
	onPrimary = Color(0xFF001418),
	secondary = Color(0xFF7EB8C4),
	onSecondary = Color(0xFF003039),
	background = Color(0xFF0A1218),
	onBackground = Color(0xFFE8F1F4),
	surface = Color(0xFF12202A),
	onSurface = Color(0xFFE8F1F4),
	border = Color(0xFF2A3F4D),
	borderVariant = Color(0xFF1B9AAA)
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SmartCanalesTheme(
	content: @Composable () -> Unit
) {
	MaterialTheme(
		colorScheme = TvDarkColors,
		content = content
	)
}

package com.smartcanales.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.smartcanales.app.ui.channels.ChannelListScreen
import com.smartcanales.app.ui.channels.ChannelViewModel
import com.smartcanales.app.ui.navigation.Routes
import com.smartcanales.app.ui.player.PlayerScreen
import com.smartcanales.app.ui.theme.SmartCanalesTheme

class MainActivity : ComponentActivity() {

	private val channelViewModel: ChannelViewModel by viewModels()

	private val permissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requestReadStorageIfNeeded()
		enableEdgeToEdge()
		setContent {
			SmartCanalesTheme {
				SmartCanalesAppRoot(viewModel = channelViewModel)
			}
		}
	}

	private fun requestReadStorageIfNeeded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return
		}
		val permission = Manifest.permission.READ_EXTERNAL_STORAGE
		if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
			permissionLauncher.launch(arrayOf(permission))
		}
	}
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SmartCanalesAppRoot(
	viewModel: ChannelViewModel
) {
	val navController = rememberNavController()

	Surface(modifier = Modifier.fillMaxSize()) {
		NavHost(
			navController = navController,
			startDestination = Routes.CHANNELS
		) {
			composable(Routes.CHANNELS) {
				ChannelListScreen(
					viewModel = viewModel,
					onNavigateToPlayer = {
						navController.navigate(Routes.PLAYER) {
							launchSingleTop = true
						}
					}
				)
			}
			composable(Routes.PLAYER) {
				PlayerScreen(
					viewModel = viewModel,
					onBack = {
						navController.popBackStack(Routes.CHANNELS, inclusive = false)
					}
				)
			}
		}
	}
}
package io.github.haratak.foldusage.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

@Composable
fun FoldUsageNav(viewModel: UsageViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshStatus() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbar.showSnackbar(message)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    state = state,
                    onRange = viewModel::setRange,
                    onOpenSetup = { nav.navigate("setup") },
                    onExport = viewModel::shareCsv,
                    onGrantUsage = { openUsageAccessSettings(context) },
                )
            }
            composable("setup") {
                LaunchedEffect(Unit) {
                    while (true) {
                        viewModel.refreshStatus()
                        delay(2_000)
                    }
                }
                SetupScreen(
                    state = state,
                    onBack = {
                        viewModel.refresh()
                        nav.popBackStack()
                    },
                    onGrantUsage = { openUsageAccessSettings(context) },
                    onGrantNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.refreshStatus()
                        }
                    },
                    onBattery = { openBatteryExemption(context) },
                    onToggleMonitoring = viewModel::setMonitoring,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

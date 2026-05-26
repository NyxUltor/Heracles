package com.heracles.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeraclesApp(viewModel: AppViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.lastSaveMessage) {
        val message = viewModel.lastSaveMessage
        if (message.isNotBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSaveMessage()
        }
    }

    BackHandler(enabled = viewModel.canGoBackToLogger()) {
        viewModel.navigateBack()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !viewModel.scrubberGestureActive,
        drawerContent = {
            ModalDrawerSheet {
                Text("Heracles", modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                DrawerItem("Logger") {
                    scope.launch {
                        drawerState.close()
                        viewModel.switchScreen("Logger")
                    }
                }
                DrawerItem("Sessions") {
                    scope.launch {
                        drawerState.close()
                        viewModel.switchScreen("Sessions")
                    }
                }
                DrawerItem("Tracker") {
                    scope.launch {
                        drawerState.close()
                        viewModel.switchScreen("Tracker")
                    }
                }
                DrawerItem("Settings") {
                    scope.launch {
                        drawerState.close()
                        viewModel.switchScreen("Settings")
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Heracles") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startNewSession() }) {
                            Icon(Icons.Default.Add, contentDescription = "New session")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (viewModel.currentScreen) {
                    "Sessions" -> HeraclesSessionsScreen(viewModel)
                    "Tracker" -> HeraclesTrackerScreen(viewModel)
                    "Settings" -> HeraclesSettingsScreen(viewModel)
                    else -> {
                        LaunchedEffect(Unit) {
                            viewModel.startStartupHydration()
                        }
                        HeraclesLoggerScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(label)
    }
}


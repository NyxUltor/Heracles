package com.heracles.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.SystemUiMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeraclesApp(viewModel: AppViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var setupMode by remember { mutableStateOf(SystemUiMode.AUTO) }
    var setupModId by remember { mutableStateOf(viewModel.settings.currentModId) }

    LaunchedEffect(viewModel.lastSaveMessage) {
        val message = viewModel.lastSaveMessage
        if (message.isNotBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSaveMessage()
        }
    }

        LaunchedEffect(viewModel.lastPrebuiltMessage) {
            val message = viewModel.lastPrebuiltMessage
            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
                viewModel.consumePrebuiltMessage()
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
                DrawerItem("Theme") {
                    scope.launch {
                        drawerState.close()
                        viewModel.switchScreen("Theme")
                    }
                }
                DrawerItem("Pre-built sessions") {
                    scope.launch {
                        drawerState.close()
                        viewModel.switchScreen("PrebuiltSessions")
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (viewModel.currentScreen) {
                        "Sessions" -> HeraclesSessionsScreen(viewModel)
                        "Tracker" -> HeraclesTrackerScreen(viewModel)
                        "Theme" -> HeraclesThemeScreen(viewModel)
                        "PrebuiltSessions" -> PrebuiltSessionsScreen(viewModel)
                        "ThemeEditor" -> ThemeEditorScreen(viewModel)
                        "Settings" -> HeraclesSettingsScreen(viewModel)
                        else -> {
                            LaunchedEffect(Unit) {
                                viewModel.startStartupHydration()
                            }
                            HeraclesLoggerScreen(viewModel)
                        }
                    }
                }

                if (!viewModel.settings.quickSetupCompleted) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Quick Setup") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Choose initial UI mode and mod pack.")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = setupMode.name,
                                        onValueChange = {},
                                        label = { Text("Mode") },
                                        readOnly = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(onClick = { setupMode = SystemUiMode.LIGHT }) { Text("Light") }
                                    Button(onClick = { setupMode = SystemUiMode.DARK }) { Text("Dark") }
                                    Button(onClick = { setupMode = SystemUiMode.AUTO }) { Text("Auto") }
                                }
                                Text("Mod pack")
                                viewModel.themeMods.forEach { mod ->
                                    Button(onClick = { setupModId = mod.id }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (setupModId == mod.id) "${mod.name} ✓" else mod.name)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val modId = setupModId.ifBlank { viewModel.themeMods.firstOrNull()?.id ?: "bare_metal" }
                                viewModel.completeQuickSetup(setupMode, modId)
                            }) {
                                Text("Start")
                            }
                        }
                    )
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


package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Heracles", modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                DrawerItem("Logger") {
                    viewModel.switchScreen("Logger")
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Sessions") {
                    viewModel.switchScreen("Sessions")
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Settings") {
                    viewModel.switchScreen("Settings")
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Heracles") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                    "Sessions" -> SessionsScreen(viewModel)
                    "Settings" -> SettingsScreen(viewModel)
                    else -> LoggerScreen(viewModel)
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

@Composable
private fun LoggerScreen(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = viewModel.workoutDuration,
            onValueChange = { viewModel.updateWorkoutDuration(it) },
            label = { Text("Duration (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            items(viewModel.exercises, key = { it.id }) { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exercise.name,
                            onValueChange = { viewModel.updateExerciseName(exercise.id, it) },
                            label = { Text("Exercise name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        exercise.sets.forEachIndexed { index, set ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = set.reps,
                                    onValueChange = { viewModel.updateSetReps(exercise.id, index, it) },
                                    label = { Text("Reps") },
                                    modifier = Modifier.fillMaxWidth(0.5f)
                                )
                                OutlinedTextField(
                                    value = set.weight,
                                    onValueChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                                    label = { Text("Weight") },
                                    modifier = Modifier.fillMaxWidth(0.5f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.addSet(exercise.id) }) { Text("Add set") }
                            Button(onClick = { viewModel.removeExercise(exercise.id) }) { Text("Remove") }
                        }
                    }
                }
            }
        }

        Text("Session volume: ${viewModel.sessionVolume()}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.addExercise() }) { Text("Add exercise") }
            Button(onClick = { viewModel.saveSession() }) { Text("Save workout") }
        }
    }
}

@Composable
private fun SessionsScreen(viewModel: AppViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        items(viewModel.sessions, key = { it.id }) { session ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Saved: ${session.savedAt}")
                    Text("Volume: ${session.volume}")
                    Text("Exercises: ${session.exercises.size}")
                    Button(onClick = { viewModel.restoreFromSession(session) }) {
                        Text("Load")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel) {
    OutlinedTextField(
        value = viewModel.settings.units,
        onValueChange = {
            viewModel.updateSettings(viewModel.settings.copy(units = it))
        },
        label = { Text("Units") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = viewModel.settings.defaultExercises.joinToString(", "),
        onValueChange = {
            val parsed = it.split(",").map { value -> value.trim() }.filter { value -> value.isNotBlank() }
            viewModel.updateSettings(viewModel.settings.copy(defaultExercises = parsed))
        },
        label = { Text("Default exercises") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text("Restore latest on open: ${viewModel.settings.restoreLatestOnOpen}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { viewModel.updateSettings(viewModel.settings.copy(restoreLatestOnOpen = true)) }) { Text("Enable") }
        Button(onClick = { viewModel.updateSettings(viewModel.settings.copy(restoreLatestOnOpen = false)) }) { Text("Disable") }
    }
}

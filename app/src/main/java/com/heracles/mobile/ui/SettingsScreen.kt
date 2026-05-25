package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel

@Composable
fun HeraclesSettingsScreen(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "A clean place to tune your training defaults, storage, and appearance.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SettingsSection(title = "Profile", icon = Icons.Default.Tune) {
            OutlinedTextField(
                value = viewModel.settings.units,
                onValueChange = { viewModel.updateSettings(viewModel.settings.copy(units = it)) },
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
                supportingText = { Text("Comma-separated list used for new sessions.") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsSection(title = "Storage", icon = Icons.Default.Storage) {
            OutlinedTextField(
                value = viewModel.pendingLogStoragePath,
                onValueChange = viewModel::updatePendingLogStoragePath,
                label = { Text("Log storage path") },
                supportingText = { Text("Use an absolute path. Existing files move when applied.") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.updateSettings(viewModel.settings.copy(logStoragePath = viewModel.pendingLogStoragePath))
            }) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text("Move logs")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.exportSessionsTo("/sdcard/HeraclesSessions") }) { Text("Export sessions") }
                Text(viewModel.lastExportMessage)
            }
        }

        SettingsSection(title = "Training", icon = Icons.Default.Tune) {
            Text("Numeric input modes", style = MaterialTheme.typography.titleMedium)
            ModeRow(
                label = "Keyboard",
                checked = viewModel.settings.numericInputModes.contains("keyboard"),
                onToggle = {
                    viewModel.updateSettings(
                        viewModel.settings.copy(
                            numericInputModes = toggleMode(viewModel.settings.numericInputModes, "keyboard")
                        )
                    )
                }
            )
            ModeRow(
                label = "Scrubber",
                checked = viewModel.settings.numericInputModes.contains("scrubber"),
                onToggle = {
                    viewModel.updateSettings(
                        viewModel.settings.copy(
                            numericInputModes = toggleMode(viewModel.settings.numericInputModes, "scrubber")
                        )
                    )
                }
            )
            Text(
                "Scrubber sensitivity: ${String.format("%.2f", viewModel.settings.scrubberSensitivity)}x",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = String.format(java.util.Locale.US, "%.2f", viewModel.settings.scrubberSensitivity),
                    onValueChange = { raw ->
                        val parsed = raw.toDoubleOrNull()
                        if (parsed != null) {
                            // apply immediately but normalization will coerce to allowed range
                            viewModel.updateSettings(viewModel.settings.copy(scrubberSensitivity = parsed))
                        }
                    },
                    label = { Text("Scrubber sensitivity") },
                    singleLine = true,
                    modifier = Modifier.weight(0.35f)
                )
                Slider(
                    value = viewModel.settings.scrubberSensitivity.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateSettings(
                            viewModel.settings.copy(scrubberSensitivity = value.toDouble())
                        )
                    },
                    valueRange = 0.05f..10.0f,
                    modifier = Modifier.weight(0.65f)
                )
            }
        }

        SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
            CustomAppIconCard()
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark mode", style = MaterialTheme.typography.titleMedium)
                    Text("Switch between light and dark themes.", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = viewModel.settings.useDarkTheme,
                    onCheckedChange = { enabled ->
                        viewModel.updateSettings(viewModel.settings.copy(useDarkTheme = enabled))
                    }
                )
            }
        }

        SettingsSection(title = "Behavior", icon = Icons.Default.Tune) {
            Text(
                "Restore latest on open: ${viewModel.settings.restoreLatestOnOpen}",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.updateSettings(viewModel.settings.copy(restoreLatestOnOpen = true)) }) { Text("Enable") }
                Button(onClick = { viewModel.updateSettings(viewModel.settings.copy(restoreLatestOnOpen = false)) }) { Text("Disable") }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(icon, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun ModeRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label)
    }
}

private fun toggleMode(selected: Set<String>, mode: String): Set<String> {
    return if (selected.contains(mode)) selected - mode else selected + mode
}
/*
 File: ui/SettingsScreen.kt
 What it does: Displays app settings for training, storage, appearance, fidelity, and behavior.
 Main inputs: current `AppSettings` and pending values from the ViewModel.
 Main outputs: updates to settings, UI mode, fidelity, storage paths, and backups.
 Key functions/classes: `HeraclesSettingsScreen`, `SettingsSection`, `FidelityChip`.
*/

package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.SystemUiMode
import com.heracles.mobile.model.UiFidelityLevel
import com.heracles.mobile.ui.LocalScreenSize
import com.heracles.mobile.ui.ScreenSize
import com.heracles.mobile.ui.RigidContent
import com.heracles.mobile.ui.minimumInteractiveSize
import com.heracles.mobile.ui.AutoSizeText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.launch

@Composable
fun HeraclesSettingsScreen(viewModel: AppViewModel) {
    val defaultExercisesTextState = remember {
        mutableStateOf(viewModel.settings.defaultExercises.joinToString(", "))
    }
    val defaultExercisesText = defaultExercisesTextState.value
    val context = LocalContext.current
    val externalRootPath = remember(context) { context.getExternalFilesDir(null)?.absolutePath }
    val internalRootPath = remember(context) { context.filesDir.absolutePath }
    val storageDisplayPath = remember(viewModel.pendingLogStoragePath, externalRootPath, internalRootPath) {
        when {
            externalRootPath != null && viewModel.pendingLogStoragePath.startsWith(externalRootPath) -> "Shared Storage → Android/data/.../files/sessions"
            viewModel.pendingLogStoragePath.startsWith(internalRootPath) -> "Internal Private Sandbox"
            else -> viewModel.pendingLogStoragePath
        }
    }
    val storageIsReadOnly = (externalRootPath != null && viewModel.pendingLogStoragePath.startsWith(externalRootPath)) ||
        viewModel.pendingLogStoragePath.startsWith(internalRootPath)
    val exportSessionsPath = remember(context, externalRootPath) {
        (externalRootPath ?: context.filesDir.absolutePath) + "/exported_sessions"
    }

    val screenSize = LocalScreenSize.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RigidContent {
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
        }

        SettingsSection(title = "Profile", icon = Icons.Default.Tune) {
            OutlinedTextField(
                value = viewModel.settings.units,
                onValueChange = { viewModel.updateSettings(viewModel.settings.copy(units = it)) },
                label = { Text("Units") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = defaultExercisesText,
                onValueChange = { rawValue ->
                    defaultExercisesTextState.value = rawValue
                    viewModel.updateSettings(
                        viewModel.settings.copy(defaultExercises = parseDefaultExercises(rawValue))
                    )
                },
                label = { Text("Default exercises") },
                supportingText = { Text("Comma-separated list used for new sessions.") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsSection(title = "Storage", icon = Icons.Default.Storage) {
            OutlinedTextField(
                value = storageDisplayPath,
                onValueChange = { value ->
                    if (!storageIsReadOnly) {
                        viewModel.updatePendingLogStoragePath(value)
                    }
                },
                label = { Text("Log storage path") },
                supportingText = { Text("App-specific paths are shown as labels. Tap Custom Path to enter an absolute path.") },
                readOnly = storageIsReadOnly,
                trailingIcon = if (storageIsReadOnly) {
                    {
                        TextButton(onClick = { viewModel.updatePendingLogStoragePath("") }) {
                            Text("Custom Path")
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.updateSettings(viewModel.settings.copy(logStoragePath = viewModel.pendingLogStoragePath))
            }) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text("Move logs")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.exportSessionsTo(exportSessionsPath) }) { Text("Export sessions") }
                Button(onClick = { viewModel.backupSessions() }) { Text("Backup sessions") }
            }
            Text(viewModel.lastExportMessage)
            Text(viewModel.lastBackupMessage)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tier: ${viewModel.settings.uiFidelity} • Accent: ${viewModel.activeCuratedScheme().name} • Mode: ${viewModel.settings.systemUiMode}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                DisplayModeProximitySlider(
                    mode = viewModel.settings.systemUiMode,
                    onModeChange = viewModel::setSystemUiMode,
                    onScrubStart = viewModel::onScrubberGestureStart,
                    onScrubEnd = viewModel::onScrubberGestureEnd,
                )
                if (screenSize == ScreenSize.COMPACT) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(SystemUiMode.LIGHT to "Light", SystemUiMode.AUTO to "Auto", SystemUiMode.DARK to "Dark").forEach { (mode, label) ->
                            OutlinedButton(onClick = { viewModel.setSystemUiMode(mode) }, modifier = Modifier.weight(1f).minimumInteractiveSize()) {
                                AutoSizeText(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { viewModel.setSystemUiMode(SystemUiMode.LIGHT) }) { Text("Light") }
                        OutlinedButton(onClick = { viewModel.setSystemUiMode(SystemUiMode.AUTO) }) { Text("Auto") }
                        OutlinedButton(onClick = { viewModel.setSystemUiMode(SystemUiMode.DARK) }) { Text("Dark") }
                    }
                }
                Button(onClick = { viewModel.switchScreen("Theme") }) {
                    Text("Open appearance")
                }
                Button(
                    onClick = { viewModel.backupConfig() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Backup settings")
                }
                Text(viewModel.lastBackupMessage)
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

private fun parseDefaultExercises(rawValue: String): List<String> {
    return rawValue
        .split(',')
        .map { value -> value.trim() }
        .filter { value -> value.isNotBlank() }
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

@Composable
private fun FidelityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text("$label ✓")
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

private fun toggleMode(selected: Set<String>, mode: String): Set<String> {
    return if (selected.contains(mode)) selected - mode else selected + mode
}

@Composable
private fun DisplayModeProximitySlider(
    mode: SystemUiMode,
    onModeChange: (SystemUiMode) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
) {
    val labels = listOf("Light", "Auto", "Dark")
    val targetIndex = mode.toSliderIndex()
    val thumbPosition = remember { Animatable(targetIndex) }
    val coroutineScope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(targetIndex, dragging) {
        if (!dragging) {
            thumbPosition.animateTo(targetIndex, animationSpec = tween(180))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(mode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrubStart()
                    dragging = true
                    val widthPx = size.width.toFloat().coerceAtLeast(1f)
                    val sectorWidth = widthPx / 3f
                    val initialX = down.position.x.coerceIn(0f, widthPx)
                    coroutineScope.launch { thumbPosition.snapTo(((initialX / widthPx) * 2f).coerceIn(0f, 2f)) }
                    onModeChange(modeForSliderIndex(initialX / sectorWidth))

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.changedToUp()) {
                            break
                        }
                        if (change.pressed) {
                            val currentX = change.position.x.coerceIn(0f, widthPx)
                            coroutineScope.launch { thumbPosition.snapTo(((currentX / widthPx) * 2f).coerceIn(0f, 2f)) }
                            onModeChange(modeForSliderIndex(currentX / sectorWidth))
                        }
                    }

                    val snappedIndex = when {
                        thumbPosition.value < 0.5f -> 0f
                        thumbPosition.value < 1.5f -> 1f
                        else -> 2f
                    }
                    coroutineScope.launch { thumbPosition.animateTo(snappedIndex, animationSpec = tween(180)) }
                    onModeChange(modeForSliderIndex(snappedIndex))
                    dragging = false
                    onScrubEnd()
                }
            }
    ) {
        val thumbWidth = 86.dp
        val thumbHeight = 56.dp
        val thumbFraction = thumbPosition.value / 2f
        val thumbOffsetX = (maxWidth - thumbWidth) * thumbFraction

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            labels.forEachIndexed { index, label ->
                val active = when {
                    thumbPosition.value < 0.5f && index == 0 -> true
                    thumbPosition.value in 0.5f..1.5f && index == 1 -> true
                    thumbPosition.value > 1.5f && index == 2 -> true
                    else -> false
                }
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX, y = 10.dp)
                .height(thumbHeight)
                .width(thumbWidth)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        )
                    )
                )
        )
    }
}

private fun modeForSliderIndex(index: Float): SystemUiMode {
    return when {
        index < 0.5f -> SystemUiMode.LIGHT
        index < 1.5f -> SystemUiMode.AUTO
        else -> SystemUiMode.DARK
    }
}

private fun SystemUiMode.toSliderIndex(): Float {
    return when (this) {
        SystemUiMode.LIGHT -> 0f
        SystemUiMode.AUTO -> 1f
        SystemUiMode.DARK -> 2f
    }
}
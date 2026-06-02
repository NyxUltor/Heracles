/*
 File: ui/ThemeScreen.kt
 What it does: Shows available theme mods, their light/dark schemes, and allows applying/previewing/editing theme modpacks.
 Main inputs: `viewModel.themeMods`, current `viewModel.settings` selections.
 Main outputs: updates to settings (current mod, active schemes), triggers for editing/previewing mods.
 Key functions/classes: `HeraclesThemeScreen`, `ThemeModCard`, `SchemeSelectorRow`.
*/

// Important: Theme selection functions call into ViewModel (`selectMod`, `selectLightScheme`, `selectDarkScheme`).

package com.heracles.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.CuratedSchemes
import com.heracles.mobile.model.CuratedScheme
import com.heracles.mobile.model.ThemeColorScheme
import com.heracles.mobile.model.ThemeMod
import com.heracles.mobile.model.UiFidelityLevel
import com.heracles.mobile.model.SystemUiMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch

@Composable
fun HeraclesThemeScreen(viewModel: AppViewModel) {
    val mods = viewModel.themeMods
    val currentTier = viewModel.settings.uiFidelity
    val currentAccent = viewModel.activeCuratedScheme()
    val systemDark = when (viewModel.settings.systemUiMode) {
        SystemUiMode.DARK -> true
        SystemUiMode.LIGHT -> false
        SystemUiMode.AUTO -> isSystemInDarkTheme()
    }
    val tierAccents = accentSchemesForTier(currentTier)
        .sortedWith(
            compareByDescending<CuratedScheme> { viewModel.curatedSchemeUsageCount(it.id) }
                .thenBy { if (it.id == currentAccent.id) 0 else 1 }
                .thenBy { it.name }
        )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Appearance", style = MaterialTheme.typography.headlineSmall)
                Text("UI tiers, accents, and color mode are separated here.", style = MaterialTheme.typography.bodySmall)
            }
            if (currentTier == UiFidelityLevel.CUSTOM) {
                OutlinedButton(onClick = { viewModel.startCreatingMod() }) {
                    Text("Create mod")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UI tiers", style = MaterialTheme.typography.titleMedium)
                Text("Choose the visual system for the app.", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TierChip("Minimal", currentTier == UiFidelityLevel.MINIMAL) { viewModel.updateSettings(viewModel.settings.copy(uiFidelity = UiFidelityLevel.MINIMAL)) }
                    TierChip("Balanced", currentTier == UiFidelityLevel.BALANCED) { viewModel.updateSettings(viewModel.settings.copy(uiFidelity = UiFidelityLevel.BALANCED)) }
                    TierChip("Rich", currentTier == UiFidelityLevel.RICH) { viewModel.updateSettings(viewModel.settings.copy(uiFidelity = UiFidelityLevel.RICH)) }
                    TierChip("Custom", currentTier == UiFidelityLevel.CUSTOM) { viewModel.updateSettings(viewModel.settings.copy(uiFidelity = UiFidelityLevel.CUSTOM)) }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accents", style = MaterialTheme.typography.titleMedium)
                if (currentTier == UiFidelityLevel.CUSTOM) {
                    Text("Custom uses full theme modpacks instead of fixed accents.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Fixed accents are preconfigured for the selected tier and color mode.", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        tierAccents.forEach { scheme ->
                            item {
                                AccentPreviewCard(
                                    scheme = scheme,
                                    selected = currentAccent.id == scheme.id,
                                    darkTheme = systemDark,
                                    onClick = { viewModel.selectCuratedScheme(scheme.id) },
                                )
                            }
                        }
                    }
                    Text("Active accent: ${currentAccent.name}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Light / Dark / Auto", style = MaterialTheme.typography.titleMedium)
                Text("Auto follows the system setting.", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("Light", viewModel.settings.systemUiMode == SystemUiMode.LIGHT) { viewModel.setSystemUiMode(SystemUiMode.LIGHT) }
                    ModeChip("Dark", viewModel.settings.systemUiMode == SystemUiMode.DARK) { viewModel.setSystemUiMode(SystemUiMode.DARK) }
                    ModeChip("Auto", viewModel.settings.systemUiMode == SystemUiMode.AUTO) { viewModel.setSystemUiMode(SystemUiMode.AUTO) }
                }
                DisplayModeProximitySlider(
                    mode = viewModel.settings.systemUiMode,
                    onModeChange = viewModel::setSystemUiMode,
                    onScrubStart = viewModel::onScrubberGestureStart,
                    onScrubEnd = viewModel::onScrubberGestureEnd,
                )
            }
        }

        if (currentTier == UiFidelityLevel.CUSTOM) {
            if (mods.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No mods found")
                        Text("Create your first mod pack.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(mods, key = { it.id }) { mod ->
                        ThemeModCard(
                            mod = mod,
                            selected = viewModel.settings.currentModId == mod.id,
                            selectedLightSchemeId = viewModel.settings.activeLightSchemeId,
                            selectedDarkSchemeId = viewModel.settings.activeDarkSchemeId,
                            onApply = { viewModel.selectMod(mod.id) },
                            onSelectLightScheme = viewModel::selectLightScheme,
                            onSelectDarkScheme = viewModel::selectDarkScheme,
                            onPreview = { viewModel.setPreviewMode(mod.id) },
                            onStopPreview = { viewModel.clearPreviewMode() },
                            onEdit = { viewModel.startEditingMod(mod.id) },
                            onDelete = { viewModel.deleteMod(mod.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierChip(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
        Text(if (selected) "$label ✓" else label)
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
        Text(if (selected) "$label ✓" else label)
    }
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
            verticalAlignment = Alignment.CenterVertically
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
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
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

@Composable
private fun AccentPreviewCard(
    scheme: com.heracles.mobile.model.CuratedScheme,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
) {
    val swatch = if (darkTheme) scheme.darkTokens else scheme.lightTokens
    val primary = Color(android.graphics.Color.parseColor(swatch.primary))
    val secondary = Color(android.graphics.Color.parseColor(swatch.secondary))
    val background = Color(android.graphics.Color.parseColor(swatch.background))
    val surface = Color(android.graphics.Color.parseColor(swatch.surface))
    val onPrimary = Color(android.graphics.Color.parseColor(swatch.onPrimary))

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp),
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val wire = Color.White.copy(alpha = 0.12f)
                val accentWire = primary.copy(alpha = 0.96f)
                val accentSoft = primary.copy(alpha = 0.35f)
                val labelColor = onPrimary
                val headerTop = 8f
                val inputLeft = 12f
                val inputRight = size.width - 12f

                drawRect(color = surface.copy(alpha = 0.18f), topLeft = Offset(0f, 0f), size = size)
                drawRoundRect(color = Color.Transparent, topLeft = Offset(0f, 0f), size = size, cornerRadius = CornerRadius(2f, 2f), style = Stroke(width = 1f))

                drawLine(wire, Offset(inputLeft, headerTop), Offset(inputRight, headerTop), strokeWidth = 1f)
                drawLine(accentWire, Offset(inputLeft, headerTop + 14f), Offset(inputLeft + 26f, headerTop + 14f), strokeWidth = 2f)
                drawLine(wire, Offset(inputLeft + 34f, headerTop + 14f), Offset(inputRight, headerTop + 14f), strokeWidth = 1f)

                // Main logger wireframe blocks
                drawRoundRect(color = wire, topLeft = Offset(10f, 42f), size = androidx.compose.ui.geometry.Size(size.width - 20f, 26f), cornerRadius = CornerRadius(1f, 1f), style = Stroke(width = 1f))
                drawRoundRect(color = wire, topLeft = Offset(10f, 74f), size = androidx.compose.ui.geometry.Size(size.width - 20f, 26f), cornerRadius = CornerRadius(1f, 1f), style = Stroke(width = 1f))
                drawRoundRect(color = wire, topLeft = Offset(10f, 106f), size = androidx.compose.ui.geometry.Size(size.width - 20f, 26f), cornerRadius = CornerRadius(1f, 1f), style = Stroke(width = 1f))

                // Accent highlights on interactive areas
                drawLine(accentSoft, Offset(16f, 48f), Offset(52f, 48f), strokeWidth = 2f)
                drawLine(accentSoft, Offset(16f, 80f), Offset(66f, 80f), strokeWidth = 2f)
                drawLine(accentSoft, Offset(16f, 112f), Offset(58f, 112f), strokeWidth = 2f)

                // Logger-like labels
                drawLine(labelColor, Offset(18f, 150f), Offset(54f, 150f), strokeWidth = 2f)
                drawLine(labelColor, Offset(18f, 160f), Offset(82f, 160f), strokeWidth = 1.6f)
                drawLine(labelColor, Offset(18f, 170f), Offset(66f, 170f), strokeWidth = 1.6f)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(scheme.name, style = MaterialTheme.typography.labelLarge)
            Text(if (selected) "Selected" else "Tap to apply", style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(primary, secondary, background, surface).forEach { swatchColor ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(swatchColor, RoundedCornerShape(0.dp))
                )
            }
        }
    }
}

private fun accentSchemesForTier(tier: UiFidelityLevel): List<com.heracles.mobile.model.CuratedScheme> {
    return when (tier) {
        UiFidelityLevel.MINIMAL -> listOf(
            CuratedSchemes.HELLFIRE,
            CuratedSchemes.BLOOD_AND_WINE,
            CuratedSchemes.ABYSSAL_SKY,
            CuratedSchemes.DEEP_SEA,
            CuratedSchemes.TERMINAL_GRASS,
            CuratedSchemes.TOXIC_SLUDGE,
            CuratedSchemes.NEON_AMBER,
            CuratedSchemes.SOLAR_FLARE,
            CuratedSchemes.OBSIDIAN_ASH,
            CuratedSchemes.WHITE_PHANTOM,
            CuratedSchemes.DEEP_SLATE_CODE,
        )
        UiFidelityLevel.BALANCED -> listOf(
            CuratedSchemes.HELLFIRE,
            CuratedSchemes.BLOOD_AND_WINE,
            CuratedSchemes.ABYSSAL_SKY,
            CuratedSchemes.DEEP_SEA,
            CuratedSchemes.TERMINAL_GRASS,
            CuratedSchemes.TOXIC_SLUDGE,
            CuratedSchemes.NEON_AMBER,
            CuratedSchemes.SOLAR_FLARE,
            CuratedSchemes.OBSIDIAN_ASH,
            CuratedSchemes.WHITE_PHANTOM,
        )
        UiFidelityLevel.RICH -> listOf(
            CuratedSchemes.HELLFIRE,
            CuratedSchemes.BLOOD_AND_WINE,
            CuratedSchemes.ABYSSAL_SKY,
            CuratedSchemes.DEEP_SEA,
            CuratedSchemes.TERMINAL_GRASS,
            CuratedSchemes.TOXIC_SLUDGE,
            CuratedSchemes.NEON_AMBER,
            CuratedSchemes.SOLAR_FLARE,
            CuratedSchemes.OBSIDIAN_ASH,
            CuratedSchemes.WHITE_PHANTOM,
            CuratedSchemes.BLACK_GOLD,
        )
        UiFidelityLevel.CUSTOM -> emptyList()
    }
}

@Composable
private fun ThemeModCard(
    mod: ThemeMod,
    selected: Boolean,
    selectedLightSchemeId: String,
    selectedDarkSchemeId: String,
    onApply: () -> Unit,
    onSelectLightScheme: (String) -> Unit,
    onSelectDarkScheme: (String) -> Unit,
    onPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val activeLight = resolveScheme(mod.lightSchemes, selectedLightSchemeId)
    val activeDark = resolveScheme(mod.darkSchemes, selectedDarkSchemeId)
    val lightPrimary = safeColor(activeLight.tokens.primary, MaterialTheme.colorScheme.primary)
    val darkPrimary = safeColor(activeDark.tokens.primary, MaterialTheme.colorScheme.primary)
    val isBuiltIn = mod.id == "bare_metal" || mod.id == "stone_temple"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mod.name, style = MaterialTheme.typography.titleMedium)
                    Text("${mod.author} • ${mod.id}", style = MaterialTheme.typography.bodySmall)
                }
                Text(if (selected) "Applied" else "Tap to apply", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(lightPrimary))
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(darkPrimary))
                Text("Light: ${activeLight.name} • Dark: ${activeDark.name}", style = MaterialTheme.typography.bodySmall)
            }

            SchemeSelectorRow(
                title = "Light Schemes",
                schemes = mod.lightSchemes,
                selectedSchemeId = activeLight.id,
                onSelectScheme = onSelectLightScheme,
            )
            SchemeSelectorRow(
                title = "Dark Schemes",
                schemes = mod.darkSchemes,
                selectedSchemeId = activeDark.id,
                onSelectScheme = onSelectDarkScheme,
            )

            Text(
                "Style: ${if (mod.style.shapeStyle.name == "RECTANGLE") "Rectangular" else "Rounded"} • Button height ${mod.style.buttonHeightDp}dp",
                style = MaterialTheme.typography.bodySmall,
            )

            if (!mod.style.textureRule.equals("default", ignoreCase = true)) {
                Text("Texture: ${mod.style.textureRule}", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply) { Text("Apply") }
                OutlinedButton(onClick = onPreview) { Text("Preview") }
                OutlinedButton(onClick = onStopPreview) { Text("Stop") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                if (!isBuiltIn) {
                    OutlinedButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun SchemeSelectorRow(
    title: String,
    schemes: List<ThemeColorScheme>,
    selectedSchemeId: String,
    onSelectScheme: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            schemes.forEach { scheme ->
                OutlinedButton(onClick = { onSelectScheme(scheme.id) }) {
                    Text(if (scheme.id == selectedSchemeId) "${scheme.name} ✓" else scheme.name)
                }
            }
        }
    }
}

private fun resolveScheme(schemes: List<ThemeColorScheme>, schemeId: String): ThemeColorScheme {
    return schemes.firstOrNull { it.id == schemeId }
        ?: schemes.firstOrNull()
        ?: ThemeColorScheme("default", "Default", com.heracles.mobile.model.TokenSet())
}

private fun safeColor(value: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}

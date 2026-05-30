package com.heracles.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.ThemeColorScheme
import com.heracles.mobile.model.ThemeMod

@Composable
fun HeraclesThemeScreen(viewModel: AppViewModel) {
    val mods = viewModel.themeMods

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Theme Modpacks", style = MaterialTheme.typography.headlineSmall)
                Text("Pick a modpack style, plus separate light and dark color schemes.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { viewModel.startCreatingMod() }) {
                Text("Create mod")
            }
        }

        if (mods.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No mods found")
                    Text("Create your first mod pack.", style = MaterialTheme.typography.bodySmall)
                }
            }
            return
        }

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

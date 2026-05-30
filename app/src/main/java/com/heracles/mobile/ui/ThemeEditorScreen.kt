package com.heracles.mobile.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.ShapeMode
import com.heracles.mobile.model.ThemeColorScheme
import com.heracles.mobile.model.ThemeMod
import com.heracles.mobile.model.ThemeStylePack
import com.heracles.mobile.model.TokenSet

@Composable
fun ThemeEditorScreen(viewModel: AppViewModel) {
    val existing = viewModel.currentEditingMod()
    val initialLightScheme = existing?.lightSchemes?.firstOrNull()
    val initialDarkScheme = existing?.darkSchemes?.firstOrNull()
    val scroll = rememberScrollState()
    val context = LocalContext.current

    val idState = remember { mutableStateOf(existing?.id ?: "custom_${System.currentTimeMillis()}") }
    val nameState = remember { mutableStateOf(existing?.name ?: "New Mod") }
    val authorState = remember { mutableStateOf(existing?.author ?: "You") }
    val wallpaperState = remember { mutableStateOf(existing?.style?.wallpaperUri.orEmpty()) }

    val lightPrimary = remember { mutableStateOf(initialLightScheme?.tokens?.primary ?: "#2457C5") }
    val lightSecondary = remember { mutableStateOf(initialLightScheme?.tokens?.secondary ?: "#4D6DB5") }
    val lightBackground = remember { mutableStateOf(initialLightScheme?.tokens?.background ?: "#FFFBFE") }
    val lightSurface = remember { mutableStateOf(initialLightScheme?.tokens?.surface ?: "#FFFFFF") }
    val lightOnPrimary = remember { mutableStateOf(initialLightScheme?.tokens?.onPrimary ?: "#FFFFFF") }

    val darkPrimary = remember { mutableStateOf(initialDarkScheme?.tokens?.primary ?: "#2457C5") }
    val darkSecondary = remember { mutableStateOf(initialDarkScheme?.tokens?.secondary ?: "#4D6DB5") }
    val darkBackground = remember { mutableStateOf(initialDarkScheme?.tokens?.background ?: "#1F1F21") }
    val darkSurface = remember { mutableStateOf(initialDarkScheme?.tokens?.surface ?: "#272729") }
    val darkOnPrimary = remember { mutableStateOf(initialDarkScheme?.tokens?.onPrimary ?: "#FFFFFF") }

    var rectangleShapes by remember { mutableStateOf((existing?.style?.shapeStyle ?: ShapeMode.DEFAULT) == ShapeMode.RECTANGLE) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            wallpaperState.value = uri.toString()
        }
    }

    fun saveAndReturn() {
        val lightSchemeId = initialLightScheme?.id ?: "default_light"
        val darkSchemeId = initialDarkScheme?.id ?: "default_dark"
        val lightSchemeName = initialLightScheme?.name ?: "Default Light"
        val darkSchemeName = initialDarkScheme?.name ?: "Default Dark"

        val updatedLightScheme = ThemeColorScheme(
            id = lightSchemeId,
            name = lightSchemeName,
            tokens = TokenSet(
                primary = lightPrimary.value,
                secondary = lightSecondary.value,
                background = lightBackground.value,
                surface = lightSurface.value,
                onPrimary = lightOnPrimary.value,
                borderWidth = initialLightScheme?.tokens?.borderWidth ?: 1.0,
                surfaceRule = initialLightScheme?.tokens?.surfaceRule ?: "default",
            ),
        )

        val updatedDarkScheme = ThemeColorScheme(
            id = darkSchemeId,
            name = darkSchemeName,
            tokens = TokenSet(
                primary = darkPrimary.value,
                secondary = darkSecondary.value,
                background = darkBackground.value,
                surface = darkSurface.value,
                onPrimary = darkOnPrimary.value,
                borderWidth = initialDarkScheme?.tokens?.borderWidth ?: 1.0,
                surfaceRule = initialDarkScheme?.tokens?.surfaceRule ?: "default",
            ),
        )

        val mod = ThemeMod(
            id = idState.value.ifBlank { "custom_${System.currentTimeMillis()}" },
            name = nameState.value.ifBlank { "New Mod" },
            author = authorState.value.ifBlank { "You" },
            lightSchemes = buildList {
                add(updatedLightScheme)
                addAll(existing?.lightSchemes.orEmpty().drop(1))
            },
            darkSchemes = buildList {
                add(updatedDarkScheme)
                addAll(existing?.darkSchemes.orEmpty().drop(1))
            },
            style = (existing?.style ?: ThemeStylePack()).copy(
                shapeStyle = if (rectangleShapes) ShapeMode.RECTANGLE else ShapeMode.DEFAULT,
                wallpaperUri = wallpaperState.value.ifBlank { null },
            ),
        )

        if (existing == null) viewModel.addMod(mod) else viewModel.updateMod(mod)
        viewModel.finishEditingMod()
        viewModel.switchScreen("Theme")
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.switchScreen("Theme") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Button(onClick = { saveAndReturn() }) { Text("Save mod") }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = idState.value, onValueChange = { idState.value = it }, label = { Text("Mod ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = nameState.value, onValueChange = { nameState.value = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = authorState.value, onValueChange = { authorState.value = it }, label = { Text("Author") }, modifier = Modifier.fillMaxWidth())

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Light tokens", style = MaterialTheme.typography.titleMedium)
                    ColorFieldCard("Primary", lightPrimary.value, { lightPrimary.value = it })
                    ColorFieldCard("Secondary", lightSecondary.value, { lightSecondary.value = it })
                    ColorFieldCard("Background", lightBackground.value, { lightBackground.value = it })
                    ColorFieldCard("Surface", lightSurface.value, { lightSurface.value = it })
                    ColorFieldCard("On-primary", lightOnPrimary.value, { lightOnPrimary.value = it })
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dark tokens", style = MaterialTheme.typography.titleMedium)
                    ColorFieldCard("Primary", darkPrimary.value, { darkPrimary.value = it })
                    ColorFieldCard("Secondary", darkSecondary.value, { darkSecondary.value = it })
                    ColorFieldCard("Background", darkBackground.value, { darkBackground.value = it })
                    ColorFieldCard("Surface", darkSurface.value, { darkSurface.value = it })
                    ColorFieldCard("On-primary", darkOnPrimary.value, { darkOnPrimary.value = it })
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shape style", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { rectangleShapes = false }) { Text("Default") }
                        OutlinedButton(onClick = { rectangleShapes = true }) { Text("Rectangle") }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }) { Text("Pick wallpaper") }
                OutlinedButton(onClick = { wallpaperState.value = "" }) { Text("Remove wallpaper") }
            }
            Text(if (wallpaperState.value.isBlank()) "No wallpaper selected" else "Wallpaper selected")
        }
    }
}

@Composable
private fun SVBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
) {
    var size by remember { mutableStateOf(Size.Zero) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(size) {
                detectDragGestures { change, _ ->
                    val pos = change.position
                    if (size.width <= 0f || size.height <= 0f) return@detectDragGestures
                    val x = pos.x.coerceIn(0f, size.width)
                    val y = pos.y.coerceIn(0f, size.height)
                    val newS = (x / size.width).coerceIn(0f, 1f)
                    val newV = (1f - (y / size.height)).coerceIn(0f, 1f)
                    onChange(newS, newV)
                }
            }
        ) {
            val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor), tileMode = TileMode.Clamp), size = size)
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = size.height.toFloat()), size = size)

            val markerX = saturation * size.width
            val markerY = (1f - value) * size.height
            drawCircle(color = Color.White, radius = 10f, center = Offset(markerX, markerY))
            drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = 5f, center = Offset(markerX, markerY))
        }
    }
}

@Composable
private fun ColorFieldCard(label: String, value: String, onValueChange: (String) -> Unit) {
    var openDialog by remember { mutableStateOf(false) }
    val swatch = safeColor(value, Color(0xFF6750A4))

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(hexForColor(swatch), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { openDialog = true }) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), CircleShape)
                )
            }
        }
    }

    if (openDialog) {
        ColorEditDialog(
            title = label,
            initialHex = value,
            onDismiss = { openDialog = false },
            onSave = {
                onValueChange(it)
                openDialog = false
            },
        )
    }
}

@Composable
private fun ColorEditDialog(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val initialHsv = remember(initialHex) { hexToHsv(initialHex) }
    var hue by remember(initialHex) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initialHex) { mutableStateOf(initialHsv[1]) }
    var brightness by remember(initialHex) { mutableStateOf(initialHsv[2]) }

    val currentColor = Color.hsv(hue, saturation, brightness)

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(hexForColor(currentColor), style = MaterialTheme.typography.bodyMedium)

                SVBox(hue = hue, saturation = saturation, value = brightness, onChange = { newSat, newVal ->
                    saturation = newSat
                    brightness = newVal
                })

                Text("Hue", style = MaterialTheme.typography.bodySmall)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), border = ButtonDefaults.outlinedButtonBorder) {
                        Text("Cancel")
                    }
                    Button(onClick = { onSave(hexForColor(currentColor)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun hexToHsv(hex: String): FloatArray {
    val color = runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(android.graphics.Color.MAGENTA)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color, hsv)
    return hsv
}

private fun hexForColor(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun safeColor(value: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}

package com.heracles.mobile.ui.theme

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.heracles.mobile.model.ShapeMode
import com.heracles.mobile.model.UiFidelityLevel
import com.heracles.mobile.model.ThemeColorScheme
import com.heracles.mobile.model.ThemeMod
import com.heracles.mobile.model.TokenSet

@Composable
fun HeraclesTheme(
    darkTheme: Boolean,
    uiScale: Double = 1.0,
    currentScreen: String = "Logger",
    themeMod: ThemeMod? = null,
    activeLightSchemeId: String = "default_light",
    activeDarkSchemeId: String = "default_dark",
    uiFidelity: UiFidelityLevel = UiFidelityLevel.BALANCED,
    content: @Composable () -> Unit,
) {
    val activeTokens = remember(themeMod, darkTheme) {
        when {
            themeMod == null -> defaultTokenSet(darkTheme)
            darkTheme -> themeMod.resolveDarkScheme(activeDarkSchemeId).tokens
            else -> themeMod.resolveLightScheme(activeLightSchemeId).tokens
        }
    }
    val colorScheme = remember(activeTokens, darkTheme) { activeTokens.toColorScheme(darkTheme) }
    val resolvedUiScale = remember(uiScale) { uiScale }
    val density = LocalDensity.current
    val wallpaperBlurStrength = when (uiFidelity) {
        UiFidelityLevel.MINIMAL -> 0f
        UiFidelityLevel.BALANCED -> 0f
        UiFidelityLevel.RICH -> 10f
    }

    CompositionLocalProvider(LocalDensity provides Density(density.density * resolvedUiScale.toFloat(), density.fontScale * resolvedUiScale.toFloat())) {
        val shapeSet = remember(themeMod, uiFidelity) {
            if (uiFidelity == UiFidelityLevel.MINIMAL || themeMod?.style?.shapeStyle == ShapeMode.RECTANGLE || themeMod?.id == "stone_temple") {
                Shapes(
                    small = RoundedCornerShape(0.dp),
                    medium = RoundedCornerShape(0.dp),
                    large = RoundedCornerShape(0.dp),
                )
            } else if (uiFidelity == UiFidelityLevel.RICH) {
                Shapes(
                    small = RoundedCornerShape(12.dp),
                    medium = RoundedCornerShape(16.dp),
                    large = RoundedCornerShape(22.dp),
                )
            } else {
                Shapes(
                    small = RoundedCornerShape(8.dp),
                    medium = RoundedCornerShape(12.dp),
                    large = RoundedCornerShape(16.dp),
                )
            }
        }

        MaterialTheme(colorScheme = colorScheme, shapes = shapeSet) {
            val wallpaperUri = themeMod?.style?.wallpaperUri
            // Show wallpaper only for RICH fidelity to keep BALANCED pixel-perfect mockups isolated
            val shouldShowWallpaper = uiFidelity == UiFidelityLevel.RICH && !wallpaperUri.isNullOrBlank()
            if (!shouldShowWallpaper) {
                content()
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    WallpaperBackground(wallpaperUri = wallpaperUri!!, blurStrengthDp = wallpaperBlurStrength)
                    content()
                }
            }
        }
    }
}

@Composable
private fun WallpaperBackground(wallpaperUri: String, blurStrengthDp: Float) {
    val context = LocalContext.current
    var bitmap by remember(wallpaperUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(wallpaperUri) {
        bitmap = runCatching {
            val uri = Uri.parse(wallpaperUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull()
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(blurStrengthDp.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun defaultTokenSet(darkTheme: Boolean): TokenSet {
    return if (darkTheme) {
        TokenSet(
            primary = "#2457C5",
            secondary = "#4D6DB5",
            background = "#1F1F21",
            surface = "#272729",
            onPrimary = "#FFFFFF",
        )
    } else {
        TokenSet(
            primary = "#2457C5",
            secondary = "#4D6DB5",
            background = "#FFFBFE",
            surface = "#FFFFFF",
            onPrimary = "#FFFFFF",
        )
    }
}

private fun TokenSet.toColorScheme(darkTheme: Boolean): ColorScheme {
    val primaryColor = parseColor(primary, Color(0xFF2457C5))
    val secondaryColor = parseColor(secondary, Color(0xFF4D6DB5))
    val backgroundColor = parseColor(background, if (darkTheme) Color(0xFF1F1F21) else Color(0xFFFFFBFE))
    val surfaceColor = parseColor(surface, backgroundColor)
    val onPrimaryColor = parseColor(onPrimary, Color.White)
    return if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            onPrimary = onPrimaryColor,
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            onPrimary = onPrimaryColor,
        )
    }
}

private fun parseColor(value: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}

private fun ThemeMod.resolveLightScheme(schemeId: String): ThemeColorScheme {
    return lightSchemes.firstOrNull { it.id == schemeId }
        ?: lightSchemes.firstOrNull()
        ?: ThemeColorScheme("default_light", "Default Light", TokenSet(background = "#FFFBFE", surface = "#FFFFFF"))
}

private fun ThemeMod.resolveDarkScheme(schemeId: String): ThemeColorScheme {
    return darkSchemes.firstOrNull { it.id == schemeId }
        ?: darkSchemes.firstOrNull()
        ?: ThemeColorScheme("default_dark", "Default Dark", TokenSet())
}

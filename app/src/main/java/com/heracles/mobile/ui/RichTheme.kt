package com.heracles.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object RichTokens {
    @Composable
    fun accentPrimary(): Color = MaterialTheme.colorScheme.primary

    @Composable
    fun accentSecondary(): Color = MaterialTheme.colorScheme.secondary

    @Composable
    fun accentGlow(): Color = accentPrimary().copy(alpha = 0.20f)

    @Composable
    fun borderGlow(): Color = accentPrimary().copy(alpha = 0.45f)

    val SurfaceGlass = Color(0x0AFFFFFF)
    val SurfaceGlassStrong = Color(0x14FFFFFF)
    val BorderSubtle = Color(0x12FFFFFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x66FFFFFF)
    val TextTertiary = Color(0x33FFFFFF)
    val BackgroundBase = Color(0xFF0A0A0F)
    val BackgroundDeep = Color(0xFF060608)
    val CardElevation = 24.dp
    val CardRadius = 20.dp
    val SpringStiffness = 400f
    val SpringDamping = 0.6f
}

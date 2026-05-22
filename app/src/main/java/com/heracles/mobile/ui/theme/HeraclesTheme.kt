package com.heracles.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HeraclesColors = lightColorScheme(
    primary = Color(0xFF2457C5),
    secondary = Color(0xFF4D6DB5),
    tertiary = Color(0xFF006A60),
)

@Composable
fun HeraclesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HeraclesColors,
        content = content,
    )
}
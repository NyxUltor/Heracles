package com.heracles.mobile.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ScreenSize { COMPACT, EXPANDED }

val LocalScreenSize = staticCompositionLocalOf { ScreenSize.EXPANDED }

fun Dp.toScreenSize(): ScreenSize = if (this < 600.dp) ScreenSize.COMPACT else ScreenSize.EXPANDED

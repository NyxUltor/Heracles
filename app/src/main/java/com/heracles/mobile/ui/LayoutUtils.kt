package com.heracles.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

// (breakpoint & LocalScreenSize are defined in Breakpoints.kt)

// Minimum interactive component size modifier (48dp touch target)
fun Modifier.minimumInteractiveSize(minimum: Dp = 48.dp): Modifier = this.requiredSizeIn(minWidth = minimum, minHeight = minimum)

// RigidCard: forces fontScale=1f for contained content to avoid layout blowouts
@Composable
fun RigidCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixed = Density(density.density, 1f)
    CompositionLocalProvider(LocalDensity provides fixed) {
        Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            content()
        }
    }
}

@Composable
fun RigidContent(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixed = Density(density.density, 1f)
    CompositionLocalProvider(LocalDensity provides fixed) {
        content()
    }
}

// Auto-size text: naive scaler that reduces font size to avoid clipping with one line
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minFontSize: Float = 10f,
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        // approximate char width in px using the provided font size
        var fontSizeSp = (style.fontSize.value.takeIf { it > 0f } ?: 14f)
        val charWidthApprox = with(LocalDensity.current) { (fontSizeSp * 0.6f).dp.toPx() }
        val estimatedWidth = text.length * charWidthApprox
        if (estimatedWidth > maxWidthPx && estimatedWidth > 0f) {
            val scale = (maxWidthPx / estimatedWidth).coerceAtLeast(minFontSize / fontSizeSp)
            fontSizeSp = fontSizeSp * scale
        }
        Text(text = text, style = style.copy(fontSize = fontSizeSp.sp), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

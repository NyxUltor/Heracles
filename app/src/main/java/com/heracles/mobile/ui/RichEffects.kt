package com.heracles.mobile.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Glass card modifier — layered blur simulation + border glow
fun Modifier.glassCard(
    cornerRadius: Dp = RichTokens.CardRadius,
    glowColor: Color = Color.Transparent,
    glowRadius: Float = 60f,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(RichTokens.SurfaceGlass)
    .drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(glowRadius, 0f, 0f, glowColor.toArgb())
                }
            }
            canvas.drawRoundRect(
                left = 0f, top = 0f,
                right = size.width, bottom = size.height,
                radiusX = cornerRadius.toPx(),
                radiusY = cornerRadius.toPx(),
                paint = paint
            )
        }
        // Top highlight line
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0x22FFFFFF), Color.Transparent)
            ),
            start = Offset(size.width * 0.2f, 1f),
            end = Offset(size.width * 0.8f, 1f),
            strokeWidth = 1f
        )
    }

// Spring scale animation on tap
@Composable
fun springScale(): State<Float> {
    val scale = remember { Animatable(1f) }
    return scale.asState()
}

// Haptic feedback helper
@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}

// Particle burst effect for workout completion
@Composable
fun ParticleBurst(
    trigger: Boolean,
    modifier: Modifier = Modifier,
) {
    val particles = remember { List(20) { ParticleState() } }
    val progress = remember { Animatable(0f) }
    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentSecondary = MaterialTheme.colorScheme.secondary
    val accentTertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val palette = remember(accentPrimary, accentSecondary, accentTertiary) {
        listOf(
            accentPrimary,
            accentSecondary,
            accentTertiary,
            Color.White,
            onSurface,
        )
    }

    LaunchedEffect(trigger) {
        if (trigger) {
            particles.forEach { it.reset(palette) }
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        if (progress.value > 0f) {
            particles.forEach { p ->
                val x = center.x + p.vx * progress.value * size.width * 0.5f
                val y = center.y + p.vy * progress.value * size.height * 0.5f +
                        (0.5f * 980f * progress.value * progress.value)
                val alpha = (1f - progress.value).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.radius * (1f - progress.value * 0.5f),
                    center = Offset(x, y)
                )
            }
        }
    }
}

class ParticleState {
    var vx = 0f; var vy = 0f; var radius = 0f; var color = Color.White
    fun reset(palette: List<Color>) {
        val angle = (Math.random() * 2 * Math.PI).toFloat()
        val speed = (Math.random() * 0.4f + 0.1f).toFloat()
        vx = kotlin.math.cos(angle) * speed
        vy = kotlin.math.sin(angle) * speed - 0.3f
        radius = (Math.random() * 8f + 4f).toFloat()
        color = if (palette.isEmpty()) Color.White else palette.random()
    }
}

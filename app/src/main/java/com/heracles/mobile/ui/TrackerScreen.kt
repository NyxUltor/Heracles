package com.heracles.mobile.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.BodyweightEntry
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.ui.LocalScreenSize
import com.heracles.mobile.ui.ScreenSize
import com.heracles.mobile.ui.RigidContent
import com.heracles.mobile.ui.AutoSizeText
import com.heracles.mobile.ui.minimumInteractiveSize
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DailyMetric(
    val date: LocalDate,
    val value: Double,
)

private data class RadarMetric(
    val label: String,
    val value: Double,
)

private data class ChartTooltipState(
    val dateText: String,
    val volumeText: String,
    val bodyweightText: String?,
    val anchorX: Float,
    val anchorY: Float,
)

@Composable
fun HeraclesTrackerScreen(viewModel: AppViewModel) {
    val sessions = viewModel.sessions.toList()
    val bodyweights = viewModel.bodyweightHistory.toList()
    val dailyVolumes = aggregateDailyVolumes(sessions)
    val dailyBodyweights = aggregateDailyBodyweights(bodyweights)
    val radarMetrics = buildRadarMetrics(sessions)
    val latestBodyweight = bodyweights.lastOrNull()?.weight?.toString().orEmpty()

    val screenSize = LocalScreenSize.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (screenSize == ScreenSize.COMPACT) 96.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tracker", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "See bodyweight, session volume, and muscle balance at a glance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bodyweight entry", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Stored in a separate file for tracking and charting.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrubbableNumericField(
                                enabled = viewModel.settings.numericInputModes.contains("scrubber"),
                                text = viewModel.trackerBodyWeight,
                                sensitivity = viewModel.settings.scrubberSensitivity,
                                decimalPlaces = 1,
                                onScrubStart = viewModel::onScrubberGestureStart,
                                onScrubEnd = viewModel::onScrubberGestureEnd,
                                onValueChange = viewModel::updateTrackerBodyWeight,
                            )
                    ) {
                        OutlinedTextField(
                            value = viewModel.trackerBodyWeight,
                            onValueChange = viewModel::updateTrackerBodyWeight,
                            readOnly = !viewModel.settings.numericInputModes.contains("keyboard"),
                            label = { Text("Bodyweight") },
                            supportingText = { Text("Latest saved value: ${if (latestBodyweight.isBlank()) "none" else latestBodyweight}") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    Button(onClick = viewModel::saveTrackerBodyWeight) {
                        Text("Save bodyweight")
                    }
                }
            }

            MetricCard(
                title = "Training balance",
                subtitle = "Pull, push, core, cardio, and legs from your logged sessions"
            ) {
                RadarChart(metrics = radarMetrics)
            }

            MetricCard(
                title = "Volume + bodyweight",
                subtitle = "Daily session volume and bodyweight plotted against date"
            ) {
                VolumeBodyweightChart(
                    volumeMetrics = dailyVolumes,
                    bodyweightMetrics = dailyBodyweights
                )
            }

            RigidContent {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recent history", style = MaterialTheme.typography.titleMedium)
                        AutoSizeText("Sessions: ${sessions.size}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), maxLines = 1)
                        AutoSizeText("Bodyweight entries: ${bodyweights.size}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), maxLines = 1)
                    }
                }
            }
        }

        if (screenSize == ScreenSize.COMPACT) {
            // Bottom bar and FAB for compact phones
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { viewModel.saveSession() }, modifier = Modifier.fillMaxWidth().minimumInteractiveSize()) {
                        Text("Save Workout", maxLines = 1)
                    }
                }

                FloatingActionButton(
                    onClick = { viewModel.addExercise() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-16).dp, y = (-16).dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun RadarChart(metrics: List<RadarMetric>) {
    val labels = listOf("Pull", "Push", "Core", "Cardio", "Legs")
    val valueMap = labels.associateWith { label -> metrics.firstOrNull { it.label == label }?.value ?: 0.0 }
    val maxValue = (valueMap.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendRow(
            listOf(
                Color(0xFF2563EB) to "Pull",
                Color(0xFFF97316) to "Push",
                Color(0xFF10B981) to "Core",
                Color(0xFFEAB308) to "Cardio",
                Color(0xFF8B5CF6) to "Legs",
            )
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.32f
            val levelCount = 4
            val labelPaint = Paint().apply {
                color = android.graphics.Color.argb(220, 56, 65, 82)
                textSize = 26f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            repeat(levelCount) { level ->
                val fraction = (level + 1) / levelCount.toFloat()
                drawCircle(
                    color = Color(0x1A94A3B8),
                    radius = radius * fraction,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            labels.forEachIndexed { index, label ->
                val angle = (-PI / 2) + (2 * PI * index / labels.size)
                val axisEnd = Offset(
                    x = center.x + (radius * cos(angle)).toFloat(),
                    y = center.y + (radius * sin(angle)).toFloat(),
                )
                drawLine(color = Color(0x2294A3B8), start = center, end = axisEnd, strokeWidth = 2f)

                val labelOffset = Offset(
                    x = center.x + ((radius + 34f) * cos(angle)).toFloat(),
                    y = center.y + ((radius + 34f) * sin(angle)).toFloat(),
                )
                drawContext.canvas.nativeCanvas.drawText(label, labelOffset.x, labelOffset.y, labelPaint)
            }

            val polygonPath = Path()
            labels.forEachIndexed { index, label ->
                val angle = (-PI / 2) + (2 * PI * index / labels.size)
                val normalized = (valueMap[label] ?: 0.0) / maxValue
                val point = Offset(
                    x = center.x + (radius * normalized.toFloat() * cos(angle)).toFloat(),
                    y = center.y + (radius * normalized.toFloat() * sin(angle)).toFloat(),
                )
                if (index == 0) {
                    polygonPath.moveTo(point.x, point.y)
                } else {
                    polygonPath.lineTo(point.x, point.y)
                }
                drawCircle(color = Color(0xFF3D8BFF), radius = 6f, center = point)
                    drawCircle(color = Color(0xFF2563EB), radius = 6f, center = point)
            }
            polygonPath.close()
            drawPath(
                path = polygonPath,
                color = Color(0x223B82F6),
                style = Stroke(width = 4f)
            )
        }
    }
}

@Composable
private fun VolumeBodyweightChart(
    volumeMetrics: List<DailyMetric>,
    bodyweightMetrics: List<DailyMetric>,
) {
    val dates = volumeMetrics.map { it.date }
    val volumeMap = volumeMetrics.associateBy({ it.date }, { it.value })
    val bodyweightMap = bodyweightMetrics.associateBy({ it.date }, { it.value })
    val confirmedBodyweightPoints = dates.mapNotNull { date -> bodyweightMap[date]?.let { date to it } }
    val hasBodyweightData = bodyweightMetrics.isNotEmpty()
    val dateToIndex = dates.withIndex().associate { it.value to it.index }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var tooltipState by remember { mutableStateOf<ChartTooltipState?>(null) }

    val slotWidthDp = 40.dp
    val chartHeightDp = 220.dp
    val xLabelHeightDp = 30.dp
    val leftAxisWidth = 64.dp
    val rightAxisWidth = if (hasBodyweightData) 72.dp else 0.dp
    val contentWidthDp = (dates.size.coerceAtLeast(1) * 40).dp
    val contentWidthPx = with(density) { contentWidthDp.toPx() }
    val slotWidthPx = with(density) { slotWidthDp.toPx() }
    val chartHeightPx = with(density) { chartHeightDp.toPx() }
    val xLabelHeightPx = with(density) { xLabelHeightDp.toPx() }
    val tooltipWidthPx = with(density) { 180.dp.toPx() }
    val tooltipHeightPx = with(density) { 78.dp.toPx() }
    val tooltipHalfWidthPx = tooltipWidthPx / 2f
    val tooltipGapPx = with(density) { 12.dp.toPx() }
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val lineColor = Color(0xFF14B8A6)
    val volumeNumberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val tooltipDateFormatter = remember { DateTimeFormatter.ofPattern("EEE dd MMM", Locale.getDefault()) }

    fun roundToNearest500(value: Double): Int {
        return (kotlin.math.round(value / 500.0) * 500.0).roundToInt()
    }

    fun formatVolume(value: Double): String {
        return "${volumeNumberFormat.format(value.roundToInt())} kg"
    }

    fun formatBodyweight(value: Double): String {
        return String.format(Locale.US, "%.1f kg", value)
    }

    fun buildVolumeLabels(maxVolume: Double): List<String> {
        val maxScale = maxOf(500, roundToNearest500(maxVolume))
        return List(5) { index ->
            val value = maxScale * (4 - index) / 4.0
            volumeNumberFormat.format(value.roundToInt())
        }
    }

    fun buildBodyweightLabels(minBw: Double, maxBw: Double): List<String> {
        val low = minBw - 1.0
        val high = maxBw + 1.0
        return List(5) { index ->
            val value = high - ((high - low) * index / 4.0)
            String.format(Locale.US, "%.1f", value)
        }
    }

    fun findTooltip(tapX: Float, tapY: Float): ChartTooltipState? {
        if (tooltipState != null || dates.isEmpty()) return null

        val contentX = tapX + scrollState.value.toFloat()
        val index = (contentX / slotWidthPx).toInt().coerceIn(0, dates.lastIndex)
        val date = dates[index]
        val volume = volumeMap[date] ?: return null
        val maxVolume = volumeMetrics.maxOf { it.value }
        val volumeScale = if (maxVolume > 0.0) chartHeightPx / maxVolume else 0.0
        val barLeft = index * slotWidthPx
        val barRight = barLeft + slotWidthPx
        val barTop = chartHeightPx - (volume * volumeScale).toFloat()
        val onBar = contentX in barLeft..barRight && tapY in barTop..chartHeightPx

        val bodyweightMin = bodyweightMetrics.minOfOrNull { it.value } ?: 0.0
        val bodyweightMax = bodyweightMetrics.maxOfOrNull { it.value } ?: 0.0
        val bodyweightRange = (bodyweightMax - bodyweightMin).takeIf { it > 0.0 } ?: 1.0
        val bodyweightScale = if (hasBodyweightData) chartHeightPx / bodyweightRange else 0.0
        val bodyweightHit = if (hasBodyweightData) {
            confirmedBodyweightPoints.firstOrNull { (pointDate, value) ->
                val pointIndex = dateToIndex[pointDate] ?: return@firstOrNull false
                val pointX = (pointIndex * slotWidthPx) + (slotWidthPx / 2f)
                val pointY = chartHeightPx - ((value - bodyweightMin) * bodyweightScale).toFloat()
                val dx = contentX - pointX
                val dy = tapY - pointY
                (dx * dx + dy * dy) <= with(density) { 8.dp.toPx().let { it * it } }
            }
        } else null

        return if (onBar || bodyweightHit != null) {
            ChartTooltipState(
                dateText = date.format(tooltipDateFormatter),
                volumeText = formatVolume(volume),
                bodyweightText = bodyweightMap[date]?.let { formatBodyweight(it) },
                anchorX = contentX,
                anchorY = tapY,
            )
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendRow(
            listOf(
                barColor to "Volume",
                Color(0xFF14B8A6) to "Bodyweight",
            )
        )

        if (dates.isEmpty()) {
            Text(
                "No volume data available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val volumeLabels = buildVolumeLabels(volumeMetrics.maxOf { it.value })
        val bodyweightLabels = if (hasBodyweightData) {
            buildBodyweightLabels(bodyweightMetrics.minOf { it.value }, bodyweightMetrics.maxOf { it.value })
        } else {
            emptyList()
        }

        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .width(leftAxisWidth)
                    .height(chartHeightDp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                volumeLabels.forEach { label ->
                    Text(label, color = barColor, style = MaterialTheme.typography.labelSmall)
                }
            }

            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .pointerInput(dates, bodyweightMetrics, scrollState.value) {
                        detectTapGestures { tap ->
                            if (tooltipState != null) {
                                tooltipState = null
                            } else {
                                tooltipState = findTooltip(tap.x, tap.y)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .height(chartHeightDp + xLabelHeightDp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        repeat(5) { index ->
                            val fraction = index / 4f
                            val y = chartHeightPx * fraction
                            drawLine(
                                color = Color(0x1594A3B8),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.5f
                            )
                        }

                        val maxVolume = volumeMetrics.maxOf { it.value }
                        val volumeScale = if (maxVolume > 0.0) chartHeightPx / maxVolume else 0.0
                        dates.forEachIndexed { index, date ->
                            val left = index * slotWidthPx
                            val value = volumeMap[date] ?: 0.0
                            val barHeight = (value * volumeScale).toFloat()
                            val top = chartHeightPx - barHeight
                            drawRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(slotWidthPx, barHeight)
                            )

                            val label = date.format(DateTimeFormatter.ofPattern("MM/dd"))
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                left + (slotWidthPx / 2f),
                                chartHeightPx + (xLabelHeightPx * 0.75f),
                                Paint().apply {
                                    color = android.graphics.Color.argb(220, 56, 65, 82)
                                    textSize = 24f
                                    textAlign = Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                            )
                        }

                        if (hasBodyweightData) {
                            val bodyweightMin = bodyweightMetrics.minOf { it.value }
                            val bodyweightMax = bodyweightMetrics.maxOf { it.value }
                            val bodyweightRange = (bodyweightMax - bodyweightMin).takeIf { it > 0.0 } ?: 1.0
                            val bodyweightScale = chartHeightPx / bodyweightRange

                            if (confirmedBodyweightPoints.size == 1) {
                                val (date, value) = confirmedBodyweightPoints.first()
                                val index = dateToIndex[date] ?: 0
                                val point = Offset(
                                    x = (index * slotWidthPx) + (slotWidthPx / 2f),
                                    y = chartHeightPx - ((value - bodyweightMin) * bodyweightScale).toFloat()
                                )
                                drawCircle(color = lineColor, radius = 5f, center = point)
                            } else if (confirmedBodyweightPoints.size > 1) {
                                var previousPoint: Offset? = null
                                confirmedBodyweightPoints.forEach { (date, value) ->
                                    val index = dateToIndex[date] ?: return@forEach
                                    val point = Offset(
                                        x = (index * slotWidthPx) + (slotWidthPx / 2f),
                                        y = chartHeightPx - ((value - bodyweightMin) * bodyweightScale).toFloat()
                                    )
                                    previousPoint?.let { start ->
                                        drawLine(color = lineColor, start = start, end = point, strokeWidth = 3f)
                                    }
                                    drawCircle(color = lineColor, radius = 5f, center = point)
                                    previousPoint = point
                                }
                            }
                        }
                    }

                    tooltipState?.let { tooltip ->
                        val tooltipTop = (tooltip.anchorY - tooltipGapPx - tooltipHeightPx).coerceAtLeast(0f)
                        val tooltipLeft = (tooltip.anchorX - tooltipHalfWidthPx).coerceIn(0f, (contentWidthPx - tooltipWidthPx).coerceAtLeast(0f))
                        Card(
                            modifier = Modifier
                                .zIndex(1f)
                                .width(180.dp)
                                .offset { IntOffset(tooltipLeft.roundToInt(), tooltipTop.roundToInt()) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(tooltip.dateText, style = MaterialTheme.typography.labelLarge)
                                Text(tooltip.volumeText, style = MaterialTheme.typography.bodyMedium)
                                tooltip.bodyweightText?.let { text ->
                                    Text(text, style = MaterialTheme.typography.bodyMedium, color = lineColor)
                                }
                            }
                        }
                    }
                }
            }

            if (hasBodyweightData) {
                Column(
                    modifier = Modifier
                        .width(rightAxisWidth)
                        .height(chartHeightDp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    bodyweightLabels.forEach { label ->
                        Text(label, color = lineColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(items: List<Pair<Color, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (color, label) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, shape = MaterialTheme.shapes.small))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun aggregateDailyVolumes(sessions: List<WorkoutSession>): List<DailyMetric> {
    return sessions
        .mapNotNull { session ->
            session.savedAt.toLocalDateOrNull()?.let { date ->
                DailyMetric(date, session.volume)
            }
        }
        .groupBy { it.date }
        .map { (date, metrics) -> DailyMetric(date, metrics.sumOf { it.value }) }
        .sortedBy { it.date }
}

private fun aggregateDailyBodyweights(entries: List<BodyweightEntry>): List<DailyMetric> {
    return entries
        .mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { date ->
                DailyMetric(date, entry.weight)
            }
        }
        .groupBy { it.date }
        .map { (date, metrics) -> DailyMetric(date, metrics.last().value) }
        .sortedBy { it.date }
}

private fun buildRadarMetrics(sessions: List<WorkoutSession>): List<RadarMetric> {
    val totals = linkedMapOf(
        "Pull" to 0.0,
        "Push" to 0.0,
        "Core" to 0.0,
        "Cardio" to 0.0,
        "Legs" to 0.0,
    )

    sessions.forEach { session ->
        session.exercises.forEach { exercise ->
            val category = classifyExercise(exercise.name) ?: return@forEach
            val exerciseVolume = exercise.sets.sumOf { set -> set.reps * set.weight }
            totals[category] = (totals[category] ?: 0.0) + exerciseVolume
        }
    }

    return totals.map { (label, value) -> RadarMetric(label, value) }
}

private fun classifyExercise(name: String): String? {
    val normalized = name.lowercase()
    return when {
        listOf("run", "bike", "row", "cardio", "jump", "burpee", "sled").any { normalized.contains(it) } -> "Cardio"
        listOf("plank", "crunch", "sit up", "situp", "leg raise", "core", "abs", "hollow").any { normalized.contains(it) } -> "Core"
        listOf("squat", "lunge", "deadlift", "leg press", "hamstring", "quad", "calf", "split squat").any { normalized.contains(it) } -> "Legs"
        listOf("bench", "press", "push", "dip", "chest", "shoulder", "tricep").any { normalized.contains(it) } -> "Push"
        listOf("pull", "row", "curl", "lat", "chin", "reverse fly").any { normalized.contains(it) } -> "Pull"
        else -> null
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching {
        Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
}

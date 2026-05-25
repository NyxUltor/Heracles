package com.heracles.mobile.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.BodyweightEntry
import com.heracles.mobile.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class DailyMetric(
    val date: LocalDate,
    val value: Double,
)

private data class RadarMetric(
    val label: String,
    val value: Double,
)

@Composable
fun HeraclesTrackerScreen(viewModel: AppViewModel) {
    val sessions = viewModel.sessions.toList()
    val bodyweights = viewModel.bodyweightHistory.toList()
    val dailyVolumes = aggregateDailyVolumes(sessions)
    val dailyBodyweights = aggregateDailyBodyweights(bodyweights)
    val radarMetrics = buildRadarMetrics(sessions)
    val latestBodyweight = bodyweights.lastOrNull()?.weight?.toString().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
            DualLineChart(
                volumeMetrics = dailyVolumes,
                bodyweightMetrics = dailyBodyweights
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recent history", style = MaterialTheme.typography.titleMedium)
                Text("Sessions: ${sessions.size}")
                Text("Bodyweight entries: ${bodyweights.size}")
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
private fun DualLineChart(
    volumeMetrics: List<DailyMetric>,
    bodyweightMetrics: List<DailyMetric>,
) {
    val dates = (volumeMetrics.map { it.date } + bodyweightMetrics.map { it.date }).distinct().sorted()
    val volumeMap = volumeMetrics.associateBy({ it.date }, { it.value })
    val bodyweightMap = bodyweightMetrics.associateBy({ it.date }, { it.value })
    val volumeValues = dates.map { volumeMap[it] }
    val bodyweightValues = dates.map { bodyweightMap[it] }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendRow(
            listOf(
                Color(0xFF2563EB) to "Volume",
                Color(0xFF14B8A6) to "Bodyweight",
            )
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            val leftPad = 56f
            val rightPad = 20f
            val topPad = 20f
            val bottomPad = 48f
            val chartWidth = size.width - leftPad - rightPad
            val chartHeight = size.height - topPad - bottomPad
            val xStep = if (dates.size <= 1) 0f else chartWidth / (dates.size - 1)
            val volumePaint = Paint().apply {
                color = android.graphics.Color.rgb(37, 99, 235)
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val bodyweightPaint = Paint().apply {
                color = android.graphics.Color.rgb(20, 184, 166)
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val labelPaint = Paint().apply {
                color = android.graphics.Color.argb(220, 56, 65, 82)
                textSize = 24f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            drawLine(
                color = Color(0x2294A3B8),
                start = Offset(leftPad, topPad),
                end = Offset(leftPad, topPad + chartHeight),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x2294A3B8),
                start = Offset(leftPad, topPad + chartHeight),
                end = Offset(leftPad + chartWidth, topPad + chartHeight),
                strokeWidth = 2f
            )

            repeat(3) { index ->
                val fraction = index / 2f
                val y = topPad + (chartHeight * fraction)
                drawLine(
                    color = Color(0x1594A3B8),
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartWidth, y),
                    strokeWidth = 1.5f
                )
            }

            fun normalizedValues(values: List<Double?>): List<Float?> {
                val present = values.filterNotNull()
                if (present.isEmpty()) return values.map { null }
                if (present.size == 1) {
                    return values.map { value -> value?.let { 0.5f } }
                }
                val minValue = present.minOrNull() ?: 0.0
                val maxValue = present.maxOrNull() ?: 1.0
                val padding = ((maxValue - minValue).takeIf { it > 0.0 } ?: maxValue.coerceAtLeast(1.0)) * 0.15
                val low = minValue - padding
                val high = maxValue + padding
                val spread = (high - low).takeIf { it > 0.0 } ?: 1.0
                return values.map { value ->
                    value?.let { ((high - it) / spread).toFloat() }
                }
            }

            fun drawSeries(values: List<Double?>, paint: Paint, color: Color) {
                val normalized = normalizedValues(values)
                var previousPoint: Offset? = null
                normalized.forEachIndexed { index, value ->
                    val x = leftPad + (xStep * index)
                    val labelDate = dates[index].format(DateTimeFormatter.ofPattern("MM/dd"))
                    val labelY = topPad + chartHeight + 24f
                    drawContext.canvas.nativeCanvas.drawText(labelDate, x, labelY, labelPaint)
                    if (value == null) {
                        previousPoint = null
                        return@forEachIndexed
                    }
                    val y = topPad + (value * chartHeight)
                    val point = Offset(x, y)
                    previousPoint?.let { start ->
                        drawLine(color = color, start = start, end = point, strokeWidth = 5f)
                    }
                    drawCircle(color = color, radius = 6f, center = point)
                    previousPoint = point
                }
            }

            drawSeries(volumeValues, volumePaint, Color(0xFF2563EB))
            drawSeries(bodyweightValues, bodyweightPaint, Color(0xFF14B8A6))
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

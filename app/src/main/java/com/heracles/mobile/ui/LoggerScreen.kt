/*
 File: ui/LoggerScreen.kt
 What it does: Renders the workout logger, including the Balanced compose layout and the legacy logger flow.
 Main inputs: active session state, exercises, set values, timing, and fidelity selection from the ViewModel.
 Main outputs: editable workout UI, session completion, start/reset actions, and set/exercise mutations.
 Key functions/classes: `HeraclesLoggerScreen`, `LoggerScreenBalanced`, `WorkoutTab`/`HistoryTab`/`PrsTab`.
*/

package com.heracles.mobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.UiFidelityLevel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// BgColor and PanelColor removed — use MaterialTheme.colorScheme tokens instead
private val PanelSoft = Color.White.copy(alpha = 0.03f)
private val LineColor = Color.White.copy(alpha = 0.06f)
private val LineSoft = Color.White.copy(alpha = 0.04f)
private val TextColor = Color.White
private val MutedColor = Color.White.copy(alpha = 0.62f)
private val MutedSoft = Color.White.copy(alpha = 0.42f)
private val CheckDoneBg = Color.White.copy(alpha = 0.12f)
private val CheckDoneBorder = Color.White.copy(alpha = 0.16f)
private val FinishBg = Color.White
// FinishText removed — use MaterialTheme.colorScheme.onPrimary instead

private val CardShape = RoundedCornerShape(12.dp)
private val InputShape = RoundedCornerShape(8.dp)
private val PillShape = RoundedCornerShape(999.dp)

@Composable
fun LoggerScreenBalanced(viewModel: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val screenBackground = MaterialTheme.colorScheme.background

    val now = LocalDate.now()
    val savedAt = viewModel.currentSessionSavedAt
    val headerTitle = remember(savedAt) {
        if (savedAt.isNullOrBlank()) {
            "Today's session"
        } else {
            val savedDate = runCatching {
                Instant.parse(savedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }.getOrElse { now }
            val daysAgo = ChronoUnit.DAYS.between(savedDate, now)
            if (daysAgo <= 0) "Today's session" else "Session from $daysAgo days ago"
        }
    }
    val headerDate = remember(savedAt) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
        if (savedAt.isNullOrBlank()) {
            now.format(formatter)
        } else {
            runCatching {
                Instant.parse(savedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)
            }.getOrElse { now.format(formatter) }
        }
    }

    LaunchedEffect(viewModel.exercises.size) {
        if (viewModel.exercises.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.exercises.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(screenBackground)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = headerTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextColor,
                )
                Text(
                    text = headerDate,
                    fontSize = 12.sp,
                    color = MutedColor,
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .border(1.dp, LineColor, PillShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { selectedTab = 1 }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Sessions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextColor,
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LineColor))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(screenBackground)
                .padding(horizontal = 20.dp),
        ) {
            listOf("Workout", "Sessions", "PRs").forEachIndexed { index, label ->
                val active = selectedTab == index
                val tabColor by animateColorAsState(
                    targetValue = if (active) TextColor else MutedColor,
                    animationSpec = tween(150),
                    label = "tab_color_$index",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = tabColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (active) TextColor else Color.Transparent)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).background(screenBackground)) {
            when (selectedTab) {
                0 -> WorkoutTab(viewModel, listState)
                1 -> HistoryTab(viewModel)
                2 -> PrsTab(viewModel)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(screenBackground)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Session time", fontSize = 11.sp, color = MutedColor)
                Text(
                    text = viewModel.workoutDuration.ifBlank { "00:00" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.startTimedSession() }) {
                    Text("Start")
                }
                Button(onClick = { viewModel.saveSession() }) {
                    Text("Finish")
                }
            }
        }
    }
}

@Composable
private fun WorkoutTab(
    viewModel: AppViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    var addExerciseQuery by remember { mutableStateOf("") }
    val suggestions = remember(addExerciseQuery, viewModel.sessions.size, viewModel.exercises.size, viewModel.settings.defaultExercises) {
        viewModel.exerciseNameSuggestions(addExerciseQuery)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = !viewModel.scrubberGestureActive,
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    label = "Duration",
                    value = viewModel.workoutDuration.ifBlank { "00:00" },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Volume",
                    value = String.format(Locale.US, "%.0f", viewModel.sessionVolume()),
                    unit = viewModel.settings.units.ifBlank { "kg" },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = addExerciseQuery,
                    onValueChange = { addExerciseQuery = it },
                    placeholder = { Text("Search or add exercise") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    viewModel.addExercise(addExerciseQuery)
                    addExerciseQuery = ""
                }) {
                    Text("Add")
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                ExerciseSuggestionRow(
                    suggestions = suggestions,
                    onPick = { suggestion ->
                        viewModel.addExercise(suggestion)
                        addExerciseQuery = ""
                    }
                )
            }
        }

        item {
            Text(
                text = "EXERCISES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedSoft,
                letterSpacing = 0.08.sp,
            )
        }

        itemsIndexed(viewModel.exercises, key = { _, ex -> ex.id }) { _, exercise ->
            ExerciseCard(viewModel = viewModel, exercise = exercise)
        }

        if (viewModel.exercises.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .dashedBorder(1.dp, LineColor, CardShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MutedColor,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add an exercise to begin logging.",
                            fontSize = 13.sp,
                            color = MutedColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .dashedBorder(1.dp, LineColor, CardShape)
                    .clickable { viewModel.addExercise() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Add exercise", fontSize = 14.sp, color = MutedColor)
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    viewModel: AppViewModel,
    exercise: AppViewModel.ExerciseDraft,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, LineColor, CardShape)
            .background(PanelSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
                OutlinedTextField(
                    value = exercise.name,
                    onValueChange = { viewModel.updateExerciseName(exercise.id, it) },
                    label = { Text("Exercise") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                    ),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.removeExercise(exercise.id) }
                    .padding(6.dp),
            ) {
                Text("✕", fontSize = 16.sp, color = MutedColor)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "SET",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedSoft,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                "WEIGHT",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedSoft,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text(
                "REPS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedSoft,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(32.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        exercise.sets.forEachIndexed { index, set ->
            if (index > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
            }
            SetRow(
                viewModel = viewModel,
                exercise = exercise,
                set = set,
                index = index,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(InputShape)
                .dashedBorder(1.dp, LineColor, InputShape)
                .clickable { viewModel.addSet(exercise.id) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ Add set", fontSize = 12.sp, color = MutedColor)
        }
    }
}

@Composable
private fun SetRow(
    viewModel: AppViewModel,
    exercise: AppViewModel.ExerciseDraft,
    set: AppViewModel.SetDraft,
    index: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}",
            fontSize = 12.sp,
            color = MutedSoft,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .scrubbableNumericField(
                    enabled = true,
                    text = set.weight,
                    sensitivity = viewModel.settings.scrubberSensitivity,
                    decimalPlaces = 1,
                    stepPerTick = 0.5,
                    onScrubStart = viewModel::onScrubberGestureStart,
                    onScrubEnd = viewModel::onScrubberGestureEnd,
                    onValueChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                )
        ) {
            BalancedInput(
                value = set.weight,
                placeholder = set.ghostWeight ?: "0",
                onValueChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                readOnly = !viewModel.settings.numericInputModes.contains("keyboard"),
                keyboardType = KeyboardType.Decimal,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .scrubbableNumericField(
                    enabled = true,
                    text = set.reps,
                    sensitivity = viewModel.settings.scrubberSensitivity,
                    decimalPlaces = 0,
                    onScrubStart = viewModel::onScrubberGestureStart,
                    onScrubEnd = viewModel::onScrubberGestureEnd,
                    onValueChange = { viewModel.updateSetReps(exercise.id, index, it) },
                )
        ) {
            BalancedInput(
                value = set.reps,
                placeholder = set.ghostReps ?: "0",
                onValueChange = { viewModel.updateSetReps(exercise.id, index, it) },
                readOnly = !viewModel.settings.numericInputModes.contains("keyboard"),
                keyboardType = KeyboardType.Number,
            )
        }

        val checkBg by animateColorAsState(
            targetValue = if (set.completed) CheckDoneBg else Color.Transparent,
            animationSpec = tween(150),
            label = "check_bg",
        )
        val checkBorder by animateColorAsState(
            targetValue = if (set.completed) CheckDoneBorder else LineColor,
            animationSpec = tween(150),
            label = "check_border",
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, checkBorder, CircleShape)
                .background(checkBg)
                .clickable { viewModel.toggleSetCompletion(exercise.id, index) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                fontSize = 13.sp,
                color = if (set.completed) TextColor else MutedSoft,
                fontWeight = if (set.completed) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun HistoryTab(viewModel: AppViewModel) {
    val sessions = viewModel.sessions.toList()
    if (sessions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No sessions logged yet.", fontSize = 13.sp, color = MutedColor)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "RECENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedSoft,
            )
        }
        itemsIndexed(sessions, key = { _, session -> session.id }) { _, session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .border(1.dp, LineColor, CardShape)
                    .background(PanelSoft)
                    .clickable { viewModel.restoreFromSession(session) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.savedAt.toFriendlyDate(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextColor,
                    )
                    Text(
                        text = "${session.exercises.size} exercises · ${String.format(Locale.US, "%.0f", session.volume)} ${viewModel.settings.units.ifBlank { "kg" }}",
                        fontSize = 11.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.restoreFromSession(session) }) { Text("Load") }
                    Button(onClick = { viewModel.deleteSession(session) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun PrsTab(viewModel: AppViewModel) {
    val sessionSnapshot = viewModel.sessions.toList()
    val prs = remember(sessionSnapshot) {
        sessionSnapshot
            .asSequence()
            .flatMap { session ->
                session.exercises.asSequence().flatMap { exercise ->
                    exercise.sets.asSequence()
                        .filter { it.weight > 0.0 }
                        .map { set ->
                            exercise.name.trim() to set.weight
                        }
                }
            }
            .groupBy({ it.first.lowercase(Locale.US) }, { it.second })
            .mapNotNull { (key, weights) ->
                val displayName = sessionSnapshot
                    .asSequence()
                    .flatMap { session -> session.exercises.asSequence() }
                    .firstOrNull { it.name.trim().lowercase(Locale.US) == key }
                    ?.name
                    ?.trim()
                    .orEmpty()
                if (displayName.isBlank()) {
                    null
                } else {
                    displayName to (weights.maxOrNull() ?: 0.0)
                }
            }
            .sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first.lowercase(Locale.US) })
            .toList()
    }

    if (prs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Log a loaded set to build your PR list.",
                fontSize = 13.sp,
                color = MutedColor,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Highest lifted weight by exercise",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedSoft,
                letterSpacing = 0.08.sp,
            )
        }
        items(prs) { (exerciseName, bestWeight) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .border(1.dp, LineColor, CardShape)
                    .background(PanelSoft)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exerciseName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextColor,
                )
                Text(
                    text = "${String.format(Locale.US, "%.0f", bestWeight)} ${viewModel.settings.units.ifBlank { "kg" }}",
                    fontSize = 12.sp,
                    color = MutedSoft,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(InputShape)
            .border(1.dp, LineColor, InputShape)
            .background(Color.White.copy(alpha = 0.02f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = MutedColor)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextColor)
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = MutedSoft,
                    modifier = Modifier.padding(start = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseSuggestionRow(
    suggestions: List<String>,
    onPick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(4).forEach { suggestion ->
            OutlinedButton(onClick = { onPick(suggestion) }) {
                Text(suggestion)
            }
        }
    }
}

@Composable
private fun BalancedInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 13.sp,
            color = TextColor,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(TextColor),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(InputShape)
                    .border(1.dp, LineColor, InputShape)
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.28f),
                        textAlign = TextAlign.Center,
                    )
                }
                inner()
            }
        },
    )
}

private fun Modifier.dashedBorder(
    width: androidx.compose.ui.unit.Dp,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
): Modifier = this.border(width, color.copy(alpha = color.alpha * 0.6f), shape)

private fun String.toFriendlyDate(): String {
    return runCatching {
        val date = Instant.parse(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val now = LocalDate.now()
        val days = ChronoUnit.DAYS.between(date, now)
        when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }
    }.getOrElse { this }
}
@Composable
fun HeraclesLoggerScreen(viewModel: AppViewModel) {
    // If user selected RICH fidelity, render the Rich layout
    if (viewModel.settings.uiFidelity == UiFidelityLevel.RICH) {
        LoggerScreenRich(viewModel)
        return
    }

    // If user selected BALANCED fidelity, render the mockup-accurate balanced layout
    if (viewModel.settings.uiFidelity == UiFidelityLevel.BALANCED) {
        LoggerScreenBalanced(viewModel)
        return
    }

    val activeModName = viewModel.activeThemeMod()?.name ?: "Bare Metal"
    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Today’s session", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                        Text(todayLabel, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        activeModName,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    "Fast local logging with live volume, scrubber entry, and pre-built session imports.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SessionMetricCard(
                        icon = Icons.Default.Timer,
                        label = "Duration",
                        value = viewModel.workoutDuration.ifBlank { "00:00" },
                        unit = "",
                        modifier = Modifier.weight(1f)
                    )
                    SessionMetricCard(
                        icon = Icons.Default.FitnessCenter,
                        label = "Volume",
                        value = String.format(Locale.US, "%.0f", viewModel.sessionVolume()),
                        unit = viewModel.settings.units.ifBlank { "kg" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Workout", "Sessions", "PRs").forEachIndexed { index, label ->
                Button(
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (selectedTab == 1) {
            HistoryTab(viewModel)
            return@Column
        }

        if (selectedTab == 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Coming soon", color = MutedColor)
            }
            return@Column
        }

        Text("Exercises", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fieldGap = 8.dp
            val bodyWeightWidth = (maxWidth - fieldGap) * 0.43f
            val durationWidth = (maxWidth - fieldGap) * 0.57f

            Row(horizontalArrangement = Arrangement.spacedBy(fieldGap), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(bodyWeightWidth)
                        .scrubbableNumericField(
                            enabled = true,
                            text = viewModel.bodyWeight,
                            sensitivity = viewModel.settings.scrubberSensitivity,
                            decimalPlaces = 1,
                            onScrubStart = viewModel::onScrubberGestureStart,
                            onScrubEnd = viewModel::onScrubberGestureEnd,
                            onValueChange = viewModel::updateBodyWeight,
                        )
                ) {
                    OutlinedTextField(
                        value = viewModel.bodyWeight,
                        onValueChange = { viewModel.updateBodyWeight(it) },
                        label = { Text("Bodyweight") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .width(durationWidth)
                        .scrubbableNumericField(
                            enabled = true,
                            text = viewModel.workoutDuration,
                            sensitivity = viewModel.settings.scrubberSensitivity,
                            decimalPlaces = 0,
                            onScrubStart = viewModel::onScrubberGestureStart,
                            onScrubEnd = viewModel::onScrubberGestureEnd,
                            onValueChange = viewModel::updateWorkoutDuration,
                        )
                ) {
                    OutlinedTextField(
                        value = viewModel.workoutDuration,
                        onValueChange = { viewModel.updateWorkoutDuration(it) },
                        label = { Text("Duration (minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.pendingExerciseName,
                onValueChange = { viewModel.updatePendingExerciseName(it) },
                placeholder = { Text("New Exercise") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { viewModel.addPendingExercise() }) {
                Text("Add")
            }
        }

        val quickSuggestions = remember(
            viewModel.pendingExerciseName,
            viewModel.sessions.size,
            viewModel.exercises.size,
            viewModel.settings.defaultExercises,
        ) {
            viewModel.exerciseNameSuggestions(viewModel.pendingExerciseName)
        }
        if (quickSuggestions.isNotEmpty()) {
            ExerciseSuggestionRow(
                suggestions = quickSuggestions,
                onPick = { suggestion ->
                    viewModel.updatePendingExerciseName(suggestion)
                    viewModel.addPendingExercise()
                }
            )
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            userScrollEnabled = !viewModel.scrubberGestureActive
        ) {
            items(viewModel.exercises, key = { it.id }) { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = exercise.name,
                                    onValueChange = { viewModel.updateExerciseName(exercise.id, it) },
                                    label = { Text("Exercise") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        capitalization = KeyboardCapitalization.Words,
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(onClick = { viewModel.removeExercise(exercise.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove exercise")
                            }
                        }

                        Text("Set | kg | reps", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                        exercise.sets.forEachIndexed { index, set ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .scrubbableNumericField(
                                            enabled = true,
                                            text = set.reps,
                                            sensitivity = viewModel.settings.scrubberSensitivity,
                                            decimalPlaces = 0,
                                            onScrubStart = viewModel::onScrubberGestureStart,
                                            onScrubEnd = viewModel::onScrubberGestureEnd,
                                            onValueChange = { viewModel.updateSetReps(exercise.id, index, it) },
                                        )
                                ) {
                                    val repsPlaceholder: (@Composable () -> Unit)? = if (set.ghostReps.isNullOrBlank()) null else {
                                        { Text(set.ghostReps) }
                                    }
                                    OutlinedTextField(
                                        value = set.reps,
                                        onValueChange = { viewModel.updateSetReps(exercise.id, index, it) },
                                        label = { Text("Reps") },
                                        placeholder = repsPlaceholder,
                                        readOnly = !viewModel.settings.numericInputModes.contains("keyboard"),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .scrubbableNumericField(
                                            enabled = true,
                                            text = set.weight,
                                            sensitivity = viewModel.settings.scrubberSensitivity,
                                            decimalPlaces = 1,
                                            stepPerTick = 0.5,
                                            onScrubStart = viewModel::onScrubberGestureStart,
                                            onScrubEnd = viewModel::onScrubberGestureEnd,
                                            onValueChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                                        )
                                ) {
                                    val weightPlaceholder: (@Composable () -> Unit)? = if (set.ghostWeight.isNullOrBlank()) null else {
                                        { Text(set.ghostWeight) }
                                    }
                                    OutlinedTextField(
                                        value = set.weight,
                                        onValueChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                                        label = { Text("Weight") },
                                        placeholder = weightPlaceholder,
                                        readOnly = !viewModel.settings.numericInputModes.contains("keyboard"),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Button(onClick = { viewModel.addSet(exercise.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add set")
                        }
                    }
                }
            }
        }

        LaunchedEffect(key1 = viewModel.exercises.size) {
            if (viewModel.exercises.isNotEmpty()) {
                listState.animateScrollToItem(viewModel.exercises.size - 1)
            }
        }

        val sessionVolumeState = remember { derivedStateOf { viewModel.sessionVolume() } }
        val volumeUnit = viewModel.settings.units.ifBlank { "kg" }
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Session volume", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Text(
                        "${String.format(Locale.US, "%.2f", sessionVolumeState.value)} $volumeUnit",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (viewModel.isTimerRunning) {
                            viewModel.stopWorkoutTimer()
                        } else {
                            viewModel.beginWorkoutTimer()
                        }
                    }) { Text(if (viewModel.isTimerRunning) "Pause" else "Start", maxLines = 1) }
                    Button(onClick = { viewModel.saveSession() }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) { Text("Save workout", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@Composable
private fun SessionMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null)
                Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            Text(value, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            if (unit.isNotBlank()) {
                Text(unit, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    }
}

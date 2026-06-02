package com.heracles.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heracles.mobile.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RichTextSecondary = Color(0xCCFFFFFF)
private val RichTextTertiary = Color(0x88FFFFFF)

@Composable
fun LoggerScreenRich(viewModel: AppViewModel) {
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    var workoutFinished by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val timerDisplay = viewModel.workoutDuration.ifBlank { "00:00" }

    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }
    val suggestions = remember(searchQuery, viewModel.sessions.size, viewModel.exercises.size, viewModel.settings.defaultExercises) {
        viewModel.exerciseNameSuggestions(searchQuery)
    }
    val accentPrimary = RichTokens.accentPrimary()
    val accentSecondary = RichTokens.accentSecondary()
    val accentGlow = RichTokens.accentGlow()
    val borderGlow = RichTokens.borderGlow()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RichTokens.BackgroundBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentPrimary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.5f, 0f),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(glowColor = accentGlow)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Active session", fontSize = 10.sp, color = RichTextTertiary)
                            Text("Today's session", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = RichTokens.TextPrimary)
                            Text(todayLabel, fontSize = 11.sp, color = RichTextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .background(accentGlow, RoundedCornerShape(20.dp))
                                .border(1.dp, borderGlow, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Rich", fontSize = 11.sp, color = accentPrimary)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RichStatCard(label = "Duration", value = timerDisplay, modifier = Modifier.weight(1f))
                        RichStatCard(label = "Volume", value = "${String.format(Locale.US, "%.0f", viewModel.sessionVolume())} kg", modifier = Modifier.weight(1f))
                    }
                }
            }

            RichTabBar(selectedTab = selectedTab, onSelectedTabChange = { selectedTab = it })

            when (selectedTab) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .glassCard(cornerRadius = 14.dp, glowColor = accentGlow)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = RichTextTertiary, modifier = Modifier.size(16.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(color = RichTokens.TextPrimary, fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search or add exercise...", fontSize = 13.sp, color = RichTextTertiary)
                                    }
                                    inner()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                viewModel.addExercise(searchQuery)
                                searchQuery = ""
                                haptic()
                                scope.launch {
                                    delay(100)
                                    listState.animateScrollToItem((viewModel.exercises.size - 1).coerceAtLeast(0))
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add exercise", tint = accentPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (suggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.take(3).forEach { suggestion ->
                                OutlinedButton(onClick = {
                                    viewModel.addExercise(suggestion)
                                    searchQuery = ""
                                }) {
                                    Text(suggestion)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = !viewModel.scrubberGestureActive
                    ) {
                        if (viewModel.exercises.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .richDashedBorder(RichTokens.BorderSubtle, 14.dp)
                                        .padding(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = RichTextTertiary, modifier = Modifier.size(18.dp))
                                        Text("No exercises yet", fontSize = 13.sp, color = RichTokens.TextPrimary)
                                        Text("Use the search bar above to add one.", fontSize = 12.sp, color = RichTextTertiary)
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("Exercises", fontSize = 10.sp, color = RichTextTertiary, letterSpacing = 0.1.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            items(viewModel.exercises, key = { it.id }) { exercise ->
                                RichExerciseCard(exercise = exercise, viewModel = viewModel, haptic = haptic)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 0.dp, glowColor = accentGlow)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Session time", fontSize = 10.sp, color = RichTextTertiary)
                                Text(timerDisplay, fontSize = 18.sp, fontWeight = FontWeight.Light, color = RichTokens.TextPrimary, letterSpacing = 1.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        haptic()
                                        if (viewModel.isTimerRunning) {
                                            viewModel.stopWorkoutTimer()
                                        } else {
                                            viewModel.beginWorkoutTimer()
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, RichTokens.BorderSubtle),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RichTextSecondary)
                                ) {
                                    Text(if (viewModel.isTimerRunning) "Pause" else "Start")
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Brush.linearGradient(listOf(accentPrimary, accentSecondary)))
                                        .clickable {
                                            haptic()
                                            workoutFinished = true
                                            viewModel.saveSession()
                                        }
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Text("Finish", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    RichSessionsTab(viewModel)
                }
                else -> {
                    RichPrsTab()
                }
            }
        }

        if (workoutFinished) {
            ParticleBurst(trigger = workoutFinished, modifier = Modifier.fillMaxSize())
            LaunchedEffect(Unit) {
                delay(1500)
                workoutFinished = false
            }
        }
    }
}

@Composable
private fun RichStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val accentPrimary = RichTokens.accentPrimary()
    Box(
        modifier = modifier
            .glassCard(cornerRadius = 14.dp, glowColor = Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(accentPrimary, CircleShape))
                Text(label, fontSize = 10.sp, color = RichTextTertiary, letterSpacing = 0.08.sp)
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Light, color = RichTokens.TextPrimary, letterSpacing = (-0.5).sp)
        }
    }
}

@Composable
private fun RichTabBar(
    selectedTab: Int,
    onSelectedTabChange: (Int) -> Unit,
) {
    val tabs = listOf("Workout", "Sessions", "PRs")
    val accentPrimary = RichTokens.accentPrimary()
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        tabs.forEachIndexed { index, tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectedTabChange(index) }
                    .padding(vertical = 10.dp)
                    .drawBehind {
                        if (selectedTab == index) {
                            drawLine(
                                brush = Brush.horizontalGradient(listOf(Color.Transparent, accentPrimary, Color.Transparent)),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 2f
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab,
                    fontSize = 12.sp,
                    color = if (selectedTab == index) RichTokens.TextPrimary else RichTextTertiary,
                    fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal,
                    letterSpacing = 0.02.sp
                )
            }
        }
    }
}

@Composable
private fun RichSessionsTab(viewModel: AppViewModel) {
    val sessions = viewModel.sessions.toList()
    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No sessions logged yet.", fontSize = 13.sp, color = RichTextTertiary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Recent sessions", fontSize = 10.sp, color = RichTextTertiary)
        }
        items(sessions, key = { it.id }) { session ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(glowColor = RichTokens.accentGlow())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(session.savedAt.toString(), fontSize = 12.sp, color = RichTokens.TextPrimary)
                Text("${session.exercises.size} exercises • ${String.format(Locale.US, "%.0f", session.volume)} kg", fontSize = 11.sp, color = RichTextTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.restoreFromSession(session) }) { Text("Load") }
                    OutlinedButton(onClick = { viewModel.deleteSession(session) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun RichPrsTab() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Coming soon", fontSize = 13.sp, color = RichTextTertiary)
    }
}

@Composable
private fun RichExerciseCard(
    exercise: AppViewModel.ExerciseDraft,
    viewModel: AppViewModel,
    haptic: () -> Unit,
) {
    val cardScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val accentGlow = RichTokens.accentGlow()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
            }
            .glassCard(glowColor = accentGlow)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = exercise.name,
                    onValueChange = { viewModel.updateExerciseName(exercise.id, it) },
                    textStyle = TextStyle(color = RichTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(RichTokens.SurfaceGlass, CircleShape)
                        .border(1.dp, RichTokens.BorderSubtle, CircleShape)
                        .clickable { viewModel.removeExercise(exercise.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = RichTokens.TextTertiary, modifier = Modifier.size(12.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("SET", fontSize = 9.sp, color = RichTextTertiary, letterSpacing = 0.1.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("KG", fontSize = 9.sp, color = RichTextTertiary, letterSpacing = 0.1.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("REPS", fontSize = 9.sp, color = RichTextTertiary, letterSpacing = 0.1.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(28.dp))
            }

            exercise.sets.forEachIndexed { index, set ->
                RichSetRow(
                    viewModel = viewModel,
                    setNumber = index + 1,
                    set = set,
                    onWeightChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                    onRepsChange = { viewModel.updateSetReps(exercise.id, index, it) },
                    onToggle = {
                        viewModel.toggleSetCompletion(exercise.id, index)
                        haptic()
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, RichTokens.BorderSubtle, RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.addSet(exercise.id)
                        haptic()
                        scope.launch {
                            cardScale.animateTo(0.98f, spring(stiffness = RichTokens.SpringStiffness))
                            cardScale.animateTo(1f, spring(stiffness = RichTokens.SpringStiffness, dampingRatio = RichTokens.SpringDamping))
                        }
                    }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = RichTokens.TextTertiary, modifier = Modifier.size(13.dp))
                    Text("+ Add set", fontSize = 12.sp, color = RichTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RichSetRow(
    viewModel: AppViewModel,
    setNumber: Int,
    set: AppViewModel.SetDraft,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggle: () -> Unit,
) {
    val checkScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val accentPrimary = RichTokens.accentPrimary()
    val accentGlow = RichTokens.accentGlow()
    val borderGlow = RichTokens.borderGlow()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$setNumber", fontSize = 11.sp, color = RichTokens.TextTertiary, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)

        Box(
            modifier = Modifier
                .weight(1f)
                .background(RichTokens.SurfaceGlass, RoundedCornerShape(10.dp))
                .border(1.dp, RichTokens.BorderSubtle, RoundedCornerShape(10.dp))
                .scrubbableNumericField(
                    enabled = viewModel.settings.numericInputModes.contains("scrubber"),
                    text = set.weight,
                    sensitivity = viewModel.settings.scrubberSensitivity,
                    decimalPlaces = 1,
                    stepPerTick = 0.5,
                    onScrubStart = viewModel::onScrubberGestureStart,
                    onScrubEnd = viewModel::onScrubberGestureEnd,
                    onValueChange = onWeightChange
                )
                .padding(vertical = 6.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = set.weight,
                onValueChange = onWeightChange,
                textStyle = TextStyle(color = RichTokens.TextPrimary, fontSize = 13.sp, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(RichTokens.TextPrimary),
                decorationBox = { inner ->
                    if (set.weight.isEmpty()) {
                        Text(set.ghostWeight ?: "0", fontSize = 13.sp, color = RichTextTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    inner()
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(RichTokens.SurfaceGlass, RoundedCornerShape(10.dp))
                .border(1.dp, RichTokens.BorderSubtle, RoundedCornerShape(10.dp))
                .scrubbableNumericField(
                    enabled = viewModel.settings.numericInputModes.contains("scrubber"),
                    text = set.reps,
                    sensitivity = viewModel.settings.scrubberSensitivity,
                    decimalPlaces = 0,
                    onScrubStart = viewModel::onScrubberGestureStart,
                    onScrubEnd = viewModel::onScrubberGestureEnd,
                    onValueChange = onRepsChange
                )
                .padding(vertical = 6.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = set.reps,
                onValueChange = onRepsChange,
                textStyle = TextStyle(color = RichTokens.TextPrimary, fontSize = 13.sp, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(RichTokens.TextPrimary),
                decorationBox = { inner ->
                    if (set.reps.isEmpty()) {
                        Text(set.ghostReps ?: "0", fontSize = 13.sp, color = RichTextTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    inner()
                }
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                }
                .background(if (set.completed) accentGlow else RichTokens.SurfaceGlass, CircleShape)
                .border(1.dp, if (set.completed) borderGlow else RichTokens.BorderSubtle, CircleShape)
                .clickable {
                    onToggle()
                    scope.launch {
                        checkScale.animateTo(0.75f, spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh))
                        checkScale.animateTo(1.15f, spring(stiffness = RichTokens.SpringStiffness, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))
                        checkScale.animateTo(1f, spring(stiffness = RichTokens.SpringStiffness))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (set.completed) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = accentPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun Modifier.richDashedBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.dp,
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
    )
}
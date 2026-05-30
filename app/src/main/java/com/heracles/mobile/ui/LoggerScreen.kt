package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel
import com.heracles.mobile.model.UiFidelityLevel
import androidx.compose.foundation.background
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun LoggerScreenBalanced(viewModel: AppViewModel) {
    // Load the exact HTML mockup from assets into a WebView for pixel-accurate rendering.
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            // ensure the WebView background is opaque so app wallpaper doesn't show through
            setBackgroundColor(android.graphics.Color.parseColor("#111317"))
            setOnLongClickListener { true }

            // JS bridge to forward UI actions back into the ViewModel
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun startSession() {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        viewModel.startNewSession()
                        sendAppData(this@apply, viewModel)
                        // signal the page to start its local timer
                        this@apply.evaluateJavascript("(function(){ if(window.onAndroidStart) window.onAndroidStart(); })();", null)
                    }
                }

                @android.webkit.JavascriptInterface
                fun finishSession() {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        viewModel.saveSession()
                        sendAppData(this@apply, viewModel)
                        // signal the page to stop its local timer
                        this@apply.evaluateJavascript("(function(){ if(window.onAndroidFinish) window.onAndroidFinish(); })();", null)
                    }
                }

                @android.webkit.JavascriptInterface
                fun addExercise(name: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        viewModel.addExercise(name ?: "")
                        sendAppData(this@apply, viewModel)
                    }
                }

                @android.webkit.JavascriptInterface
                fun addSet(exIndex: Int) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val list = viewModel.exercises
                        if (exIndex in 0 until list.size) {
                            viewModel.addSet(list[exIndex].id)
                            sendAppData(this@apply, viewModel)
                        }
                    }
                }

                @android.webkit.JavascriptInterface
                fun updateSetReps(exIndex: Int, setIndex: Int, value: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val list = viewModel.exercises
                        if (exIndex in 0 until list.size) {
                            viewModel.updateSetReps(list[exIndex].id, setIndex, value.orEmpty())
                            sendAppData(this@apply, viewModel)
                        }
                    }
                }

                @android.webkit.JavascriptInterface
                fun updateSetWeight(exIndex: Int, setIndex: Int, value: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val list = viewModel.exercises
                        if (exIndex in 0 until list.size) {
                            viewModel.updateSetWeight(list[exIndex].id, setIndex, value.orEmpty())
                            sendAppData(this@apply, viewModel)
                        }
                    }
                }

                @android.webkit.JavascriptInterface
                fun removeExercise(exIndex: Int) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val list = viewModel.exercises
                        if (exIndex in 0 until list.size) {
                            viewModel.removeExercise(list[exIndex].id)
                            sendAppData(this@apply, viewModel)
                        }
                    }
                }

                @android.webkit.JavascriptInterface
                fun toggleSet(exIndex: Int, setIndex: Int) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val list = viewModel.exercises
                        if (exIndex in 0 until list.size) {
                            viewModel.toggleSetCompletion(list[exIndex].id, setIndex)
                            sendAppData(this@apply, viewModel)
                        }
                    }
                }
            }, "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Push current app state into the page
                    this@apply.post { sendAppData(this@apply, viewModel) }
                }
            }

                loadUrl("file:///android_asset/gym_session_logger.html")
            }
        }, modifier = Modifier.fillMaxSize())
    }
}

    // helper to serialize app state and send to the WebView's page
    private fun sendAppData(webView: WebView, viewModel: AppViewModel) {
        try {
            val root = org.json.JSONObject()
            val exercisesArr = org.json.JSONArray()
            viewModel.exercises.forEach { ex ->
                val exObj = org.json.JSONObject()
                exObj.put("id", ex.id)
                exObj.put("name", ex.name)
                exObj.put("muscle", "")
                val setsArr = org.json.JSONArray()
                ex.sets.forEach { s ->
                    val sObj = org.json.JSONObject()
                    sObj.put("weight", s.weight)
                    sObj.put("reps", s.reps)
                    sObj.put("completed", s.completed)
                    setsArr.put(sObj)
                }
                exObj.put("sets", setsArr)
                exercisesArr.put(exObj)
            }
            val now = LocalDate.now()
            val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
            val savedAt = viewModel.currentSessionSavedAt
            val headerTitle = if (savedAt.isNullOrBlank()) {
                "Today's session"
            } else {
                val savedDate = runCatching {
                    Instant.parse(savedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }.getOrElse { now }
                val daysAgo = ChronoUnit.DAYS.between(savedDate, now)
                if (daysAgo <= 0) "Today's session" else "Session from $daysAgo days ago"
            }
            val headerDate = if (savedAt.isNullOrBlank()) {
                now.format(headerDateFormatter)
            } else {
                runCatching {
                    Instant.parse(savedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(headerDateFormatter)
                }.getOrElse { now.format(headerDateFormatter) }
            }
            root.put("exercises", exercisesArr)
            root.put("historyExists", viewModel.sessions.isNotEmpty())
            root.put("workoutDuration", viewModel.workoutDuration.ifBlank { "00:00" })
            root.put("sessionTitle", headerTitle)
            root.put("sessionDate", headerDate)
            root.put("sessionVolume", viewModel.sessionVolume())
            root.put("sessionLoaded", viewModel.currentSessionId != null && !savedAt.isNullOrBlank())
            root.put("sessionSavedAt", savedAt)
            root.put("canStartSession", true)
            root.put("canFinishSession", viewModel.exercises.isNotEmpty())

            val jsonStr = root.toString()
            // call page JS handler
            val safe = org.json.JSONObject.quote(jsonStr)
            webView.evaluateJavascript("(function(){ if(window.receiveAppData) { window.receiveAppData($safe); } })();") { }
        } catch (e: Exception) {
            // swallow
        }
    }
@Composable
fun HeraclesLoggerScreen(viewModel: AppViewModel) {
    // If user selected BALANCED fidelity, render the mockup-accurate balanced layout
    if (viewModel.settings.uiFidelity == UiFidelityLevel.BALANCED) {
        LoggerScreenBalanced(viewModel)
        return
    }

    val activeModName = viewModel.activeThemeMod()?.name ?: "Bare Metal"
    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }

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
                        value = viewModel.workoutDuration.ifBlank { "0" },
                        unit = "m",
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
                            enabled = viewModel.settings.numericInputModes.contains("scrubber"),
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
                            enabled = viewModel.settings.numericInputModes.contains("scrubber"),
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
                                            enabled = viewModel.settings.numericInputModes.contains("scrubber"),
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
                                            enabled = viewModel.settings.numericInputModes.contains("scrubber"),
                                            text = set.weight,
                                            sensitivity = viewModel.settings.scrubberSensitivity,
                                            decimalPlaces = 1,
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
                    Button(onClick = { viewModel.addExercise() }) { Text("Add exercise") }
                    Button(onClick = { viewModel.saveSession() }) { Text("Save workout") }
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
            Text(unit, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }
}

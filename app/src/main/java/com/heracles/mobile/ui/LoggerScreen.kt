package com.heracles.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heracles.mobile.AppViewModel

@Composable
fun HeraclesLoggerScreen(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            // allow keyboard entry for bodyweight even when keyboard mode is off
                            readOnly = false,
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
                            // allow keyboard entry for duration even when keyboard mode is off
                            readOnly = false,
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
                label = { Text("New Exercise") },
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exercise.name,
                            onValueChange = { viewModel.updateExerciseName(exercise.id, it) },
                            label = { Text("Exercise name") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                    OutlinedTextField(
                                        value = set.reps,
                                        onValueChange = { viewModel.updateSetReps(exercise.id, index, it) },
                                            label = { Text("Reps") },
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
                                    OutlinedTextField(
                                        value = set.weight,
                                        onValueChange = { viewModel.updateSetWeight(exercise.id, index, it) },
                                            label = { Text("Weight") },
                                            readOnly = !viewModel.settings.numericInputModes.contains("keyboard"),
                                            modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.addSet(exercise.id) }) { Text("Add set") }
                            Button(onClick = { viewModel.removeExercise(exercise.id) }) { Text("Remove") }
                        }
                    }
                }
            }
        }

        // auto-scroll when a new exercise is appended
        LaunchedEffect(key1 = viewModel.exercises.size) {
            if (viewModel.exercises.isNotEmpty()) {
                listState.animateScrollToItem(viewModel.exercises.size - 1)
            }
        }

        Text("Session volume: ${String.format(java.util.Locale.US, "%.2f", viewModel.sessionVolume())}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.addExercise() }) { Text("Add exercise") }
            Button(onClick = { viewModel.saveSession() }) { Text("Save workout") }
        }
    }
}
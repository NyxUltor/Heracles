package com.heracles.mobile.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class WorkoutSet(
    val reps: Int = 0,
    val weight: Double = 0.0,
)

@Serializable
data class ExerciseEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<WorkoutSet> = emptyList(),
)

@Serializable
data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val savedAt: String = Instant.now().toString(),
    val bodyWeight: String? = null,
    val workoutDuration: String? = null,
    val exercises: List<ExerciseEntry> = emptyList(),
    val volume: Double = 0.0,
)

@Serializable
data class BodyweightEntry(
    val date: String,
    val weight: Double,
)

@Serializable
data class AppSettings(
    val units: String = "kg",
    val defaultExercises: List<String> = emptyList(),
    val restoreLatestOnOpen: Boolean = true,
    val useDarkTheme: Boolean = false,
    val logStoragePath: String = "sessions",
    val numericInputModes: Set<String> = setOf("keyboard"),
    val scrubberSensitivity: Double = 1.0,
)

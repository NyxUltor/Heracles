package com.heracles.mobile.logic

import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.PrebuiltWorkoutExercise
import com.heracles.mobile.model.PrebuiltWorkoutSession
import com.heracles.mobile.model.PrebuiltWorkoutSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

fun calculateVolume(exercises: List<ExerciseEntry>): Double {
    return exercises.sumOf { exercise ->
        exercise.sets.sumOf { set ->
            set.reps * set.weight
        }
    }
}

fun buildSessionFilename(volume: Double, sessionIndex: Int, date: LocalDate = LocalDate.now()): String {
    val datePart = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
    val volumeK = (volume / 1000.0).toInt()
    return "$datePart-$sessionIndex-${volumeK}k.json"
}

data class PrebuiltWorkoutParseResult(
    val session: PrebuiltWorkoutSession? = null,
    val error: String? = null,
)

fun parsePrebuiltWorkoutTemplate(sourceText: String): PrebuiltWorkoutParseResult {
    val lines = sourceText.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()

    if (lines.isEmpty()) {
        return PrebuiltWorkoutParseResult(error = "Paste a workout template before importing.")
    }

    var title = "Imported workout"
    var bodyWeight: String? = null
    var workoutDuration: String? = null
    val exercises = mutableListOf<PrebuiltWorkoutExercise>()

    var currentExerciseName: String? = null
    var currentSets = mutableListOf<PrebuiltWorkoutSet>()

    fun finishExercise() {
        val name = currentExerciseName
        if (!name.isNullOrBlank()) {
            exercises.add(
                PrebuiltWorkoutExercise(
                    name = name,
                    sets = currentSets.toList(),
                )
            )
        }
        currentExerciseName = null
        currentSets = mutableListOf()
    }

    lines.forEachIndexed { index, rawLine ->
        when {
            rawLine.startsWith("Workout:", ignoreCase = true) -> {
                title = rawLine.substringAfter(":", "").trim().ifBlank { title }
            }

            rawLine.startsWith("Bodyweight:", ignoreCase = true) -> {
                bodyWeight = rawLine.substringAfter(":", "").trim().ifBlank { null }
            }

            rawLine.startsWith("Duration:", ignoreCase = true) -> {
                workoutDuration = rawLine.substringAfter(":", "").trim().ifBlank { null }
            }

            rawLine.startsWith("Exercise:", ignoreCase = true) -> {
                finishExercise()
                currentExerciseName = rawLine.substringAfter(":", "").trim().ifBlank {
                    return PrebuiltWorkoutParseResult(error = "Exercise name is missing on line ${index + 1}.")
                }
            }

            rawLine.startsWith("Set:", ignoreCase = true) -> {
                if (currentExerciseName.isNullOrBlank()) {
                    return PrebuiltWorkoutParseResult(error = "Set found before any exercise on line ${index + 1}.")
                }

                val setText = rawLine.substringAfter(":", "").trim()
                val normalized = setText
                    .replace(Regex("\\breps?\\b", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\bkg\\b", RegexOption.IGNORE_CASE), "")
                    .replace("@", "x")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val parts = normalized.split("x").map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size < 2) {
                    return PrebuiltWorkoutParseResult(error = "Set line must look like 'Set: 8 x 60' on line ${index + 1}.")
                }

                val reps = parts[0]
                val weight = parts[1]
                if (reps.toIntOrNull() == null) {
                    return PrebuiltWorkoutParseResult(error = "Reps must be a whole number on line ${index + 1}.")
                }
                if (weight.toDoubleOrNull() == null) {
                    return PrebuiltWorkoutParseResult(error = "Weight must be numeric on line ${index + 1}.")
                }

                currentSets.add(PrebuiltWorkoutSet(reps = reps, weight = weight))
            }

            else -> {
                return PrebuiltWorkoutParseResult(error = "Unknown line format on line ${index + 1}: '$rawLine'.")
            }
        }
    }

    finishExercise()

    if (exercises.isEmpty()) {
        return PrebuiltWorkoutParseResult(error = "Add at least one exercise with one or more sets.")
    }

    return PrebuiltWorkoutParseResult(
        session = PrebuiltWorkoutSession(
            id = UUID.randomUUID().toString(),
            title = title,
            sourceText = sourceText,
            bodyWeight = bodyWeight,
            workoutDuration = workoutDuration,
            exercises = exercises,
        )
    )
}

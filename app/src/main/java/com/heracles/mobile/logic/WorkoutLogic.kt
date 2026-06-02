/*
 File: logic/WorkoutLogic.kt
 What it does: Contains core workout calculation utilities (volume, totals) and the fault-tolerant pre-built session parser.
 Main inputs: structured session/exercise data (reps, weight, sets) and raw template text strings.
 Main outputs: numeric results such as session volume and parsed PrebuiltWorkoutSession objects.
 Key functions/classes: `calculateVolume`, `buildSessionFilename`, `parsePrebuiltWorkoutTemplate`.
*/

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

private val SET_PATTERN = Regex("""(\d+)\s*[xX*@]\s*(\d+(?:\.\d+)?)""")

private fun isValidExerciseName(line: String): Boolean {
    val cleaned = line.substringAfter(":", line).trim()
    if (cleaned.length < 2) return false
    val digitCount = cleaned.count { it.isDigit() }
    val digitRatio = digitCount.toDouble() / cleaned.length.toDouble()
    return digitRatio <= 0.5
}

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
    val exercises = mutableListOf<PrebuiltWorkoutExercise>()

    var currentExerciseName: String? = null
    var currentSets = mutableListOf<PrebuiltWorkoutSet>()

    fun finishExercise() {
        val name = currentExerciseName
        if (!name.isNullOrBlank() && currentSets.isNotEmpty()) {
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

    val knownPrefixes = listOf(
        "workout:", "bodyweight:", "exercise:", "set:"
    )

    lines.forEach { rawLine ->
        val lineLower = rawLine.lowercase()

        when {
            lineLower.startsWith("workout:") -> {
                title = rawLine.substringAfter(":", "").trim().ifBlank { title }
            }

            lineLower.startsWith("bodyweight:") -> {
                bodyWeight = rawLine.substringAfter(":", "").trim().ifBlank { null }
            }

            lineLower.startsWith("exercise:") -> {
                finishExercise()
                val name = rawLine.substringAfter(":", "").trim()
                if (name.isNotBlank()) {
                    currentExerciseName = name
                }
                // blank name after "Exercise:" — skip silently
            }

            else -> {
                // Priority 1: Try to match as a set line using regex
                // Catches: "Set: 8 x 60", "8x60", "8 @ 60", "5 * 100", "8 reps 60kg", "Set: 8reps @ 60kg"
                val setMatch = SET_PATTERN.find(rawLine)
                if (setMatch != null) {
                    val repsToken = setMatch.groupValues[1]
                    val weightToken = setMatch.groupValues[2]
                    // Guard: set arrived before any exercise was declared
                    if (currentExerciseName.isNullOrBlank()) {
                        currentExerciseName = "Uncategorized Exercise"
                    }
                    currentSets.add(PrebuiltWorkoutSet(reps = repsToken, weight = weightToken))
                    return@forEach
                }

                // Priority 2: Try to treat as an exercise name without the "Exercise:" prefix
                // Only valid if line passes name guard and is not a known prefix
                val hasKnownPrefix = knownPrefixes.any { lineLower.startsWith(it) }
                if (!hasKnownPrefix && isValidExerciseName(rawLine)) {
                    // Start a new exercise group only if:
                    // - no current exercise exists, OR
                    // - current exercise already has sets committed (new block starting)
                    if (currentExerciseName.isNullOrBlank() || currentSets.isNotEmpty()) {
                        finishExercise()
                        currentExerciseName = rawLine.trim()
                    }
                    // If current exercise has no sets yet — likely a stray line, skip silently
                    return@forEach
                }

                // Priority 3: Unknown/garbage line — skip silently, never crash
            }
        }
    }

    // Commit the final exercise buffer
    finishExercise()

    if (exercises.isEmpty()) {
        return PrebuiltWorkoutParseResult(
            error = "No valid exercises or sets could be parsed. Check that sets use a format like '8 x 60' or '8 @ 60'."
        )
    }

    return PrebuiltWorkoutParseResult(
        session = PrebuiltWorkoutSession(
            id = UUID.randomUUID().toString(),
            title = title,
            sourceText = sourceText,
            bodyWeight = bodyWeight,
            exercises = exercises,
        )
    )
}
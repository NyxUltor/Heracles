package com.heracles.mobile.logic

import com.heracles.mobile.model.ExerciseEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

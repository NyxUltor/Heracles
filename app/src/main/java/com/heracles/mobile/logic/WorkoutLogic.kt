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

fun buildSessionFilename(volume: Double, date: LocalDate = LocalDate.now()): String {
    val datePart = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault()))
    val volumeK = (volume / 1000.0).toInt()
    return "$datePart-${volumeK}k.json"
}

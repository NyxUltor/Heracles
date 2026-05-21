package com.heracles.mobile.logic

import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WorkoutLogicTest {
    @Test
    fun calculates_volume_from_exercises() {
        val exercises = listOf(
            ExerciseEntry(
                name = "Push Ups",
                sets = listOf(WorkoutSet(reps = 10, weight = 0.0)),
            ),
            ExerciseEntry(
                name = "Bench Press",
                sets = listOf(WorkoutSet(reps = 5, weight = 100.0)),
            ),
        )

        assertEquals(500.0, calculateVolume(exercises), 0.001)
    }

    @Test
    fun builds_session_filename_with_date_and_volume() {
        val name = buildSessionFilename(2500.0, LocalDate.of(2026, 5, 22))
        assertEquals("22-05-2026-2k.json", name)
    }
}

package com.heracles.mobile

import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.AppSettings
import com.heracles.mobile.model.WorkoutSet
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.storage.SessionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class AppViewModelTest {
    @Test
    fun restoreFromSession_normalizes_dirty_numeric_fields() {
        val rootDir = Files.createTempDirectory("heracles-viewmodel").toFile()
        rootDir.deleteOnExit()

        val viewModel = AppViewModel(SessionRepository(rootDir))
        val dirtySession = WorkoutSession(
            id = "session-dirty",
            bodyWeight = "59.1b",
            workoutDuration = "1m2",
            exercises = listOf(
                ExerciseEntry(
                    name = "Bench Press",
                    sets = listOf(
                        WorkoutSet(reps = -5, weight = -10.0),
                        WorkoutSet(reps = 12, weight = 45.0),
                    )
                )
            )
        )

        viewModel.restoreFromSession(dirtySession)

        assertEquals("59.1", viewModel.bodyWeight)
        assertEquals("12", viewModel.workoutDuration)
        assertEquals(1, viewModel.exercises.size)
        assertEquals(2, viewModel.exercises.first().sets.size)
        assertEquals("0", viewModel.exercises.first().sets.first().reps)
        assertEquals("0.0", viewModel.exercises.first().sets.first().weight)
        assertFalse(viewModel.canGoBackToLogger())
    }

    @Test
    fun starts_new_session_empty_when_default_exercises_are_empty() {
        val rootDir = Files.createTempDirectory("heracles-viewmodel-empty-defaults").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        repository.saveSettings(AppSettings(defaultExercises = emptyList()))

        val viewModel = AppViewModel(repository)

        assertEquals(0, viewModel.exercises.size)
    }

    @Test
    fun exercise_name_updates_preserve_punctuation() {
        val rootDir = Files.createTempDirectory("heracles-viewmodel-name-punctuation").toFile()
        rootDir.deleteOnExit()

        val viewModel = AppViewModel(SessionRepository(rootDir))
        viewModel.addExercise("Bench._press")

        assertTrue(viewModel.exercises.isNotEmpty())
        assertEquals("Bench._press", viewModel.exercises.first().name)

        viewModel.updateExerciseName(viewModel.exercises.first().id, "Push._up 2")

        assertEquals("Push._up 2", viewModel.exercises.first().name)
    }
}

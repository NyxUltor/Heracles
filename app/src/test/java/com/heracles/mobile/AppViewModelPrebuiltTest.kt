package com.heracles.mobile

import com.heracles.mobile.model.PrebuiltWorkoutExercise
import com.heracles.mobile.model.PrebuiltWorkoutSession
import com.heracles.mobile.model.PrebuiltWorkoutSet
import com.heracles.mobile.storage.SessionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class AppViewModelPrebuiltTest {
    @Test
    fun restoreFromPrebuiltSession_uses_ghost_values_for_volume() {
        val rootDir = Files.createTempDirectory("heracles-viewmodel-prebuilt").toFile()
        rootDir.deleteOnExit()

        val viewModel = AppViewModel(SessionRepository(rootDir))
        val prebuilt = PrebuiltWorkoutSession(
            id = "prebuilt-1",
            title = "Push A",
            sourceText = "Workout: Push A",
            exercises = listOf(
                PrebuiltWorkoutExercise(
                    name = "Bench Press",
                    sets = listOf(PrebuiltWorkoutSet(reps = "8", weight = "60")),
                )
            ),
        )

        viewModel.restoreFromPrebuiltSession(prebuilt)

        assertEquals(1, viewModel.exercises.size)
        assertEquals("", viewModel.exercises.first().sets.first().reps)
        assertEquals("8", viewModel.exercises.first().sets.first().ghostReps)
        assertEquals("60", viewModel.exercises.first().sets.first().ghostWeight)
        assertEquals(480.0, viewModel.sessionVolume(), 0.0)
        assertTrue(viewModel.pendingPrebuiltText.isEmpty())
    }
}

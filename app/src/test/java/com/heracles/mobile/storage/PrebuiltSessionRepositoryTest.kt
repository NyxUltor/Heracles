package com.heracles.mobile.storage

import com.heracles.mobile.model.PrebuiltWorkoutExercise
import com.heracles.mobile.model.PrebuiltWorkoutSession
import com.heracles.mobile.model.PrebuiltWorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PrebuiltSessionRepositoryTest {
    @Test
    fun saves_loads_and_deletes_prebuilt_session() {
        val rootDir = Files.createTempDirectory("heracles-prebuilt-repo").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        val session = PrebuiltWorkoutSession(
            id = "prebuilt-1",
            title = "Push A",
            sourceText = "Workout: Push A",
            bodyWeight = "82.4",
            workoutDuration = "45",
            exercises = listOf(
                PrebuiltWorkoutExercise(
                    name = "Bench Press",
                    sets = listOf(PrebuiltWorkoutSet(reps = "8", weight = "60")),
                )
            ),
        )

        val file = repository.savePrebuiltSession(session)
        val loaded = repository.loadPrebuiltSessions()

        assertTrue(file.exists())
        assertEquals(1, loaded.size)
        assertEquals(session, loaded.single())

        val deleted = repository.deletePrebuiltSession(session.id)

        assertTrue(deleted)
        assertFalse(file.exists())
        assertEquals(0, repository.loadPrebuiltSessions().size)
    }
}

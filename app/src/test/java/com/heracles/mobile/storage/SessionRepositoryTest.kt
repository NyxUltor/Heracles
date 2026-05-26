package com.heracles.mobile.storage

import com.heracles.mobile.model.AppSettings
import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.WorkoutSet
import com.heracles.mobile.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SessionRepositoryTest {
    @Test
    fun save_and_delete_session_file() {
        val tempDir = createTempDir(prefix = "sessions-test")
        try {
            val repo = SessionRepository(tempDir)
            val session = WorkoutSession(id = "test-id-1")
            val file = repo.saveSession(session)
            assertTrue(file.exists())
            val deleted = repo.deleteSession(session.id)
            assertTrue(deleted)
            // ensure file removed
            assertFalse(file.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun deleteSession_removes_only_one_matching_file() {
        val rootDir = Files.createTempDirectory("heracles-session-repo-delete-one").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        val session = WorkoutSession(id = "shared-id")

        val firstFile = repository.saveSession(session)
        val secondFile = repository.saveSession(session)

        val deleted = repository.deleteSession(session.id)

        assertTrue(deleted)
        assertEquals(1, repository.loadSessions().size)
        assertTrue(firstFile.exists() xor secondFile.exists())
    }

    @Test
    fun saves_and_loads_a_session_round_trip() {
        val rootDir = Files.createTempDirectory("heracles-session-repo").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        val session = WorkoutSession(
            id = "session-1",
            savedAt = "2026-05-23T12:34:56Z",
            bodyWeight = "72.5",
            workoutDuration = "45",
            exercises = listOf(
                ExerciseEntry(
                    id = "exercise-1",
                    name = "Bench Press",
                    sets = listOf(
                        WorkoutSet(reps = 5, weight = 100.0),
                    ),
                ),
            ),
            volume = 500.0,
        )

        val savedFile = repository.saveSession(session)
        val loadedSessions = repository.loadSessions()

        assertTrue(savedFile.exists())
        assertEquals(1, loadedSessions.size)
        assertEquals(session, loadedSessions.single())
    }

    @Test
    fun saveSession_creates_unique_file_names_for_same_volume() {
        val rootDir = Files.createTempDirectory("heracles-session-repo-unique").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        val session = WorkoutSession(
            id = "session-1",
            savedAt = "2026-05-23T12:34:56Z",
            exercises = emptyList(),
            volume = 2500.0,
        )

        val firstFile = repository.saveSession(session)
        val secondFile = repository.saveSession(session)

        assertNotEquals(firstFile.name, secondFile.name)
        assertTrue(firstFile.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}-\\d+-\\dk\\.json")))
        assertTrue(secondFile.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}-\\d+-\\dk\\.json")))
        assertEquals(2, repository.loadSessions().size)
    }

    @Test
    fun saveSettings_moves_existing_log_files_to_new_directory() {
        val rootDir = Files.createTempDirectory("heracles-session-repo-move").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        val session = WorkoutSession(
            id = "session-1",
            savedAt = "2026-05-23T12:34:56Z",
            exercises = emptyList(),
            volume = 750.0,
        )

        val savedFile = repository.saveSession(session)
        val movedSettings = AppSettings(logStoragePath = "archive/logs")

        repository.saveSettings(movedSettings)

        val newDirectory = java.io.File(rootDir, "archive/logs")
        val movedFile = java.io.File(newDirectory, savedFile.name)

        assertTrue(!savedFile.exists())
        assertTrue(movedFile.exists())
        assertEquals(1, repository.loadSessions().size)
        assertEquals(session, repository.loadSessions().single())
    }

    @Test
    fun loadSettings_returns_absolute_log_path() {
        val rootDir = Files.createTempDirectory("heracles-session-repo-settings").toFile()
        rootDir.deleteOnExit()

        val repository = SessionRepository(rootDir)
        val savedSettings = repository.saveSettings(AppSettings(logStoragePath = "custom/logs"))

        assertTrue(savedSettings.logStoragePath.startsWith(rootDir.absolutePath))
        assertTrue(savedSettings.logStoragePath.contains("custom"))
    }
}

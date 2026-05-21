package com.heracles.mobile.storage

import android.content.Context
import com.heracles.mobile.model.AppSettings
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.logic.buildSessionFilename
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SessionRepository(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val sessionsDir: File = File(context.filesDir, "sessions")
    private val autosaveFile: File = File(context.filesDir, "autosave.json")
    private val settingsFile: File = File(context.filesDir, "settings.json")

    init {
        sessionsDir.mkdirs()
    }

    fun loadSettings(): AppSettings {
        if (!settingsFile.exists()) {
            return AppSettings()
        }
        return runCatching {
            json.decodeFromString(AppSettings.serializer(), settingsFile.readText())
        }.getOrDefault(AppSettings())
    }

    fun saveSettings(settings: AppSettings) {
        settingsFile.writeText(json.encodeToString(settings))
    }

    fun saveAutosave(session: WorkoutSession) {
        autosaveFile.writeText(json.encodeToString(session))
    }

    fun loadAutosave(): WorkoutSession? {
        return runCatching {
            if (!autosaveFile.exists()) null else json.decodeFromString(WorkoutSession.serializer(), autosaveFile.readText())
        }.getOrNull()
    }

    fun loadSessions(): List<WorkoutSession> {
        return sessionsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                runCatching { json.decodeFromString(WorkoutSession.serializer(), file.readText()) }.getOrNull()
            }
            ?: emptyList()
    }

    fun saveSession(session: WorkoutSession): File {
        val file = uniqueSessionFile(session.volume)
        file.writeText(json.encodeToString(session))
        return file
    }

    private fun uniqueSessionFile(volume: Double): File {
        val baseName = buildSessionFilename(volume)
        var candidate = File(sessionsDir, baseName)
        var counter = 2
        while (candidate.exists()) {
            val extension = candidate.extension
            val stem = candidate.nameWithoutExtension
            candidate = File(sessionsDir, "$stem-$counter.$extension")
            counter += 1
        }
        return candidate
    }

}

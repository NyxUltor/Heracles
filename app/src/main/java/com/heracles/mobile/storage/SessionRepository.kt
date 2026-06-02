/*
 File: storage/SessionRepository.kt
 What it does: Loads and saves workout sessions, bodyweight history, prebuilt sessions, settings, and theme mods on disk.
 Main inputs: app settings, serialized session/theme data, filesystem paths.
 Main outputs: persisted JSON files, backups, and repository load results for the ViewModel.
 Key functions/classes: `SessionRepository` and its load/save/backup APIs.
*/

package com.heracles.mobile.storage

import android.content.Context
import android.media.MediaScannerConnection
import com.heracles.mobile.model.AppConfigBackup
import com.heracles.mobile.model.AppSettings
import com.heracles.mobile.model.BodyweightEntry
import com.heracles.mobile.model.ExerciseProfile
import com.heracles.mobile.model.PrebuiltWorkoutSession
import com.heracles.mobile.model.SessionBackup
import com.heracles.mobile.model.StrengthHistoryEntry
import com.heracles.mobile.model.ThemeMod
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.logic.buildSessionFilename
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SessionRepository(private val context: Context, private val rootDir: File) {
    constructor(context: Context) : this(context, context.filesDir)

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var sessionsDir: File = defaultStorageDir()
    private val autosaveFile: File = File(rootDir, "autosave.json")
    private val settingsFile: File = File(rootDir, "settings.json")
    private val bodyweightFile: File = File(rootDir, "bodyweight-history.json")
    private val modsFile: File = File(rootDir, "theme-mods.json")
    private val exerciseProfilesFile: File = File(rootDir, "exercise-profiles.json")
    private val strengthHistoryFile: File = File(rootDir, "strength-history.json")
    private val prebuiltDir: File = File(rootDir, "prebuilt-sessions")
    private val backupDir: File = File(rootDir, "backups")

    fun loadSettings(): AppSettings {
        val loadedSettings = if (!settingsFile.exists()) {
            normalizeSettings(AppSettings(logStoragePath = defaultStorageDir().absolutePath))
        } else {
            runCatching {
                normalizeSettings(json.decodeFromString(AppSettings.serializer(), settingsFile.readText()))
            }.getOrDefault(normalizeSettings(AppSettings(logStoragePath = defaultStorageDir().absolutePath)))
        }
        var normalizedSettings = loadedSettings

        val preferredStorageDir = defaultStorageDir()
        val legacyInternalSessionsDir = File(rootDir, "sessions")
        val normalizedStorageDir = resolveStorageDir(normalizedSettings.logStoragePath)
        if (preferredStorageDir.canonicalPath != legacyInternalSessionsDir.canonicalPath &&
            normalizedStorageDir.canonicalPath == legacyInternalSessionsDir.canonicalPath
        ) {
            normalizedSettings = normalizedSettings.copy(logStoragePath = preferredStorageDir.absolutePath)
        }

        if (normalizedSettings.logStoragePath != loadedSettings.logStoragePath && settingsFile.exists()) {
            writeTextAtomically(settingsFile, json.encodeToString(normalizedSettings))
        }

        sessionsDir = resolveStorageDir(normalizedSettings.logStoragePath)
        sessionsDir.mkdirs()
        migrateLegacyInternalLogs()
        return normalizedSettings
    }

    fun saveSettings(settings: AppSettings): AppSettings {
        val normalizedSettings = normalizeSettings(settings)
        val newSessionsDir = resolveStorageDir(normalizedSettings.logStoragePath)
        if (newSessionsDir.canonicalPath != sessionsDir.canonicalPath) {
            migrateStorageDirectory(sessionsDir, newSessionsDir)
            sessionsDir = newSessionsDir
        }
        writeTextAtomically(settingsFile, json.encodeToString(normalizedSettings))
        return normalizedSettings
    }

    fun saveAutosave(session: WorkoutSession) {
        writeTextAtomically(autosaveFile, json.encodeToString(session))
    }

    fun loadBodyweightHistory(): List<BodyweightEntry> {
        return runCatching {
            if (!bodyweightFile.exists()) emptyList() else json.decodeFromString(ListSerializer(BodyweightEntry.serializer()), bodyweightFile.readText())
        }.getOrDefault(emptyList())
    }

    fun loadThemeMods(): List<ThemeMod> {
        return runCatching {
            if (!modsFile.exists()) emptyList() else json.decodeFromString(ListSerializer(ThemeMod.serializer()), modsFile.readText())
        }.getOrDefault(emptyList())
    }

    fun saveThemeMods(mods: List<ThemeMod>) {
        writeTextAtomically(modsFile, json.encodeToString(ListSerializer(ThemeMod.serializer()), mods))
    }

    fun loadPrebuiltSessions(): List<PrebuiltWorkoutSession> {
        return runCatching {
            prebuiltDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.sortedByDescending { it.lastModified() }
                ?.mapNotNull { file ->
                    runCatching { json.decodeFromString(PrebuiltWorkoutSession.serializer(), file.readText()) }.getOrNull()
                }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun savePrebuiltSession(session: PrebuiltWorkoutSession): File {
        prebuiltDir.mkdirs()
        val file = File(prebuiltDir, "${session.id}.json")
        writeTextAtomically(file, json.encodeToString(session))
        return file
    }

    fun deletePrebuiltSession(sessionId: String): Boolean {
        val files = prebuiltDir.listFiles()?.filter { it.extension == "json" } ?: return false
        var deletedAny = false
        files.forEach { file ->
            val session = runCatching { json.decodeFromString(PrebuiltWorkoutSession.serializer(), file.readText()) }.getOrNull()
            if (session != null && session.id == sessionId) {
                if (file.delete()) deletedAny = true
            }
        }
        return deletedAny
    }

    fun exportConfigBackup(settings: AppSettings, themeMods: List<ThemeMod>): File {
        backupDir.mkdirs()
        val backupFile = File(backupDir, "config-backup.json")
        val payload = AppConfigBackup(settings = settings, themeMods = themeMods)
        writeTextAtomically(backupFile, json.encodeToString(payload))
        return backupFile
    }

    fun exportSessionsBackup(sessions: List<WorkoutSession>): File {
        backupDir.mkdirs()
        val backupFile = File(backupDir, "sessions-backup.json")
        val payload = SessionBackup(sessions = sessions)
        writeTextAtomically(backupFile, json.encodeToString(payload))
        return backupFile
    }

    fun saveBodyweightEntry(entry: BodyweightEntry): List<BodyweightEntry> {
        val updated = loadBodyweightHistory()
            .filterNot { it.date == entry.date }
            .plus(entry)
            .sortedBy { it.date }
        writeTextAtomically(bodyweightFile, json.encodeToString(ListSerializer(BodyweightEntry.serializer()), updated))
        return updated
    }

    fun loadExerciseProfiles(): Map<String, ExerciseProfile> {
        return runCatching {
            if (!exerciseProfilesFile.exists()) emptyList()
            else json.decodeFromString(
                ListSerializer(ExerciseProfile.serializer()),
                exerciseProfilesFile.readText()
            )
        }.getOrDefault(emptyList<ExerciseProfile>())
            .associateBy { it.exerciseId.lowercase().trim() }
    }

    fun saveExerciseProfiles(profiles: List<ExerciseProfile>) {
        writeTextAtomically(
            exerciseProfilesFile,
            json.encodeToString(
                ListSerializer(ExerciseProfile.serializer()),
                profiles
            )
        )
    }

    fun loadStrengthHistory(): List<StrengthHistoryEntry> {
        return runCatching {
            if (!strengthHistoryFile.exists()) emptyList()
            else json.decodeFromString(
                ListSerializer(StrengthHistoryEntry.serializer()),
                strengthHistoryFile.readText()
            )
        }.getOrDefault(emptyList())
    }

    fun appendStrengthHistoryEntry(entry: StrengthHistoryEntry): List<StrengthHistoryEntry> {
        val updated = loadStrengthHistory()
            .filter { !(it.exerciseName == entry.exerciseName && it.date == entry.date) }
            .plus(entry)
            .sortedBy { it.date }
        writeTextAtomically(
            strengthHistoryFile,
            json.encodeToString(
                ListSerializer(StrengthHistoryEntry.serializer()),
                updated
            )
        )
        return updated
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
        writeTextAtomically(file, json.encodeToString(session))
        scanForMedia(file)
        return file
    }

    fun deleteSession(sessionId: String): Boolean {
        val files = sessionsDir.listFiles()?.filter { it.extension == "json" } ?: return false
        var deletedAny = false
        files.sortedByDescending { it.lastModified() }.forEach { file ->
            val session = runCatching { json.decodeFromString(WorkoutSession.serializer(), file.readText()) }.getOrNull()
            if (session != null && session.id == sessionId) {
                if (file.delete()) deletedAny = true
                scanForMedia(file)
                return deletedAny
            }
        }
        return deletedAny
    }

    /**
     * Export session JSON files to an absolute directory path. Returns number of files exported.
     */
    fun exportSessionsToDirectory(destinationPath: String): Int {
        val destDir = File(destinationPath)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val files = sessionsDir.listFiles()?.filter { it.extension == "json" } ?: return 0
        var exported = 0
        files.forEach { file ->
            try {
                val dest = File(destDir, file.name)
                file.copyTo(dest, overwrite = true)
                scanForMedia(dest)
                exported += 1
            } catch (_: Throwable) {
            }
        }
        return exported
    }

    private fun writeTextAtomically(file: File, content: String) {
        file.parentFile?.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(content)
        if (file.exists() && !file.delete()) {
            temporaryFile.delete()
            throw IllegalStateException("Unable to replace ${file.absolutePath}")
        }
        if (!temporaryFile.renameTo(file)) {
            temporaryFile.copyTo(file, overwrite = true)
            temporaryFile.delete()
        }
    }

    private fun uniqueSessionFile(volume: Double): File {
        val sessionDate = LocalDate.now()
        var index = nextSessionIndex(sessionDate)
        var candidate = File(sessionsDir, buildSessionFilename(volume, index, sessionDate))
        while (candidate.exists()) {
            index += 1
            candidate = File(sessionsDir, buildSessionFilename(volume, index, sessionDate))
        }
        return candidate
    }

    private fun resolveStorageDir(rawPath: String): File {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) {
            return defaultStorageDir()
        }

        val asFile = File(trimmed)
        return if (asFile.isAbsolute) asFile else File(rootDir, trimmed)
    }

    private fun normalizeSettings(settings: AppSettings): AppSettings {
        return settings.copy(
            logStoragePath = resolveStorageDir(settings.logStoragePath).absolutePath,
            numericInputModes = settings.numericInputModes.ifEmpty { setOf("keyboard") },
            scrubberSensitivity = settings.scrubberSensitivity.coerceIn(0.05, 10.0),
            uiFidelity = settings.uiFidelity,
            activeLightSchemeId = settings.activeLightSchemeId.ifBlank { "default_light" },
            activeDarkSchemeId = settings.activeDarkSchemeId.ifBlank { "default_dark" },
        )
    }

    private fun defaultStorageDir(): File = File(context.getExternalFilesDir(null) ?: rootDir, "sessions")

    private fun migrateLegacyInternalLogs() {
        val legacyDir = File(rootDir, "sessions")
        if (!legacyDir.exists()) {
            return
        }
        if (legacyDir.canonicalPath == sessionsDir.canonicalPath) {
            return
        }

        val legacyFiles = legacyDir.listFiles()?.filter { it.isFile } ?: return
        if (legacyFiles.isEmpty()) {
            return
        }

        sessionsDir.mkdirs()
        legacyFiles.forEach { sourceFile ->
            val destinationFile = uniqueDestinationFile(sessionsDir, sourceFile.name)
            try {
                sourceFile.copyTo(destinationFile, overwrite = false)
                scanForMedia(destinationFile)
                if (!sourceFile.delete()) {
                    throw IllegalStateException("Unable to remove ${sourceFile.absolutePath}")
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun scanForMedia(file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    private fun nextSessionIndex(date: LocalDate): Int {
        val prefix = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val existingCount = sessionsDir.listFiles()
            ?.count { file -> file.extension == "json" && file.name.startsWith("$prefix-") }
            ?: 0
        return existingCount + 1
    }

    private fun migrateStorageDirectory(fromDir: File, toDir: File) {
        if (fromDir.canonicalPath == toDir.canonicalPath) {
            return
        }

        toDir.mkdirs()
        fromDir.listFiles()?.forEach { sourceFile ->
            val destinationFile = uniqueDestinationFile(toDir, sourceFile.name)
            if (!sourceFile.renameTo(destinationFile)) {
                sourceFile.copyTo(destinationFile, overwrite = true)
                if (!sourceFile.delete()) {
                    throw IllegalStateException("Unable to move ${sourceFile.absolutePath}")
                }
            }
        }
    }

    private fun uniqueDestinationFile(directory: File, fileName: String): File {
        var candidate = File(directory, fileName)
        var counter = 2
        while (candidate.exists()) {
            val extension = candidate.extension
            val stem = candidate.nameWithoutExtension
            candidate = File(directory, "$stem-$counter.$extension")
            counter += 1
        }
        return candidate
    }
}

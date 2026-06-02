/*
 File: AppViewModel.kt
 What it does: Central state holder for the app, coordinating workouts, sessions, theme mods, settings, and persistence actions.
 Main inputs: repository data, user interactions from UI composables, and startup hydration state.
 Main outputs: mutable UI state, session/theme changes, and repository writes.
 Key functions/classes: `AppViewModel`, session/timer helpers, theme selection helpers.
*/

package com.heracles.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.heracles.mobile.model.CuratedScheme
import com.heracles.mobile.model.CuratedSchemes
import com.heracles.mobile.model.AppSettings
import com.heracles.mobile.model.DualAxisMetric
import com.heracles.mobile.model.ExerciseProfile
import com.heracles.mobile.model.ShapeMode
import com.heracles.mobile.model.SystemUiMode
import com.heracles.mobile.model.PrebuiltWorkoutSession
import com.heracles.mobile.model.StrengthHistoryEntry
import com.heracles.mobile.model.ThemeColorScheme
import com.heracles.mobile.model.ThemeMod
import com.heracles.mobile.model.ThemeStylePack
import com.heracles.mobile.model.TokenSet
import com.heracles.mobile.model.BodyweightEntry
import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.model.WorkoutSet
import com.heracles.mobile.logic.calculateVolume
import com.heracles.mobile.logic.parsePrebuiltWorkoutTemplate
import com.heracles.mobile.logic.sanitizeNumericText
import com.heracles.mobile.logic.sanitizeExerciseName
import com.heracles.mobile.storage.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.Locale

class AppViewModel(private val repository: SessionRepository) : ViewModel() {
    class SetDraft(
        initialReps: String = "",
        initialWeight: String = "",
        initialCompleted: Boolean = false,
        val ghostReps: String? = null,
        val ghostWeight: String? = null,
    ) {
        var reps: String by mutableStateOf(initialReps)
        var weight: String by mutableStateOf(initialWeight)
        var completed: Boolean by mutableStateOf(initialCompleted)
    }

    class ExerciseDraft(name: String) {
        val id: String = UUID.randomUUID().toString()
        var name: String by mutableStateOf(name)
        val sets = mutableStateListOf(SetDraft())
    }

    var settings by mutableStateOf(AppSettings())
        private set

    // temporary preview mod id; when non-null it overrides current mod selection for live preview only
    var previewUiMode by mutableStateOf<String?>(null)
        private set

    var currentScreen by mutableStateOf("Logger")
        private set

    var workoutDuration by mutableStateOf("")
        private set

    var isTimerRunning by mutableStateOf(false)
        private set

    var bodyWeight by mutableStateOf("")
        private set

    var pendingExerciseName by mutableStateOf("")
        private set

    var pendingLogStoragePath by mutableStateOf("")
        private set

    var lastExportMessage by mutableStateOf("")
        private set

    var lastBackupMessage by mutableStateOf("")
        private set

    var lastSaveMessage by mutableStateOf("")
        private set

    var lastPrebuiltMessage by mutableStateOf("")
        private set

    var pendingPrebuiltText by mutableStateOf("")
        private set

    var trackerBodyWeight by mutableStateOf("")
        private set

    var scrubberGestureActive by mutableStateOf(false)
        private set

    var editingModId by mutableStateOf<String?>(null)
        private set

    var currentSessionId: String? by mutableStateOf(null)
        private set

    var currentSessionSavedAt: String? by mutableStateOf(null)
        private set

    val exercises = mutableStateListOf<ExerciseDraft>()
    val sessions = mutableStateListOf<WorkoutSession>()
    val bodyweightHistory = mutableStateListOf<BodyweightEntry>()
    val themeMods = mutableStateListOf<ThemeMod>()
    val prebuiltSessions = mutableStateListOf<PrebuiltWorkoutSession>()
    val exerciseProfiles = mutableStateListOf<ExerciseProfile>()
    val strengthHistory = mutableStateListOf<StrengthHistoryEntry>()
    val sufferingInputs = mutableStateMapOf<String, Double>()
    private val curatedSchemeUsageCounts = mutableStateMapOf<String, Int>()

    private var autosaveJob: Job? = null
    private var workoutTimerJob: Job? = null
    private var workoutTimerElapsedSeconds by mutableLongStateOf(0L)
    private var startupHydrationStarted = false

    init {
        settings = repository.loadSettings()
        pendingLogStoragePath = settings.logStoragePath
        // load theme mods and initialize defaults
        viewModelScope.launch(Dispatchers.IO) {
            val loadedMods = repository.loadThemeMods()
            val loadedPrebuilt = repository.loadPrebuiltSessions()
            withContext(Dispatchers.Main) {
                themeMods.clear()
                themeMods.addAll(loadedMods.map { it.normalizedThemeModel() })
                prebuiltSessions.clear()
                prebuiltSessions.addAll(loadedPrebuilt)
                if (themeMods.isEmpty()) {
                    val builtIns = listOf(
                        ThemeMod(
                            id = "bare_metal",
                            name = "Bare Metal",
                            author = "Heracles",
                            lightSchemes = listOf(
                                ThemeColorScheme(
                                    id = "metal_classic_light",
                                    name = "Classic Light",
                                    tokens = TokenSet(
                                        primary = "#2457C5",
                                        secondary = "#4D6DB5",
                                        background = "#FFFBFE",
                                        surface = "#FFFFFF",
                                        onPrimary = "#FFFFFF",
                                        borderWidth = 1.0,
                                        surfaceRule = "android_default",
                                    ),
                                ),
                                ThemeColorScheme(
                                    id = "metal_sky_light",
                                    name = "Sky Light",
                                    tokens = TokenSet(
                                        primary = "#00639B",
                                        secondary = "#3A6584",
                                        background = "#F7F9FF",
                                        surface = "#FFFFFF",
                                        onPrimary = "#FFFFFF",
                                        borderWidth = 1.0,
                                        surfaceRule = "android_default",
                                    ),
                                ),
                            ),
                            darkSchemes = listOf(
                                ThemeColorScheme(
                                    id = "metal_classic_dark",
                                    name = "Classic Dark",
                                    tokens = TokenSet(
                                        primary = "#2457C5",
                                        secondary = "#4D6DB5",
                                        background = "#1F1F21",
                                        surface = "#272729",
                                        onPrimary = "#FFFFFF",
                                        borderWidth = 1.0,
                                        surfaceRule = "android_default",
                                    ),
                                ),
                                ThemeColorScheme(
                                    id = "metal_midnight_dark",
                                    name = "Midnight",
                                    tokens = TokenSet(
                                        primary = "#3C7DFF",
                                        secondary = "#73A1FF",
                                        background = "#111318",
                                        surface = "#1A1D24",
                                        onPrimary = "#FFFFFF",
                                        borderWidth = 1.0,
                                        surfaceRule = "android_default",
                                    ),
                                ),
                            ),
                            style = ThemeStylePack(
                                shapeStyle = ShapeMode.DEFAULT,
                                buttonHeightDp = 48,
                                textureRule = "android_default",
                            ),
                        ),
                        ThemeMod(
                            id = "stone_temple",
                            name = "Stone Temple",
                            author = "You",
                            lightSchemes = listOf(
                                ThemeColorScheme(
                                    id = "temple_granite_light",
                                    name = "Granite",
                                    tokens = TokenSet(
                                        primary = "#757575",
                                        secondary = "#9E9E9E",
                                        background = "#F5F5F5",
                                        surface = "#EEEEEE",
                                        onPrimary = "#111111",
                                        borderWidth = 2.0,
                                        surfaceRule = "flat",
                                    ),
                                ),
                                ThemeColorScheme(
                                    id = "temple_sand_light",
                                    name = "Sand",
                                    tokens = TokenSet(
                                        primary = "#8D6E63",
                                        secondary = "#A1887F",
                                        background = "#FAF6F1",
                                        surface = "#F1E9DF",
                                        onPrimary = "#201814",
                                        borderWidth = 2.0,
                                        surfaceRule = "flat",
                                    ),
                                ),
                            ),
                            darkSchemes = listOf(
                                ThemeColorScheme(
                                    id = "temple_obsidian_dark",
                                    name = "Obsidian",
                                    tokens = TokenSet(
                                        primary = "#616161",
                                        secondary = "#9E9E9E",
                                        background = "#212121",
                                        surface = "#2C2C2C",
                                        onPrimary = "#E0E0E0",
                                        borderWidth = 2.0,
                                        surfaceRule = "flat",
                                    ),
                                ),
                                ThemeColorScheme(
                                    id = "temple_moss_dark",
                                    name = "Moss",
                                    tokens = TokenSet(
                                        primary = "#6B8F71",
                                        secondary = "#9AB59E",
                                        background = "#1B201C",
                                        surface = "#242A25",
                                        onPrimary = "#E8F2E9",
                                        borderWidth = 2.0,
                                        surfaceRule = "flat",
                                    ),
                                ),
                            ),
                            style = ThemeStylePack(
                                shapeStyle = ShapeMode.RECTANGLE,
                                buttonHeightDp = 50,
                                textureRule = "stone",
                            ),
                        ),
                    )
                    themeMods.addAll(builtIns)
                    viewModelScope.launch(Dispatchers.IO) { repository.saveThemeMods(themeMods.toList()) }
                }
                settings = repository.saveSettings(normalizeThemeSelection(settings))
                restoreDefaults(scheduleAutosave = false)
            }
        }
    }

    fun switchScreen(name: String) {
        currentScreen = name
    }

    fun updateWorkoutDuration(value: String) {
        workoutDuration = value.normalizeWorkoutDurationDisplay()
        autosave()
    }

    fun updateBodyWeight(value: String) {
        bodyWeight = sanitizeNumericText(value)
        autosave()
    }

    fun updatePendingExerciseName(value: String) {
        pendingExerciseName = value
    }

    fun updatePendingLogStoragePath(value: String) {
        pendingLogStoragePath = value
    }

    fun updatePendingPrebuiltText(value: String) {
        pendingPrebuiltText = value
    }

    fun exportSessionsTo(destinationPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exported = repository.exportSessionsToDirectory(destinationPath)
                withContext(Dispatchers.Main) {
                    lastExportMessage = "Exported $exported session(s) to $destinationPath"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lastExportMessage = "Export failed: ${e.message}"
                }
            }
        }
    }

    fun backupConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = repository.exportConfigBackup(settings, themeMods.toList())
                withContext(Dispatchers.Main) {
                    lastBackupMessage = "Saved settings backup: ${backupFile.absolutePath}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lastBackupMessage = "Settings backup failed: ${e.message}"
                }
            }
        }
    }

    fun backupSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = repository.exportSessionsBackup(sessions.toList())
                withContext(Dispatchers.Main) {
                    lastBackupMessage = "Saved sessions backup: ${backupFile.absolutePath}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lastBackupMessage = "Sessions backup failed: ${e.message}"
                }
            }
        }
    }

    fun updateTrackerBodyWeight(value: String) {
        trackerBodyWeight = sanitizeNumericText(value)
    }

    fun onScrubberGestureStart() {
        scrubberGestureActive = true
    }

    fun onScrubberGestureEnd() {
        scrubberGestureActive = false
    }

    fun startStartupHydration() {
        if (startupHydrationStarted) {
            return
        }
        startupHydrationStarted = true

        viewModelScope.launch(Dispatchers.IO) {
            val loadedSessions = repository.loadSessions()
            val loadedBodyweights = repository.loadBodyweightHistory()
            val loadedPrebuilt = repository.loadPrebuiltSessions()
            val autosave = repository.loadAutosave()

            withContext(Dispatchers.Main) {
                sessions.clear()
                sessions.addAll(loadedSessions)
                bodyweightHistory.clear()
                bodyweightHistory.addAll(loadedBodyweights)
                prebuiltSessions.clear()
                prebuiltSessions.addAll(loadedPrebuilt)
                trackerBodyWeight = bodyweightHistory.lastOrNull()?.weight?.toString().orEmpty()

                if (autosave != null && settings.restoreLatestOnOpen && currentScreen == "Logger") {
                    restoreFromSession(autosave)
                }
                loadTrackerExtendedData()
            }
        }
    }

    fun loadTrackerExtendedData() {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = repository.loadExerciseProfiles()
            val history = repository.loadStrengthHistory()
            withContext(Dispatchers.Main) {
                exerciseProfiles.clear()
                exerciseProfiles.addAll(profiles.values)
                strengthHistory.clear()
                strengthHistory.addAll(history)
            }
        }
    }

    fun addPendingExercise() {
        addExercise(pendingExerciseName)
        pendingExerciseName = ""
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = repository.saveSettings(normalizeThemeSelection(newSettings))
        pendingLogStoragePath = settings.logStoragePath
        autosave()
    }

    // preview helpers
    fun setPreviewMode(mode: String?) {
        previewUiMode = mode
    }

    fun clearPreviewMode() {
        previewUiMode = null
    }

    fun activeThemeMod(): ThemeMod? {
        val previewId = previewUiMode
        if (!previewId.isNullOrBlank()) {
            return themeMods.firstOrNull { it.id == previewId }
        }
        return themeMods.firstOrNull { it.id == settings.currentModId }
    }

    fun activeLightScheme(mod: ThemeMod? = activeThemeMod()): ThemeColorScheme? {
        val target = mod ?: return null
        return target.lightSchemes.firstOrNull { it.id == settings.activeLightSchemeId } ?: target.lightSchemes.firstOrNull()
    }

    fun activeDarkScheme(mod: ThemeMod? = activeThemeMod()): ThemeColorScheme? {
        val target = mod ?: return null
        return target.darkSchemes.firstOrNull { it.id == settings.activeDarkSchemeId } ?: target.darkSchemes.firstOrNull()
    }


    fun restoreDefaults(scheduleAutosave: Boolean = true) {
        currentSessionId = null
        currentSessionSavedAt = null
        exercises.clear()
        settings.defaultExercises.forEach { exercises.add(ExerciseDraft(it)) }
        workoutTimerElapsedSeconds = 0L
        isTimerRunning = false
        workoutTimerJob?.cancel()
        workoutTimerJob = null
        workoutDuration = ""
        bodyWeight = ""
        if (scheduleAutosave) {
            scheduleAutosave()
        }
    }

    fun startNewSession() {
        stopWorkoutTimer()
        workoutTimerElapsedSeconds = 0L
        workoutDuration = ""
        restoreDefaults()
        currentScreen = "Logger"
    }

    fun startTimedSession() {
        beginWorkoutTimer()
    }

    fun beginWorkoutTimer() {
        if (isTimerRunning) {
            return
        }

        if (workoutTimerJob == null) {
            workoutTimerElapsedSeconds = workoutDuration.toWorkoutTimerSeconds().takeIf { it > 0L } ?: workoutTimerElapsedSeconds
        }
        isTimerRunning = true
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive) {
                workoutDuration = formatWorkoutDuration(workoutTimerElapsedSeconds)
                delay(1000)
                workoutTimerElapsedSeconds += 1
            }
        }
    }

    fun stopWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = null
        isTimerRunning = false
    }

    private fun formatWorkoutDuration(elapsedSeconds: Long): String {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun String.normalizeWorkoutDurationDisplay(): String {
        val trimmed = trim()
        if (trimmed.isBlank()) return ""
        return formatWorkoutDuration(toWorkoutTimerSeconds())
    }

    private fun String.toWorkoutTimerSeconds(): Long {
        val normalized = trim()
        if (normalized.isBlank()) return 0L
        if (normalized.all { it.isDigit() }) {
            return normalized.toLongOrNull() ?: 0L
        }
        val parts = normalized.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
            2 -> (parts[0] * 60) + parts[1]
            1 -> parts[0]
            else -> 0L
        }
    }

    fun startCreatingMod() {
        editingModId = null
        currentScreen = "ThemeEditor"
    }

    fun startEditingMod(modId: String) {
        editingModId = modId
        currentScreen = "ThemeEditor"
    }

    fun finishEditingMod() {
        editingModId = null
    }

    fun addMod(mod: ThemeMod) {
        val normalizedMod = mod.normalizedThemeModel()
        themeMods.add(normalizedMod)
        updateSettings(
            settings.copy(
                currentModId = normalizedMod.id,
                activeLightSchemeId = normalizedMod.lightSchemes.first().id,
                activeDarkSchemeId = normalizedMod.darkSchemes.first().id,
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveThemeMods(themeMods.toList())
        }
    }

    fun updateMod(mod: ThemeMod) {
        val normalizedMod = mod.normalizedThemeModel()
        val idx = themeMods.indexOfFirst { it.id == mod.id }
        if (idx >= 0) {
            themeMods[idx] = normalizedMod
            if (settings.currentModId == normalizedMod.id) {
                settings = repository.saveSettings(normalizeThemeSelection(settings))
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveThemeMods(themeMods.toList())
        }
    }

    fun deleteMod(modId: String) {
        themeMods.removeAll { it.id == modId && it.id != "bare_metal" && it.id != "stone_temple" }
        if (settings.currentModId == modId) {
            updateSettings(settings.copy(currentModId = themeMods.firstOrNull()?.id ?: "bare_metal"))
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveThemeMods(themeMods.toList())
        }
    }

    fun selectMod(modId: String) {
        updateSettings(settings.copy(currentModId = modId))
    }

    fun selectLightScheme(schemeId: String) {
        updateSettings(settings.copy(activeLightSchemeId = schemeId))
    }

    fun selectDarkScheme(schemeId: String) {
        updateSettings(settings.copy(activeDarkSchemeId = schemeId))
    }

    fun selectCuratedScheme(schemeId: String) {
        curatedSchemeUsageCounts[schemeId] = (curatedSchemeUsageCounts[schemeId] ?: 0) + 1
        updateSettings(settings.copy(curatedSchemeId = schemeId))
    }

    fun activeCuratedScheme(): CuratedScheme {
        return CuratedSchemes.ALL.firstOrNull { it.id == settings.curatedSchemeId }
            ?: CuratedSchemes.HELLFIRE
    }

    fun curatedSchemeUsageCount(schemeId: String): Int {
        return curatedSchemeUsageCounts[schemeId] ?: 0
    }

    fun currentEditingMod(): ThemeMod? {
        return themeMods.firstOrNull { it.id == editingModId }
    }

    fun renameMod(modId: String, newName: String) {
        val target = themeMods.firstOrNull { it.id == modId } ?: return
        updateMod(target.copy(name = newName.ifBlank { target.name }))
    }

    fun setSystemUiMode(mode: SystemUiMode) {
        updateSettings(settings.copy(systemUiMode = mode, useDarkTheme = mode == SystemUiMode.DARK))
    }

    fun completeQuickSetup(mode: SystemUiMode, modId: String) {
        val targetMod = themeMods.firstOrNull { it.id == modId }
        updateSettings(
            settings.copy(
                systemUiMode = mode,
                currentModId = modId,
                activeLightSchemeId = targetMod?.lightSchemes?.firstOrNull()?.id ?: settings.activeLightSchemeId,
                activeDarkSchemeId = targetMod?.darkSchemes?.firstOrNull()?.id ?: settings.activeDarkSchemeId,
                quickSetupCompleted = true,
                useDarkTheme = mode == SystemUiMode.DARK,
            )
        )
    }

    fun saveTrackerBodyWeight() {
        val parsedWeight = trackerBodyWeight.toFlexibleDoubleOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedHistory = repository.saveBodyweightEntry(
                BodyweightEntry(date = LocalDate.now().toString(), weight = parsedWeight)
            )
            withContext(Dispatchers.Main) {
                bodyweightHistory.clear()
                bodyweightHistory.addAll(updatedHistory)
                trackerBodyWeight = parsedWeight.toString()
            }
        }
    }

    fun restoreFromSession(session: WorkoutSession) {
        stopWorkoutTimer()
        val normalizedSession = session.normalizedForEditing()
        currentSessionId = normalizedSession.id
        currentSessionSavedAt = normalizedSession.savedAt
        bodyWeight = normalizedSession.bodyWeight.orEmpty()
        val restoredDuration = normalizedSession.workoutDuration.orEmpty()
        workoutDuration = restoredDuration.normalizeWorkoutDurationDisplay()
        workoutTimerElapsedSeconds = workoutDuration.toWorkoutTimerSeconds()
        exercises.clear()
        normalizedSession.exercises.forEach { exercise ->
            val draft = ExerciseDraft(exercise.name)
            draft.sets.clear()
            if (exercise.sets.isEmpty()) {
                draft.sets.add(SetDraft())
            } else {
                exercise.sets.forEach { set ->
                    draft.sets.add(SetDraft(set.reps.toString(), set.weight.toString(), set.completed))
                }
            }
            exercises.add(draft)
        }
        currentScreen = "Logger"
        scheduleAutosave()
    }

    fun restoreFromPrebuiltSession(session: PrebuiltWorkoutSession) {
        stopWorkoutTimer()
        currentSessionId = null
        currentSessionSavedAt = null
        bodyWeight = session.bodyWeight.orEmpty()
        workoutDuration = ""
        workoutTimerElapsedSeconds = 0L
        exercises.clear()
        session.exercises.forEach { exercise ->
            val draft = ExerciseDraft(exercise.name)
            draft.sets.clear()
            if (exercise.sets.isEmpty()) {
                draft.sets.add(SetDraft())
            } else {
                exercise.sets.forEach { set ->
                    draft.sets.add(SetDraft(ghostReps = set.reps, ghostWeight = set.weight))
                }
            }
            exercises.add(draft)
        }
        currentScreen = "Logger"
        scheduleAutosave()
    }

    fun addExercise(name: String = "") {
        exercises.add(ExerciseDraft(sanitizeExerciseName(name).toTitleCaseOrDefault()))
        scheduleAutosave()
    }

    fun exerciseNameSuggestions(query: String, limit: Int = 6): List<String> {
        val normalizedQuery = sanitizeExerciseName(query).lowercase(Locale.getDefault())
        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        val candidates = buildSet {
            settings.defaultExercises.forEach { add(it) }
            exercises.forEach { add(it.name) }
            sessions.forEach { session -> session.exercises.forEach { add(it.name) } }
            prebuiltSessions.forEach { session -> session.exercises.forEach { add(it.name) } }
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }

        val usageCounts = buildMap<String, Int> {
            candidates.forEach { candidate -> put(candidate.lowercase(Locale.getDefault()), 0) }
            settings.defaultExercises.forEach { exercise ->
                val key = exercise.lowercase(Locale.getDefault())
                put(key, (get(key) ?: 0) + 3)
            }
            exercises.forEach { draft ->
                val key = draft.name.lowercase(Locale.getDefault())
                put(key, (get(key) ?: 0) + 5)
            }
            sessions.forEach { session ->
                session.exercises.forEach { exercise ->
                    val key = exercise.name.lowercase(Locale.getDefault())
                    put(key, (get(key) ?: 0) + 2)
                }
            }
            prebuiltSessions.forEach { session ->
                session.exercises.forEach { exercise ->
                    val key = exercise.name.lowercase(Locale.getDefault())
                    put(key, (get(key) ?: 0) + 1)
                }
            }
        }

        return candidates
            .mapNotNull { candidate ->
                val score = fuzzyScore(candidate, normalizedQuery) ?: return@mapNotNull null
                candidate to score
            }
            .sortedWith(
                compareByDescending<Pair<String, Int>> { usageCounts[it.first.lowercase(Locale.getDefault())] ?: 0 }
                    .thenBy { it.second }
                    .thenBy { it.first.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
            .take(limit)
    }

    fun removeExercise(id: String) {
        exercises.removeAll { it.id == id }
        scheduleAutosave()
    }

    fun addSet(exerciseId: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.add(SetDraft())
        scheduleAutosave()
    }

    fun removeSet(exerciseId: String, index: Int) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.let { sets ->
            if (index in sets.indices) {
                sets.removeAt(index)
            }
        }
        scheduleAutosave()
    }

    fun updateExerciseName(exerciseId: String, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.name = sanitizeExerciseName(value)
        scheduleAutosave()
    }

    fun updateSetReps(exerciseId: String, index: Int, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.reps = sanitizeNumericText(value)
        scheduleAutosave()
    }

    fun updateSetWeight(exerciseId: String, index: Int, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.weight = sanitizeNumericText(value)
        scheduleAutosave()
    }

    fun toggleSetCompletion(exerciseId: String, index: Int) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.let { set ->
            set.completed = !set.completed
        }
        scheduleAutosave()
    }

    fun sessionVolume(): Double {
        val snapshot = exercises.map { exercise ->
            ExerciseEntry(
                name = exercise.name,
                sets = exercise.sets
                    .filter { it.completed }
                    .map { set ->
                        WorkoutSet(
                            reps = resolveSetReps(set),
                            weight = resolveSetWeight(set),
                            completed = true,
                        )
                    }
            )
        }
        return calculateVolume(snapshot)
    }

    fun saveSession() {
        stopWorkoutTimer()
        val session = toSession(UUID.randomUUID().toString())
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSession(session)
            repository.saveAutosave(session)
            session.bodyWeight?.toFlexibleDoubleOrNull()?.let { weight ->
                repository.saveBodyweightEntry(
                    BodyweightEntry(
                        date = session.savedAt.toLocalDateString(),
                        weight = weight,
                    )
                )
            }
            val updatedSessions = repository.loadSessions()
            val updatedBodyweights = repository.loadBodyweightHistory()
            withContext(Dispatchers.Main) {
                currentSessionId = session.id
                currentSessionSavedAt = session.savedAt
                sessions.clear()
                sessions.addAll(updatedSessions)
                bodyweightHistory.clear()
                bodyweightHistory.addAll(updatedBodyweights)
                trackerBodyWeight = bodyweightHistory.lastOrNull()?.weight?.toString().orEmpty()
                lastSaveMessage = "Session saved"
                recordStrengthEntries()
            }
        }
    }

    fun updateSufferingInput(exerciseName: String, value: Double) {
        sufferingInputs[exerciseName.lowercase().trim()] = value.coerceIn(0.0, 5.0)
    }

    fun buildDualAxisMetrics(): List<DualAxisMetric> {
        val bw = bodyweightHistory.lastOrNull()?.weight ?: 70.0
        val profileMap = exerciseProfiles.associateBy { it.exerciseId.lowercase().trim() }
        return com.heracles.mobile.logic.HerculesMathEngine.buildDualAxisMetrics(
            sessions = sessions.toList(),
            profileMap = profileMap,
            strengthHistory = strengthHistory.toList(),
            sufferingInputs = sufferingInputs.toMap(),
            bodyweightKg = bw,
        )
    }

    fun recordStrengthEntries() {
        val today = java.time.LocalDate.now().toString()
        val entries = exercises.flatMap { exercise ->
            exercise.sets
                .filter { (it.weight.toDoubleOrNull() ?: 0.0) > 0.0 && (it.reps.toIntOrNull() ?: 0) > 0 }
                .map { set ->
                    val w = set.weight.toDoubleOrNull() ?: 0.0
                    val r = set.reps.toIntOrNull() ?: 0
                    val orm = com.heracles.mobile.logic.HerculesMathEngine.computeEpleyOneRepMax(w, r)
                    StrengthHistoryEntry(
                        exerciseName = exercise.name,
                        date = today,
                        estimatedOneRepMax = orm,
                    )
                }
        }
        if (entries.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            entries.forEach { entry -> repository.appendStrengthHistoryEntry(entry) }
            val updated = repository.loadStrengthHistory()
            withContext(Dispatchers.Main) {
                strengthHistory.clear()
                strengthHistory.addAll(updated)
            }
        }
    }

    fun importPrebuiltSessionText() {
        val parsed = parsePrebuiltWorkoutTemplate(pendingPrebuiltText)
        val session = parsed.session
        if (session == null) {
            lastPrebuiltMessage = parsed.error ?: "Unable to import pre-built session"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.savePrebuiltSession(session)
            val updated = repository.loadPrebuiltSessions()
            withContext(Dispatchers.Main) {
                prebuiltSessions.clear()
                prebuiltSessions.addAll(updated)
                pendingPrebuiltText = ""
                lastPrebuiltMessage = "Imported pre-built session"
            }
        }
    }

    fun consumePrebuiltMessage() {
        lastPrebuiltMessage = ""
    }

    fun loadPrebuiltSession(session: PrebuiltWorkoutSession) {
        restoreFromPrebuiltSession(session)
    }

    fun deletePrebuiltSession(session: PrebuiltWorkoutSession) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = repository.deletePrebuiltSession(session.id)
            if (deleted) {
                val updated = repository.loadPrebuiltSessions()
                withContext(Dispatchers.Main) {
                    prebuiltSessions.clear()
                    prebuiltSessions.addAll(updated)
                    lastPrebuiltMessage = "Deleted pre-built session"
                }
            }
        }
    }

    fun consumeSaveMessage() {
        lastSaveMessage = ""
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = repository.deleteSession(session.id)
            if (deleted) {
                val updatedSessions = repository.loadSessions()
                withContext(Dispatchers.Main) {
                    if (currentSessionId == session.id) {
                        currentSessionId = null
                    }
                    sessions.clear()
                    sessions.addAll(updatedSessions)
                }
            }
        }
    }

    fun autosave() {
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        val sessionId = ensureCurrentSessionId()
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400)
            val session = toSession(sessionId)
            repository.saveAutosave(session)
        }
    }

    private fun ensureCurrentSessionId(): String {
        val existingSessionId = currentSessionId
        if (existingSessionId != null) {
            return existingSessionId
        }
        val generatedSessionId = UUID.randomUUID().toString()
        currentSessionId = generatedSessionId
        return generatedSessionId
    }

    private fun toSession(sessionId: String? = currentSessionId): WorkoutSession {
        val entries = exercises.map { exercise ->
            ExerciseEntry(
                name = exercise.name.ifBlank { "Exercise" },
                sets = exercise.sets
                    .filter { it.completed }
                    .map { set ->
                        WorkoutSet(
                            reps = resolveSetReps(set),
                            weight = resolveSetWeight(set),
                            completed = true,
                        )
                    }
            )
        }
        return WorkoutSession(
            id = sessionId ?: UUID.randomUUID().toString(),
            bodyWeight = bodyWeight.toSanitizedNumericString().ifBlank { null },
            workoutDuration = workoutDuration.ifBlank { null },
            exercises = entries,
            volume = sessionVolume(),
        )
    }

    fun canGoBackToLogger(): Boolean {
        return currentScreen != "Logger"
    }

    fun navigateBack(): Boolean {
        if (currentScreen == "Logger") {
            return false
        }
        currentScreen = "Logger"
        return true
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val repository = SessionRepository(context.applicationContext)
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(repository) as T
                }
            }
        }
    }

    private fun String.toTitleCaseOrDefault(): String {
        val normalized = trim()
        if (normalized.isBlank()) {
            return "New Exercise"
        }

        return normalized
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.getDefault()).replaceFirstChar { character ->
                    character.titlecase(Locale.getDefault())
                }
            }
    }

    private fun String.toLocalDateString(): String {
        return runCatching {
            Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }.getOrElse { LocalDate.now().toString() }
    }

    private fun String.toFlexibleDoubleOrNull(): Double? {
        val sanitized = toSanitizedNumericString()
        return sanitized.toDoubleOrNull()
    }

    private fun String.toSanitizedNumericString(): String {
        return sanitizeNumericText(this)
    }

    private fun WorkoutSession.normalizedForEditing(): WorkoutSession {
        return copy(
            bodyWeight = bodyWeight?.toSanitizedNumericString()?.ifBlank { null },
            workoutDuration = workoutDuration?.toSanitizedNumericString()?.ifBlank { null },
            exercises = exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        WorkoutSet(
                            reps = set.reps.coerceAtLeast(0),
                            weight = set.weight.coerceAtLeast(0.0),
                        )
                    }
                )
            }
        )
    }

    private fun resolveSetReps(set: SetDraft): Int {
        return (set.reps.ifBlank { set.ghostReps.orEmpty() }).toIntOrNull() ?: 0
    }

    private fun resolveSetWeight(set: SetDraft): Double {
        return (set.weight.ifBlank { set.ghostWeight.orEmpty() }).toDoubleOrNull() ?: 0.0
    }

    private fun normalizeThemeSelection(source: AppSettings): AppSettings {
        val fallbackMod = themeMods.firstOrNull()?.id ?: "bare_metal"
        val selectedMod = themeMods.firstOrNull { it.id == source.currentModId } ?: themeMods.firstOrNull()

        val validLightScheme = selectedMod?.lightSchemes?.firstOrNull { it.id == source.activeLightSchemeId }
            ?: selectedMod?.lightSchemes?.firstOrNull()
        val validDarkScheme = selectedMod?.darkSchemes?.firstOrNull { it.id == source.activeDarkSchemeId }
            ?: selectedMod?.darkSchemes?.firstOrNull()

        return source.copy(
            currentModId = selectedMod?.id ?: fallbackMod,
            activeLightSchemeId = validLightScheme?.id ?: "default_light",
            activeDarkSchemeId = validDarkScheme?.id ?: "default_dark",
        )
    }

    private fun ThemeMod.normalizedThemeModel(): ThemeMod {
        val normalizedLight = if (lightSchemes.isNotEmpty()) {
            lightSchemes
        } else {
            listOf(
                ThemeColorScheme(
                    id = "default_light",
                    name = "Default Light",
                    tokens = legacyLightTokens ?: TokenSet(background = "#FFFBFE", surface = "#FFFFFF"),
                )
            )
        }

        val normalizedDark = if (darkSchemes.isNotEmpty()) {
            darkSchemes
        } else {
            listOf(
                ThemeColorScheme(
                    id = "default_dark",
                    name = "Default Dark",
                    tokens = legacyDarkTokens ?: TokenSet(),
                )
            )
        }

        val normalizedStyle = style.copy(
            shapeStyle = legacyShapeStyle ?: style.shapeStyle,
            textureRule = if (style.textureRule == "default") {
                legacyLightTokens?.surfaceRule ?: legacyDarkTokens?.surfaceRule ?: "default"
            } else {
                style.textureRule
            },
            wallpaperUri = style.wallpaperUri ?: legacyWallpaperUri,
        )

        return copy(
            lightSchemes = normalizedLight,
            darkSchemes = normalizedDark,
            style = normalizedStyle,
        )
    }

    private fun fuzzyScore(candidate: String, normalizedQuery: String): Int? {
        val value = candidate.lowercase(Locale.getDefault())
        if (value == normalizedQuery) return 0
        if (value.startsWith(normalizedQuery)) return 1
        if (value.contains(normalizedQuery)) return 2

        var queryIndex = 0
        value.forEach { character ->
            if (queryIndex < normalizedQuery.length && character == normalizedQuery[queryIndex]) {
                queryIndex += 1
            }
        }
        if (queryIndex == normalizedQuery.length) return 3
        return null
    }
}

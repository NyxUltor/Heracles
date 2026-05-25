package com.heracles.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.heracles.mobile.model.AppSettings
import com.heracles.mobile.model.BodyweightEntry
import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.model.WorkoutSet
import com.heracles.mobile.logic.calculateVolume
import com.heracles.mobile.logic.sanitizeNumericText
import com.heracles.mobile.storage.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    ) {
        var reps: String by mutableStateOf(initialReps)
        var weight: String by mutableStateOf(initialWeight)
    }

    class ExerciseDraft(name: String) {
        val id: String = UUID.randomUUID().toString()
        var name: String by mutableStateOf(name)
        val sets = mutableStateListOf(SetDraft())
    }

    var settings by mutableStateOf(AppSettings())
        private set

    var currentScreen by mutableStateOf("Logger")
        private set

    var workoutDuration by mutableStateOf("")
        private set

    var bodyWeight by mutableStateOf("")
        private set

    var pendingExerciseName by mutableStateOf("")
        private set

    var pendingLogStoragePath by mutableStateOf("")
        private set

    var lastExportMessage by mutableStateOf("")
        private set

    var trackerBodyWeight by mutableStateOf("")
        private set

    var scrubberGestureActive by mutableStateOf(false)
        private set

    var currentSessionId: String? by mutableStateOf(null)
        private set

    val exercises = mutableStateListOf<ExerciseDraft>()
    val sessions = mutableStateListOf<WorkoutSession>()
    val bodyweightHistory = mutableStateListOf<BodyweightEntry>()

    private var autosaveJob: Job? = null
    private var startupHydrationStarted = false

    init {
        settings = repository.loadSettings()
        pendingLogStoragePath = settings.logStoragePath
        restoreDefaults(scheduleAutosave = false)
    }

    fun switchScreen(name: String) {
        currentScreen = name
    }

    fun updateWorkoutDuration(value: String) {
        workoutDuration = sanitizeNumericText(value)
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
            val autosave = repository.loadAutosave()

            withContext(Dispatchers.Main) {
                sessions.clear()
                sessions.addAll(loadedSessions)
                bodyweightHistory.clear()
                bodyweightHistory.addAll(loadedBodyweights)
                trackerBodyWeight = bodyweightHistory.lastOrNull()?.weight?.toString().orEmpty()

                if (autosave != null && settings.restoreLatestOnOpen && currentScreen == "Logger") {
                    restoreFromSession(autosave)
                }
            }
        }
    }

    fun addPendingExercise() {
        addExercise(pendingExerciseName)
        pendingExerciseName = ""
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = repository.saveSettings(newSettings)
        pendingLogStoragePath = settings.logStoragePath
        autosave()
    }


    fun restoreDefaults(scheduleAutosave: Boolean = true) {
        currentSessionId = null
        exercises.clear()
        settings.defaultExercises.forEach { exercises.add(ExerciseDraft(it)) }
        workoutDuration = ""
        bodyWeight = ""
        if (scheduleAutosave) {
            scheduleAutosave()
        }
    }

    fun startNewSession() {
        restoreDefaults()
        currentScreen = "Logger"
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
        val normalizedSession = session.normalizedForEditing()
        currentSessionId = normalizedSession.id
        bodyWeight = normalizedSession.bodyWeight.orEmpty()
        workoutDuration = normalizedSession.workoutDuration.orEmpty()
        exercises.clear()
        normalizedSession.exercises.forEach { exercise ->
            val draft = ExerciseDraft(exercise.name)
            draft.sets.clear()
            if (exercise.sets.isEmpty()) {
                draft.sets.add(SetDraft())
            } else {
                exercise.sets.forEach { set ->
                    draft.sets.add(SetDraft(set.reps.toString(), set.weight.toString()))
                }
            }
            exercises.add(draft)
        }
        currentScreen = "Logger"
        scheduleAutosave()
    }

    fun addExercise(name: String = "") {
        exercises.add(ExerciseDraft(name.toTitleCaseOrDefault()))
        scheduleAutosave()
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
        exercises.firstOrNull { it.id == exerciseId }?.name = value
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

    fun sessionVolume(): Double {
        val snapshot = exercises.map { exercise ->
            ExerciseEntry(
                name = exercise.name,
                sets = exercise.sets.map { set ->
                    WorkoutSet(
                        reps = set.reps.toIntOrNull() ?: 0,
                        weight = set.weight.toDoubleOrNull() ?: 0.0,
                    )
                }
            )
        }
        return calculateVolume(snapshot)
    }

    fun saveSession() {
        val session = toSession(ensureCurrentSessionId())
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
                sessions.clear()
                sessions.addAll(updatedSessions)
                bodyweightHistory.clear()
                bodyweightHistory.addAll(updatedBodyweights)
                trackerBodyWeight = bodyweightHistory.lastOrNull()?.weight?.toString().orEmpty()
            }
        }
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
        val session = toSession(ensureCurrentSessionId())
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400)
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
                sets = exercise.sets.map { set ->
                    WorkoutSet(
                        reps = set.reps.toIntOrNull() ?: 0,
                        weight = set.weight.toDoubleOrNull() ?: 0.0,
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
}

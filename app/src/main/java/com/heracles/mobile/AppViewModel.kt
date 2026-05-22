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
import com.heracles.mobile.model.ExerciseEntry
import com.heracles.mobile.model.WorkoutSession
import com.heracles.mobile.model.WorkoutSet
import com.heracles.mobile.logic.calculateVolume
import com.heracles.mobile.storage.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    var currentSessionId: String? by mutableStateOf(null)
        private set

    val exercises = mutableStateListOf<ExerciseDraft>()
    val sessions = mutableStateListOf<WorkoutSession>()

    private var autosaveJob: Job? = null

    init {
        settings = repository.loadSettings()
        sessions.addAll(repository.loadSessions())
        val autosave = repository.loadAutosave()
        if (autosave != null && settings.restoreLatestOnOpen) {
            restoreFromSession(autosave)
        } else {
            restoreDefaults()
        }
    }

    fun switchScreen(name: String) {
        currentScreen = name
    }

    fun updateWorkoutDuration(value: String) {
        workoutDuration = value
        autosave()
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        repository.saveSettings(newSettings)
        autosave()
    }

    fun restoreDefaults() {
        currentSessionId = null
        exercises.clear()
        settings.defaultExercises.forEach { exercises.add(ExerciseDraft(it)) }
        if (exercises.isEmpty()) {
            exercises.add(ExerciseDraft("Push Ups"))
            exercises.add(ExerciseDraft("Pull Ups"))
        }
        workoutDuration = ""
        scheduleAutosave()
    }

    fun restoreFromSession(session: WorkoutSession) {
        currentSessionId = session.id
        workoutDuration = session.workoutDuration.orEmpty()
        exercises.clear()
        session.exercises.forEach { exercise ->
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
        if (exercises.isEmpty()) {
            restoreDefaults()
        }
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
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.reps = value
        scheduleAutosave()
    }

    fun updateSetWeight(exerciseId: String, index: Int, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.weight = value
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
            val updatedSessions = repository.loadSessions()
            withContext(Dispatchers.Main) {
                currentSessionId = session.id
                sessions.clear()
                sessions.addAll(updatedSessions)
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
            workoutDuration = workoutDuration.ifBlank { null },
            exercises = entries,
            volume = sessionVolume(),
        )
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
}

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AppViewModel(private val repository: SessionRepository) : ViewModel() {
    data class SetDraft(
        var reps: String by mutableStateOf(reps),
        var weight: String by mutableStateOf(weight),
    )

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

    val exercises = mutableStateListOf<ExerciseDraft>()
    val sessions = mutableStateListOf<WorkoutSession>()

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
        exercises.clear()
        settings.defaultExercises.forEach { exercises.add(ExerciseDraft(it)) }
        if (exercises.isEmpty()) {
            exercises.add(ExerciseDraft("Push Ups"))
            exercises.add(ExerciseDraft("Pull Ups"))
        }
        workoutDuration = ""
    }

    fun restoreFromSession(session: WorkoutSession) {
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
        autosave()
    }

    fun addExercise(name: String = "") {
        exercises.add(ExerciseDraft(name))
        autosave()
    }

    fun removeExercise(id: String) {
        exercises.removeAll { it.id == id }
        autosave()
    }

    fun addSet(exerciseId: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.add(SetDraft())
        autosave()
    }

    fun removeSet(exerciseId: String, index: Int) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.let { sets ->
            if (index in sets.indices) {
                sets.removeAt(index)
            }
        }
        autosave()
    }

    fun updateExerciseName(exerciseId: String, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.name = value
        autosave()
    }

    fun updateSetReps(exerciseId: String, index: Int, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.reps = value
        autosave()
    }

    fun updateSetWeight(exerciseId: String, index: Int, value: String) {
        exercises.firstOrNull { it.id == exerciseId }?.sets?.getOrNull(index)?.weight = value
        autosave()
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
        val session = toSession()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSession(session)
            repository.saveAutosave(session)
            val updatedSessions = repository.loadSessions()
            withContext(Dispatchers.Main) {
                sessions.clear()
                sessions.addAll(updatedSessions)
            }
        }
    }

    fun autosave() {
        val session = toSession()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAutosave(session)
        }
    }

    private fun toSession(): WorkoutSession {
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
}

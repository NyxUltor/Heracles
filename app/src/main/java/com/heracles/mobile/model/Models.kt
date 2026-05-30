package com.heracles.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class WorkoutSet(
    val reps: Int = 0,
    val weight: Double = 0.0,
    val completed: Boolean = false,
)

@Serializable
data class ExerciseEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<WorkoutSet> = emptyList(),
)

@Serializable
data class PrebuiltWorkoutSession(
    val id: String,
    val title: String,
    val sourceText: String,
    val createdAt: String = Instant.now().toString(),
    val bodyWeight: String? = null,
    val workoutDuration: String? = null,
    val exercises: List<PrebuiltWorkoutExercise> = emptyList(),
)

@Serializable
data class PrebuiltWorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<PrebuiltWorkoutSet> = emptyList(),
)

@Serializable
data class PrebuiltWorkoutSet(
    val reps: String = "",
    val weight: String = "",
)

@Serializable
data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val savedAt: String = Instant.now().toString(),
    val bodyWeight: String? = null,
    val workoutDuration: String? = null,
    val exercises: List<ExerciseEntry> = emptyList(),
    val volume: Double = 0.0,
)

@Serializable
data class BodyweightEntry(
    val date: String,
    val weight: Double,
)

@Serializable
data class AppSettings(
    val units: String = "kg",
    val defaultExercises: List<String> = emptyList(),
    val restoreLatestOnOpen: Boolean = true,
    val useDarkTheme: Boolean = false,
    val systemUiMode: SystemUiMode = SystemUiMode.AUTO,
    val uiFidelity: UiFidelityLevel = UiFidelityLevel.BALANCED,
    val currentModId: String = "bare_metal",
    val activeLightSchemeId: String = "default_light",
    val activeDarkSchemeId: String = "default_dark",
    val quickSetupCompleted: Boolean = false,
    val uiScale: Double = 1.0,
    val logStoragePath: String = "sessions",
    val numericInputModes: Set<String> = setOf("keyboard"),
    val scrubberSensitivity: Double = 1.0,
)

@Serializable
enum class SystemUiMode {
    LIGHT,
    DARK,
    AUTO,
}

@Serializable
enum class UiFidelityLevel {
    MINIMAL,
    BALANCED,
    RICH,
}

@Serializable
enum class ShapeMode {
    DEFAULT,
    RECTANGLE,
}

@Serializable
data class TokenSet(
    val primary: String = "#2457C5",
    val secondary: String = "#4D6DB5",
    val background: String = "#1F1F21",
    val surface: String = "#272729",
    val onPrimary: String = "#FFFFFF",
    val borderWidth: Double = 1.0,
    val surfaceRule: String = "default",
)

@Serializable
data class ThemeMod(
    val id: String,
    val name: String,
    val author: String = "Heracles",
    val lightSchemes: List<ThemeColorScheme> = listOf(
        ThemeColorScheme(
            id = "default_light",
            name = "Default Light",
            tokens = TokenSet(background = "#FFFBFE", surface = "#FFFFFF"),
        )
    ),
    val darkSchemes: List<ThemeColorScheme> = listOf(
        ThemeColorScheme(
            id = "default_dark",
            name = "Default Dark",
            tokens = TokenSet(),
        )
    ),
    val style: ThemeStylePack = ThemeStylePack(),
    @SerialName("lightTokens") val legacyLightTokens: TokenSet? = null,
    @SerialName("darkTokens") val legacyDarkTokens: TokenSet? = null,
    @SerialName("shapeStyle") val legacyShapeStyle: ShapeMode? = null,
    @SerialName("wallpaperUri") val legacyWallpaperUri: String? = null,
)

@Serializable
data class ThemeColorScheme(
    val id: String,
    val name: String,
    val tokens: TokenSet,
)

@Serializable
data class ThemeStylePack(
    val shapeStyle: ShapeMode = ShapeMode.DEFAULT,
    val buttonHeightDp: Int = 48,
    val textureRule: String = "default",
    val wallpaperUri: String? = null,
)

@Serializable
data class AppConfigBackup(
    val exportedAt: String = Instant.now().toString(),
    val settings: AppSettings,
    val themeMods: List<ThemeMod>,
)

@Serializable
data class SessionBackup(
    val exportedAt: String = Instant.now().toString(),
    val sessions: List<WorkoutSession>,
)

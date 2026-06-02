/*
 File: model/Models.kt
 What it does: Defines the serializable data model for the app (sessions, exercises, theme tokens, settings).
 Main inputs: persisted JSON configuration and session files loaded by the repository.
 Main outputs: in-memory Kotlin data classes used across the app and persisted backups.
 Key functions/classes: `AppSettings`, `TokenSet`, `ThemeMod`, `WorkoutSession` and related model types.
*/

// Important: model definitions below are central to serialization; keep names and defaults stable.

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
    val curatedSchemeId: String = "hellfire",
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
    CUSTOM,
}

@Serializable
data class CuratedScheme(
    val id: String,
    val name: String,
    val lightTokens: TokenSet,
    val darkTokens: TokenSet,
)

object CuratedSchemes {
    val HELLFIRE = CuratedScheme(
        id = "hellfire",
        name = "HELLFIRE",
        lightTokens = TokenSet(primary = "#EF4444", secondary = "#B91C1C", background = "#FFF7F7", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#EF4444", secondary = "#991B1B", background = "#180707", surface = "#2A0C0C", onPrimary = "#FFFFFF"),
    )
    val BLOOD_AND_WINE = CuratedScheme(
        id = "blood_and_wine",
        name = "BLOOD & WINE",
        lightTokens = TokenSet(primary = "#991B1B", secondary = "#7F1D1D", background = "#FFF7F7", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#991B1B", secondary = "#450A0A", background = "#1A0909", surface = "#2B1111", onPrimary = "#FFFFFF"),
    )
    val ABYSSAL_SKY = CuratedScheme(
        id = "abyssal_sky",
        name = "ABYSSAL SKY",
        lightTokens = TokenSet(primary = "#0EA5E9", secondary = "#0284C7", background = "#F5FBFF", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#0EA5E9", secondary = "#075985", background = "#06131A", surface = "#102735", onPrimary = "#FFFFFF"),
    )
    val DEEP_SEA = CuratedScheme(
        id = "deep_sea",
        name = "DEEP SEA",
        lightTokens = TokenSet(primary = "#2563EB", secondary = "#1D4ED8", background = "#F6F9FF", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#2563EB", secondary = "#1E3A8A", background = "#07111F", surface = "#101B33", onPrimary = "#FFFFFF"),
    )
    val TERMINAL_GRASS = CuratedScheme(
        id = "terminal_grass",
        name = "TERMINAL GRASS",
        lightTokens = TokenSet(primary = "#22C55E", secondary = "#16A34A", background = "#F4FFF7", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#22C55E", secondary = "#166534", background = "#07140B", surface = "#102516", onPrimary = "#FFFFFF"),
    )
    val TOXIC_SLUDGE = CuratedScheme(
        id = "toxic_sludge",
        name = "TOXIC SLUDGE",
        lightTokens = TokenSet(primary = "#84CC16", secondary = "#65A30D", background = "#FBFFF2", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#84CC16", secondary = "#365314", background = "#101403", surface = "#1C2309", onPrimary = "#FFFFFF"),
    )
    val NEON_AMBER = CuratedScheme(
        id = "neon_amber",
        name = "NEON AMBER",
        lightTokens = TokenSet(primary = "#F59E0B", secondary = "#D97706", background = "#FFF9ED", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#F59E0B", secondary = "#92400E", background = "#1A1203", surface = "#2B1D07", onPrimary = "#FFFFFF"),
    )
    val SOLAR_FLARE = CuratedScheme(
        id = "solar_flare",
        name = "SOLAR FLARE",
        lightTokens = TokenSet(primary = "#F97316", secondary = "#EA580C", background = "#FFF8F3", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#F97316", secondary = "#9A3412", background = "#1A1007", surface = "#2D170C", onPrimary = "#FFFFFF"),
    )
    val OBSIDIAN_ASH = CuratedScheme(
        id = "obsidian_ash",
        name = "OBSIDIAN ASH",
        lightTokens = TokenSet(primary = "#64748B", secondary = "#475569", background = "#F7FAFC", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#64748B", secondary = "#334155", background = "#0D1117", surface = "#171C24", onPrimary = "#FFFFFF"),
    )
    val WHITE_PHANTOM = CuratedScheme(
        id = "white_phantom",
        name = "WHITE PHANTOM",
        lightTokens = TokenSet(primary = "#CBD5E1", secondary = "#94A3B8", background = "#FFFFFF", surface = "#F8FAFC", onPrimary = "#0F172A"),
        darkTokens = TokenSet(primary = "#CBD5E1", secondary = "#64748B", background = "#0B1120", surface = "#111827", onPrimary = "#FFFFFF"),
    )
    val DEEP_SLATE_CODE = CuratedScheme(
        id = "deep_slate_code",
        name = "Deep Slate Code",
        lightTokens = TokenSet(primary = "#06B6D4", secondary = "#0EA5E9", background = "#F5FDFF", surface = "#FFFFFF", onPrimary = "#FFFFFF"),
        darkTokens = TokenSet(primary = "#06B6D4", secondary = "#0F766E", background = "#041316", surface = "#0A2026", onPrimary = "#FFFFFF"),
    )
    val BLACK_GOLD = CuratedScheme(
        id = "black_gold",
        name = "Black Gold",
        lightTokens = TokenSet(primary = "#EAB308", secondary = "#A16207", background = "#FFF9E7", surface = "#FFFFFF", onPrimary = "#111827"),
        darkTokens = TokenSet(primary = "#EAB308", secondary = "#713F12", background = "#120D03", surface = "#211805", onPrimary = "#FFFFFF"),
    )

    val ALL = listOf(
        HELLFIRE,
        BLOOD_AND_WINE,
        ABYSSAL_SKY,
        DEEP_SEA,
        TERMINAL_GRASS,
        TOXIC_SLUDGE,
        NEON_AMBER,
        SOLAR_FLARE,
        OBSIDIAN_ASH,
        WHITE_PHANTOM,
        DEEP_SLATE_CODE,
        BLACK_GOLD,
    )
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

@Serializable
data class AxisDistribution(
    val axis: String,
    val fraction: Double,
)

@Serializable
data class ExerciseProfile(
    val exerciseId: String,
    val kBw: Double = 0.0,
    val kStab: Double = 1.0,
    val kRom: Double = 1.0,
    val kCurve: Double = 1.0,
    val kDens: Double = 1.0,
    val kCardio: Double = 1.0,
    val axisDistribution: List<AxisDistribution> = emptyList(),
    val isUnconfigured: Boolean = true,
)

@Serializable
data class StrengthHistoryEntry(
    val exerciseName: String,
    val date: String,
    val estimatedOneRepMax: Double,
)

@Serializable
data class TrackerSufferingInput(
    val exerciseName: String,
    val sReported: Double = 0.0,
)

@Serializable
data class DualAxisMetric(
    val axis: String,
    val avi: Double,
    val rvi: Double,
)

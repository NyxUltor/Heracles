package com.heracles.mobile.logic

import com.heracles.mobile.model.AxisDistribution
import com.heracles.mobile.model.DualAxisMetric
import com.heracles.mobile.model.ExerciseProfile
import com.heracles.mobile.model.StrengthHistoryEntry
import com.heracles.mobile.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object HerculesMathEngine {

    private val AXES = listOf("Push", "Pull", "Legs", "Core", "Cardio")

    private val DEFAULT_AXIS_DISTRIBUTIONS: Map<String, List<AxisDistribution>> = mapOf(
        "bench" to listOf(AxisDistribution("Push", 1.0)),
        "press" to listOf(AxisDistribution("Push", 0.7), AxisDistribution("Pull", 0.3)),
        "overhead press" to listOf(AxisDistribution("Push", 1.0)),
        "dip" to listOf(AxisDistribution("Push", 1.0)),
        "pushup" to listOf(AxisDistribution("Push", 1.0)),
        "push up" to listOf(AxisDistribution("Push", 1.0)),
        "tricep" to listOf(AxisDistribution("Push", 1.0)),
        "chest" to listOf(AxisDistribution("Push", 1.0)),
        "shoulder" to listOf(AxisDistribution("Push", 0.8), AxisDistribution("Pull", 0.2)),
        "pullup" to listOf(AxisDistribution("Pull", 1.0)),
        "pull up" to listOf(AxisDistribution("Pull", 1.0)),
        "chinup" to listOf(AxisDistribution("Pull", 1.0)),
        "chin up" to listOf(AxisDistribution("Pull", 1.0)),
        "row" to listOf(AxisDistribution("Pull", 1.0)),
        "curl" to listOf(AxisDistribution("Pull", 1.0)),
        "lat" to listOf(AxisDistribution("Pull", 1.0)),
        "bicep" to listOf(AxisDistribution("Pull", 1.0)),
        "back" to listOf(AxisDistribution("Pull", 1.0)),
        "reverse fly" to listOf(AxisDistribution("Pull", 1.0)),
        "deadlift" to listOf(AxisDistribution("Legs", 0.6), AxisDistribution("Pull", 0.4)),
        "squat" to listOf(AxisDistribution("Legs", 1.0)),
        "lunge" to listOf(AxisDistribution("Legs", 1.0)),
        "leg press" to listOf(AxisDistribution("Legs", 1.0)),
        "leg curl" to listOf(AxisDistribution("Legs", 1.0)),
        "leg extension" to listOf(AxisDistribution("Legs", 1.0)),
        "hamstring" to listOf(AxisDistribution("Legs", 1.0)),
        "quad" to listOf(AxisDistribution("Legs", 1.0)),
        "calf" to listOf(AxisDistribution("Legs", 1.0)),
        "glute" to listOf(AxisDistribution("Legs", 1.0)),
        "split squat" to listOf(AxisDistribution("Legs", 1.0)),
        "hip thrust" to listOf(AxisDistribution("Legs", 0.8), AxisDistribution("Core", 0.2)),
        "rdl" to listOf(AxisDistribution("Legs", 0.7), AxisDistribution("Pull", 0.3)),
        "plank" to listOf(AxisDistribution("Core", 1.0)),
        "crunch" to listOf(AxisDistribution("Core", 1.0)),
        "situp" to listOf(AxisDistribution("Core", 1.0)),
        "sit up" to listOf(AxisDistribution("Core", 1.0)),
        "leg raise" to listOf(AxisDistribution("Core", 1.0)),
        "hollow" to listOf(AxisDistribution("Core", 1.0)),
        "abs" to listOf(AxisDistribution("Core", 1.0)),
        "core" to listOf(AxisDistribution("Core", 1.0)),
        "run" to listOf(AxisDistribution("Cardio", 1.0)),
        "bike" to listOf(AxisDistribution("Cardio", 1.0)),
        "cardio" to listOf(AxisDistribution("Cardio", 1.0)),
        "jump" to listOf(AxisDistribution("Cardio", 0.6), AxisDistribution("Legs", 0.4)),
        "burpee" to listOf(AxisDistribution("Cardio", 0.5), AxisDistribution("Push", 0.3), AxisDistribution("Core", 0.2)),
        "sled" to listOf(AxisDistribution("Cardio", 0.5), AxisDistribution("Legs", 0.5)),
        "rowing" to listOf(AxisDistribution("Cardio", 0.5), AxisDistribution("Pull", 0.5)),
    )

    fun resolveAxisDistribution(
        exerciseName: String,
        profile: ExerciseProfile?,
    ): List<AxisDistribution> {
        if (profile != null && !profile.isUnconfigured && profile.axisDistribution.isNotEmpty()) {
            return profile.axisDistribution
        }
        val normalized = exerciseName.lowercase().trim()
        for ((keyword, distribution) in DEFAULT_AXIS_DISTRIBUTIONS) {
            if (normalized.contains(keyword)) return distribution
        }
        return listOf(AxisDistribution("Push", 1.0))
    }

    fun computeEpleyOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0.0) return 0.0
        if (reps == 1) return weight

        val a = 0.5202
        val b = 0.4718
        val c = 0.0851

        val denominator = a + b * kotlin.math.exp(-c * reps.toDouble())
        return weight / denominator
    }

    fun computePredictedMaxReps(
        weight: Double,
        oneRepMax: Double,
    ): Double {
        if (oneRepMax <= 0.0 || weight <= 0.0) return 1.0
        if (weight >= oneRepMax) return 1.0

        val a = 0.5202
        val b = 0.4718
        val c = 0.0851

        // Inverting the exponential model to solve for reps given a weight and a known 1RM
        val terms = ((weight / oneRepMax) - a) / b
        if (terms <= 0.0) return 30.0 // Safety cap for ultra-light workloads

        return -kotlin.math.ln(terms) / c
    }

    fun computeOmega(
        rPerformed: Int,
        rPredictedMax: Double,
        sReported: Double,
    ): Double {
        val rPredSafe = rPredictedMax.coerceAtLeast(1.0)
        val sSafe = sReported.coerceIn(0.0, 5.0)
        return (rPerformed.toDouble() / rPredSafe) * (1.0 + sSafe / 10.0)
    }

    fun computeAVI(
        sets: Int,
        reps: Int,
        weight: Double,
        bw: Double,
        profile: ExerciseProfile,
        omega: Double,
        kT: Double,
    ): Double {
        val loadBracket = weight + (bw * profile.kBw)
        val mechanicalMultiplier = profile.kStab * profile.kRom * profile.kCurve * profile.kDens
        return sets * reps * loadBracket * mechanicalMultiplier * omega * kT
    }

    fun computeRVI(
        sets: Int,
        reps: Int,
        weight: Double,
        tMinutes: Double,
        bw: Double,
        profile: ExerciseProfile,
        omega: Double,
        kT: Double,
    ): Double {
        val bwSafe = if (bw <= 0.0) 1.0 else bw
        val loadNumerator = weight + (tMinutes * profile.kCardio)
        val loadBracket = (loadNumerator / bwSafe) + profile.kBw
        val mechanicalMultiplier = profile.kStab * profile.kRom * profile.kCurve * profile.kDens
        return sets * reps * loadBracket * mechanicalMultiplier * omega * kT
    }

    fun computeDynamicKtMap(
        sessions: List<WorkoutSession>,
        profileMap: Map<String, ExerciseProfile>,
        bodyweightKg: Double,
    ): Map<String, Double> {
        val cutoff = LocalDate.now().minusDays(30)
        val recentSessions = sessions.filter { session ->
            runCatching {
                Instant.parse(session.savedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .let { !it.isBefore(cutoff) }
            }.getOrDefault(false)
        }

        val axisTotals = AXES.associateWith { 0.0 }.toMutableMap()

        recentSessions.forEach { session ->
            session.exercises.forEach { exercise ->
                val profile = profileMap[exercise.name.lowercase().trim()]
                    ?: ExerciseProfile(exerciseId = exercise.name, isUnconfigured = true)
                val distribution = resolveAxisDistribution(exercise.name, profile)
                val rawVolume = exercise.sets.sumOf { set ->
                    val omega = 1.0
                    val kT = 1.0
                    computeAVI(
                        sets = 1,
                        reps = set.reps,
                        weight = set.weight,
                        bw = bodyweightKg,
                        profile = profile,
                        omega = omega,
                        kT = kT,
                    )
                }
                distribution.forEach { dist ->
                    val axisKey = dist.axis
                    if (axisKey in axisTotals) {
                        axisTotals[axisKey] = (axisTotals[axisKey] ?: 0.0) + rawVolume * dist.fraction
                    }
                }
            }
        }

        if (axisTotals.values.all { it <= 0.0 }) return AXES.associateWith { 1.0 }

        val mean = axisTotals.values.average()
        return AXES.associateWith { axis ->
            val axisAvg = axisTotals[axis] ?: 0.0
            if (axisAvg <= 0.0) 1.0 else (mean / axisAvg).coerceIn(0.1, 10.0)
        }
    }

    fun buildDualAxisMetrics(
        sessions: List<WorkoutSession>,
        profileMap: Map<String, ExerciseProfile>,
        strengthHistory: List<StrengthHistoryEntry>,
        sufferingInputs: Map<String, Double>,
        bodyweightKg: Double,
    ): List<DualAxisMetric> {
        val ktMap = computeDynamicKtMap(sessions, profileMap, bodyweightKg)
        val axisTotalsAvi = AXES.associateWith { 0.0 }.toMutableMap()
        val axisTotalsRvi = AXES.associateWith { 0.0 }.toMutableMap()

        val latestOneRepMaxByExercise = strengthHistory
            .groupBy { it.exerciseName.lowercase().trim() }
            .mapValues { (_, entries) -> entries.maxByOrNull { it.date }?.estimatedOneRepMax ?: 0.0 }

        sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                val normalizedName = exercise.name.lowercase().trim()
                val profile = profileMap[normalizedName]
                    ?: ExerciseProfile(exerciseId = exercise.name, isUnconfigured = true)
                val distribution = resolveAxisDistribution(exercise.name, profile)
                val kT = distribution.map { ktMap[it.axis] ?: 1.0 }.average()
                val sReported = sufferingInputs[normalizedName] ?: 0.0
                val historicalOrm = latestOneRepMaxByExercise[normalizedName] ?: 0.0

                exercise.sets.forEach { set ->
                    val predictedMax = if (historicalOrm > 0.0) {
                        computePredictedMaxReps(set.weight, historicalOrm)
                    } else {
                        set.reps.toDouble().coerceAtLeast(1.0)
                    }
                    val omega = if (set.reps > 0 && set.weight > 0.0) {
                        computeOmega(set.reps, predictedMax, sReported)
                    } else {
                        1.0
                    }
                    val avi = computeAVI(
                        sets = 1,
                        reps = set.reps,
                        weight = set.weight,
                        bw = bodyweightKg,
                        profile = profile,
                        omega = omega,
                        kT = kT,
                    )
                    val rvi = computeRVI(
                        sets = 1,
                        reps = set.reps,
                        weight = set.weight,
                        tMinutes = 0.0,
                        bw = bodyweightKg,
                        profile = profile,
                        omega = omega,
                        kT = kT,
                    )
                    distribution.forEach { dist ->
                        val ax = dist.axis
                        if (ax in axisTotalsAvi) {
                            axisTotalsAvi[ax] = (axisTotalsAvi[ax] ?: 0.0) + avi * dist.fraction
                            axisTotalsRvi[ax] = (axisTotalsRvi[ax] ?: 0.0) + rvi * dist.fraction
                        }
                    }
                }
            }
        }

        return AXES.map { axis ->
            DualAxisMetric(
                axis = axis,
                avi = axisTotalsAvi[axis] ?: 0.0,
                rvi = axisTotalsRvi[axis] ?: 0.0,
            )
        }
    }
}
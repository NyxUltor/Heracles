/*
 File: logic/NumericInput.kt
 What it does: Helper functions for numeric input parsing and formatting used by scrubbing and text fields.
 Main inputs: raw text strings (user input), drag deltas for scrub behavior.
 Main outputs: sanitized numeric strings or parsed numeric values suitable for storage/UI.
 Key functions/classes: `scrubNumericText`, parsing helpers.
*/

package com.heracles.mobile.logic

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.pow

private const val DEFAULT_SCRUBBER_PIXEL_STEP = 12f

fun sanitizeNumericText(input: String): String {
    val builder = StringBuilder()
    var hasDecimalPoint = false

    input.forEachIndexed { index, character ->
        when {
            character.isDigit() -> builder.append(character)
            character == '.' && !hasDecimalPoint -> {
                builder.append(character)
                hasDecimalPoint = true
            }
            character == '-' && index == 0 -> builder.append(character)
        }
    }

    return builder.toString()
        .let { sanitized -> if (sanitized == "-" || sanitized == "." || sanitized == "-.") "" else sanitized }
}

fun scrubNumericText(
    currentText: String,
    dragX: Float,
    sensitivity: Double,
    decimalPlaces: Int,
    stepPerTick: Double? = null,
): String {
    val baseValue = sanitizeNumericText(currentText).toDoubleOrNull() ?: 0.0
    val unitsPerTick = stepPerTick ?: if (decimalPlaces <= 0) 1.0 else 0.1
    val sensitivityClamped = sensitivity.coerceIn(0.05, 10.0)
    val pixelsPerTick = (DEFAULT_SCRUBBER_PIXEL_STEP / sensitivityClamped).coerceAtLeast(1.0)
    val rawTicks = dragX.toDouble() / pixelsPerTick
    val ticks = if (rawTicks >= 0f) floor(rawTicks.toDouble()).toInt() else ceil(rawTicks.toDouble()).toInt()
    val delta = ticks * unitsPerTick
    var nextValue = baseValue + delta
    // Prevent scrubber from producing negative values for fields that shouldn't go negative
    if (nextValue < 0.0) nextValue = 0.0
    return formatNumericValue(nextValue, decimalPlaces)
}

fun sanitizeExerciseName(input: String): String {
    return input
        .filterNot { it.isISOControl() }
        .trim()
}

fun formatNumericValue(value: Double, decimalPlaces: Int): String {
    return if (decimalPlaces <= 0) {
        value.roundToInt().toString()
    } else {
        val factor = 10.0.pow(decimalPlaces.toDouble())
        val rounded = kotlin.math.round(value * factor) / factor
        val raw = rounded.toString()
        if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
    }
}
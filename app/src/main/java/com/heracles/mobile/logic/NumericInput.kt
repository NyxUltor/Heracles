package com.heracles.mobile.logic

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

fun scrubNumericText(currentText: String, dragX: Float, sensitivity: Double, decimalPlaces: Int): String {
    val baseValue = sanitizeNumericText(currentText).toDoubleOrNull() ?: 0.0
    val unitsPerStep = if (decimalPlaces <= 0) 1.0 else 0.1
    // continuous mapping: dragX scales linearly to value change
    val delta = (dragX / DEFAULT_SCRUBBER_PIXEL_STEP) * sensitivity * unitsPerStep
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
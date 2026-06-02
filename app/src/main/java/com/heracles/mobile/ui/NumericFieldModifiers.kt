/*
 File: ui/NumericFieldModifiers.kt
 What it does: Provides a `Modifier.scrubbableNumericField` to enable pointer-drag scrubbing for numeric inputs.
 Main inputs: pointer gestures, current text value, sensitivity settings.
 Main outputs: calls `onValueChange` with scrubbed numeric strings and toggles scrubbing state via callbacks.
 Key functions/classes: `scrubbableNumericField` extension.
*/

// Note: The pointerInput intentionally omits `text` from keys to avoid cancelling active gestures when text updates.

package com.heracles.mobile.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import com.heracles.mobile.logic.scrubNumericText

fun Modifier.scrubbableNumericField(
    enabled: Boolean,
    text: String,
    sensitivity: Double,
    decimalPlaces: Int,
    stepPerTick: Double? = null,
    onScrubStart: (() -> Unit)? = null,
    onScrubEnd: (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
): Modifier {
    if (!enabled) {
        return this
    }

    var downX = 0f
    var downY = 0f
    var scrubbing = false
    var gestureStartText = text

    // Do not include `text` in the pointerInput keys: restarting the pointer
    // handler when the text changes cancels the ongoing gesture and makes
    // scrubbing stop after the first update. Keep sensitivity/decimalPlaces
    // so changes to those still restart the handler.
    return pointerInput(enabled, sensitivity, decimalPlaces) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            downX = down.position.x
            downY = down.position.y
            gestureStartText = text
            scrubbing = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToUp()) {
                    scrubbing = false
                    onScrubEnd?.invoke()
                    break
                }

                val dragX = change.position.x - downX
                val dragY = kotlin.math.abs(change.position.y - downY)
                if (!scrubbing && kotlin.math.abs(dragX) > 10f && dragY < 80f) {
                    scrubbing = true
                    onScrubStart?.invoke()
                }
                if (scrubbing) {
                    // when scrubbing we mark start/end via callbacks; the screen
                    // will disable parent scrolling using `scrubberGestureActive`.
                    val nextValue = scrubNumericText(
                        currentText = gestureStartText,
                        dragX = dragX,
                        sensitivity = sensitivity,
                        decimalPlaces = decimalPlaces,
                        stepPerTick = stepPerTick,
                    )
                    if (nextValue.isNotBlank() && nextValue != text) {
                        onValueChange(nextValue)
                    }
                }
            }
        }
    }
}
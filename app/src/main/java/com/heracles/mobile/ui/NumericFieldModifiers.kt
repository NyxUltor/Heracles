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
                    val nextValue = scrubNumericText(gestureStartText, dragX, sensitivity, decimalPlaces)
                    if (nextValue.isNotBlank() && nextValue != text) {
                        onValueChange(nextValue)
                    }
                }
            }
        }
    }
}
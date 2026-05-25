package com.heracles.mobile.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class NumericInputEdgeCasesTest {
    @Test
    fun sanitize_empty_and_nonsense() {
        assertEquals("", sanitizeNumericText(""))
        assertEquals("", sanitizeNumericText("abc"))
        assertEquals("", sanitizeNumericText("-"))
        assertEquals("", sanitizeNumericText("."))
        assertEquals("", sanitizeNumericText("-."))
    }

    @Test
    fun sanitize_various_formats() {
        assertEquals("123", sanitizeNumericText(" 1a2b3 "))
        assertEquals("12.34", sanitizeNumericText("12..3.4"))
        assertEquals("-12.3", sanitizeNumericText("-12.3kg"))
        assertEquals(".5", sanitizeNumericText(".5"))
    }

    @Test
    fun scrub_with_zero_sensitivity_returns_same() {
        assertEquals("50", scrubNumericText("50", dragX = 100f, sensitivity = 0.0, decimalPlaces = 0))
        assertEquals("5", scrubNumericText("5.0", dragX = -200f, sensitivity = 0.0, decimalPlaces = 1))
    }

    @Test
    fun scrub_large_positive_and_negative_drag() {
        // With DEFAULT_SCRUBBER_PIXEL_STEP = 24, a 240px drag is ~10 steps
        assertEquals("60", scrubNumericText("50", dragX = 240f, sensitivity = 1.0, decimalPlaces = 0))
        assertEquals("40", scrubNumericText("50", dragX = -240f, sensitivity = 1.0, decimalPlaces = 0))
    }

    @Test
    fun scrub_decimal_step_precision() {
        val result = scrubNumericText("57.2", dragX = 24f, sensitivity = 1.0, decimalPlaces = 1)
        // one step with decimalPlaces=1 increments by 0.1
        assertEquals("57.3", result)
    }
}

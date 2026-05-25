package com.heracles.mobile.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class NumericInputTest {
    @Test
    fun sanitizes_mixed_numeric_text() {
        assertEquals("50", sanitizeNumericText("5b0"))
        assertEquals("12.34", sanitizeNumericText("12a.3b4"))
    }

    @Test
    fun scrubs_integer_values_horizontally() {
        assertEquals("51", scrubNumericText("50", dragX = 24f, sensitivity = 1.0, decimalPlaces = 0))
        assertEquals("49", scrubNumericText("50", dragX = -24f, sensitivity = 1.0, decimalPlaces = 0))
    }

    @Test
    fun scrubs_decimal_values_with_fractional_step() {
        assertEquals("57.3", scrubNumericText("57.2", dragX = 24f, sensitivity = 1.0, decimalPlaces = 1))
    }
}

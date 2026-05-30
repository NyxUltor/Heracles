package com.heracles.mobile.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PrebuiltWorkoutParserTest {
    @Test
    fun parses_strict_workout_template() {
        val result = parsePrebuiltWorkoutTemplate(
            """
            Workout: Push A
            Bodyweight: 82.4
            Duration: 45

            Exercise: Bench Press
            Set: 8 x 60
            Set: 8 x 60
            Set: 6 x 65

            Exercise: Barbell Row
            Set: 10 x 45
            Set: 10 x 45
            """.trimIndent()
        )

        assertNull(result.error)
        val session = result.session
        assertNotNull(session)
        assertEquals("Push A", session!!.title)
        assertEquals("82.4", session.bodyWeight)
        assertEquals("45", session.workoutDuration)
        assertEquals(2, session.exercises.size)
        assertEquals("Bench Press", session.exercises.first().name)
        assertEquals("8", session.exercises.first().sets.first().reps)
        assertEquals("60", session.exercises.first().sets.first().weight)
    }

    @Test
    fun rejects_set_before_exercise() {
        val result = parsePrebuiltWorkoutTemplate(
            """
            Workout: Invalid
            Set: 8 x 60
            """.trimIndent()
        )

        assertNull(result.session)
        assertEquals("Set found before any exercise on line 2.", result.error)
    }
}

package com.example

import com.example.data.repository.AiPlanRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FitJourneyLogicUnitTest {
    @Test
    fun parseWeeklySchedule_validJson_parsesDaysCorrectly() {
        val json = """
            [
              {
                "dayName": "Day 1",
                "focus": "Lower Body",
                "durationMinutes": 40,
                "exercises": ["Bodyweight Squat", "Glute Bridge"],
                "isRestDay": false
              },
              {
                "dayName": "Day 2",
                "focus": "Recovery",
                "durationMinutes": 0,
                "exercises": [],
                "isRestDay": true
              }
            ]
        """.trimIndent()

        val parsed = AiPlanRepository.parseWeeklySchedule(json)
        assertEquals(2, parsed.size)
        assertEquals("Day 1", parsed[0].dayName)
        assertEquals(40, parsed[0].durationMinutes)
        assertFalse(parsed[0].isRestDay)
        assertEquals(2, parsed[0].exercises.size)
        assertTrue(parsed[1].isRestDay)
    }

    @Test
    fun parseDailyMeals_validJson_parsesMacrosCorrectly() {
        val json = """
            [
              {
                "mealType": "Lunch",
                "mealName": "Chicken Quinoa Bowl",
                "calories": 540,
                "proteinGrams": 42,
                "carbsGrams": 48,
                "fatsGrams": 19
              }
            ]
        """.trimIndent()

        val meals = AiPlanRepository.parseDailyMeals(json)
        assertEquals(1, meals.size)
        assertEquals("Chicken Quinoa Bowl", meals[0].mealName)
        assertEquals(540, meals[0].calories)
        assertEquals(42, meals[0].proteinGrams)
    }
}

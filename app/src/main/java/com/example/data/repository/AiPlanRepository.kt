package com.example.data.repository

import android.util.Log
import com.example.data.model.DailyMealEntry
import com.example.data.model.FitnessPlan
import com.example.data.model.UserProfile
import com.example.data.model.WeeklyDayPlan
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AiPlanRepository"

class AiPlanRepository {
    private val planSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Concise title of the personalized plan"),
            "summary" to Schema.string(description = "2-3 sentence program overview and progression strategy"),
            "parqSafetyNote" to Schema.string(description = "Safety guidance based on PAR-Q status"),
            "adjustmentNotes" to Schema.string(description = "Adaptive progression or deload notes"),
            "weeklySchedule" to Schema.array(
                description = "7-day weekly workout schedule",
                items = Schema.obj(
                    properties = mapOf(
                        "dayName" to Schema.string(),
                        "focus" to Schema.string(),
                        "durationMinutes" to Schema.integer(),
                        "exercises" to Schema.array(items = Schema.string()),
                        "isRestDay" to Schema.boolean()
                    )
                )
            ),
            "dailyMeals" to Schema.array(
                description = "Daily nutrition meal plan with macros",
                items = Schema.obj(
                    properties = mapOf(
                        "mealType" to Schema.string(),
                        "mealName" to Schema.string(),
                        "calories" to Schema.integer(),
                        "proteinGrams" to Schema.integer(),
                        "carbsGrams" to Schema.integer(),
                        "fatsGrams" to Schema.integer()
                    )
                )
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planSchema
            }
        )
    }

    /**
     * Generates a structured fitness & nutrition plan via Firebase AI Logic.
     * Privacy invariant: NO personally identifiable information (name, email, UID) is sent to Gemini.
     */
    suspend fun generatePersonalizedPlan(
        profile: UserProfile,
        durationWeeks: Long,
        adherenceRatePercent: Int,
        completedSessionsCount: Int,
        previousVersion: Long,
        languageCode: String
    ): Result<FitnessPlan> {
        val outputLanguage = if (languageCode == "ar") "Arabic" else "English"
        val safetyConstraint = if (profile.parqFlagged) {
            "CRITICAL SAFETY CONSTRAINT: The adult user answered YES to one or more PAR-Q health screening indicators. " +
                "Do NOT prescribe high-intensity, maximal exertion, plyometric, or heavy load exercises. " +
                "Only generate gentle mobility, walking, breathing, and low-impact restorative guidance, and prominently state that medical clearance from a physician is required before starting."
        } else {
            "User passed PAR-Q screening with zero flagged risk indicators. Design an evidence-based progressive routine."
        }

        // Anonymized physiological & behavioral context ONLY (zero PII).
        val prompt = """
            You are a certified strength, conditioning, and sports nutrition specialist.
            Generate a personalized ${durationWeeks}-week fitness and nutrition plan in $outputLanguage.
            
            Anonymized Adult Athlete Metrics (18+):
            - Age: ${profile.age} years
            - Height: ${profile.heightCm} cm
            - Weight: ${profile.weightKg} kg
            - Activity Level: ${profile.activityLevel}
            - Fitness Goal: ${profile.fitnessGoal}
            - Experience Level: ${profile.experienceLevel}
            - Nutrition Preference: ${profile.nutritionPreference}
            - Health/Joint Notes (anonymized): ${profile.healthNotes.ifBlank { "None reported" }}
            - Recent Adherence: $adherenceRatePercent% ($completedSessionsCount sessions logged)
            - Plan Duration: $durationWeeks weeks
            
            $safetyConstraint
            
            Requirements:
            - Provide exactly 7 entries in weeklySchedule covering Day 1 through Day 7.
            - Provide 4 dailyMeals entries (Breakfast, Lunch, Dinner, Snack) with realistic macros matching the nutrition preference.
            - Keep all text in natural $outputLanguage.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Model returned an empty response."))

            val parsed = parseAndValidatePlan(
                rawJson = rawJson,
                userId = profile.userId,
                durationWeeks = durationWeeks,
                version = previousVersion + 1L,
                parqFlagged = profile.parqFlagged
            )
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI plan generation timed out", e)
            Result.failure(IllegalStateException("AI generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI plan generation failed", e)
            Result.failure(e)
        }
    }

    private fun parseAndValidatePlan(
        rawJson: String,
        userId: String,
        durationWeeks: Long,
        version: Long,
        parqFlagged: Boolean
    ): FitnessPlan {
        val root = JSONObject(rawJson)
        val title = root.optString("title").trim().take(115)
        val summary = root.optString("summary").trim().take(950)
        val parqSafetyNote = root.optString("parqSafetyNote").trim().take(480)
        val adjustmentNotes = root.optString("adjustmentNotes").trim().take(950)

        require(title.isNotBlank()) { "Generated plan missing title" }
        require(summary.isNotBlank()) { "Generated plan missing summary" }

        val scheduleArray = root.optJSONArray("weeklySchedule") ?: JSONArray()
        require(scheduleArray.length() > 0) { "Generated plan missing weeklySchedule" }

        val mealsArray = root.optJSONArray("dailyMeals") ?: JSONArray()
        require(mealsArray.length() > 0) { "Generated plan missing dailyMeals" }

        // Validate nested entries cleanly before saving
        for (i in 0 until scheduleArray.length()) {
            val day = scheduleArray.getJSONObject(i)
            require(day.optString("dayName").isNotBlank()) { "Invalid dayName at index $i" }
        }
        for (i in 0 until mealsArray.length()) {
            val meal = mealsArray.getJSONObject(i)
            require(meal.optString("mealName").isNotBlank()) { "Invalid mealName at index $i" }
        }

        val enforcedSafety = if (parqFlagged && parqSafetyNote.isBlank()) {
            "PAR-Q risk indicator flagged: Medical clearance recommended prior to exercise."
        } else {
            parqSafetyNote
        }

        return FitnessPlan(
            userId = userId,
            durationWeeks = durationWeeks,
            title = title,
            summary = summary,
            weeklyScheduleJson = scheduleArray.toString(),
            dailyMealsJson = mealsArray.toString(),
            adjustmentNotes = adjustmentNotes,
            reviewStatus = "pending_review",
            version = version,
            parqSafetyNote = enforcedSafety
        )
    }

    companion object {
        fun parseWeeklySchedule(json: String): List<WeeklyDayPlan> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { idx ->
                    val obj = arr.getJSONObject(idx)
                    val exArr = obj.optJSONArray("exercises") ?: JSONArray()
                    val exercises = (0 until exArr.length()).map { exIdx -> exArr.optString(exIdx) }
                        .filter { it.isNotBlank() }
                    WeeklyDayPlan(
                        dayName = obj.optString("dayName", "Day ${idx + 1}"),
                        focus = obj.optString("focus", ""),
                        durationMinutes = obj.optInt("durationMinutes", 30),
                        exercises = exercises,
                        isRestDay = obj.optBoolean("isRestDay", false)
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun parseDailyMeals(json: String): List<DailyMealEntry> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { idx ->
                    val obj = arr.getJSONObject(idx)
                    DailyMealEntry(
                        mealType = obj.optString("mealType", "Meal"),
                        mealName = obj.optString("mealName", ""),
                        calories = obj.optInt("calories", 0),
                        proteinGrams = obj.optInt("proteinGrams", 0),
                        carbsGrams = obj.optInt("carbsGrams", 0),
                        fatsGrams = obj.optInt("fatsGrams", 0)
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

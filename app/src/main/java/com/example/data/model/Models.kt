package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("uid", uid)
        put("email", email)
        if (grantedBy.isNotBlank()) put("grantedBy", grantedBy)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val age: Long = 25L,
    val heightCm: Double = 172.0,
    val weightKg: Double = 72.0,
    val activityLevel: String = "moderate",
    val fitnessGoal: String = "general_wellness",
    val nutritionPreference: String = "balanced",
    val experienceLevel: String = "beginner",
    val healthNotes: String = "",
    val parqAnswers: List<Boolean> = List(7) { false },
    val parqFlagged: Boolean = false,
    val selectedPlanDurationWeeks: Long = 8L,
    val preferredLanguage: String = "ar",
    val themeMode: String = "system",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName)
        put("age", age)
        put("heightCm", heightCm)
        put("weightKg", weightKg)
        put("activityLevel", activityLevel)
        put("fitnessGoal", fitnessGoal)
        put("nutritionPreference", nutritionPreference)
        put("experienceLevel", experienceLevel)
        put("healthNotes", healthNotes)
        put("parqAnswers", parqAnswers)
        put("parqFlagged", parqFlagged)
        put("selectedPlanDurationWeeks", selectedPlanDurationWeeks)
        put("preferredLanguage", preferredLanguage)
        put("themeMode", themeMode)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toUpdateMap(): Map<String, Any> = buildMap {
        put("displayName", displayName)
        put("age", age)
        put("heightCm", heightCm)
        put("weightKg", weightKg)
        put("activityLevel", activityLevel)
        put("fitnessGoal", fitnessGoal)
        put("nutritionPreference", nutritionPreference)
        put("experienceLevel", experienceLevel)
        put("healthNotes", healthNotes)
        put("parqAnswers", parqAnswers)
        put("parqFlagged", parqFlagged)
        put("selectedPlanDurationWeeks", selectedPlanDurationWeeks)
        put("preferredLanguage", preferredLanguage)
        put("themeMode", themeMode)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val workoutTitle: String = "",
    val durationMinutes: Long = 30L,
    val caloriesBurned: Long = 200L,
    val completed: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("dateIso", dateIso)
        put("workoutTitle", workoutTitle)
        put("durationMinutes", durationMinutes)
        put("caloriesBurned", caloriesBurned)
        put("completed", completed)
        if (notes.isNotBlank()) put("notes", notes)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class MeasurementLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val weightKg: Double = 70.0,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val bodyFatPercent: Double? = null,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("dateIso", dateIso)
        put("weightKg", weightKg)
        waistCm?.let { put("waistCm", it) }
        chestCm?.let { put("chestCm", it) }
        bodyFatPercent?.let { put("bodyFatPercent", it) }
        if (notes.isNotBlank()) put("notes", notes)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class FitnessPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val durationWeeks: Long = 8L,
    val title: String = "",
    val summary: String = "",
    val weeklyScheduleJson: String = "[]",
    val dailyMealsJson: String = "[]",
    val adjustmentNotes: String = "",
    val reviewStatus: String = "pending_review",
    val adminFeedback: String = "",
    val version: Long = 1L,
    val parqSafetyNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("durationWeeks", durationWeeks)
        put("title", title)
        put("summary", summary)
        put("weeklyScheduleJson", weeklyScheduleJson)
        put("dailyMealsJson", dailyMealsJson)
        if (adjustmentNotes.isNotBlank()) put("adjustmentNotes", adjustmentNotes)
        put("reviewStatus", reviewStatus)
        if (adminFeedback.isNotBlank()) put("adminFeedback", adminFeedback)
        put("version", version)
        if (parqSafetyNote.isNotBlank()) put("parqSafetyNote", parqSafetyNote)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class WeeklyDayPlan(
    val dayName: String,
    val focus: String,
    val durationMinutes: Int,
    val exercises: List<String>,
    val isRestDay: Boolean
)

data class DailyMealEntry(
    val mealType: String,
    val mealName: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int
)

@IgnoreExtraProperties
data class ExerciseItem(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val descriptionEn: String = "",
    val descriptionAr: String = "",
    val targetMusclesEn: String = "",
    val targetMusclesAr: String = "",
    val difficulty: String = "beginner",
    val repsOrDuration: String = "3 x 12",
    val equipmentEn: String = "",
    val equipmentAr: String = "",
    val instructionsEn: String = "",
    val instructionsAr: String = "",
    val mistakesEn: String = "",
    val mistakesAr: String = "",
    val animationType: String = "squat",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "descriptionEn" to descriptionEn,
        "descriptionAr" to descriptionAr,
        "targetMusclesEn" to targetMusclesEn,
        "targetMusclesAr" to targetMusclesAr,
        "difficulty" to difficulty,
        "repsOrDuration" to repsOrDuration,
        "equipmentEn" to equipmentEn,
        "equipmentAr" to equipmentAr,
        "instructionsEn" to instructionsEn,
        "instructionsAr" to instructionsAr,
        "mistakesEn" to mistakesEn,
        "mistakesAr" to mistakesAr,
        "animationType" to animationType,
        "isActive" to isActive,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "descriptionEn" to descriptionEn,
        "descriptionAr" to descriptionAr,
        "targetMusclesEn" to targetMusclesEn,
        "targetMusclesAr" to targetMusclesAr,
        "difficulty" to difficulty,
        "repsOrDuration" to repsOrDuration,
        "equipmentEn" to equipmentEn,
        "equipmentAr" to equipmentAr,
        "instructionsEn" to instructionsEn,
        "instructionsAr" to instructionsAr,
        "mistakesEn" to mistakesEn,
        "mistakesAr" to mistakesAr,
        "animationType" to animationType,
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MealItem(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val ingredientsEn: String = "",
    val ingredientsAr: String = "",
    val calories: Long = 450L,
    val proteinGrams: Long = 35L,
    val carbsGrams: Long = 45L,
    val fatsGrams: Long = 14L,
    val prepStepsEn: String = "",
    val prepStepsAr: String = "",
    val imageKey: String = "protein_bowl",
    val category: String = "lunch",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "ingredientsEn" to ingredientsEn,
        "ingredientsAr" to ingredientsAr,
        "calories" to calories,
        "proteinGrams" to proteinGrams,
        "carbsGrams" to carbsGrams,
        "fatsGrams" to fatsGrams,
        "prepStepsEn" to prepStepsEn,
        "prepStepsAr" to prepStepsAr,
        "imageKey" to imageKey,
        "category" to category,
        "isActive" to isActive,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "ingredientsEn" to ingredientsEn,
        "ingredientsAr" to ingredientsAr,
        "calories" to calories,
        "proteinGrams" to proteinGrams,
        "carbsGrams" to carbsGrams,
        "fatsGrams" to fatsGrams,
        "prepStepsEn" to prepStepsEn,
        "prepStepsAr" to prepStepsAr,
        "imageKey" to imageKey,
        "category" to category,
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PlanTemplate(
    @DocumentId val id: String = "",
    val code: String = "8w",
    val durationWeeks: Long = 8L,
    val titleEn: String = "",
    val titleAr: String = "",
    val descriptionEn: String = "",
    val descriptionAr: String = "",
    val isEnabled: Boolean = true,
    val sortOrder: Long = 1L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "code" to code,
        "durationWeeks" to durationWeeks,
        "titleEn" to titleEn,
        "titleAr" to titleAr,
        "descriptionEn" to descriptionEn,
        "descriptionAr" to descriptionAr,
        "isEnabled" to isEnabled,
        "sortOrder" to sortOrder,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CoachRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val goalSummary: String = "",
    val preferredContactMethod: String = "in_app_note",
    val notes: String = "",
    val status: String = "pending",
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("userDisplayName", userDisplayName)
        put("goalSummary", goalSummary)
        put("preferredContactMethod", preferredContactMethod)
        if (notes.isNotBlank()) put("notes", notes)
        put("status", "pending")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val summary: String = "",
    val version: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "actorUid" to actorUid,
        "actorEmail" to actorEmail,
        "action" to action,
        "targetCollection" to targetCollection,
        "targetId" to targetId,
        "summary" to summary,
        "version" to version,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

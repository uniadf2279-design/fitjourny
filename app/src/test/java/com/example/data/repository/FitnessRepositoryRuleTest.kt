package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CoachRequest
import com.example.data.model.FitnessPlan
import com.example.data.model.MeasurementLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FitnessRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun validAdultProfile(uid: String) = UserProfile(
        userId = uid,
        displayName = "Test Athlete",
        age = 27L,
        heightCm = 176.0,
        weightKg = 74.5,
        activityLevel = "moderate",
        fitnessGoal = "muscle_gain",
        nutritionPreference = "high_protein",
        experienceLevel = "intermediate",
        healthNotes = "",
        parqAnswers = List(7) { false },
        parqFlagged = false,
        selectedPlanDurationWeeks = 8L,
        preferredLanguage = "ar",
        themeMode = "system"
    )

    @Test
    fun saveAndObserveUserProfile_validAdult_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitnessRepository(firestore, auth)

        val existing = withTimeout(DEFAULT_TIMEOUT_MS) { repo.observeUserProfile(uid).first() }
        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserProfile(validAdultProfile(uid), isNew = existing == null)
        }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertEquals(27L, observed?.age)
        assertEquals("muscle_gain", observed?.fitnessGoal)
    }

    @Test
    fun addAndObserveWorkoutLogsAndMeasurements_owner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitnessRepository(firestore, auth)

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addWorkoutLog(
                WorkoutLog(
                    userId = uid,
                    dateIso = "2026-11-14",
                    workoutTitle = "Upper Body Strength ${UUID.randomUUID().toString().take(4)}",
                    durationMinutes = 40L,
                    caloriesBurned = 290L,
                    completed = true
                )
            )
        }
        assertTrue(logRes.isSuccess)
        val logId = logRes.getOrThrow()

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeWorkoutLogs(uid).first { list -> list.any { it.id == logId } }
        }
        assertTrue(logs.any { it.id == logId })

        val measRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addMeasurement(
                MeasurementLog(
                    userId = uid,
                    dateIso = "2026-11-14",
                    weightKg = 74.2,
                    waistCm = 81.0
                )
            )
        }
        assertTrue(measRes.isSuccess)
    }

    @Test
    fun saveFitnessPlanAndCoachRequest_owner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FitnessRepository(firestore, auth)

        val planRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveFitnessPlan(
                FitnessPlan(
                    userId = uid,
                    durationWeeks = 8L,
                    title = "8-Week Fit Journey",
                    summary = "Balanced progressive training",
                    weeklyScheduleJson = "[{\"dayName\":\"Day 1\",\"focus\":\"Full Body\"}]",
                    dailyMealsJson = "[{\"mealType\":\"Lunch\",\"mealName\":\"Quinoa Bowl\"}]",
                    reviewStatus = "pending_review",
                    version = 1L
                )
            )
        }
        assertTrue(planRes.isSuccess)

        val coachRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitCoachRequest(
                CoachRequest(
                    userId = uid,
                    userDisplayName = "Alice",
                    goalSummary = "Need form check on squats",
                    preferredContactMethod = "in_app_note"
                )
            )
        }
        assertTrue(coachRes.isSuccess)
    }

    @Test
    fun observeWorkoutLogs_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        signInTestUser(BOB_EMAIL)
        val bobRepo = FitnessRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeWorkoutLogs(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeWorkoutLogs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FitnessRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeWorkoutLogs("some_user_id").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_fit@test.com"
        const val BOB_EMAIL = "bob_fit@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

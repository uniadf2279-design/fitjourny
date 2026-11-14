package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.PreferencesRepository
import com.example.core.ThemePreference
import com.example.core.UiState
import com.example.data.model.CoachRequest
import com.example.data.model.DefaultCatalogData
import com.example.data.model.ExerciseItem
import com.example.data.model.FitnessPlan
import com.example.data.model.MealItem
import com.example.data.model.MeasurementLog
import com.example.data.model.PlanTemplate
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.repository.AiPlanRepository
import com.example.data.repository.FitnessRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data object Loading : AiGenerationState
    data class Success(val message: String) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

class FitJourneyViewModel(
    private val repository: FitnessRepository,
    private val aiRepository: AiPlanRepository,
    private val preferencesRepository: PreferencesRepository,
    val currentUserId: String,
    val currentUserDisplayName: String,
    val currentUserEmail: String?,
    val isEmailVerified: Boolean
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(
        tagName: String,
        fallbackError: String
    ): StateFlow<UiState<T>> = this
        .map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing $tagName", rootError)
            emit(UiState.Error(rootError.message ?: fallbackError))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun <T> Flow<List<T>>.toListStateFlow(
        tagName: String,
        defaultValue: List<T> = emptyList()
    ): StateFlow<List<T>> = this
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing $tagName", firestoreError ?: error)
            emit(defaultValue)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = defaultValue
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("userProfile", "Failed to load profile")

    val workoutLogs: StateFlow<List<WorkoutLog>> =
        repository.observeWorkoutLogs(currentUserId)
            .toListStateFlow("workoutLogs")

    val measurements: StateFlow<List<MeasurementLog>> =
        repository.observeMeasurements(currentUserId)
            .toListStateFlow("measurements")

    val plans: StateFlow<List<FitnessPlan>> =
        repository.observePlans(currentUserId)
            .toListStateFlow("plans")

    val exercises: StateFlow<List<ExerciseItem>> =
        repository.observeExercises()
            .toListStateFlow("exercises", DefaultCatalogData.defaultExercises)

    val meals: StateFlow<List<MealItem>> =
        repository.observeMeals()
            .toListStateFlow("meals", DefaultCatalogData.defaultMeals)

    val planTemplates: StateFlow<List<PlanTemplate>> =
        repository.observePlanTemplates()
            .toListStateFlow("planTemplates", DefaultCatalogData.defaultPlanTemplates)

    val coachRequests: StateFlow<List<CoachRequest>> =
        repository.observeUserCoachRequests(currentUserId)
            .toListStateFlow("coachRequests")

    val isAdmin: StateFlow<Boolean> =
        repository.observeIsAdmin(currentUserId, currentUserEmail, isEmailVerified)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = currentUserEmail == FitnessRepository.BOOTSTRAP_ADMIN_EMAIL && isEmailVerified
            )

    private val _aiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiState: StateFlow<AiGenerationState> = _aiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionStateReset()
    }

    private fun _actionStateReset() {
        _actionMessage.value = null
    }

    fun clearAiState() {
        _aiState.value = AiGenerationState.Idle
    }

    fun setLanguage(language: AppLanguage, currentProfile: UserProfile?) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            if (currentProfile != null) {
                repository.saveUserProfile(
                    currentProfile.copy(preferredLanguage = language.code),
                    isNew = false
                )
            }
        }
    }

    fun setTheme(theme: ThemePreference, currentProfile: UserProfile?) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
            if (currentProfile != null) {
                repository.saveUserProfile(
                    currentProfile.copy(themeMode = theme.code),
                    isNew = false
                )
            }
        }
    }

    fun saveProfile(
        profile: UserProfile,
        isNew: Boolean,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveUserProfile(profile.copy(userId = currentUserId), isNew)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
            }
            onComplete(result)
        }
    }

    fun logWorkout(title: String, durationMinutes: Long, caloriesBurned: Long, notes: String) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val result = repository.addWorkoutLog(
                WorkoutLog(
                    userId = currentUserId,
                    dateIso = today,
                    workoutTitle = title.trim(),
                    durationMinutes = durationMinutes.coerceIn(1L, 300L),
                    caloriesBurned = caloriesBurned.coerceIn(0L, 3000L),
                    completed = true,
                    notes = notes.trim()
                )
            )
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to log workout"
            }
        }
    }

    fun deleteWorkoutLog(logId: String) {
        viewModelScope.launch {
            repository.deleteWorkoutLog(currentUserId, logId)
        }
    }

    fun logMeasurement(
        weightKg: Double,
        waistCm: Double?,
        chestCm: Double?,
        bodyFatPercent: Double?,
        notes: String
    ) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val result = repository.addMeasurement(
                MeasurementLog(
                    userId = currentUserId,
                    dateIso = today,
                    weightKg = weightKg.coerceIn(30.0, 300.0),
                    waistCm = waistCm?.coerceIn(40.0, 200.0),
                    chestCm = chestCm?.coerceIn(50.0, 200.0),
                    bodyFatPercent = bodyFatPercent?.coerceIn(3.0, 70.0),
                    notes = notes.trim()
                )
            )
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to log measurement"
            }
        }
    }

    fun generateOrAdaptPlan(profile: UserProfile, durationWeeks: Long, languageCode: String) {
        viewModelScope.launch {
            _aiState.value = AiGenerationState.Loading
            val completedCount = workoutLogs.value.count { it.completed }
            val adherence = ((completedCount.coerceAtMost(20) / 20.0) * 100).toInt().coerceIn(0, 100)
            val prevVersion = plans.value.maxOfOrNull { it.version } ?: 0L

            val genResult = aiRepository.generatePersonalizedPlan(
                profile = profile,
                durationWeeks = durationWeeks,
                adherenceRatePercent = adherence,
                completedSessionsCount = completedCount,
                previousVersion = prevVersion,
                languageCode = languageCode
            )

            genResult.fold(
                onSuccess = { generatedPlan ->
                    val saveResult = repository.saveFitnessPlan(generatedPlan)
                    saveResult.fold(
                        onSuccess = {
                            _aiState.value = AiGenerationState.Success(generatedPlan.title)
                        },
                        onFailure = { err ->
                            _aiState.value = AiGenerationState.Error(err.localizedMessage ?: "Failed to save plan")
                        }
                    )
                },
                onFailure = { err ->
                    _aiState.value = AiGenerationState.Error(err.localizedMessage ?: "AI plan generation failed")
                }
            )
        }
    }

    fun submitCoachRequest(
        displayName: String,
        goalSummary: String,
        contactMethod: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.submitCoachRequest(
                CoachRequest(
                    userId = currentUserId,
                    userDisplayName = displayName.ifBlank { currentUserDisplayName },
                    goalSummary = goalSummary.trim(),
                    preferredContactMethod = contactMethod,
                    notes = notes.trim()
                )
            )
            if (result.isSuccess) {
                onSuccess()
            } else {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit coach request"
            }
        }
    }

    private companion object {
        const val TAG = "FitJourneyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

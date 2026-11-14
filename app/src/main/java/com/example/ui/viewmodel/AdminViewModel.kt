package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRecord
import com.example.data.model.AuditLog
import com.example.data.model.CoachRequest
import com.example.data.model.DefaultCatalogData
import com.example.data.model.ExerciseItem
import com.example.data.model.FitnessPlan
import com.example.data.model.MealItem
import com.example.data.model.PlanTemplate
import com.example.data.model.UserProfile
import com.example.data.repository.FitnessRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: FitnessRepository
) : ViewModel() {

    private fun <T> Flow<List<T>>.toAdminStateFlow(
        tagName: String,
        initial: List<T> = emptyList()
    ): StateFlow<List<T>> = this
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Admin stream error ($tagName)", firestoreError ?: error)
            emit(initial)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initial
        )

    val allUsers: StateFlow<List<UserProfile>> =
        repository.observeAllUsersAdmin().toAdminStateFlow("users")

    val allPlans: StateFlow<List<FitnessPlan>> =
        repository.observeAllPlansAdmin().toAdminStateFlow("plans")

    val allCoachRequests: StateFlow<List<CoachRequest>> =
        repository.observeAllCoachRequestsAdmin().toAdminStateFlow("coachRequests")

    val auditLogs: StateFlow<List<AuditLog>> =
        repository.observeAuditLogsAdmin().toAdminStateFlow("auditLogs")

    val adminRecords: StateFlow<List<AdminRecord>> =
        repository.observeAdminRecords().toAdminStateFlow("admins")

    val exercises: StateFlow<List<ExerciseItem>> =
        repository.observeExercises().toAdminStateFlow("exercises", DefaultCatalogData.defaultExercises)

    val meals: StateFlow<List<MealItem>> =
        repository.observeMeals().toAdminStateFlow("meals", DefaultCatalogData.defaultMeals)

    val planTemplates: StateFlow<List<PlanTemplate>> =
        repository.observePlanTemplates().toAdminStateFlow("planTemplates", DefaultCatalogData.defaultPlanTemplates)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun seedDefaults() {
        viewModelScope.launch {
            val res = repository.adminSeedCatalogDefaults()
            _statusMessage.value = if (res.isSuccess) "Catalog defaults synced to Firestore"
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to sync defaults"
        }
    }

    fun upsertExercise(item: ExerciseItem, isCreate: Boolean) {
        viewModelScope.launch {
            val res = repository.adminUpsertExercise(item, isCreate)
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save exercise"
            }
        }
    }

    fun deleteExercise(id: String) {
        viewModelScope.launch {
            repository.adminDeleteExercise(id)
        }
    }

    fun upsertMeal(item: MealItem, isCreate: Boolean) {
        viewModelScope.launch {
            val res = repository.adminUpsertMeal(item, isCreate)
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save meal"
            }
        }
    }

    fun deleteMeal(id: String) {
        viewModelScope.launch {
            repository.adminDeleteMeal(id)
        }
    }

    fun updatePlanTemplate(template: PlanTemplate) {
        viewModelScope.launch {
            repository.adminUpsertPlanTemplate(template)
        }
    }

    fun reviewPlan(
        targetUserId: String,
        planId: String,
        status: String,
        feedback: String,
        editedTitle: String?,
        editedSummary: String?,
        currentVersion: Long
    ) {
        viewModelScope.launch {
            val res = repository.adminReviewPlan(
                targetUserId = targetUserId,
                planId = planId,
                status = status,
                feedback = feedback,
                editedTitle = editedTitle,
                editedSummary = editedSummary,
                newVersion = currentVersion + 1L
            )
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update plan"
            }
        }
    }

    fun updateCoachRequest(requestId: String, status: String, adminNotes: String) {
        viewModelScope.launch {
            repository.adminUpdateCoachRequest(requestId, status, adminNotes)
        }
    }

    fun grantAdmin(uid: String, email: String) {
        viewModelScope.launch {
            val res = repository.adminGrantAccess(uid.trim(), email.trim())
            if (res.isFailure) {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to grant admin"
            }
        }
    }

    fun revokeAdmin(uid: String) {
        viewModelScope.launch {
            repository.adminRevokeAccess(uid)
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}

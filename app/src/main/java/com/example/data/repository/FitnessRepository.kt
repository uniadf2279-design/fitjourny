package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AdminRecord
import com.example.data.model.AuditLog
import com.example.data.model.CoachRequest
import com.example.data.model.DefaultCatalogData
import com.example.data.model.ExerciseItem
import com.example.data.model.FitnessPlan
import com.example.data.model.MealItem
import com.example.data.model.MeasurementLog
import com.example.data.model.PlanTemplate
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FitnessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext),
        Firebase.auth
    )

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "uni.adf2279@gmail.com"

        private fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            try {
                // Keep sensitive health/PAR-Q data in memory only; never persist to disk cache.
                instance.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            } catch (_: IllegalStateException) {
                // Firestore settings were already applied for this singleton instance.
            }
            return instance
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> handleSnapshotError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String? = null
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleSnapshotError<UserProfile?>(error, OperationType.GET, path, userId)
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile, isNew: Boolean): Result<Unit> {
        val uid = requireUserId()
        require(profile.age >= 18L) { "Adults 18+ only" }
        val docRef = db.collection("users").document(uid)
        return try {
            val payload = if (isNew) {
                profile.copy(userId = uid).toCreateMap()
            } else {
                profile.copy(userId = uid).toUpdateMap()
            }
            if (isNew) {
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeWorkoutLogs(userId: String): Flow<List<WorkoutLog>> = flow {
        val path = "users/$userId/workoutLogs"
        emitAll(
            db.collection("users").document(userId).collection("workoutLogs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(WorkoutLog::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.dateIso }
                }
                .catch { error ->
                    handleSnapshotError<List<WorkoutLog>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun addWorkoutLog(log: WorkoutLog): Result<String> {
        val uid = requireUserId()
        val id = "wl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("workoutLogs").document(id)
        return try {
            docRef.set(log.copy(userId = uid).toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkoutLog(userId: String, logId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("workoutLogs").document(logId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMeasurements(userId: String): Flow<List<MeasurementLog>> = flow {
        val path = "users/$userId/measurements"
        emitAll(
            db.collection("users").document(userId).collection("measurements")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MeasurementLog::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.dateIso }
                }
                .catch { error ->
                    handleSnapshotError<List<MeasurementLog>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun addMeasurement(log: MeasurementLog): Result<String> {
        val uid = requireUserId()
        val id = "ms_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("measurements").document(id)
        return try {
            docRef.set(log.copy(userId = uid).toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePlans(userId: String): Flow<List<FitnessPlan>> = flow {
        val path = "users/$userId/plans"
        emitAll(
            db.collection("users").document(userId).collection("plans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FitnessPlan::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.version }
                }
                .catch { error ->
                    handleSnapshotError<List<FitnessPlan>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun saveFitnessPlan(plan: FitnessPlan): Result<String> {
        val uid = requireUserId()
        val id = if (plan.id.isNotBlank()) plan.id else "plan_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("plans").document(id)
        return try {
            val sanitized = plan.copy(userId = uid, reviewStatus = "pending_review")
            docRef.set(sanitized.toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeExercises(): Flow<List<ExerciseItem>> = flow {
        val path = "exercises"
        emitAll(
            db.collection("exercises")
                .snapshots()
                .map { snap ->
                    val remote = snap.toObjects(ExerciseItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    if (remote.isEmpty()) DefaultCatalogData.defaultExercises else remote
                }
                .catch { error ->
                    handleSnapshotError<List<ExerciseItem>>(error, OperationType.LIST, path)
                }
        )
    }

    fun observeMeals(): Flow<List<MealItem>> = flow {
        val path = "meals"
        emitAll(
            db.collection("meals")
                .snapshots()
                .map { snap ->
                    val remote = snap.toObjects(MealItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    if (remote.isEmpty()) DefaultCatalogData.defaultMeals else remote
                }
                .catch { error ->
                    handleSnapshotError<List<MealItem>>(error, OperationType.LIST, path)
                }
        )
    }

    fun observePlanTemplates(): Flow<List<PlanTemplate>> = flow {
        val path = "planTemplates"
        emitAll(
            db.collection("planTemplates")
                .snapshots()
                .map { snap ->
                    val remote = snap.toObjects(PlanTemplate::class.java, ServerTimestampBehavior.ESTIMATE)
                    val list = if (remote.isEmpty()) DefaultCatalogData.defaultPlanTemplates else remote
                    list.sortedBy { it.sortOrder }
                }
                .catch { error ->
                    handleSnapshotError<List<PlanTemplate>>(error, OperationType.LIST, path)
                }
        )
    }

    fun observeUserCoachRequests(userId: String): Flow<List<CoachRequest>> = flow {
        val path = "coachRequests"
        emitAll(
            db.collection("coachRequests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CoachRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleSnapshotError<List<CoachRequest>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun submitCoachRequest(request: CoachRequest): Result<String> {
        val uid = requireUserId()
        val id = "cr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("coachRequests").document(id)
        return try {
            docRef.set(request.copy(userId = uid, status = "pending").toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeIsAdmin(userId: String, email: String?, emailVerified: Boolean): Flow<Boolean> {
        if (email == BOOTSTRAP_ADMIN_EMAIL && emailVerified) {
            return flowOf(true)
        }
        val path = "admins/$userId"
        return db.collection("admins").document(userId)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(false)
            }
    }

    // --- Admin Operations ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleSnapshotError<List<UserProfile>>(error, OperationType.LIST, path) }
        )
    }

    fun observeAllCoachRequestsAdmin(): Flow<List<CoachRequest>> = flow {
        val path = "coachRequests"
        emitAll(
            db.collection("coachRequests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CoachRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleSnapshotError<List<CoachRequest>>(error, OperationType.LIST, path) }
        )
    }

    fun observeAllPlansAdmin(): Flow<List<FitnessPlan>> = flow {
        val path = "plans(collectionGroup)"
        emitAll(
            db.collectionGroup("plans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FitnessPlan::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error -> handleSnapshotError<List<FitnessPlan>>(error, OperationType.LIST, path) }
        )
    }

    fun observeAuditLogsAdmin(): Flow<List<AuditLog>> = flow {
        val path = "auditLogs"
        emitAll(
            db.collection("auditLogs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLog::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleSnapshotError<List<AuditLog>>(error, OperationType.LIST, path) }
        )
    }

    fun observeAdminRecords(): Flow<List<AdminRecord>> = flow {
        val path = "admins"
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { snap -> snap.toObjects(AdminRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleSnapshotError<List<AdminRecord>>(error, OperationType.LIST, path) }
        )
    }

    private suspend fun writeAuditLog(
        action: String,
        targetCollection: String,
        targetId: String,
        summary: String,
        version: Long = 1L
    ) {
        val uid = requireUserId()
        val email = auth.currentUser?.email.orEmpty()
        val id = "aud_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("auditLogs").document(id)
        try {
            docRef.set(
                AuditLog(
                    actorUid = uid,
                    actorEmail = email,
                    action = action,
                    targetCollection = targetCollection,
                    targetId = targetId,
                    summary = summary.take(390),
                    version = version
                ).toCreateMap()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    suspend fun adminSeedCatalogDefaults(): Result<Unit> {
        requireUserId()
        return try {
            val batch = db.batch()
            DefaultCatalogData.defaultExercises.forEach { ex ->
                batch.set(db.collection("exercises").document(ex.id), ex.toCreateMap())
            }
            DefaultCatalogData.defaultMeals.forEach { meal ->
                batch.set(db.collection("meals").document(meal.id), meal.toCreateMap())
            }
            DefaultCatalogData.defaultPlanTemplates.forEach { tpl ->
                batch.set(db.collection("planTemplates").document(tpl.id), tpl.toMap())
            }
            batch.commit().await()
            writeAuditLog("SEED_CATALOG", "catalog", "defaults", "Synced default exercises, meals, and plan templates")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "catalog")
            Result.failure(e)
        }
    }

    suspend fun adminUpsertExercise(item: ExerciseItem, isCreate: Boolean): Result<Unit> {
        requireUserId()
        val id = if (item.id.isNotBlank()) item.id else "ex_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("exercises").document(id)
        return try {
            if (isCreate) {
                docRef.set(item.toCreateMap()).await()
            } else {
                docRef.update(item.toUpdateMap()).await()
            }
            writeAuditLog(
                if (isCreate) "CREATE_EXERCISE" else "UPDATE_EXERCISE",
                "exercises",
                id,
                "${if (isCreate) "Created" else "Updated"} exercise: ${item.nameEn}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteExercise(exerciseId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("exercises").document(exerciseId)
        return try {
            docRef.delete().await()
            writeAuditLog("DELETE_EXERCISE", "exercises", exerciseId, "Deleted exercise $exerciseId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpsertMeal(item: MealItem, isCreate: Boolean): Result<Unit> {
        requireUserId()
        val id = if (item.id.isNotBlank()) item.id else "meal_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("meals").document(id)
        return try {
            if (isCreate) {
                docRef.set(item.toCreateMap()).await()
            } else {
                docRef.update(item.toUpdateMap()).await()
            }
            writeAuditLog(
                if (isCreate) "CREATE_MEAL" else "UPDATE_MEAL",
                "meals",
                id,
                "${if (isCreate) "Created" else "Updated"} meal: ${item.nameEn}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteMeal(mealId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("meals").document(mealId)
        return try {
            docRef.delete().await()
            writeAuditLog("DELETE_MEAL", "meals", mealId, "Deleted meal $mealId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpsertPlanTemplate(template: PlanTemplate): Result<Unit> {
        requireUserId()
        val id = if (template.id.isNotBlank()) template.id else "plan_${template.code}"
        val docRef = db.collection("planTemplates").document(id)
        return try {
            docRef.set(template.toMap()).await()
            writeAuditLog("UPDATE_PLAN_TEMPLATE", "planTemplates", id, "Updated plan template ${template.code} (enabled=${template.isEnabled})")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminReviewPlan(
        targetUserId: String,
        planId: String,
        status: String,
        feedback: String,
        editedTitle: String?,
        editedSummary: String?,
        newVersion: Long
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("users").document(targetUserId).collection("plans").document(planId)
        val updates = buildMap<String, Any> {
            put("reviewStatus", status)
            put("adminFeedback", feedback)
            put("version", newVersion)
            if (!editedTitle.isNullOrBlank()) put("title", editedTitle)
            if (!editedSummary.isNullOrBlank()) put("summary", editedSummary)
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.update(updates).await()
            writeAuditLog(
                "REVIEW_AI_PLAN",
                "plans",
                planId,
                "Set status=$status (v$newVersion) for user $targetUserId",
                newVersion
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateCoachRequest(
        requestId: String,
        status: String,
        adminNotes: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("coachRequests").document(requestId)
        val updates = mapOf(
            "status" to status,
            "adminNotes" to adminNotes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            writeAuditLog("UPDATE_COACH_REQUEST", "coachRequests", requestId, "Updated coach request status to $status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminGrantAccess(targetUid: String, targetEmail: String): Result<Unit> {
        val actorUid = requireUserId()
        val docRef = db.collection("admins").document(targetUid)
        return try {
            docRef.set(
                AdminRecord(
                    uid = targetUid,
                    email = targetEmail,
                    grantedBy = actorUid
                ).toCreateMap()
            ).await()
            writeAuditLog("GRANT_ADMIN", "admins", targetUid, "Granted admin role to $targetEmail")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminRevokeAccess(targetUid: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("admins").document(targetUid)
        return try {
            docRef.delete().await()
            writeAuditLog("REVOKE_ADMIN", "admins", targetUid, "Revoked admin role for $targetUid")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}

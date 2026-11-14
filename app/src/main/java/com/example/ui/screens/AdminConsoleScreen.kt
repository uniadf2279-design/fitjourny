package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.LocalFitStrings
import com.example.data.model.ExerciseItem
import com.example.data.model.FitnessPlan
import com.example.data.model.MealItem
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = LocalFitStrings.current
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val exercises by adminViewModel.exercises.collectAsStateWithLifecycle()
    val meals by adminViewModel.meals.collectAsStateWithLifecycle()
    val planTemplates by adminViewModel.planTemplates.collectAsStateWithLifecycle()
    val plans by adminViewModel.allPlans.collectAsStateWithLifecycle()
    val coachRequests by adminViewModel.allCoachRequests.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val admins by adminViewModel.adminRecords.collectAsStateWithLifecycle()
    val statusMessage by adminViewModel.statusMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<FitnessPlan?>(null) }
    var showGrantAdminDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        strings.adminTabUsers,
        strings.adminTabExercises,
        strings.adminTabMeals,
        strings.adminTabPlans,
        strings.adminTabAiReview,
        strings.adminTabCoach,
        strings.adminTabAudit
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.adminTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { adminViewModel.seedDefaults() },
                        modifier = Modifier.testTag("admin_sync_defaults_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = strings.adminSyncDefaultsBtn)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!statusMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusMessage.orEmpty(), modifier = Modifier.weight(1f))
                        TextButton(onClick = { adminViewModel.clearStatusMessage() }) {
                            Text("OK")
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { index ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(tabs[index]) },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedTab) {
                    // 0. Users & Permissions
                    0 -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${strings.adminTabUsers} (${users.size}) • Admins (${admins.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Button(onClick = { showGrantAdminDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (strings.isArabic) "منح صلاحية" else "Grant Admin")
                                }
                            }
                        }
                        items(admins, key = { it.uid }) { adm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(adm.email, fontWeight = FontWeight.Bold)
                                        Text("UID: ${adm.uid}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { adminViewModel.revokeAdmin(adm.uid) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.deleteBtn)
                                    }
                                }
                            }
                        }
                        items(users, key = { it.userId }) { u ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${u.displayName} (${u.age} yrs)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Goal: ${u.fitnessGoal} • Plan: ${u.selectedPlanDurationWeeks}w • PAR-Q Flag: ${u.parqFlagged}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "UID: ${u.userId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 1. Exercises Management
                    1 -> {
                        item {
                            Button(
                                onClick = { showAddExerciseDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.addExerciseBtn)
                            }
                        }
                        items(exercises, key = { it.id }) { ex ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ex.nameEn} / ${ex.nameAr}", fontWeight = FontWeight.Bold)
                                        Text("${ex.difficulty} • ${ex.repsOrDuration}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { adminViewModel.deleteExercise(ex.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.deleteBtn)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Meals Management
                    2 -> {
                        item {
                            Button(
                                onClick = { showAddMealDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.addMealBtn)
                            }
                        }
                        items(meals, key = { it.id }) { meal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${meal.nameEn} / ${meal.nameAr}", fontWeight = FontWeight.Bold)
                                        Text("${meal.calories} kcal • ${meal.proteinGrams}g P", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { adminViewModel.deleteMeal(meal.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.deleteBtn)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Plan Templates (4w, 8w, 12w, 6m, 12m)
                    3 -> {
                        items(planTemplates, key = { it.id }) { tpl ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (strings.isArabic) tpl.titleAr else tpl.titleEn,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tpl.durationWeeks} weeks (${tpl.code})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Switch(
                                        checked = tpl.isEnabled,
                                        onCheckedChange = { enabled ->
                                            adminViewModel.updatePlanTemplate(tpl.copy(isEnabled = enabled))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. AI Output Review (Approve / Reject / Edit)
                    4 -> {
                        if (plans.isEmpty()) {
                            item { Text("No AI-generated plans submitted yet.") }
                        }
                        items(plans, key = { "${it.userId}_${it.id}" }) { plan ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(plan.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("${plan.reviewStatus} (v${plan.version})", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text(plan.summary, style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                adminViewModel.reviewPlan(
                                                    plan.userId,
                                                    plan.id,
                                                    "approved",
                                                    "Approved by Admin",
                                                    null,
                                                    null,
                                                    plan.version
                                                )
                                            }
                                        ) { Text(strings.approveBtn) }
                                        OutlinedButton(
                                            onClick = { editingPlan = plan }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.requestRevisionBtn)
                                        }
                                        TextButton(
                                            onClick = {
                                                adminViewModel.reviewPlan(
                                                    plan.userId,
                                                    plan.id,
                                                    "rejected",
                                                    "Rejected by Admin review",
                                                    null,
                                                    null,
                                                    plan.version
                                                )
                                            }
                                        ) { Text(strings.rejectBtn) }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Coach Requests
                    5 -> {
                        if (coachRequests.isEmpty()) {
                            item { Text("No coach contact requests yet.") }
                        }
                        items(coachRequests, key = { it.id }) { req ->
                            var adminNoteDraft by remember(req.id) { mutableStateOf(req.adminNotes) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${req.userDisplayName} • ${req.preferredContactMethod}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(req.goalSummary)
                                    if (req.notes.isNotBlank()) {
                                        Text(req.notes, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text("Status: ${req.status}", color = MaterialTheme.colorScheme.primary)
                                    OutlinedTextField(
                                        value = adminNoteDraft,
                                        onValueChange = { adminNoteDraft = it },
                                        label = { Text("Admin Follow-up Note") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("in_review", "contacted", "closed").forEach { st ->
                                            FilterChip(
                                                selected = req.status == st,
                                                onClick = {
                                                    adminViewModel.updateCoachRequest(req.id, st, adminNoteDraft)
                                                },
                                                label = { Text(st) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Audit & Revision Log
                    6 -> {
                        items(auditLogs, key = { it.id }) { log ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("${log.action} • v${log.version}", fontWeight = FontWeight.Bold)
                                    Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${log.actorEmail} -> ${log.targetCollection}/${log.targetId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        var nameEn by remember { mutableStateOf("") }
        var nameAr by remember { mutableStateOf("") }
        var musclesEn by remember { mutableStateOf("Full Body") }
        var reps by remember { mutableStateOf("3 x 12") }
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text(strings.addExerciseBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name (EN)") })
                    OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("Name (AR)") })
                    OutlinedTextField(value = musclesEn, onValueChange = { musclesEn = it }, label = { Text("Target Muscles") })
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps / Duration") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameEn.isNotBlank()) {
                        adminViewModel.upsertExercise(
                            ExerciseItem(
                                nameEn = nameEn.trim(),
                                nameAr = nameAr.ifBlank { nameEn }.trim(),
                                descriptionEn = "Admin-curated exercise for $musclesEn.",
                                descriptionAr = "تمرين مضاف من الإدارة يستهدف $musclesEn.",
                                targetMusclesEn = musclesEn,
                                targetMusclesAr = musclesEn,
                                difficulty = "beginner",
                                repsOrDuration = reps,
                                equipmentEn = "Bodyweight",
                                equipmentAr = "وزن الجسم",
                                instructionsEn = "Perform with controlled tempo and steady breathing.",
                                instructionsAr = "أدِّ التمرين بتحكم وتنفس منتظم.",
                                mistakesEn = "Avoid rushing repetitions or losing posture.",
                                mistakesAr = "تجنب التسرع أو فقدان استقامة الجسم.",
                                animationType = "squat",
                                isActive = true
                            ),
                            isCreate = true
                        )
                        showAddExerciseDialog = false
                    }
                }) { Text(strings.saveBtn) }
            },
            dismissButton = { TextButton(onClick = { showAddExerciseDialog = false }) { Text(strings.cancelBtn) } }
        )
    }

    if (showAddMealDialog) {
        var nameEn by remember { mutableStateOf("") }
        var nameAr by remember { mutableStateOf("") }
        var cals by remember { mutableStateOf("450") }
        var protein by remember { mutableStateOf("35") }
        AlertDialog(
            onDismissRequest = { showAddMealDialog = false },
            title = { Text(strings.addMealBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Meal Name (EN)") })
                    OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("Meal Name (AR)") })
                    OutlinedTextField(value = cals, onValueChange = { cals = it.filter { c -> c.isDigit() } }, label = { Text("Calories") })
                    OutlinedTextField(value = protein, onValueChange = { protein = it.filter { c -> c.isDigit() } }, label = { Text("Protein (g)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameEn.isNotBlank()) {
                        adminViewModel.upsertMeal(
                            MealItem(
                                nameEn = nameEn.trim(),
                                nameAr = nameAr.ifBlank { nameEn }.trim(),
                                ingredientsEn = "Balanced whole-food ingredients.",
                                ingredientsAr = "مكونات طبيعية متوازنة.",
                                calories = cals.toLongOrNull() ?: 450L,
                                proteinGrams = protein.toLongOrNull() ?: 35L,
                                carbsGrams = 40L,
                                fatsGrams = 14L,
                                prepStepsEn = "Prepare fresh and serve balanced.",
                                prepStepsAr = "تحضّر طازجة وتقدّم بشكل متوازن.",
                                imageKey = "protein_bowl",
                                category = "lunch",
                                isActive = true
                            ),
                            isCreate = true
                        )
                        showAddMealDialog = false
                    }
                }) { Text(strings.saveBtn) }
            },
            dismissButton = { TextButton(onClick = { showAddMealDialog = false }) { Text(strings.cancelBtn) } }
        )
    }

    editingPlan?.let { plan ->
        var titleDraft by remember(plan) { mutableStateOf(plan.title) }
        var summaryDraft by remember(plan) { mutableStateOf(plan.summary) }
        var feedbackDraft by remember(plan) { mutableStateOf(plan.adminFeedback) }
        AlertDialog(
            onDismissRequest = { editingPlan = null },
            title = { Text("Review & Edit AI Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = titleDraft, onValueChange = { titleDraft = it }, label = { Text("Title") })
                    OutlinedTextField(value = summaryDraft, onValueChange = { summaryDraft = it }, label = { Text("Summary") }, minLines = 2)
                    OutlinedTextField(value = feedbackDraft, onValueChange = { feedbackDraft = it }, label = { Text("Admin Feedback") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.reviewPlan(
                        targetUserId = plan.userId,
                        planId = plan.id,
                        status = "needs_revision",
                        feedback = feedbackDraft,
                        editedTitle = titleDraft,
                        editedSummary = summaryDraft,
                        currentVersion = plan.version
                    )
                    editingPlan = null
                }) { Text(strings.saveBtn) }
            },
            dismissButton = { TextButton(onClick = { editingPlan = null }) { Text(strings.cancelBtn) } }
        )
    }

    if (showGrantAdminDialog) {
        var uidInput by remember { mutableStateOf("") }
        var emailInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGrantAdminDialog = false },
            title = { Text("Grant Admin Permission") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uidInput, onValueChange = { uidInput = it }, label = { Text("User UID") })
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it }, label = { Text("Verified Email") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (uidInput.isNotBlank() && emailInput.isNotBlank()) {
                        adminViewModel.grantAdmin(uidInput, emailInput)
                        showGrantAdminDialog = false
                    }
                }) { Text(strings.saveBtn) }
            },
            dismissButton = { TextButton(onClick = { showGrantAdminDialog = false }) { Text(strings.cancelBtn) } }
        )
    }
}

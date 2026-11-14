package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalFitStrings
import com.example.data.model.FitnessPlan
import com.example.data.model.MealItem
import com.example.data.model.MeasurementLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.repository.AiPlanRepository
import com.example.ui.components.WeightTrendChart
import kotlin.math.pow

@Composable
fun DashboardScreen(
    profile: UserProfile,
    workoutLogs: List<WorkoutLog>,
    measurements: List<MeasurementLog>,
    plans: List<FitnessPlan>,
    meals: List<MealItem>,
    onLogWorkout: (title: String, minutes: Long, calories: Long, notes: String) -> Unit,
    onDeleteWorkoutLog: (String) -> Unit,
    onLogMeasurement: (weightKg: Double, waistCm: Double?, chestCm: Double?, bodyFat: Double?, notes: String) -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalFitStrings.current
    var showLogWorkoutDialog by remember { mutableStateOf(false) }
    var showLogMeasurementDialog by remember { mutableStateOf(false) }

    val latestWeight = measurements.lastOrNull()?.weightKg ?: profile.weightKg
    val bmi = latestWeight / (profile.heightCm / 100.0).pow(2)
    val totalSessions = workoutLogs.count { it.completed }
    val totalCalories = workoutLogs.sumOf { it.caloriesBurned }
    val adherencePercent = ((totalSessions.coerceAtMost(20) / 20.0) * 100).toInt().coerceIn(0, 100)
    val activePlan = plans.firstOrNull()
    val schedule = remember(activePlan) {
        activePlan?.let { AiPlanRepository.parseWeeklySchedule(it.weeklyScheduleJson) } ?: emptyList()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_fitness),
                        contentDescription = strings.todaySummary,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.78f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = if (strings.isArabic) "مرحبًا، ${profile.displayName}" else "Welcome, ${profile.displayName}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${strings.goalsTitle}: ${goalLabel(profile.fitnessGoal, strings.isArabic)} • ${profile.selectedPlanDurationWeeks} ${if (strings.isArabic) "أسبوع" else "wks"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // PAR-Q Flag Banner if active
        if (profile.parqFlagged) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.parqWarningBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Today's Metrics Grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    label = strings.sessionsCompleted,
                    value = "$totalSessions",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.LocalFireDepartment,
                    label = strings.caloriesBurnedLabel,
                    value = "$totalCalories",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.MonitorWeight,
                    label = strings.currentWeightLabel,
                    value = "${"%.1f".format(latestWeight)} kg",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Adherence + Quick Log Buttons
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.adherenceTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$adherencePercent% • ${strings.bmiLabel}: ${"%.1f".format(bmi)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { adherencePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showLogWorkoutDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_log_workout_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.logWorkoutBtn, maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { showLogMeasurementDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_log_measurement_button")
                        ) {
                            Icon(Icons.Default.MonitorWeight, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.logMeasurementBtn, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Active Workout Plan Snapshot
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.workoutPlanTitle, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onNavigateToPlans) {
                            Text(if (strings.isArabic) "عرض الكل" else "Open Plans")
                        }
                    }
                    if (activePlan == null) {
                        Text(
                            text = if (strings.isArabic) "لم يتم توليد خطة شخصية بعد. انتقل إلى الخطط لإنشاء برنامجك الذكي." else "No personalized plan generated yet. Open Plans to generate yours with Gemini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = activePlan.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = activePlan.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        schedule.firstOrNull()?.let { today ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = "${today.dayName}: ${today.focus} (${today.durationMinutes} min)",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Nutrition Plan Snapshot
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.nutritionPlanTitle, style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = onNavigateToNutrition) {
                            Text(strings.navNutrition)
                        }
                    }
                    meals.take(2).forEach { meal ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (strings.isArabic) meal.nameAr else meal.nameEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${meal.calories} ${strings.caloriesShort} • ${meal.proteinGrams}g P",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weight Progress Chart
        item {
            WeightTrendChart(
                measurements = measurements,
                title = strings.progressTitle,
                emptyHint = strings.noLogsYet,
                modifier = Modifier.widthIn(max = 620.dp)
            )
        }

        // Activity Log Header
        item {
            Text(
                text = strings.activityLogTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .padding(top = 4.dp)
            )
        }

        if (workoutLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                ) {
                    Text(
                        text = strings.noLogsYet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(workoutLogs.take(10), key = { it.id }) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.workoutTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${log.dateIso} • ${log.durationMinutes} min • ${log.caloriesBurned} ${strings.caloriesShort}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteWorkoutLog(log.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = strings.deleteBtn
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showLogWorkoutDialog) {
        var title by remember { mutableStateOf(if (strings.isArabic) "تمرين كامل الجسم" else "Full Body Session") }
        var mins by remember { mutableStateOf("35") }
        var cals by remember { mutableStateOf("240") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogWorkoutDialog = false },
            title = { Text(strings.logWorkoutBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (strings.isArabic) "اسم التمرين" else "Workout Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mins,
                        onValueChange = { mins = it.filter { c -> c.isDigit() } },
                        label = { Text(if (strings.isArabic) "المدة (دقيقة)" else "Duration (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cals,
                        onValueChange = { cals = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.caloriesBurnedLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.notesLabel) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogWorkout(
                            title.ifBlank { "Workout" },
                            mins.toLongOrNull() ?: 30L,
                            cals.toLongOrNull() ?: 200L,
                            notes
                        )
                        showLogWorkoutDialog = false
                    },
                    modifier = Modifier.testTag("confirm_log_workout_button")
                ) {
                    Text(strings.saveBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogWorkoutDialog = false }) { Text(strings.cancelBtn) }
            }
        )
    }

    if (showLogMeasurementDialog) {
        var weight by remember { mutableStateOf(latestWeight.toString()) }
        var waist by remember { mutableStateOf("") }
        var chest by remember { mutableStateOf("") }
        var bodyFat by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogMeasurementDialog = false },
            title = { Text(strings.logMeasurementBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.weightLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waist,
                        onValueChange = { waist = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (strings.isArabic) "محيط الخصر (سم - اختياري)" else "Waist (cm - optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = chest,
                        onValueChange = { chest = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (strings.isArabic) "محيط الصدر (سم - اختياري)" else "Chest (cm - optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bodyFat,
                        onValueChange = { bodyFat = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (strings.isArabic) "نسبة الدهون % (اختياري)" else "Body Fat % (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.notesLabel) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogMeasurement(
                            weight.toDoubleOrNull() ?: latestWeight,
                            waist.toDoubleOrNull(),
                            chest.toDoubleOrNull(),
                            bodyFat.toDoubleOrNull(),
                            notes
                        )
                        showLogMeasurementDialog = false
                    },
                    modifier = Modifier.testTag("confirm_log_measurement_button")
                ) {
                    Text(strings.saveBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogMeasurementDialog = false }) { Text(strings.cancelBtn) }
            }
        )
    }
}

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

private fun goalLabel(goal: String, isArabic: Boolean): String = when (goal) {
    "fat_loss" -> if (isArabic) "حرق الدهون" else "Fat Loss"
    "muscle_gain" -> if (isArabic) "بناء العضلات" else "Muscle Gain"
    "endurance" -> if (isArabic) "التحمل" else "Endurance"
    "flexibility" -> if (isArabic) "المرونة" else "Flexibility"
    else -> if (isArabic) "صحة عامة" else "General Wellness"
}

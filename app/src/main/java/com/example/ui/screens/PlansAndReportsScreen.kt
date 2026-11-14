package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.LocalFitStrings
import com.example.data.model.FitnessPlan
import com.example.data.model.MeasurementLog
import com.example.data.model.PlanTemplate
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.repository.AiPlanRepository
import com.example.ui.components.WeeklyActivityBarChart
import com.example.ui.components.WeightTrendChart
import com.example.ui.viewmodel.AiGenerationState

@Composable
fun PlansAndReportsScreen(
    profile: UserProfile,
    planTemplates: List<PlanTemplate>,
    plans: List<FitnessPlan>,
    workoutLogs: List<WorkoutLog>,
    measurements: List<MeasurementLog>,
    aiState: AiGenerationState,
    onGeneratePlan: (durationWeeks: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalFitStrings.current
    val enabledTemplates = remember(planTemplates) { planTemplates.filter { it.isEnabled } }
    var selectedDurationWeeks by remember(profile.selectedPlanDurationWeeks) {
        mutableLongStateOf(profile.selectedPlanDurationWeeks)
    }

    val latestPlan = plans.firstOrNull()
    val weeklySchedule = remember(latestPlan) {
        latestPlan?.let { AiPlanRepository.parseWeeklySchedule(it.weeklyScheduleJson) } ?: emptyList()
    }
    val dailyMeals = remember(latestPlan) {
        latestPlan?.let { AiPlanRepository.parseDailyMeals(it.dailyMealsJson) } ?: emptyList()
    }

    val last7Count = workoutLogs.take(7).count { it.completed }
    val last30Count = workoutLogs.take(30).count { it.completed }
    val last30Minutes = workoutLogs.take(30).sumOf { it.durationMinutes }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Duration Selector Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.selectDurationTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(enabledTemplates, key = { it.code }) { tpl ->
                            FilterChip(
                                selected = selectedDurationWeeks == tpl.durationWeeks,
                                onClick = { selectedDurationWeeks = tpl.durationWeeks },
                                label = {
                                    Text(if (strings.isArabic) tpl.titleAr else tpl.titleEn)
                                },
                                modifier = Modifier.testTag("duration_chip_${tpl.code}")
                            )
                        }
                    }

                    enabledTemplates.firstOrNull { it.durationWeeks == selectedDurationWeeks }?.let { tpl ->
                        Text(
                            text = if (strings.isArabic) tpl.descriptionAr else tpl.descriptionEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.aiPrivacyNotice,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Button(
                        onClick = { onGeneratePlan(selectedDurationWeeks) },
                        enabled = aiState !is AiGenerationState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_ai_plan_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (aiState is AiGenerationState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.generatingPlan)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (latestPlan == null) strings.generateAiPlanBtn else strings.adjustAiPlanBtn
                            )
                        }
                    }

                    if (aiState is AiGenerationState.Error) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Latest AI Plan Display
        if (latestPlan != null) {
            item {
                val statusLabel = when (latestPlan.reviewStatus) {
                    "approved" -> strings.reviewStatusApproved
                    "rejected" -> strings.reviewStatusRejected
                    "needs_revision" -> strings.reviewStatusNeedsRevision
                    else -> strings.reviewStatusPending
                }
                val statusColor = when (latestPlan.reviewStatus) {
                    "approved" -> MaterialTheme.colorScheme.primaryContainer
                    "rejected" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }

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
                            Surface(shape = RoundedCornerShape(10.dp), color = statusColor) {
                                Text(
                                    text = "$statusLabel • v${latestPlan.version}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${latestPlan.durationWeeks} ${if (strings.isArabic) "أسبوع" else "wks"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = latestPlan.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = latestPlan.summary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (latestPlan.parqSafetyNote.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = latestPlan.parqSafetyNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (latestPlan.adminFeedback.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Admin: ${latestPlan.adminFeedback}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (weeklySchedule.isNotEmpty()) {
                            Text(
                                text = strings.weeklyScheduleLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            weeklySchedule.forEach { day ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(day.dayName, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (day.isRestDay) (if (strings.isArabic) "راحة" else "Rest")
                                                else "${day.durationMinutes} min",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(day.focus, style = MaterialTheme.typography.bodyMedium)
                                        if (day.exercises.isNotEmpty()) {
                                            Text(
                                                text = day.exercises.joinToString(" • "),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (dailyMeals.isNotEmpty()) {
                            Text(
                                text = strings.dailyMealsLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            dailyMeals.forEach { meal ->
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(meal.mealType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            Text(meal.mealName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = "${meal.calories} kcal • ${meal.proteinGrams}P/${meal.carbsGrams}C/${meal.fatsGrams}F",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weekly & Monthly Reports
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
                    Text(
                        text = strings.weeklyMonthlyReports,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.weeklySessionsLabel, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$last7Count ${strings.sessionsCompleted}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.monthlySessionsLabel, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$last30Count (${last30Minutes}m)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            WeeklyActivityBarChart(
                logs = workoutLogs,
                title = strings.weeklySessionsLabel,
                emptyHint = strings.noLogsYet,
                modifier = Modifier.widthIn(max = 620.dp)
            )
        }

        item {
            WeightTrendChart(
                measurements = measurements,
                title = strings.weightTrendLabel,
                emptyHint = strings.noLogsYet,
                modifier = Modifier.widthIn(max = 620.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

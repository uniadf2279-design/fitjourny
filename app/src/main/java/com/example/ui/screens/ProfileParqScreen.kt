package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.LocalFitStrings
import com.example.data.model.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileParqScreen(
    existingProfile: UserProfile?,
    defaultDisplayName: String,
    currentLanguageCode: String,
    currentThemeCode: String,
    onSave: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalFitStrings.current
    var displayName by remember(existingProfile) {
        mutableStateOf(existingProfile?.displayName?.ifBlank { defaultDisplayName } ?: defaultDisplayName)
    }
    var ageText by remember(existingProfile) {
        mutableStateOf((existingProfile?.age ?: 26L).toString())
    }
    var heightText by remember(existingProfile) {
        mutableStateOf((existingProfile?.heightCm ?: 172.0).toInt().toString())
    }
    var weightText by remember(existingProfile) {
        mutableStateOf((existingProfile?.weightKg ?: 72.0).toString())
    }
    var activityLevel by remember(existingProfile) {
        mutableStateOf(existingProfile?.activityLevel ?: "moderate")
    }
    var fitnessGoal by remember(existingProfile) {
        mutableStateOf(existingProfile?.fitnessGoal ?: "general_wellness")
    }
    var nutritionPref by remember(existingProfile) {
        mutableStateOf(existingProfile?.nutritionPreference ?: "balanced")
    }
    var experienceLevel by remember(existingProfile) {
        mutableStateOf(existingProfile?.experienceLevel ?: "beginner")
    }
    var healthNotes by remember(existingProfile) {
        mutableStateOf(existingProfile?.healthNotes ?: "")
    }
    val parqAnswers = remember(existingProfile) {
        mutableStateListOf<Boolean>().apply {
            val initial = existingProfile?.parqAnswers
            if (initial != null && initial.size == 7) addAll(initial)
            else repeat(7) { add(false) }
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    val parqFlagged = parqAnswers.any { it }

    val activityOptions = listOf(
        "sedentary" to (if (strings.isArabic) "خامل" else "Sedentary"),
        "light" to (if (strings.isArabic) "خفيف" else "Light"),
        "moderate" to (if (strings.isArabic) "متوسط" else "Moderate"),
        "active" to (if (strings.isArabic) "نشيط" else "Active"),
        "athlete" to (if (strings.isArabic) "رياضي" else "Athlete")
    )
    val goalOptions = listOf(
        "fat_loss" to (if (strings.isArabic) "حرق الدهون" else "Fat Loss"),
        "muscle_gain" to (if (strings.isArabic) "بناء العضلات" else "Muscle Gain"),
        "endurance" to (if (strings.isArabic) "التحمل واللياقة" else "Endurance"),
        "flexibility" to (if (strings.isArabic) "المرونة" else "Flexibility"),
        "general_wellness" to (if (strings.isArabic) "صحة عامة" else "Wellness")
    )
    val nutritionOptions = listOf(
        "balanced" to (if (strings.isArabic) "متوازن" else "Balanced"),
        "high_protein" to (if (strings.isArabic) "عالي البروتين" else "High Protein"),
        "mediterranean" to (if (strings.isArabic) "متوسطي" else "Mediterranean"),
        "vegetarian" to (if (strings.isArabic) "نباتي" else "Vegetarian"),
        "low_carb" to (if (strings.isArabic) "منخفض الكارب" else "Low Carb")
    )
    val experienceOptions = listOf(
        "beginner" to strings.beginner,
        "intermediate" to strings.intermediate,
        "advanced" to strings.advanced
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.onboardingTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.onboardingSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(strings.displayNameLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.ageLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_age_input")
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(strings.heightLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_height_input")
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(strings.weightLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_weight_input")
                )
            }

            Text(text = strings.activityLevelLabel, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activityOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = activityLevel == key,
                        onClick = { activityLevel = key },
                        label = { Text(label) }
                    )
                }
            }

            Text(text = strings.fitnessGoalLabel, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goalOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = fitnessGoal == key,
                        onClick = { fitnessGoal = key },
                        label = { Text(label) }
                    )
                }
            }

            Text(text = strings.nutritionPrefLabel, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nutritionOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = nutritionPref == key,
                        onClick = { nutritionPref = key },
                        label = { Text(label) }
                    )
                }
            }

            Text(text = strings.experienceLabel, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                experienceOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = experienceLevel == key,
                        onClick = { experienceLevel = key },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = healthNotes,
                onValueChange = { healthNotes = it.take(480) },
                label = { Text(strings.healthNotesLabel) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // PAR-Q Questionnaire
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.parqTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.parqSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    strings.parqQuestions.forEachIndexed { idx, q ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${idx + 1}. $q",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilterChip(
                                        selected = parqAnswers[idx],
                                        onClick = { parqAnswers[idx] = true },
                                        label = { Text(strings.yesLabel) },
                                        modifier = Modifier.testTag("parq_yes_$idx")
                                    )
                                    FilterChip(
                                        selected = !parqAnswers[idx],
                                        onClick = { parqAnswers[idx] = false },
                                        label = { Text(strings.noLabel) },
                                        modifier = Modifier.testTag("parq_no_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (parqFlagged) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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

            if (!validationError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val age = ageText.toLongOrNull() ?: 0L
                    val height = heightText.toDoubleOrNull() ?: 0.0
                    val weight = weightText.toDoubleOrNull() ?: 0.0
                    when {
                        displayName.isBlank() -> {
                            validationError = if (strings.isArabic) "يرجى إدخال الاسم" else "Please enter your name."
                        }
                        age < 18L || age > 120L -> {
                            validationError = strings.under18Error
                        }
                        height < 100.0 || height > 250.0 -> {
                            validationError = if (strings.isArabic) "يرجى إدخال طول صحيح (100 - 250 سم)" else "Please enter a valid height (100 - 250 cm)."
                        }
                        weight < 30.0 || weight > 300.0 -> {
                            validationError = if (strings.isArabic) "يرجى إدخال وزن صحيح (30 - 300 كجم)" else "Please enter a valid weight (30 - 300 kg)."
                        }
                        else -> {
                            validationError = null
                            onSave(
                                UserProfile(
                                    id = existingProfile?.id.orEmpty(),
                                    userId = existingProfile?.userId.orEmpty(),
                                    displayName = displayName.trim().take(75),
                                    age = age,
                                    heightCm = height,
                                    weightKg = weight,
                                    activityLevel = activityLevel,
                                    fitnessGoal = fitnessGoal,
                                    nutritionPreference = nutritionPref,
                                    experienceLevel = experienceLevel,
                                    healthNotes = healthNotes.trim(),
                                    parqAnswers = parqAnswers.toList(),
                                    parqFlagged = parqFlagged,
                                    selectedPlanDurationWeeks = existingProfile?.selectedPlanDurationWeeks ?: 8L,
                                    preferredLanguage = currentLanguageCode,
                                    themeMode = currentThemeCode,
                                    createdAt = existingProfile?.createdAt,
                                    updatedAt = existingProfile?.updatedAt
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(strings.saveProfileBtn, style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

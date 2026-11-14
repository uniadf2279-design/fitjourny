package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.LocalFitStrings
import com.example.data.model.MealItem

@Composable
fun NutritionLibraryScreen(
    meals: List<MealItem>,
    modifier: Modifier = Modifier
) {
    val strings = LocalFitStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedMeal by remember { mutableStateOf<MealItem?>(null) }

    val categories = listOf(
        "all" to strings.allDifficulties,
        "breakfast" to (if (strings.isArabic) "فطور" else "Breakfast"),
        "lunch" to (if (strings.isArabic) "غداء" else "Lunch"),
        "dinner" to (if (strings.isArabic) "عشاء" else "Dinner"),
        "snack" to (if (strings.isArabic) "سناك" else "Snack")
    )

    val filtered = remember(meals, searchQuery, selectedCategory) {
        meals.filter { it.isActive }.filter { meal ->
            val catMatch = selectedCategory == "all" || meal.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val textMatch = q.isEmpty() ||
                meal.nameEn.lowercase().contains(q) ||
                meal.nameAr.lowercase().contains(q) ||
                meal.ingredientsEn.lowercase().contains(q) ||
                meal.ingredientsAr.lowercase().contains(q)
            catMatch && textMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchMealsHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .testTag("meal_search_input")
            )
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { meal ->
            val name = if (strings.isArabic) meal.nameAr else meal.nameEn
            val ingredients = if (strings.isArabic) meal.ingredientsAr else meal.ingredientsEn

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .clickable { selectedMeal = meal }
                    .testTag("meal_card_${meal.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = mealDrawable(meal.imageKey)),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ingredients,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroChip("${meal.calories} ${strings.caloriesShort}", MaterialTheme.colorScheme.tertiaryContainer)
                            MacroChip("${strings.proteinShort} ${meal.proteinGrams}g", MaterialTheme.colorScheme.primaryContainer)
                            MacroChip("${strings.carbsShort} ${meal.carbsGrams}g", MaterialTheme.colorScheme.secondaryContainer)
                            MacroChip("${strings.fatsShort} ${meal.fatsGrams}g", MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    selectedMeal?.let { meal ->
        val name = if (strings.isArabic) meal.nameAr else meal.nameEn
        Dialog(
            onDismissRequest = { selectedMeal = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 600.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedMeal = null }) {
                            Icon(Icons.Default.Close, contentDescription = strings.cancelBtn)
                        }
                    }

                    Image(
                        painter = painterResource(id = mealDrawable(meal.imageKey)),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroChip("${meal.calories} ${strings.caloriesShort}", MaterialTheme.colorScheme.tertiaryContainer)
                        MacroChip("${strings.proteinShort} ${meal.proteinGrams}g", MaterialTheme.colorScheme.primaryContainer)
                        MacroChip("${strings.carbsShort} ${meal.carbsGrams}g", MaterialTheme.colorScheme.secondaryContainer)
                        MacroChip("${strings.fatsShort} ${meal.fatsGrams}g", MaterialTheme.colorScheme.surfaceVariant)
                    }

                    Text(strings.ingredientsLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (strings.isArabic) meal.ingredientsAr else meal.ingredientsEn,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(strings.prepStepsLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (strings.isArabic) meal.prepStepsAr else meal.prepStepsEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroChip(text: String, containerColor: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

private fun mealDrawable(imageKey: String): Int = when (imageKey) {
    "salmon" -> R.drawable.img_meal_salmon
    "hero_fitness" -> R.drawable.img_hero_fitness
    else -> R.drawable.img_meal_protein_bowl
}

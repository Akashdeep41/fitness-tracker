package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DietaryProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    currentProfile: DietaryProfile,
    isGenerating: Boolean,
    onProfileSave: (DietaryProfile) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVegan by remember(currentProfile) { mutableStateOf(currentProfile.isVegan) }
    var isVegetarian by remember(currentProfile) { mutableStateOf(currentProfile.isVegetarian) }
    var isGlutenFree by remember(currentProfile) { mutableStateOf(currentProfile.isGlutenFree) }
    var isKeto by remember(currentProfile) { mutableStateOf(currentProfile.isKeto) }
    var isPaleo by remember(currentProfile) { mutableStateOf(currentProfile.isPaleo) }
    var isHalal by remember(currentProfile) { mutableStateOf(currentProfile.isHalal) }
    var isKosher by remember(currentProfile) { mutableStateOf(currentProfile.isKosher) }

    var allergyPeanuts by remember(currentProfile) { mutableStateOf(currentProfile.allergyPeanuts) }
    var allergyTreeNuts by remember(currentProfile) { mutableStateOf(currentProfile.allergyTreeNuts) }
    var allergyDairy by remember(currentProfile) { mutableStateOf(currentProfile.allergyDairy) }
    var allergySoy by remember(currentProfile) { mutableStateOf(currentProfile.allergySoy) }
    var allergyEgg by remember(currentProfile) { mutableStateOf(currentProfile.allergyEgg) }
    var allergyShellfish by remember(currentProfile) { mutableStateOf(currentProfile.allergyShellfish) }
    var allergyGluten by remember(currentProfile) { mutableStateOf(currentProfile.allergyGluten) }

    var calorieTarget by remember(currentProfile) { mutableStateOf(currentProfile.calorieTarget) }
    var ratioType by remember(currentProfile) { mutableStateOf(currentProfile.ratioType) }
    var dislikedIngredients by remember(currentProfile) { mutableStateOf(currentProfile.dislikedIngredients) }
    var generalPreferences by remember(currentProfile) { mutableStateOf(currentProfile.generalPreferences) }

    val scrollState = rememberScrollState()

    // Trigger save whenever changes occur to maintain robust local state
    val saveTrigger = {
        onProfileSave(
            DietaryProfile(
                isVegan = isVegan,
                isVegetarian = isVegetarian,
                isGlutenFree = isGlutenFree,
                isKeto = isKeto,
                isPaleo = isPaleo,
                isHalal = isHalal,
                isKosher = isKosher,
                allergyPeanuts = allergyPeanuts,
                allergyTreeNuts = allergyTreeNuts,
                allergyDairy = allergyDairy,
                allergySoy = allergySoy,
                allergyEgg = allergyEgg,
                allergyShellfish = allergyShellfish,
                allergyGluten = allergyGluten,
                calorieTarget = calorieTarget,
                ratioType = ratioType,
                dislikedIngredients = dislikedIngredients,
                generalPreferences = generalPreferences
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Preferences icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Culinary Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize your diet plan targets and restrictions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 1. Dietary Restrictions
        Text(
            text = "Dietary Pattern",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val diets = listOf(
                Triple("Vegan 🌱", isVegan, { v: Boolean -> isVegan = v }),
                Triple("Vegetarian 🍏", isVegetarian, { v: Boolean -> isVegetarian = v }),
                Triple("Gluten-Free 🌾", isGlutenFree, { v: Boolean -> isGlutenFree = v }),
                Triple("Keto 🥓", isKeto, { v: Boolean -> isKeto = v }),
                Triple("Paleo 🥩", isPaleo, { v: Boolean -> isPaleo = v }),
                Triple("Halal 🌙", isHalal, { v: Boolean -> isHalal = v }),
                Triple("Kosher ✡️", isKosher, { v: Boolean -> isKosher = v })
            )

            diets.forEach { (label, isSelected, setter) ->
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        setter(!isSelected)
                        saveTrigger()
                    },
                    label = { Text(label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 2. Calorie targets
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Calorie Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$calorieTarget kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = calorieTarget.toFloat(),
                onValueChange = {
                    calorieTarget = it.toInt()
                },
                onValueChangeFinished = {
                    saveTrigger()
                },
                valueRange = 1200f..4000f,
                steps = 28, // 100 kcal steps
                modifier = Modifier.testTag("calorie_slider")
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 3. Macronutrient Priority
        Text(
            text = "Macronutrient Distribution",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val macroRatios = listOf("Balanced", "Low Carb", "High Protein")
            macroRatios.forEach { ratio ->
                val isSelected = ratioType == ratio
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            ratioType = ratio
                            saveTrigger()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ratio,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (ratio) {
                                "Balanced" -> "50% C / 20% P / 30% F"
                                "Low Carb" -> "20% C / 35% P / 45% F"
                                "High Protein" -> "40% C / 35% P / 25% F"
                                else -> ""
                            },
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 4. Allergies To Avoid
        Text(
            text = "Strict Allergies & Exclusions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allergies = listOf(
                Triple("Peanuts 🥜", allergyPeanuts, { v: Boolean -> allergyPeanuts = v }),
                Triple("Tree Nuts 🌰", allergyTreeNuts, { v: Boolean -> allergyTreeNuts = v }),
                Triple("Dairy 🥛", allergyDairy, { v: Boolean -> allergyDairy = v }),
                Triple("Soy 🫘", allergySoy, { v: Boolean -> allergySoy = v }),
                Triple("Eggs 🥚", allergyEgg, { v: Boolean -> allergyEgg = v }),
                Triple("Shellfish 🦐", allergyShellfish, { v: Boolean -> allergyShellfish = v }),
                Triple("Gluten 🌾", allergyGluten, { v: Boolean -> allergyGluten = v })
            )

            allergies.forEach { (label, isSelected, setter) ->
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        setter(!isSelected)
                        saveTrigger()
                    },
                    label = { Text(label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 5. Excluded Ingredients
        OutlinedTextField(
            value = dislikedIngredients,
            onValueChange = {
                dislikedIngredients = it
                saveTrigger()
            },
            label = { Text("Disliked ingredients (comma separated)") },
            placeholder = { Text("e.g. Cilantro, Mushrooms, Olives") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // 6. Custom General Notes
        OutlinedTextField(
            value = generalPreferences,
            onValueChange = {
                generalPreferences = it
                saveTrigger()
            },
            label = { Text("Custom Preferences or Requests") },
            placeholder = { Text("e.g. Simple 15 min recipes, high fiber, cheap ingredients") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(12.dp)
        )

        // 7. Large CTA Button
        Button(
            onClick = {
                saveTrigger()
                onGenerate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("generate_meal_plan_button"),
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Generating Custom Menu...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Generate")
                Spacer(modifier = Modifier.width(12.dp))
                Text("Generate Complete Meal Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

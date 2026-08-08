package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DietaryProfile
import com.example.data.model.FavoriteMeal
import com.example.data.model.MealPlan
import com.example.data.model.MealPlanMeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    activePlan: MealPlan?,
    favoritesList: List<FavoriteMeal>,
    profile: DietaryProfile,
    selectedDay: Int,
    isGenerating: Boolean,
    errorMessage: String?,
    onSelectDay: (Int) -> Unit,
    onMealClick: (MealPlanMeal) -> Unit,
    onToggleFavorite: (MealPlanMeal) -> Unit,
    onSwapMeal: (dayNumber: Int, mealId: String) -> Unit,
    onGenerateDefaultPlan: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteIds = favoritesList.map { it.id }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Handle Error bar gracefully
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Error", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = onClearError) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Error", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (activePlan == null) {
            // Un-generated state: Show onboarding and configure trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍲", fontSize = 36.sp)
                    }

                    Text(
                        text = "Build Your Custom Meal Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Set up your dietary goals, restrictions, or allergies under the Preferences tab, and generate an AI-optimized menu instantly!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isGenerating) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                "AI is custom-designing your menu. Please stand by...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = onGenerateDefaultPlan,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(52.dp)
                                .testTag("generate_initial_plan_button"),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text("Generate 3-Day Plan Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else {
            // Active Meal Plan State
            val planDays = activePlan.days
            val selectedDayPlan = planDays.find { it.dayNumber == selectedDay }

            // Day Picker Tabs
            PrimaryTabRow(
                selectedTabIndex = planDays.indexOfFirst { it.dayNumber == selectedDay }.coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                planDays.forEach { day ->
                    Tab(
                        selected = day.dayNumber == selectedDay,
                        onClick = { onSelectDay(day.dayNumber) },
                        text = {
                            Text(
                                text = "Day ${day.dayNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("day_tab_${day.dayNumber}")
                    )
                }
            }

            // Overview Bento Grid & Nutrition Layout for current day
            selectedDayPlan?.let { day ->
                val dailyTotalCal = day.meals.sumOf { it.calories }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isGenerating) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text("Regenerating dishes alignment...", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        // BENTO GRID CARD 1: Featured Meal Spotlight (col-span-2)
                        // Picks lunch or first meal to showcase dynamically with high visual fidelity
                        val featuredMeal = day.meals.find { it.mealType.lowercase() == "lunch" } ?: day.meals.firstOrNull()
                        featuredMeal?.let { meal ->
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onMealClick(meal) }
                                        .testTag("bento_featured_meal_card"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🔥 FEATURED DISH • ${meal.mealType.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${meal.calories} KCAL",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = meal.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 26.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Ingredients: ${meal.ingredients.take(3).joinToString(", ")}...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { onMealClick(meal) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("View Recipe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = { onSwapMeal(selectedDay, meal.id) },
                                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Swap Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // BENTO GRID CARD 2: Macro Tracker Card & Preferences Restrictions Card Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Cell A: Macro Summary Box (col-span-1) with complete Day Calories and Protein/Carbs/Fats breakdown
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(195.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info, 
                                                contentDescription = "Analysis",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "MACROS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        val calTargetVal = profile.calorieTarget.toDouble()
                                        val percentage = ((dailyTotalCal.toDouble() / calTargetVal) * 100).toInt().coerceIn(0, 100)

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text(
                                                    text = "$percentage",
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "%",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = "Calorie Budget Met",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { (dailyTotalCal.toFloat() / calTargetVal.toFloat()).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        )

                                        // Estimated daily macro intake vs customized goals targets
                                        val dailyTotalProtein = day.meals.sumOf { it.proteinGrams }
                                        val dailyTotalCarbs = day.meals.sumOf { it.carbsGrams }
                                        val dailyTotalFats = day.meals.sumOf { it.fatsGrams }

                                        val macroRatio = when (profile.ratioType) {
                                            "Low Carb" -> Triple(0.30, 0.15, 0.55)
                                            "High Protein" -> Triple(0.40, 0.30, 0.30)
                                            else -> Triple(0.20, 0.50, 0.30) // Balanced
                                        }

                                        val targetProtein = ((profile.calorieTarget * macroRatio.first) / 4).toInt()
                                        val targetCarbs = ((profile.calorieTarget * macroRatio.second) / 4).toInt()
                                        val targetFats = ((profile.calorieTarget * macroRatio.third) / 9).toInt()

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("PROT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${dailyTotalProtein}g", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF9800))
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("CARB", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${dailyTotalCarbs}g", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF2196F3))
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("FAT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${dailyTotalFats}g", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                                            }
                                        }
                                    }
                                }

                                // Cell B: Active Profile restrictions and tag alerts (col-span-1)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(195.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🛡️", fontSize = 20.sp)

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Preferences",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )

                                            // Compile active diets
                                            val tags = mutableListOf<String>()
                                            if (profile.isVegan) tags.add("Vegan")
                                            if (profile.isVegetarian) tags.add("Veg")
                                            if (profile.isGlutenFree) tags.add("No Gluten")
                                            if (profile.isKeto) tags.add("Keto")
                                            if (profile.isPaleo) tags.add("Paleo")
                                            if (profile.isHalal) tags.add("Halal")
                                            if (profile.isKosher) tags.add("Kosher")

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                tags.take(2).forEach { tag ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.White.copy(alpha = 0.4f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                                if (tags.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.White.copy(alpha = 0.4f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Balanced",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Text(
                                            text = "Ratio: ${profile.ratioType}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // BENTO GRID CARD 3: Water Tracker & Prep metric efficiency (col-span-1 each)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Stat card A - Water droplet representation
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFD0E4FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("💧", fontSize = 18.sp)
                                        }
                                        Column {
                                            Text(
                                                text = "Water Intake",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Daily status active",
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                // Stat card B - Average day prep efficiency
                                val avgPrep = day.meals.map { it.prepTimeMinutes }.average().toInt()
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFE8F5E9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⏱️", fontSize = 18.sp)
                                        }
                                        Column {
                                            Text(
                                                text = "${avgPrep} mins",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                             )
                                             Text(
                                                text = "Avg prep speed",
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // BENTO GRID CARD 4: Regenerate Next solid action block (col-span-2)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGenerateDefaultPlan() }
                                    .testTag("bento_regenerate_card"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1D1B20), // Charcoal Solid Black
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Re-Generate Entire Menu",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Creates new customized 3-day layout now",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Refresh, 
                                        contentDescription = "Regenerate",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // SECTION SUBHEADER: Today's Complete Dish Menu
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Today's Dynamic Meals (${day.meals.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        items(day.meals, key = { it.id }) { meal ->
                            MealRowCard(
                                meal = meal,
                                isFavorited = favoriteIds.contains(meal.id),
                                onClick = { onMealClick(meal) },
                                onToggleFav = { onToggleFavorite(meal) },
                                onSwap = { onSwapMeal(selectedDay, meal.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealRowCard(
    meal: MealPlanMeal,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onToggleFav: () -> Unit,
    onSwap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("meal_card_${meal.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Meal type food emoji representation box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (meal.mealType.lowercase()) {
                        "breakfast" -> "🍳"
                        "lunch" -> "🥪"
                        "dinner" -> "🍽️"
                        "snack" -> "🍎"
                        else -> "🍱"
                    },
                    fontSize = 26.sp
                )
            }

            // Middle main texts
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = meal.mealType.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⏱️ ${meal.prepTimeMinutes}m",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Inline Nutrient counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${meal.calories} kcal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("•", fontSize = 10.sp, color = Color.LightGray)
                    Text("P: ${meal.proteinGrams}g", fontSize = 11.sp, color = Color.Gray)
                    Text("C: ${meal.carbsGrams}g", fontSize = 11.sp, color = Color.Gray)
                    Text("F: ${meal.fatsGrams}g", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Quick interaction button columns
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Heart favorite
                IconButton(
                    onClick = onToggleFav,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("fav_btn_${meal.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Swap refresh
                IconButton(
                    onClick = onSwap,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("swap_btn_${meal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Swap Meal",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

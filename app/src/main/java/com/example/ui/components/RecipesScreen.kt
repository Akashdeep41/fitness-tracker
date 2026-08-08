package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MealPlan
import com.example.data.model.MealPlanMeal
import com.example.data.model.Recipe

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    recipes: List<Recipe>,
    activePlan: MealPlan?,
    onReplaceMeal: (dayNumber: Int, mealId: String, recipe: Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Search and Filters state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCuisine by remember { mutableStateOf("All") }
    var maxPrepTime by remember { mutableStateOf(40f) } // default up to 40mins
    var maxCalories by remember { mutableStateOf(800f) }

    // Dietary Options state
    var requireVegan by remember { mutableStateOf(false) }
    var requireVegetarian by remember { mutableStateOf(false) }
    var requireGlutenFree by remember { mutableStateOf(false) }
    var requireKeto by remember { mutableStateOf(false) }
    var requirePaleo by remember { mutableStateOf(false) }

    var isFilterExpanded by remember { mutableStateOf(false) }

    // Selected recipe for details view
    var selectedRecipeDetail by remember { mutableStateOf<Recipe?>(null) }
    var recipeToIntegrate by remember { mutableStateOf<Recipe?>(null) }

    val cuisinesList = listOf("All", "Italian", "Indian", "Mediterranean", "Mexican", "Asian", "American")

    // Filter recipes locally
    val filteredRecipes = remember(
        recipes, searchQuery, selectedCuisine, maxPrepTime, maxCalories,
        requireVegan, requireVegetarian, requireGlutenFree, requireKeto, requirePaleo
    ) {
        recipes.filter { r ->
            val matchesSearch = searchQuery.isBlank() || 
                    r.name.contains(searchQuery, ignoreCase = true) || 
                    r.ingredientsCsv.contains(searchQuery, ignoreCase = true)
            
            val matchesCuisine = selectedCuisine == "All" || r.cuisine.equals(selectedCuisine, ignoreCase = true)
            val matchesPrep = r.prepTimeMinutes <= maxPrepTime.toInt()
            val matchesCalories = r.calories <= maxCalories.toInt()

            val matchesVegan = !requireVegan || r.isVegan
            val matchesVegetarian = !requireVegetarian || r.isVegetarian || r.isVegan
            val matchesGlutenFree = !requireGlutenFree || r.isGlutenFree
            val matchesKeto = !requireKeto || r.isKeto
            val matchesPaleo = !requirePaleo || r.isPaleo

            matchesSearch && matchesCuisine && matchesPrep && matchesCalories &&
                    matchesVegan && matchesVegetarian && matchesGlutenFree && matchesKeto && matchesPaleo
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter by ingredients (e.g. avocado, chicken)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recipe_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            )
        )

        // Filter Header Bar with toggle expansion button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredRecipes.size} recipes found",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { isFilterExpanded = !isFilterExpanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFilterExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isFilterExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp).testTag("recipes_filter_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Filters",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Expanded Filters Panel
        AnimatedVisibility(
            visible = isFilterExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cuisine selector
                    Text(
                        text = "CUISINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cuisinesList.forEach { cuisine ->
                            val isSelected = selectedCuisine == cuisine
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCuisine = cuisine },
                                label = { Text(cuisine, fontSize = 11.sp) },
                                modifier = Modifier.testTag("cuisine_chip_$cuisine")
                            )
                        }
                    }

                    // Numeric Sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MAX PREP: ${maxPrepTime.toInt()} MINS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = maxPrepTime,
                                onValueChange = { maxPrepTime = it },
                                valueRange = 15f..40f,
                                steps = 4,
                                modifier = Modifier.height(24.dp).testTag("prep_time_slider")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MAX CALORIES: ${maxCalories.toInt()} KCAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = maxCalories,
                                onValueChange = { maxCalories = it },
                                valueRange = 300f..800f,
                                steps = 9,
                                modifier = Modifier.height(24.dp).testTag("calories_slider")
                            )
                        }
                    }

                    // Dietary selections
                    Text(
                        text = "DIETARY PREFERENCES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dietaryChips = listOf(
                            Triple("Vegan", requireVegan, { requireVegan = !requireVegan }),
                            Triple("Vegetarian", requireVegetarian, { requireVegetarian = !requireVegetarian }),
                            Triple("Gluten-Free", requireGlutenFree, { requireGlutenFree = !requireGlutenFree }),
                            Triple("Keto", requireKeto, { requireKeto = !requireKeto }),
                            Triple("Paleo", requirePaleo, { requirePaleo = !requirePaleo })
                        )

                        dietaryChips.forEach { (label, isSelected, onToggle) ->
                            FilterChip(
                                selected = isSelected,
                                onClick = onToggle,
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("dietary_chip_$label")
                            )
                        }
                    }

                    // Clear button
                    TextButton(
                        onClick = {
                            selectedCuisine = "All"
                            maxPrepTime = 40f
                            maxCalories = 800f
                            requireVegan = false
                            requireVegetarian = false
                            requireGlutenFree = false
                            requireKeto = false
                            requirePaleo = false
                            searchQuery = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.align(Alignment.End).testTag("clear_filters_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Filters", fontSize = 12.sp)
                    }
                }
            }
        }

        // Recipes Grid/List
        if (filteredRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 36.sp)
                    Text(
                        text = "No matching recipes found",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Try adjusting your filters or searching for different ingredients",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipeRowItem(
                        recipe = recipe,
                        activePlan = activePlan,
                        onClick = { selectedRecipeDetail = recipe },
                        onIntegrateClick = { recipeToIntegrate = recipe }
                    )
                }
            }
        }
    }

    // Modal Recipe Details
    selectedRecipeDetail?.let { r ->
        RecipeDetailDialog(
            recipe = r,
            activePlan = activePlan,
            onIntegrateClick = {
                selectedRecipeDetail = null
                recipeToIntegrate = r
            },
            onDismiss = { selectedRecipeDetail = null }
        )
    }

    // Integration Selection Dialog
    recipeToIntegrate?.let { r ->
        IntegrationSelectionDialog(
            recipe = r,
            activePlan = activePlan,
            onConfirmReplacement = { dayNum, mealId ->
                onReplaceMeal(dayNum, mealId, r)
                recipeToIntegrate = null
            },
            onDismiss = { recipeToIntegrate = null }
        )
    }
}

@Composable
fun RecipeRowItem(
    recipe: Recipe,
    activePlan: MealPlan?,
    onClick: () -> Unit,
    onIntegrateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recipe_row_${recipe.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(recipe.cuisine.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.height(20.dp)
                    )

                    SuggestionChip(
                        onClick = {},
                        label = { Text(recipe.mealType, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🔥 ${recipe.calories} kcal", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(text = "⏱️ ${recipe.prepTimeMinutes} mins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "🧬 P:${recipe.proteinGrams}g C:${recipe.carbsGrams}g F:${recipe.fatsGrams}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Integration Action
            if (activePlan != null) {
                IconButton(
                    onClick = onIntegrateClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("integrate_recipe_btn_${recipe.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Integrate into active plan",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View details"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailDialog(
    recipe: Recipe,
    activePlan: MealPlan?,
    onIntegrateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .testTag("recipe_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(recipe.cuisine.uppercase(), fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text(recipe.mealType, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("recipe_detail_close_btn")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                    }
                }

                // Scroll Contents
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("⏱️ Cook: ${recipe.prepTimeMinutes}m", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("🔥 ${recipe.calories} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Nutrient Row
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MacroIndicator(label = "Protein", value = "${recipe.proteinGrams}g", color = Color(0xFFFF9800))
                            MacroIndicator(label = "Carbs", value = "${recipe.carbsGrams}g", color = Color(0xFF2196F3))
                            MacroIndicator(label = "Fats", value = "${recipe.fatsGrams}g", color = Color(0xFF4CAF50))
                        }
                    }

                    // Diet Tags List
                    val activeDiets = remember(recipe) {
                        buildList {
                            if (recipe.isVegan) add("Vegan 🌿")
                            if (recipe.isVegetarian) add("Vegetarian 🥗")
                            if (recipe.isGlutenFree) add("Gluten-Free 🌾")
                            if (recipe.isKeto) add("Keto 🥓")
                            if (recipe.isPaleo) add("Paleo 🥩")
                            if (recipe.isHalal) add("Halal 🕋")
                            if (recipe.isKosher) add("Kosher 🕎")
                        }
                    }
                    if (activeDiets.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeDiets.forEach { diet ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(diet, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Ingredients
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    recipe.ingredientsCsv.split("\n").filter { it.isNotBlank() }.forEach { ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("•", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = ing, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    // Instructions
                    Text(
                        text = "Directions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    recipe.instructionsCsv.split("\n").filter { it.isNotBlank() }.forEachIndexed { idx, dir ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (idx + 1).toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(text = dir, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Footer Actions
                if (activePlan != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = onIntegrateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("recipe_integrate_confirm_btn"),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Replace")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Integrate This Recipe Into Current Plan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationSelectionDialog(
    recipe: Recipe,
    activePlan: MealPlan?,
    onConfirmReplacement: (dayNumber: Int, mealId: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (activePlan == null) return

    var selectedDayNum by remember { mutableStateOf(activePlan.days.firstOrNull()?.dayNumber ?: 1) }
    val dayMeals = activePlan.days.find { it.dayNumber == selectedDayNum }?.meals ?: emptyList()
    var selectedMealId by remember { mutableStateOf(dayMeals.firstOrNull()?.id ?: "") }

    // Auto update selected meal ID if day changes
    LaunchedEffect(selectedDayNum) {
        val mealsForDay = activePlan.days.find { it.dayNumber == selectedDayNum }?.meals ?: emptyList()
        selectedMealId = mealsForDay.firstOrNull()?.id ?: ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("integration_select_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Replace Plan Meal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Which slot in your generated meal plan would you like to replace with the '${recipe.name}'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. SELECT DAY
                Text(text = "Select Day Number", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activePlan.days.forEach { day ->
                        val isSelected = selectedDayNum == day.dayNumber
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { selectedDayNum = day.dayNumber },
                            label = { Text("Day ${day.dayNumber}") },
                            modifier = Modifier.testTag("day_select_chip_${day.dayNumber}")
                        )
                    }
                }

                // 2. SELECT MEAL
                Text(text = "Select Meal Slot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayMeals.forEach { meal ->
                        val isSelected = selectedMealId == meal.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMealId = meal.id }
                                .testTag("meal_slot_card_${meal.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isSelected) null else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = meal.mealType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = meal.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Confirm / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("integration_cancel_btn")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedMealId.isNotEmpty()) {
                                onConfirmReplacement(selectedDayNum, selectedMealId)
                            }
                        },
                        enabled = selectedMealId.isNotEmpty(),
                        modifier = Modifier.testTag("integration_confirm_btn")
                    ) {
                        Text("Integrate Recipe")
                    }
                }
            }
        }
    }
}

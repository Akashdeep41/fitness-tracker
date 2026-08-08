package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.MealPlannerViewModel

enum class NavigationScreen(val title: String, val icon: @Composable () -> Unit) {
    DASHBOARD("Dashboard", { Icon(Icons.Default.Home, contentDescription = "Menu") }),
    RECIPES("Recipes", { Icon(Icons.Default.List, contentDescription = "Recipes") }),
    PREFERENCES("Preferences", { Icon(Icons.Default.Settings, contentDescription = "Prefs") }),
    SHOPPING("Shopping List", { Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping") }),
    FAVORITES("Favorites", { Icon(Icons.Default.Favorite, contentDescription = "Favs") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerAppUi(
    viewModel: MealPlannerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavigationScreen.DASHBOARD) }

    val activePlan by viewModel.activePlan.collectAsState()
    val currentProfile by viewModel.profile.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()
    val shoppingItems by viewModel.shoppingList.collectAsState()
    val allRecipes by viewModel.allRecipes.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedMealDetail by viewModel.selectedMealDetail.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Show reset button on Dashboard screen if there's an active plan
                    if (currentTab == NavigationScreen.DASHBOARD && activePlan != null) {
                        TextButton(
                            onClick = { viewModel.resetPlan() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("reset_plan_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Menu", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Plan", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Show custom info chip about API Key if unresolved
                    val hasKey = !com.example.BuildConfig.GEMINI_API_KEY.isNullOrBlank() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    if (!hasKey) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("⚠️ Setup Key", color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.padding(end = 8.dp).testTag("setup_key_indicator")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationScreen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentTab == screen,
                        onClick = { currentTab = screen },
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_${screen.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationScreen.DASHBOARD -> {
                    DashboardScreen(
                        activePlan = activePlan,
                        favoritesList = favoritesList,
                        profile = currentProfile,
                        selectedDay = selectedDay,
                        isGenerating = isGenerating,
                        errorMessage = errorMessage,
                        onSelectDay = { viewModel.selectDay(it) },
                        onMealClick = { viewModel.showMealDetail(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onSwapMeal = { dayNum, mealId -> viewModel.swapMeal(dayNum, mealId) },
                        onGenerateDefaultPlan = {
                            viewModel.generateMealPlan()
                            currentTab = NavigationScreen.DASHBOARD
                        },
                        onClearError = { viewModel.clearError() }
                    )
                }

                NavigationScreen.RECIPES -> {
                    RecipesScreen(
                        recipes = allRecipes,
                        activePlan = activePlan,
                        onReplaceMeal = { dayNum, mealId, recipe ->
                            viewModel.replaceMealWithRecipe(dayNum, mealId, recipe)
                            currentTab = NavigationScreen.DASHBOARD
                        }
                    )
                }

                NavigationScreen.PREFERENCES -> {
                    ProfileScreen(
                        currentProfile = currentProfile,
                        isGenerating = isGenerating,
                        onProfileSave = { viewModel.saveProfile(it) },
                        onGenerate = {
                            viewModel.generateMealPlan()
                            currentTab = NavigationScreen.DASHBOARD // jump dynamically back to dashboard display
                        }
                    )
                }

                NavigationScreen.SHOPPING -> {
                    ShoppingListScreen(
                        items = shoppingItems,
                        onToggleItem = { viewModel.toggleShoppingItem(it) },
                        onDeleteItem = { viewModel.deleteShoppingItem(it) },
                        onAddItem = { name, cat, details -> viewModel.addCustomShoppingItem(name, cat, details) },
                        onClearCompleted = { viewModel.deleteCompletedShoppingItems() }
                    )
                }

                NavigationScreen.FAVORITES -> {
                    FavoritesScreen(
                        favorites = favoritesList,
                        onRemoveFavorite = { viewModel.removeFavorite(it) },
                        onMealClick = { viewModel.showMealDetail(it) }
                    )
                }
            }
        }
    }

    // Secondary Screen detailing recipe instructions
    selectedMealDetail?.let { meal ->
        // Check if meal is favorited
        val isFav = favoritesList.any { it.id == meal.id }
        MealDetailDialog(
            meal = meal,
            isFavorited = isFav,
            isGenerating = isGenerating,
            onToggleFavorite = { viewModel.toggleFavorite(meal) },
            onSwapMeal = if (activePlan?.days?.any { day -> day.meals.any { it.id == meal.id } } == true) {
                // Find matching day number to execute swap correctly
                val dayNum = activePlan?.days?.find { day -> day.meals.any { it.id == meal.id } }?.dayNumber ?: selectedDay
                { viewModel.swapMeal(dayNum, meal.id) }
            } else null, // Disable swap if displaying from Favs hubs
            onDismiss = { viewModel.showMealDetail(null) }
        )
    }
}

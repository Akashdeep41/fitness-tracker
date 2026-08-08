package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.MealPlannerDatabase
import com.example.data.model.DietaryProfile
import com.example.data.model.FavoriteMeal
import com.example.data.model.MealPlan
import com.example.data.model.MealPlanMeal
import com.example.data.model.SavedMealPlan
import com.example.data.model.ShoppingListItem
import com.example.data.repository.PlannerRepository
import com.example.data.network.GeminiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealPlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MealPlannerViewModel"

    private val db = MealPlannerDatabase.getDatabase(application)
    private val repository = PlannerRepository(db.plannerDao())

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Expose flows from Repository backed by SQLite
    val profile: StateFlow<DietaryProfile> = repository.profile
        .combine(MutableStateFlow(Unit)) { p, _ -> p ?: DietaryProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DietaryProfile()
        )

    val favorites: StateFlow<List<FavoriteMeal>> = repository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shoppingList: StateFlow<List<ShoppingListItem>> = repository.shoppingList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRecipes: StateFlow<List<com.example.data.model.Recipe>> = repository.allRecipes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Parse Saved Meal Plan dynamically
    private val _activePlan = MutableStateFlow<MealPlan?>(null)
    val activePlan: StateFlow<MealPlan?> = _activePlan.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDay = MutableStateFlow(1)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _selectedMealDetail = MutableStateFlow<MealPlanMeal?>(null)
    val selectedMealDetail: StateFlow<MealPlanMeal?> = _selectedMealDetail.asStateFlow()

    init {
        seedRecipesIfNecessary()
        // Hydrate initial meal plan from DB cache if available
        viewModelScope.launch {
            try {
                repository.savedMealPlan.collect { savedPlan ->
                    if (savedPlan != null) {
                        val adapter = moshi.adapter(MealPlan::class.java)
                        _activePlan.value = adapter.fromJson(savedPlan.rawJson)
                    } else {
                        _activePlan.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hydrate saved meal plan", e)
            }
        }
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun showMealDetail(meal: MealPlanMeal?) {
        _selectedMealDetail.value = meal
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Saves the meal planner profile parameters
     */
    fun saveProfile(profile: DietaryProfile) {
        viewModelScope.launch {
            try {
                repository.saveProfile(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
            }
        }
    }

    /**
     * Triggers the central meal plan generation via Gemini API
     */
    fun generateMealPlan(days: Int = 3) {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            try {
                val currentProfile = profile.value
                val generated = GeminiService.generateMealPlan(currentProfile, days)
                if (generated != null) {
                    // Update Cache
                    val adapter = moshi.adapter(MealPlan::class.java)
                    val rawJson = adapter.toJson(generated)
                    repository.saveMealPlan(SavedMealPlan(rawJson = rawJson))

                    // Repopulate Shoppable Items based on new ingredients
                    rebuildShoppingListFromPlan(generated)
                    _selectedDay.value = 1
                } else {
                    _errorMessage.value = "Unable to connect or generate plan. Verify your Gemini API Key in the Secrets Panel."
                }
            } catch (e: Exception) {
                Log.e(TAG, "API interaction failed", e)
                _errorMessage.value = "Network or Parse Error: ${e.localizedMessage ?: "Try again"}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Swaps out/regenerates a single meal element in the current plan on-the-fly
     */
    fun swapMeal(dayNumber: Int, mealId: String) {
        val currentPlan = _activePlan.value ?: return
        val day = currentPlan.days.find { it.dayNumber == dayNumber } ?: return
        val mealToReplace = day.meals.find { it.id == mealId } ?: return

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            try {
                val currentProfile = profile.value
                val replacementMeal = GeminiService.regenerateMeal(currentProfile, mealToReplace, mealToReplace.mealType)
                if (replacementMeal != null) {
                    // Inject replacement meal into plan list
                    val updatedDays = currentPlan.days.map { d ->
                        if (d.dayNumber == dayNumber) {
                            val updatedMeals = d.meals.map { m ->
                                if (m.id == mealId) replacementMeal else m
                            }
                            d.copy(meals = updatedMeals)
                        } else {
                            d
                        }
                    }
                    val updatedPlan = currentPlan.copy(days = updatedDays)

                    // Write back to DB cache
                    val adapter = moshi.adapter(MealPlan::class.java)
                    val rawJson = adapter.toJson(updatedPlan)
                    repository.saveMealPlan(SavedMealPlan(rawJson = rawJson))

                    // Synchronize Shopping list item replacements
                    rebuildShoppingListFromPlan(updatedPlan)

                    // Update UI detail sheet if open
                    if (_selectedMealDetail.value?.id == mealId) {
                        _selectedMealDetail.value = replacementMeal
                    }
                } else {
                    _errorMessage.value = "Failed to regenerate replacement meal. Check your network or API settings."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Meal replacement failed", e)
                _errorMessage.value = "Replacement error: ${e.localizedMessage}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Erases plan cache and shuts shopping list
     */
    fun resetPlan() {
        viewModelScope.launch {
            try {
                repository.deleteSavedMealPlan()
                repository.clearShoppingList()
                _activePlan.value = null
                _selectedDay.value = 1
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting plan", e)
            }
        }
    }

    // --- FAVORITES ACTION ---
    fun toggleFavorite(mealMeal: MealPlanMeal) {
        viewModelScope.launch {
            try {
                val fav = FavoriteMeal(
                    id = mealMeal.id,
                    mealType = mealMeal.mealType,
                    name = mealMeal.name,
                    calories = mealMeal.calories,
                    proteinGrams = mealMeal.proteinGrams,
                    carbsGrams = mealMeal.carbsGrams,
                    fatsGrams = mealMeal.fatsGrams,
                    prepTimeMinutes = mealMeal.prepTimeMinutes,
                    ingredientsCsv = mealMeal.ingredients.joinToString("\n"),
                    instructionsCsv = mealMeal.instructions.joinToString("\n")
                )
                repository.toggleFavorite(fav)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
            }
        }
    }

    fun removeFavorite(favId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFavoriteById(favId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting favorite", e)
            }
        }
    }

    // --- SHOPPING LIST ACTION ---
    fun toggleShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            try {
                repository.updateShoppingItem(item.copy(isCompleted = !item.isCompleted))
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling shopping item", e)
            }
        }
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            try {
                repository.deleteShoppingItem(item)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting shopping item", e)
            }
        }
    }

    fun addCustomShoppingItem(name: String, category: String, details: String) {
        viewModelScope.launch {
            try {
                val newItem = ShoppingListItem(
                    name = name,
                    category = category,
                    details = details,
                    isCompleted = false,
                    mealSource = "Custom Item"
                )
                repository.addShoppingItem(newItem)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding custom shopping item", e)
            }
        }
    }

    fun deleteCompletedShoppingItems() {
        viewModelScope.launch {
            try {
                repository.deleteCompletedShoppingItems()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing completed items", e)
            }
        }
    }

    /**
     * Programmatically parses and reconstructs the user's shopping checklist automatically
     */
    private suspend fun rebuildShoppingListFromPlan(plan: MealPlan) {
        repository.clearShoppingList()
        val listItems = mutableListOf<ShoppingListItem>()
        for (day in plan.days) {
            for (meal in day.meals) {
                for (ingredient in meal.ingredients) {
                    val category = categorizeIngredient(ingredient)
                    // Split a bit to extract amount vs item name
                    val parsedResult = extractIngredientDetails(ingredient)
                    listItems.add(
                        ShoppingListItem(
                            name = parsedResult.first,
                            category = category,
                            details = parsedResult.second,
                            isCompleted = false,
                            mealSource = meal.name
                        )
                    )
                }
            }
        }
        repository.addShoppingItems(listItems)
    }

    private fun extractIngredientDetails(raw: String): Pair<String, String> {
        // Try parsing e.g. "1 cup whipped cream", "100g spinach", "2 leaves of lettuce"
        // Let's search if it starts with numbers or units
        val regex = Regex("^([\\d\\/\\s\\.\\-\\u00BC-\\u00BE]+)\\s*(cups|cup|tbsp|tsp|g|grams|oz|ounces|ml|pcs|large|small|medium|cans|can|slices|slice)?\\s*(of)?\\s*(.*)", RegexOption.IGNORE_CASE)
        val match = regex.find(raw.trim())
        if (match != null) {
            val amount = match.groupValues[1].trim()
            val unit = match.groupValues[2].trim()
            val name = match.groupValues[4].trim()
            val details = if (unit.isNotEmpty()) "$amount $unit" else amount
            return Pair(if (name.isNotEmpty()) name else raw, details)
        }
        return Pair(raw, "")
    }

    private fun categorizeIngredient(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("milk") || n.contains("cheese") || n.contains("butter") || n.contains("yogurt") || n.contains("cream") || n.contains("parmesan") || n.contains("mozzarella") || n.contains("feta") -> "Dairy"
            n.contains("chicken") || n.contains("beef") || n.contains("pork") || n.contains("turkey") || n.contains("bacon") || n.contains("salmon") || n.contains("tuna") || n.contains("fish") || n.contains("shrimp") || n.contains("steak") || n.contains("lamb") -> "Meats & Seafood"
            n.contains("spinach") || n.contains("kale") || n.contains("garlic") || n.contains("onion") || n.contains("tomato") || n.contains("lemon") || n.contains("lime") || n.contains("pepper") || n.contains("cucumber") || n.contains("broccoli") || n.contains("lettuce") || n.contains("carrot") || n.contains("apple") || n.contains("banana") || n.contains("berry") || n.contains("blueberry") || n.contains("strawberry") || n.contains("avocado") || n.contains("ginger") || n.contains("cilantro") || n.contains("parsley") || n.contains("mushroom") || n.contains("zucchini") || n.contains("cabbage") || n.contains("herb") -> "Produce"
            n.contains("rice") || n.contains("oat") || n.contains("bread") || n.contains("quinoa") || n.contains("pasta") || n.contains("flour") || n.contains("tortilla") || n.contains("noodle") || n.contains("toast") || n.contains("bagel") -> "Grains & Baking"
            n.contains("oil") || n.contains("salt") || n.contains("pepper") || n.contains("sauce") || n.contains("honey") || n.contains("maple") || n.contains("sugar") || n.contains("seed") || n.contains("almond") || n.contains("walnut") || n.contains("nut") || n.contains("vinegar") || n.contains("spice") || n.contains("cinnamon") || n.contains("powder") || n.contains("dressing") || n.contains("mayo") || n.contains("mustard") || n.contains("paste") || n.contains("syrup") || n.contains("olive") -> "Pantry & Spices"
            else -> "Other"
        }
    }

    /**
     * Replaces a specific meal in the active plan with a selected database recipe!
     */
    fun replaceMealWithRecipe(dayNumber: Int, mealId: String, recipe: com.example.data.model.Recipe) {
        val currentPlan = _activePlan.value ?: return
        val updatedDays = currentPlan.days.map { d ->
            if (d.dayNumber == dayNumber) {
                val updatedMeals = d.meals.map { m ->
                    if (m.id == mealId) {
                        recipe.toMealPlanMeal(uniqueId = m.id)
                    } else m
                }
                d.copy(meals = updatedMeals)
            } else d
        }
        val updatedPlan = currentPlan.copy(days = updatedDays)
        viewModelScope.launch {
            try {
                // Write back to DB cache
                val adapter = moshi.adapter(MealPlan::class.java)
                val rawJson = adapter.toJson(updatedPlan)
                repository.saveMealPlan(SavedMealPlan(rawJson = rawJson))
                // Refresh Shoppable items automatically
                rebuildShoppingListFromPlan(updatedPlan)
            } catch (e: Exception) {
                Log.e(TAG, "Error replacing meal with recipe", e)
            }
        }
    }

    private fun seedRecipesIfNecessary() {
        viewModelScope.launch {
            try {
                if (repository.getRecipeCount() == 0) {
                    val seedList = listOf(
                        com.example.data.model.Recipe(
                            name = "Classic Margherita Pizza",
                            cuisine = "Italian",
                            mealType = "Dinner",
                            calories = 550,
                            proteinGrams = 20,
                            carbsGrams = 72,
                            fatsGrams = 18,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "1 pre-made thin pizza crust\n1/2 cup organic tomato sauce\n1 cup fresh mozzarella cheese, sliced\n1/4 cup fresh basil leaves\n1 tbsp olive oil",
                            instructionsCsv = "Preheat oven to 450°F (230°C).\nSpread tomato sauce evenly over the crust.\nArrange fresh mozzarella slices across the surface.\nBake for 12-15 minutes until the crust is golden and cheese is bubbly.\nTop with fresh basil leaves and drizzle with olive oil before slicing.",
                            isVegetarian = true,
                            isKosher = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Creamy Fettuccine Alfredo",
                            cuisine = "Italian",
                            mealType = "Dinner",
                            calories = 780,
                            proteinGrams = 25,
                            carbsGrams = 85,
                            fatsGrams = 38,
                            prepTimeMinutes = 25,
                            ingredientsCsv = "12 oz fettuccine pasta\n1/2 cup unsalted butter\n1 cup heavy cream\n1 cup freshly grated Parmesan cheese\n2 cloves garlic, minced\nSalt and black pepper to taste",
                            instructionsCsv = "Cook fettuccine in boiling salted water according to package directions.\nIn a saucepan, melt butter over medium heat. Add garlic and cook for 1 minute.\nPour in heavy cream and bring to a gentle simmer for 5 minutes.\nStir in Parmesan cheese until fully melted and creamy.\nDrain pasta, toss in the Alfredo sauce, and season with black pepper and fresh parsley.",
                            isVegetarian = true,
                            isKosher = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Tuscan White Bean Soup",
                            cuisine = "Italian",
                            mealType = "Lunch",
                            calories = 320,
                            proteinGrams = 14,
                            carbsGrams = 52,
                            fatsGrams = 6,
                            prepTimeMinutes = 15,
                            ingredientsCsv = "2 cans cannellini beans, drained\n1 tbsp olive oil\n1 medium onion, diced\n2 carrots, sliced\n2 celery stalks, sliced\n3 cloves garlic, minced\n4 cups vegetable broth\n1 cup chopped kale or spinach",
                            instructionsCsv = "Heat olive oil in a large stockpot over medium heat.\nSauté onions, carrots, and celery until softened, about 6-8 minutes.\nAdd garlic and cook for 1 minute.\nStir in cannellini beans and vegetable broth. Bring to a boil, then simmer for 15 minutes.\nAdd kale/spinach and simmer for another 3 minutes until wilted. Season with salt and herbs.",
                            isVegan = true,
                            isVegetarian = true,
                            isGlutenFree = true,
                            isKosher = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Indian Chicken Tikka Masala",
                            cuisine = "Indian",
                            mealType = "Dinner",
                            calories = 620,
                            proteinGrams = 42,
                            carbsGrams = 28,
                            fatsGrams = 22,
                            prepTimeMinutes = 35,
                            ingredientsCsv = "1.5 lbs chicken breasts, cubed\n1 cup plain Greek yogurt\n2 tbsp lemon juice\n1 tbsp garam masala\n1 tbsp turmeric\n1 can (15 oz) tomato sauce\n1 cup heavy cream\n1 large onion, finely chopped\n3 cloves garlic, grated\n1 tbsp grated ginger",
                            instructionsCsv = "Marinate cubed chicken in yogurt, lemon juice, turmeric, and garam masala for at least 15 minutes.\nSear chicken pieces in a pan over medium-high heat until browned; set aside.\nIn the same pan, sauté chopped onions, garlic, and ginger until sweet and golden.\nStir in tomato sauce and simmer for 10 minutes.\nStir in heavy cream, add chicken back to the pan, and simmer on low for 15 minutes until chicken is cooked through.",
                            isGlutenFree = true,
                            isKeto = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Chana Masala (Chickpea Curry)",
                            cuisine = "Indian",
                            mealType = "Dinner",
                            calories = 380,
                            proteinGrams = 15,
                            carbsGrams = 64,
                            fatsGrams = 8,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "2 cans (15 oz) chickpeas, rinsed and drained\n1 tbsp coconut oil\n1 large onion, diced\n3 cloves garlic, minced\n1 tbsp finely grated ginger\n1 tbsp coriander powder\n1 tbsp cumin powder\n1 tsp cayenne pepper\n1 can crushed tomatoes\n1 cup water\nFresh cilantro for garnish",
                            instructionsCsv = "Heat coconut oil in a deep pot over medium heat. Sauté onions until golden, then add garlic and ginger.\nStir in cumin, coriander, and cayenne pepper and cook for 30 seconds to release aromatics.\nAdd crushed tomatoes, water, and drained chickpeas. Bring to a boil, then reduce heat and simmer uncovered for 15 minutes until thick.\nGarnish with fresh chopped cilantro and serve.",
                            isVegan = true,
                            isVegetarian = true,
                            isGlutenFree = true,
                            isKosher = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Classic Greek Salmon Bowl",
                            cuisine = "Mediterranean",
                            mealType = "Lunch",
                            calories = 510,
                            proteinGrams = 38,
                            carbsGrams = 42,
                            fatsGrams = 19,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "2 salmon fillets\n1 cup dry quinoa\n2 cups water\n1 cup cucumber, diced\n1 cup cherry tomatoes, halved\n1/2 cup Kalamata olives, sliced\n1/4 cup crumbled feta cheese\n2 tbsp Greek vinaigrette dressing",
                            instructionsCsv = "Rinse quinoa and boil in 2 cups of water. Cover and simmer for 15 minutes until tender.\nSeason salmon with salt, pepper, and Mediterranean herbs. Sear in a skillet for 4-5 minutes per side until flaky.\nDivide cooked quinoa into bowls.\nTop with cucumber, tomatoes, olives, feta, and the grilled salmon.\nDrizzle dressing over each bowl and serve cold or warm.",
                            isGlutenFree = true,
                            isHalal = true,
                            isKosher = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Hummus & Falafel Wrap",
                            cuisine = "Mediterranean",
                            mealType = "Lunch",
                            calories = 440,
                            proteinGrams = 14,
                            carbsGrams = 68,
                            fatsGrams = 12,
                            prepTimeMinutes = 15,
                            ingredientsCsv = "2 large whole wheat tortillas\n1/2 cup ready-to-eat hummus\n6 baked precooked falafel balls\n1 cup shredded lettuce\n1/2 red onion, sliced\n1 cucumber, sliced\n2 tbsp tahini sauce",
                            instructionsCsv = "Warm whole wheat tortillas slightly in a dry pan or microwave.\nSpread a generous layer of hummus in the center of each wrap.\nArrange 3 warm falafels on each wrap, pressing them down slightly to flatten them.\nAdd shredded lettuce, red onion, and cucumber slices over the falafels.\nDrizzle with creamy tahini sauce and wrap tightly. Cut in half and enjoy.",
                            isVegan = true,
                            isVegetarian = true,
                            isKosher = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Spicy Sizzling Black Bean Tacos",
                            cuisine = "Mexican",
                            mealType = "Lunch",
                            calories = 360,
                            proteinGrams = 13,
                            carbsGrams = 54,
                            fatsGrams = 10,
                            prepTimeMinutes = 15,
                            ingredientsCsv = "1 can black beans, drained and rinsed\n4 small corn tortillas\n1/2 cup salsa verde\n1 avocado, cubed\n1/2 red onion, finely chopped\n1 tsp cumin and chili powder\nFresh cilantro and lime wedges",
                            instructionsCsv = "In a small saucepan, combine black beans, cumin, chili powder, and 2 tablespoons of water. Cook over medium heat for 5 minutes, mashing slightly.\nWarm corn tortillas in a dry skillet over medium-high heat for 30 seconds on each side.\nFill each tortilla with seasoned black beans.\nTop with fresh cubed avocado, red onion, salsa verde, and fresh cilantro.\nServe immediately with a squeeze of fresh lime juice.",
                            isVegan = true,
                            isVegetarian = true,
                            isGlutenFree = true,
                            isKosher = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Mexican Chicken Fajita Platter",
                            cuisine = "Mexican",
                            mealType = "Dinner",
                            calories = 490,
                            proteinGrams = 36,
                            carbsGrams = 16,
                            fatsGrams = 18,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "1 lb chicken breast, cut into strips\n2 bell peppers (any color), sliced\n1 large onion, sliced\n1 tbsp chili powder\n1 tsp ground cumin\n1 tsp onion powder\n2 tbsp olive oil\nGuacamole for serving",
                            instructionsCsv = "Toss chicken strips, sliced bell peppers, and onion in olive oil, chili powder, cumin, and onion powder.\nHeat a large heavy skillet or cast-iron pan over medium-high heat.\nAdd the fajita mixture and sauté, stirring frequently, for 8-10 minutes until chicken is completely cooked and peppers are lightly charred.\nServe hot on a platter accompanied by freshly prepared guacamole.",
                            isGlutenFree = true,
                            isKeto = true,
                            isPaleo = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Crispy Ginger Tofu Stir-Fry",
                            cuisine = "Asian",
                            mealType = "Lunch",
                            calories = 390,
                            proteinGrams = 18,
                            carbsGrams = 44,
                            fatsGrams = 14,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "1 block extra firm tofu, pressed and cubed\n2 cups broccoli florets\n1 cup snap peas\n1 bell pepper, sliced\n2 tbsp low-sodium soy sauce (or gluten-free tamari)\n1 tbsp maple syrup\n1 tbsp fresh ginger, minced\n2 cloves garlic, minced\n1 tbsp sesame oil",
                            instructionsCsv = "Cube tofu and dry with a paper towel. Pan-fry tofu in sesame oil over medium-high heat for 6-8 minutes until golden and crispy; remove from pan.\nIn the same pan, toss broccoli florets, snap peas, and sweet bell peppers. Cook for 4-5 minutes until tender-crisp.\nIn a small bowl, whisk soy sauce, maple syrup, ginger, and minced garlic.\nAdd tofu back into the stir-fry pan and pour sauce over. Toss thoroughly for 2 minutes to glaze.",
                            isVegan = true,
                            isVegetarian = true,
                            isGlutenFree = true,
                            isKosher = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Thai Green Curry Bowl",
                            cuisine = "Asian",
                            mealType = "Dinner",
                            calories = 540,
                            proteinGrams = 32,
                            carbsGrams = 36,
                            fatsGrams = 28,
                            prepTimeMinutes = 25,
                            ingredientsCsv = "1 lb boneless chicken thighs, cubed\n2 tbsp Thai green curry paste\n1 can (14 oz) unsweetened light coconut milk\n1 cup sliced bamboo shoots\n1 red bell pepper, sliced\n1 cup fresh Thai basil leaves\n1 tbsp fish sauce\n1 tbsp coconut sugar",
                            instructionsCsv = "Heat a large deep pan over medium heat. Sauté green curry paste for 1 minute until fragrant.\nGradually stir in coconut milk, mixing thoroughly to dissolve the curry paste.\nAdd cubed chicken thighs, fish sauce, and coconut sugar. Simmer gently for 10 minutes.\nAdd bamboo shoots and bell pepper slices, and simmer 5 minutes until vegetables are tender.\nRemove from heat, fold in fresh Thai basil leaves, and serve with steamed jasmine rice.",
                            isGlutenFree = true,
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Beef & Broccoli Noodles",
                            cuisine = "Asian",
                            mealType = "Dinner",
                            calories = 610,
                            proteinGrams = 38,
                            carbsGrams = 68,
                            fatsGrams = 18,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "12 oz flank steak, sliced thin across grain\n2 cups broccoli florets\n6 oz ramen or soba noodles\n1/4 cup oyster sauce\n2 tbsp soy sauce\n1 tbsp brown sugar\n2 cloves garlic, minced\n1 tsp cornstarch mixed with 2 tbsp water\n1 tbsp vegetable oil",
                            instructionsCsv = "Cook noodles in boiling water, drain, and set aside.\nWhisk oyster sauce, soy sauce, brown sugar, garlic, and cornstarch slurry in a bowl.\nHeat oil in a wok or large skillet. Sear sliced beef strips for 2-3 minutes until browned; set aside.\nAdd broccoli and cook for 3 minutes with a splash of water to steam.\nReturn beef to wok, add noodles, pour in sauce glaze, and stir-fry for 2 minutes until hot and bubbly.",
                            isHalal = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Low-Carb Garlic Herb Burger",
                            cuisine = "American",
                            mealType = "Lunch",
                            calories = 490,
                            proteinGrams = 34,
                            carbsGrams = 6,
                            fatsGrams = 36,
                            prepTimeMinutes = 15,
                            ingredientsCsv = "1/2 lb lean ground beef\n1 tbsp fresh rosemary, minced\n1 tsp garlic powder\n2 large crisp iceberg lettuce leaves (for buns)\n1 thick slice red onion\n1 slice cheddar cheese\n1 tbsp mayonnaise",
                            instructionsCsv = "In a bowl, combine ground beef with rosemary, garlic powder, salt, and pepper. Shape into a thick patty.\nGrill or pan-sear patty over medium-high heat for 4-5 minutes per side to desired doneness.\nPlace cheese slice on top during last minute of cooking to melt.\nSpread mayonnaise on lettuce leaves.\nAssemble the burger by wrapping the cooked patty and onion slice in the lettuce leaves.",
                            isKeto = true,
                            isPaleo = true,
                            isHalal = true,
                            isKosher = true
                        ),
                        com.example.data.model.Recipe(
                            name = "Gluten-Free Crispy Chicken Tenders",
                            cuisine = "American",
                            mealType = "Lunch",
                            calories = 420,
                            proteinGrams = 38,
                            carbsGrams = 18,
                            fatsGrams = 16,
                            prepTimeMinutes = 20,
                            ingredientsCsv = "1 lb chicken breast tenderloins\n1 cup almond flour\n1/2 cup grated Parmesan cheese\n1 tsp paprika\n1/2 tsp onion powder\n1 large egg, whisked\nCooking spray",
                            instructionsCsv = "Preheat oven or air fryer to 400°F (200°C).\nIn one shallow bowl, place whisked egg. In another, mix almond flour, Parmesan, paprika, onion powder, salt, and pepper.\nDip each tenderloin in egg, then dredge thoroughly in the seasoned flour mixture.\nArrange coated chicken on a parchment-lined baking sheet and mist with cooking spray.\nBake for 15-18 minutes until crisp, golden brown, and cooked through.",
                            isGlutenFree = true,
                            isKeto = true,
                            isKosher = true
                        ),
                        com.example.data.model.Recipe(
                            name = "BBQ Pulled Chicken Sweet Potato",
                            cuisine = "American",
                            mealType = "Dinner",
                            calories = 460,
                            proteinGrams = 32,
                            carbsGrams = 58,
                            fatsGrams = 8,
                            prepTimeMinutes = 30,
                            ingredientsCsv = "2 medium sweet potatoes\n1 cup shredded pre-cooked chicken breast\n1/3 cup low-sugar BBQ sauce\n1/4 cup green onions, chopped\nSalt and pepper to taste",
                            instructionsCsv = "Wash sweet potatoes and poke several times with a fork. Microwave on high for 6-8 minutes until completely tender.\nIn a bowl, toss shredded cooked chicken with low-sugar BBQ sauce until well coated.\nSlice cooked potatoes in half lengthwise and mash the insides slightly with a fork.\nStuff with the BBQ shredded chicken and microwave for 1 minute to warm through.\nGarnish with chopped green onions and serve.",
                            isGlutenFree = true,
                            isPaleo = true,
                            isKosher = true,
                            isHalal = true
                        )
                    )
                    repository.insertRecipes(seedList)
                    Log.i(TAG, "Seeded recipes successfully!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed recipes database", e)
            }
        }
    }
}

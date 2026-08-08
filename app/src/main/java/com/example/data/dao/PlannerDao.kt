package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DietaryProfile
import com.example.data.model.FavoriteMeal
import com.example.data.model.SavedMealPlan
import com.example.data.model.ShoppingListItem
import com.example.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // --- Dietary Profile ---
    @Query("SELECT * FROM dietary_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<DietaryProfile?>

    @Query("SELECT * FROM dietary_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): DietaryProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DietaryProfile)

    // --- Active Meal Plan ---
    @Query("SELECT * FROM saved_meal_plan WHERE id = 1 LIMIT 1")
    fun getSavedMealPlanFlow(): Flow<SavedMealPlan?>

    @Query("SELECT * FROM saved_meal_plan WHERE id = 1 LIMIT 1")
    suspend fun getSavedMealPlan(): SavedMealPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMealPlan(plan: SavedMealPlan)

    @Query("DELETE FROM saved_meal_plan WHERE id = 1")
    suspend fun deleteSavedMealPlan()

    // --- Favorite Meals ---
    @Query("SELECT * FROM favorite_meals ORDER BY savedTime DESC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteMeal>>

    @Query("SELECT * FROM favorite_meals WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: String): FavoriteMeal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorite(meal: FavoriteMeal)

    @Query("DELETE FROM favorite_meals WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    // --- Shopping List ---
    @Query("SELECT * FROM shopping_list ORDER BY category ASC, id ASC")
    fun getShoppingListFlow(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingListItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingListItem>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingListItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list WHERE isCompleted = 1")
    suspend fun deleteCompletedShoppingItems()

    @Query("DELETE FROM shopping_list")
    suspend fun clearShoppingList()

    // --- Recipes Database ---
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipesFlow(): Flow<List<Recipe>>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<Recipe>)

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: Int): Recipe?
}

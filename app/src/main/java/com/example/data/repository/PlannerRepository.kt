package com.example.data.repository

import com.example.data.dao.PlannerDao
import com.example.data.model.DietaryProfile
import com.example.data.model.FavoriteMeal
import com.example.data.model.SavedMealPlan
import com.example.data.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

import com.example.data.model.Recipe

class PlannerRepository(private val dao: PlannerDao) {

    val profile: Flow<DietaryProfile?> = dao.getProfileFlow()
    val savedMealPlan: Flow<SavedMealPlan?> = dao.getSavedMealPlanFlow()
    val favorites: Flow<List<FavoriteMeal>> = dao.getAllFavoritesFlow()
    val shoppingList: Flow<List<ShoppingListItem>> = dao.getShoppingListFlow()
    val allRecipes: Flow<List<Recipe>> = dao.getAllRecipesFlow()

    suspend fun getRecipeCount(): Int = dao.getRecipeCount()

    suspend fun insertRecipes(recipes: List<Recipe>) = dao.insertRecipes(recipes)

    suspend fun getRecipeById(id: Int): Recipe? = dao.getRecipeById(id)

    suspend fun getProfile(): DietaryProfile? = dao.getProfile()

    suspend fun saveProfile(profile: DietaryProfile) {
        dao.insertProfile(profile)
    }

    suspend fun getSavedMealPlan(): SavedMealPlan? = dao.getSavedMealPlan()

    suspend fun saveMealPlan(plan: SavedMealPlan) {
        dao.saveMealPlan(plan)
    }

    suspend fun deleteSavedMealPlan() {
        dao.deleteSavedMealPlan()
    }

    suspend fun isFavorite(id: String): Boolean {
        return dao.getFavoriteById(id) != null
    }

    suspend fun toggleFavorite(meal: FavoriteMeal) {
        if (isFavorite(meal.id)) {
            dao.deleteFavoriteById(meal.id)
        } else {
            dao.saveFavorite(meal)
        }
    }

    suspend fun deleteFavoriteById(id: String) {
        dao.deleteFavoriteById(id)
    }

    suspend fun addShoppingItem(item: ShoppingListItem) {
        dao.insertShoppingItem(item)
    }

    suspend fun addShoppingItems(items: List<ShoppingListItem>) {
        dao.insertShoppingItems(items)
    }

    suspend fun updateShoppingItem(item: ShoppingListItem) {
        dao.updateShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingListItem) {
        dao.deleteShoppingItem(item)
    }

    suspend fun deleteCompletedShoppingItems() {
        dao.deleteCompletedShoppingItems()
    }

    suspend fun clearShoppingList() {
        dao.clearShoppingList()
    }
}

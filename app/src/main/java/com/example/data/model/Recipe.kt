package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cuisine: String, // Italian, Asian, Mexican, Indian, Mediterranean, American, French, Spanish
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val prepTimeMinutes: Int,
    val ingredientsCsv: String, // Newline separated ingredients
    val instructionsCsv: String, // Newline separated instructions
    val isVegan: Boolean = false,
    val isVegetarian: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isKeto: Boolean = false,
    val isPaleo: Boolean = false,
    val isHalal: Boolean = false,
    val isKosher: Boolean = false
) {
    fun toMealPlanMeal(uniqueId: String = "recipe_$id"): MealPlanMeal {
        return MealPlanMeal(
            id = uniqueId,
            mealType = mealType,
            name = name,
            calories = calories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatsGrams = fatsGrams,
            prepTimeMinutes = prepTimeMinutes,
            ingredients = ingredientsCsv.split("\n").filter { it.isNotBlank() },
            instructions = instructionsCsv.split("\n").filter { it.isNotBlank() }
        )
    }
}

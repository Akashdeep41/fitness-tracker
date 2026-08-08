package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MealPlan(
    val days: List<MealPlanDay> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MealPlanDay(
    val dayNumber: Int,
    val dayName: String,
    val title: String = "",
    val meals: List<MealPlanMeal> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MealPlanMeal(
    val id: String,
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val name: String,
    val calories: Int,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatsGrams: Int = 0,
    val prepTimeMinutes: Int = 15,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
)

@Entity(tableName = "saved_meal_plan")
data class SavedMealPlan(
    @PrimaryKey val id: Int = 1, // Store the active plan
    val rawJson: String,
    val generatedTime: Long = System.currentTimeMillis()
)

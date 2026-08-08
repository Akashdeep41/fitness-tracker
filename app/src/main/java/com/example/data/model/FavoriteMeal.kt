package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meals")
data class FavoriteMeal(
    @PrimaryKey val id: String,
    val mealType: String,
    val name: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val prepTimeMinutes: Int,
    val ingredientsCsv: String,
    val instructionsCsv: String,
    val savedTime: Long = System.currentTimeMillis()
)

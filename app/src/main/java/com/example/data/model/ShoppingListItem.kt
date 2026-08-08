package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Produce, Pantry, Meats, Dairy, Grains, etc.
    val details: String = "",
    val isCompleted: Boolean = false,
    val mealSource: String = ""
)

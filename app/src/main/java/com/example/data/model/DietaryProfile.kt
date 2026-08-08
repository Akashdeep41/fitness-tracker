package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dietary_profile")
data class DietaryProfile(
    @PrimaryKey val id: Int = 1,
    val isVegan: Boolean = false,
    val isVegetarian: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isKeto: Boolean = false,
    val isPaleo: Boolean = false,
    val isHalal: Boolean = false,
    val isKosher: Boolean = false,
    val allergyPeanuts: Boolean = false,
    val allergyTreeNuts: Boolean = false,
    val allergyDairy: Boolean = false,
    val allergySoy: Boolean = false,
    val allergyEgg: Boolean = false,
    val allergyShellfish: Boolean = false,
    val allergyGluten: Boolean = false,
    val calorieTarget: Int = 2000,
    val ratioType: String = "Balanced", // Balanced, Low Carb, High Protein
    val dislikedIngredients: String = "",
    val generalPreferences: String = ""
) {
    fun toRestrictionsString(): String {
        val list = mutableListOf<String>()
        if (isVegan) list.add("Vegan")
        if (isVegetarian) list.add("Vegetarian")
        if (isGlutenFree) list.add("Gluten-Free")
        if (isKeto) list.add("Keto")
        if (isPaleo) list.add("Paleo")
        if (isHalal) list.add("Halal")
        if (isKosher) list.add("Kosher")
        return list.joinToString(", ")
    }

    fun toAllergiesString(): String {
        val list = mutableListOf<String>()
        if (allergyPeanuts) list.add("Peanuts")
        if (allergyTreeNuts) list.add("Tree Nuts")
        if (allergyDairy) list.add("Dairy")
        if (allergySoy) list.add("Soy")
        if (allergyEgg) list.add("Eggs")
        if (allergyShellfish) list.add("Shellfish")
        if (allergyGluten) list.add("Gluten")
        return list.joinToString(", ")
    }
}

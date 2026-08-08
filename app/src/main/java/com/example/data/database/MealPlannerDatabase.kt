package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.PlannerDao
import com.example.data.model.DietaryProfile
import com.example.data.model.FavoriteMeal
import com.example.data.model.SavedMealPlan
import com.example.data.model.ShoppingListItem

import com.example.data.model.Recipe

@Database(
    entities = [
        DietaryProfile::class,
        SavedMealPlan::class,
        FavoriteMeal::class,
        ShoppingListItem::class,
        Recipe::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MealPlannerDatabase : RoomDatabase() {

    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var INSTANCE: MealPlannerDatabase? = null

        fun getDatabase(context: Context): MealPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MealPlannerDatabase::class.java,
                    "meal_planner_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

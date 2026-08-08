# 🥗 Custom Meal Planner - AI-Powered Nutrition & Recipe Assistant

A modern, offline-first Android application built with **Kotlin** and **Jetpack Compose** that creates personalized meal plans, recipe suggestions, and automated grocery shopping lists powered by **Google Gemini AI**.

---

## ✨ Features

- 🤖 **AI-Powered Meal Plan Generation**: Instantly craft balanced, tailored weekly meal plans based on your calorie targets, dietary preferences, and macro goals using Google Gemini.
- 📊 **Interactive Dashboard**: Track your daily calories and macronutrients (Carbs, Protein, Fat) with progress indicators and view meals organized by day (Breakfast, Lunch, Dinner, Snack).
- 🍲 **Recipe Explorer & Custom Integration**: Discover healthy recipes complete with preparation time, difficulty level, full ingredient breakdowns, and step-by-step instructions. Seamlessly integrate any recipe into your active meal plan.
- 🛒 **Smart Shopping List**: Automatically aggregates ingredients from your current meal plan into categorized shopping lists (Produce, Dairy, Meat, Pantry, etc.) with checkable items.
- ⚙️ **Dietary Profile & Preferences**: Configure dietary restrictions (Vegan, Vegetarian, Keto, Gluten-Free, Paleo, Halal, etc.), allergy exclusions, calorie goals, and cooking skill levels.
- ⭐ **Favorites & Local Saved Plans**: Bookmark favorite dishes and save entire meal plans locally for offline access.
- 💾 **Offline-First Storage**: Powered by Android Jetpack Room database for local caching and fast performance.

---

## 🛠 Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Data Layer
- **Local Database**: [Room Database](https://developer.android.com/training/data-storage/room) with KSP
- **Asynchronous Operations**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Networking & AI Integration**: Retrofit / Ktor & Google Gemini REST API
- **Design System**: Material 3 Design System with edge-to-edge support

---

## 📂 Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt               # Main entry point & theme initialization
├── data/
│   ├── dao/                      # Room DAOs for offline data persistence
│   ├── database/                 # Room Database configuration
│   ├── model/                    # Data models (MealPlan, Recipe, ShoppingListItem, etc.)
│   ├── network/                  # Gemini AI API integration service
│   └── repository/               # Central repository for meal data & user preferences
├── ui/
│   ├── components/               # Jetpack Compose UI Screens
│   │   ├── DashboardScreen.kt    # Daily/Weekly planner & calorie/macro tracking
│   │   ├── RecipesScreen.kt      # Recipe discovery, filtering & detail view
│   │   ├── ProfileScreen.kt      # Dietary preferences & macro target setup
│   │   ├── ShoppingListScreen.kt # Auto-generated grocery list
│   │   └── FavoritesScreen.kt    # Bookmarked meals & saved plans
│   ├── theme/                    # Material 3 Color palette, typography, and theme setup
│   └── viewmodel/                # ViewModel managing state & business logic
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Jellyfish | 2023.3.1 or newer
- **JDK**: Version 17
- **Android SDK**: Minimum API Level 24 (Android 7.0), Target API Level 34+
- **Gemini API Key**: Obtain an API key from [Google AI Studio](https://aistudio.google.com/)

### Configuration

1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/custom-meal-planner.git
   cd custom-meal-planner
   ```

2. Configure your Gemini API key:
   Create a `.env` file in the project root (or set `GEMINI_API_KEY` in your environment):
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. Open the project in **Android Studio** and sync Gradle dependencies.

4. Run the app on an Android Emulator or connected physical device:
   ```bash
   ./gradlew installDebug
   ```

---

## 🧪 Testing

Run local unit and JVM tests with Robolectric:

```bash
./gradlew :app:testDebugUnitTest
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

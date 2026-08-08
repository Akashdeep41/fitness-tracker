package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.DietaryProfile
import com.example.data.model.MealPlan
import com.example.data.model.MealPlanMeal
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generates a 3-day meal plan based on the user's dietary preferences
     */
    suspend fun generateMealPlan(profile: DietaryProfile, numDays: Int = 3): MealPlan? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            Log.e(TAG, "API Key is missing or default setup.")
            return@withContext null
        }

        // Build the prompt containing dietary limitations and nutrition priorities
        val restrictions = profile.toRestrictionsString()
        val allergies = profile.toAllergiesString()
        val macros = profile.ratioType
        val calorieTarget = profile.calorieTarget
        val exclusions = profile.dislikedIngredients
        val preferences = profile.generalPreferences

        val prompt = """
            Generate a completely customized $numDays-day meal plan based on these user settings:
            - Dietary Restrictions: ${if (restrictions.isBlank()) "None" else restrictions}
            - Major Allergies to strictly AVOID: ${if (allergies.isBlank()) "None" else allergies}
            - Daily Calories Target: $calorieTarget kcal per day
            - Macronutrient Ratio Focus: $macros
            - Excluded Ingredients (DO NOT include these in any meal): ${if (exclusions.isBlank()) "None" else exclusions}
            - Custom Preferences/Requests: ${if (preferences.isBlank()) "None" else preferences}

            Structure the response as a single, valid JSON object matching the following structure:
            {
               "days": [
                  {
                     "dayNumber": 1,
                     "dayName": "Day 1",
                     "title": "Clean & Balanced Day",
                     "meals": [
                        {
                           "id": "unique_string_1",
                           "mealType": "Breakfast",
                           "name": "Meal designation",
                           "calories": 450,
                           "proteinGrams": 25,
                           "carbsGrams": 40,
                           "fatsGrams": 15,
                           "prepTimeMinutes": 15,
                           "ingredients": [
                              "1 cup cooked steel-cut oats",
                              "1 tbsp pumpkin seeds",
                              "1/2 cup fresh blueberries"
                           ],
                           "instructions": [
                              "Prepare the steel-cut oats according to package directions.",
                              "Stir in pumpkin seeds and blueberries before serving."
                           ]
                        },
                        ...
                     ]
                  }
               ]
            }

            Rule requirements:
            1. Generate exactly $numDays days.
            2. For each day, include exactly 4 meals: "Breakfast", "Lunch", "Dinner", and "Snack".
            3. Each meal MUST have exact realistic calories, proteinGrams, carbsGrams, fatsGrams matching the target and diet type (e.g., higher fat/protein for Keto, low carbs for Keto, high protein for High Protein profile). The sum of calories for the day's 4 meals should be close to $calorieTarget calories (+/- 100 kcal).
            4. Make sure all ingredients are fully compatible with the dietary profile and allergy lists.
            5. Return ONLY a valid JSON block, NO supplementary markdown formatting.
        """.trimIndent()

        try {
            val jsonPayload = JSONObject()
            val contentsArray = org.json.JSONArray()
            val contentObj = JSONObject()
            val partsArray = org.json.JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonPayload.put("contents", contentsArray)

            // Setup generation config to demand JSON response-type
            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            jsonPayload.put("generationConfig", genConfig)

            val url = "$BASE_URL/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed with code: ${response.code}, message: ${response.message}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: return@withContext null

            Log.d(TAG, "Received raw text from Gemini: $rawText")

            // Parse response body into classes
            val adapter = moshi.adapter(MealPlan::class.java)
            return@withContext adapter.fromJson(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or parsing meal plan", e)
            return@withContext null
        }
    }

    /**
     * Swaps/regenerates a single meal element in the plan
     */
    suspend fun regenerateMeal(
        profile: DietaryProfile,
        currentMeal: MealPlanMeal,
        mealType: String
    ): MealPlanMeal? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            Log.e(TAG, "API Key is missing or default setup.")
            return@withContext null
        }

        val restrictions = profile.toRestrictionsString()
        val allergies = profile.toAllergiesString()
        val macros = profile.ratioType
        val excluded = profile.dislikedIngredients

        val prompt = """
            We are swapping a single meal in our meal plan.
            Our restrictions are:
            - Dietary Restrictions: ${if (restrictions.isBlank()) "None" else restrictions}
            - Strictly Avoid Allergies: ${if (allergies.isBlank()) "None" else allergies}
            - Focus Macrons: $macros
            - Exclude Ingredients: ${if (excluded.isBlank()) "None" else excluded}

            We want to completely swap this specific meal:
            - Current Meal Name: "${currentMeal.name}"
            - Target Calories: ${currentMeal.calories} kcal
            - Meal Type: $mealType

            Generate a delicious, alternative recipe of type "$mealType" that perfectly satisfies the restrictions.
            Format the response as a single valid JSON object matching the MealPlanMeal model:
            {
               "id": "${java.util.UUID.randomUUID()}",
               "mealType": "$mealType",
               "name": "Exciting replacement dish name",
               "calories": ${currentMeal.calories},
               "proteinGrams": ${currentMeal.proteinGrams},
               "carbsGrams": ${currentMeal.carbsGrams},
               "fatsGrams": ${currentMeal.fatsGrams},
               "prepTimeMinutes": 20,
               "ingredients": [
                  "detailed ingredient with quantities",
                  "another ingredient"
               ],
               "instructions": [
                  "step 1 instruction",
                  "step 2 instruction"
               ]
            }

            Return ONLY the valid raw JSON schema of a meal element, NO markdown formatting or descriptions.
        """.trimIndent()

        try {
            val jsonPayload = JSONObject()
            val contentsArray = org.json.JSONArray()
            val contentObj = JSONObject()
            val partsArray = org.json.JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonPayload.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            jsonPayload.put("generationConfig", genConfig)

            val url = "$BASE_URL/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed with code: ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val rootJson = JSONObject(responseBody)
            val rawText = rootJson.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: return@withContext null

            val adapter = moshi.adapter(MealPlanMeal::class.java)
            return@withContext adapter.fromJson(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Error swapping single meal", e)
            return@withContext null
        }
    }
}

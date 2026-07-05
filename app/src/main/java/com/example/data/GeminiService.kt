package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Volatile field to allow passing from outside dynamically
    @Volatile
    private var dynamicApiKey: String? = null

    fun setDynamicApiKey(key: String?) {
        dynamicApiKey = key
    }

    /**
     * Checks if a valid API key has been injected.
     */
    fun getApiKey(context: android.content.Context? = null): String {
        // 1. Prioritize dynamic in-memory key (passed from outside)
        val dynKey = dynamicApiKey
        if (!dynKey.isNullOrBlank()) {
            return dynKey
        }

        // 2. Fallback to local persistent SharedPreferences
        if (context != null) {
            val prefs = context.getSharedPreferences("mindsparq_prefs", android.content.Context.MODE_PRIVATE)
            val savedKey = prefs.getString("custom_api_key", null)
            if (!savedKey.isNullOrBlank()) {
                return savedKey
            }
        }

        // 3. Fallback to BuildConfig key
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY") {
            ""
        } else {
            key
        }
    }

    fun isApiKeyConfigured(context: android.content.Context? = null): Boolean {
        return getApiKey(context).isNotEmpty()
    }

    /**
     * Generates text from the Gemini model.
     * Always runs on Dispatchers.IO.
     */
    suspend fun generateText(prompt: String, systemInstruction: String? = null, context: android.content.Context? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured. Please add it to your Settings or Secrets in AI Studio."))
        }

        try {
            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })

                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }

                // Add a generationConfig to keep temperature balanced
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequest.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    val errorMsg = "HTTP ${response.code}: ${response.message}\n$responseBody"
                    Log.e(TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No generation candidates returned from Gemini."))
                }

                val candidate = candidates.optJSONObject(0)
                val content = candidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    return@withContext Result.failure(Exception("No content parts found in candidate."))
                }

                val responseText = parts.optJSONObject(0)?.optString("text") ?: ""
                return@withContext Result.success(responseText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating text from Gemini API", e)
            return@withContext Result.failure(e)
        }
    }
}

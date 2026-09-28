package com.example.data.network

import com.example.data.model.ViralMoment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        // Exact fallback model chain required
        val FALLBACK_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-3.1-flash-lite"
        )
    }

    /**
     * Calls Gemini to analyze the video transcript with automatic model fallback
     * and exponential backoff retry for 503 / high demand conditions.
     */
    suspend fun analyzeTranscript(
        apiKey: String,
        transcript: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Result<ViralMoment> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing or not configured. Please paste your free Google AI Studio Gemini API key.")
            )
        }

        val promptText = """
You are an expert video editor and viral short-form content strategist. Analyze the provided video transcript and identify the single most engaging, high-retention 30 to 50-second moment.

Return ONLY raw JSON matching this schema (do NOT include markdown code blocks or additional text):

{
  "title": "Short Catchy Title",
  "hook_score": 9.8,
  "start_time_seconds": 15,
  "end_time_seconds": 45,
  "reasoning": "High energy hook."
}

Transcript input:
$transcript
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject().apply {
                put("text", promptText)
            }
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.4)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", generationConfig)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var lastErrorMessage = "Unknown error"

        // Iterate through fallback chain
        for (modelIndex in FALLBACK_MODELS.indices) {
            val currentModel = FALLBACK_MODELS[modelIndex]
            val maxRetries = 3

            // Retry loop with exponential backoff
            for (attempt in 0..maxRetries) {
                try {
                    val url = "$BASE_URL$currentModel:generateContent?key=$cleanKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseString = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        // Successfully received response, parse JSON
                        val rootJson = JSONObject(responseString)
                        val candidates = rootJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
                            val rawText = parts?.optJSONObject(0)?.optString("text").orEmpty().trim()

                            if (rawText.isNotBlank()) {
                                val cleanJsonString = rawText
                                    .removePrefix("```json")
                                    .removePrefix("```")
                                    .removeSuffix("```")
                                    .trim()

                                val viralJson = JSONObject(cleanJsonString)
                                val title = viralJson.optString("title", "Viral Highlight")
                                val hookScore = viralJson.optDouble("hook_score", 9.2)
                                var startSec = viralJson.optInt("start_time_seconds", 15)
                                var endSec = viralJson.optInt("end_time_seconds", 45)
                                val reasoning = viralJson.optString("reasoning", "High retention hook identified with rapid pacing.")

                                // Sanity check timestamps
                                if (startSec < 0) startSec = 0
                                if (endSec <= startSec) endSec = startSec + 30
                                if (endSec - startSec < 15) endSec = startSec + 30
                                if (endSec - startSec > 60) endSec = startSec + 50

                                return@withContext Result.success(
                                    ViralMoment(
                                        title = title,
                                        hookScore = hookScore,
                                        startTimeSeconds = startSec,
                                        endTimeSeconds = endSec,
                                        reasoning = reasoning
                                    )
                                )
                            }
                        }
                    }

                    // Extract error message
                    val errorMsg = try {
                        val errJson = JSONObject(responseString)
                        errJson.optJSONObject("error")?.optString("message") ?: response.message
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    lastErrorMessage = "[$currentModel] $errorMsg"

                    // If API key is invalid (400/403 with API_KEY_INVALID), fail immediately without useless retries
                    if (response.code == 400 && errorMsg.contains("API_KEY_INVALID", ignoreCase = true)) {
                        return@withContext Result.failure(
                            Exception("Invalid Gemini API Key: $errorMsg")
                        )
                    }

                    // Check if error is 503 (High Demand / Model Overloaded) or 429
                    val isHighDemand = response.code == 503 ||
                            response.code == 429 ||
                            errorMsg.contains("503", ignoreCase = true) ||
                            errorMsg.contains("overloaded", ignoreCase = true) ||
                            errorMsg.contains("high demand", ignoreCase = true) ||
                            errorMsg.contains("UNAVAILABLE", ignoreCase = true) ||
                            errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)

                    if (isHighDemand) {
                        if (attempt < maxRetries) {
                            // Exponential backoff: 2s -> 4s -> 8s + random jitter
                            val baseDelaySec = when (attempt) {
                                0 -> 2L
                                1 -> 4L
                                else -> 8L
                            }
                            val jitterMs = Random.nextLong(100, 500)
                            val totalDelayMs = (baseDelaySec * 1000L) + jitterMs

                            onStatusUpdate("Gemini high demand on $currentModel. Retrying in ${baseDelaySec}s (attempt ${attempt + 1}/$maxRetries)...")
                            delay(totalDelayMs)
                            continue // Retry with same model
                        } else {
                            // Retries exhausted for this model. Switch to next backup model if available
                            val nextModelIndex = modelIndex + 1
                            if (nextModelIndex < FALLBACK_MODELS.size) {
                                val nextModel = FALLBACK_MODELS[nextModelIndex]
                                onStatusUpdate("Gemini high demand. Retrying on backup model ($nextModel)...")
                                break // Break inner loop, proceed to next model in FALLBACK_MODELS
                            }
                        }
                    } else {
                        // Non-503 error, try next backup model
                        val nextModelIndex = modelIndex + 1
                        if (nextModelIndex < FALLBACK_MODELS.size) {
                            val nextModel = FALLBACK_MODELS[nextModelIndex]
                            onStatusUpdate("Retrying on backup model ($nextModel)...")
                            break
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMessage = "[$currentModel] ${e.localizedMessage ?: e.message}"
                    if (attempt < maxRetries) {
                        val baseDelaySec = when (attempt) {
                            0 -> 2L
                            1 -> 4L
                            else -> 8L
                        }
                        val jitterMs = Random.nextLong(100, 500)
                        val totalDelayMs = (baseDelaySec * 1000L) + jitterMs
                        onStatusUpdate("Connection error on $currentModel. Retrying in ${baseDelaySec}s...")
                        delay(totalDelayMs)
                    } else {
                        val nextModelIndex = modelIndex + 1
                        if (nextModelIndex < FALLBACK_MODELS.size) {
                            val nextModel = FALLBACK_MODELS[nextModelIndex]
                            onStatusUpdate("Gemini high demand. Retrying on backup model ($nextModel)...")
                            break
                        }
                    }
                }
            }
        }

        Result.failure(
            Exception("Gemini AI Analysis failed across all fallback models. Last error: $lastErrorMessage")
        )
    }
}

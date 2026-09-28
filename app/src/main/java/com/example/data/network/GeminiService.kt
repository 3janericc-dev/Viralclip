package com.example.data.network

import com.example.data.model.ViralMoment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    /**
     * Calls Gemini to analyze the video transcript and extract the viral moment.
     */
    suspend fun analyzeTranscript(
        apiKey: String,
        transcript: String
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

        try {
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
            val url = "$BASE_URL$MODEL_NAME:generateContent?key=$cleanKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.optJSONObject("error")?.optString("message") ?: response.message
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                return@withContext Result.failure(
                    Exception("Gemini API Error ($MODEL_NAME): $errorMsg")
                )
            }

            // Parse response
            val rootJson = JSONObject(responseString)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(
                    Exception("Gemini returned no response candidates.")
                )
            }

            val firstCandidate = candidates.getJSONObject(0)
            val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text").orEmpty().trim()

            if (rawText.isBlank()) {
                return@withContext Result.failure(
                    Exception("Gemini generated empty response text.")
                )
            }

            // Clean code fences if any
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

            Result.success(
                ViralMoment(
                    title = title,
                    hookScore = hookScore,
                    startTimeSeconds = startSec,
                    endTimeSeconds = endSec,
                    reasoning = reasoning
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze viral moment with Gemini: ${e.localizedMessage ?: e.message}"))
        }
    }
}

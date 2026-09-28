package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("viral_clip_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_CUSTOM_RENDER_URL = "custom_render_url"
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var customRenderUrl: String
        get() = prefs.getString(KEY_CUSTOM_RENDER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_RENDER_URL, value.trim()).apply()
}

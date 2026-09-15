package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() {
            val savedKey = prefs.getString(KEY_API_KEY, "") ?: ""
            if (savedKey.isNotBlank()) return savedKey
            // Fallback to BuildConfig if provided via .env
            return try {
                val field = BuildConfig::class.java.getField("GAPGPT_API_KEY")
                field.get(null) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
        }
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var defaultAlarmMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_ALARM_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_ALARM_MINUTES, value).apply()

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value).apply()

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    companion object {
        private const val PREF_NAME = "voice_assistant_prefs"
        private const val KEY_API_KEY = "key_gapgpt_api_key"
        private const val KEY_DEFAULT_ALARM_MINUTES = "key_default_alarm_minutes"
        private const val KEY_AI_MODEL = "key_ai_model"
    }
}

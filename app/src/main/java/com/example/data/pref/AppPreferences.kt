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

    var wpSiteUrl: String
        get() = prefs.getString(KEY_WP_SITE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WP_SITE_URL, value.trim().removeSuffix("/")).apply()

    var wpUsername: String
        get() = prefs.getString(KEY_WP_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WP_USERNAME, value.trim()).apply()

    var wpAppPassword: String
        get() = prefs.getString(KEY_WP_APP_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WP_APP_PASSWORD, value.trim()).apply()

    var wcConsumerKey: String
        get() = prefs.getString(KEY_WC_CONSUMER_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WC_CONSUMER_KEY, value.trim()).apply()

    var wcConsumerSecret: String
        get() = prefs.getString(KEY_WC_CONSUMER_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WC_CONSUMER_SECRET, value.trim()).apply()

    var pwaUrl: String
        get() = prefs.getString(KEY_PWA_URL, DEFAULT_PWA_URL) ?: DEFAULT_PWA_URL
        set(value) = prefs.edit().putString(KEY_PWA_URL, value.trim()).apply()

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    fun isWordPressConfigured(): Boolean = wpSiteUrl.isNotBlank() && 
        ((wpUsername.isNotBlank() && wpAppPassword.isNotBlank()) || (wcConsumerKey.isNotBlank() && wcConsumerSecret.isNotBlank()))

    companion object {
        private const val PREF_NAME = "voice_assistant_prefs"
        private const val KEY_API_KEY = "key_gapgpt_api_key"
        private const val KEY_DEFAULT_ALARM_MINUTES = "key_default_alarm_minutes"
        private const val KEY_AI_MODEL = "key_ai_model"
        private const val KEY_WP_SITE_URL = "key_wp_site_url"
        private const val KEY_WP_USERNAME = "key_wp_username"
        private const val KEY_WP_APP_PASSWORD = "key_wp_app_password"
        private const val KEY_WC_CONSUMER_KEY = "key_wc_consumer_key"
        private const val KEY_WC_CONSUMER_SECRET = "key_wc_consumer_secret"
        private const val KEY_PWA_URL = "key_pwa_url"
        const val DEFAULT_PWA_URL = "https://ais-pre-7btfgxrxbfd2ofyjq537a2-782499955673.europe-west2.run.app"
    }
}

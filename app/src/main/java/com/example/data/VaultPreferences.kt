package com.example.data

import android.content.Context
import android.content.SharedPreferences

class VaultPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vault_secure_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VAULT_PIN = "vault_pin"
        private const val KEY_PIN_SET = "vault_pin_set"
        private const val KEY_SECURITY_QUESTION = "vault_security_question"
        private const val KEY_SECURITY_ANSWER = "vault_security_answer"
        private const val KEY_ACCENT_INDEX = "vault_accent_index"
        private const val KEY_LOCK_ON_EXIT = "vault_lock_on_exit"
        private const val KEY_SEEDED_DATA = "vault_seeded_data"
        private const val KEY_RESET_ON_EXIT = "reset_on_exit"
        private const val KEY_HIDE_RECENTS = "hide_recents_preview"
        private const val KEY_BLOCK_SCREENSHOTS = "block_screenshots"
        private const val KEY_SEARCH_ENGINE = "search_engine"
    }

    var resetOnExit: Boolean
        get() = prefs.getBoolean(KEY_RESET_ON_EXIT, true)
        set(value) = prefs.edit().putBoolean(KEY_RESET_ON_EXIT, value).apply()

    var hideRecentsPreview: Boolean
        get() = prefs.getBoolean(KEY_HIDE_RECENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_RECENTS, value).apply()

    var blockScreenshots: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, value).apply()

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "Google") ?: "Google"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    fun getSearchUrl(query: String): String {
        val encoded = try {
            java.net.URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query
        }
        return when (searchEngine.lowercase()) {
            "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
            "brave" -> "https://search.brave.com/search?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    var pin: String
        get() = prefs.getString(KEY_VAULT_PIN, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_VAULT_PIN, value).apply()

    var isPinSet: Boolean
        get() = prefs.getBoolean(KEY_PIN_SET, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_SET, value).apply()

    var securityQuestion: String
        get() = prefs.getString(KEY_SECURITY_QUESTION, "What is your favorite city?") ?: "What is your favorite city?"
        set(value) = prefs.edit().putString(KEY_SECURITY_QUESTION, value).apply()

    var securityAnswer: String
        get() = prefs.getString(KEY_SECURITY_ANSWER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECURITY_ANSWER, value.lowercase().trim()).apply()

    var accentColorIndex: Int
        get() = prefs.getInt(KEY_ACCENT_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_ACCENT_INDEX, value).apply()

    var lockOnExit: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ON_EXIT, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ON_EXIT, value).apply()

    var hasSeededData: Boolean
        get() = prefs.getBoolean(KEY_SEEDED_DATA, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED_DATA, value).apply()

    var hasCleanedLegacySeed: Boolean
        get() = prefs.getBoolean("vault_cleaned_legacy_seed", false)
        set(value) = prefs.edit().putBoolean("vault_cleaned_legacy_seed", value).apply()

    fun verifyPin(input: String): Boolean {
        return input == pin
    }

    fun verifySecurityAnswer(input: String): Boolean {
        val cleanInput = input.lowercase().trim()
        val stored = securityAnswer
        return stored.isNotEmpty() && cleanInput == stored
    }
}

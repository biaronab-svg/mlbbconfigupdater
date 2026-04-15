package com.heroconfigmanager.data.repository

import android.content.Context
import com.heroconfigmanager.BuildConfig

object AppSettingsRepository {

    private const val PREFS_NAME = "hero_config_settings"
    private const val KEY_GITHUB_TOKEN = "github_token"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getCustomGitHubToken(): String {
        return prefs().getString(KEY_GITHUB_TOKEN, "")?.trim().orEmpty()
    }

    fun getEffectiveGitHubToken(): String {
        return getCustomGitHubToken().ifBlank { BuildConfig.GITHUB_TOKEN.trim() }
    }

    fun saveGitHubToken(token: String) {
        prefs().edit()
            .putString(KEY_GITHUB_TOKEN, token.trim())
            .apply()
    }

    fun resetGitHubToken() {
        prefs().edit()
            .remove(KEY_GITHUB_TOKEN)
            .apply()
    }

    fun isUsingCustomGitHubToken(): Boolean = getCustomGitHubToken().isNotBlank()

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

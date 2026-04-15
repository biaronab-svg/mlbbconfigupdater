package com.heroconfigmanager.data.repository

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.heroconfigmanager.data.model.HeroConfig
import com.heroconfigmanager.data.remote.GitHubUpdateRequest
import com.heroconfigmanager.data.remote.NetworkClient

object ConfigRepository {

    private const val OWNER = "biaronab-svg"
    private const val REPO = "mlbb-biar"
    private const val FILE_PATH = "config.json"

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val api = NetworkClient.gitHubApiService

    private var cachedSha: String = ""

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    fun clearCachedState() {
        cachedSha = ""
    }

    suspend fun fetchConfig(): Result<HeroConfig> {
        return try {
            val authToken = authTokenOrNull()
                ?: return Result.Error("GitHub token is missing. Open Settings and add one.")

            val response = api.getFile(
                token = authToken,
                owner = OWNER,
                repo = REPO,
                path = FILE_PATH
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty response body")
                cachedSha = body.sha
                val cleaned = body.content.replace("\n", "").replace("\r", "")
                val json = String(Base64.decode(cleaned, Base64.DEFAULT))
                val config = gson.fromJson(json, HeroConfig::class.java)
                Result.Success(config)
            } else {
                Result.Error("GitHub API error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error fetching config")
        }
    }

    suspend fun pushConfig(config: HeroConfig): Result<Unit> {
        return try {
            val authToken = authTokenOrNull()
                ?: return Result.Error("GitHub token is missing. Open Settings and add one.")

            if (cachedSha.isEmpty()) {
                val fetchResult = fetchConfig()
                if (fetchResult is Result.Error) return fetchResult
            }

            val json = gson.toJson(config)
            val encoded = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val request = GitHubUpdateRequest(
                message = "Update config.json v${config.configVersion}",
                content = encoded,
                sha = cachedSha
            )
            val response = api.updateFile(
                token = authToken,
                owner = OWNER,
                repo = REPO,
                path = FILE_PATH,
                body = request
            )
            if (response.isSuccessful) {
                response.body()?.content?.sha?.let { cachedSha = it }
                Result.Success(Unit)
            } else {
                Result.Error("Push failed ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error pushing config")
        }
    }

    private fun authTokenOrNull(): String? {
        val rawToken = AppSettingsRepository.getEffectiveGitHubToken()
        return rawToken.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
    }
}

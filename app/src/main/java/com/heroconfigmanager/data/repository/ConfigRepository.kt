package com.heroconfigmanager.data.repository

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.heroconfigmanager.data.model.HeroConfig
import com.heroconfigmanager.data.remote.GitHubUpdateRequest
import com.heroconfigmanager.data.remote.NetworkClient

object ConfigRepository {
        
    // GitHub repo coordinates — match the web app
    private const val OWNER      = "biaronab-svg"
    private const val REPO       = "mlbb-biar"
    private const val FILE_PATH  = "config.json"
    private val AUTH_TOKEN = "Bearer ${com.heroconfigmanager.BuildConfig.GITHUB_TOKEN}"

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val api = NetworkClient.gitHubApiService

    // Cached SHA required by GitHub to update a file
    private var cachedSha: String = ""

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    suspend fun fetchConfig(): Result<HeroConfig> {
        return try {
            val response = api.getFile(
                token  = AUTH_TOKEN,
                owner  = OWNER,
                repo   = REPO,
                path   = FILE_PATH
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error("Empty response body")
                cachedSha = body.sha
                // GitHub returns content as Base64 with newlines — strip them before decoding
                val cleaned  = body.content.replace("\n", "").replace("\r", "")
                val json     = String(Base64.decode(cleaned, Base64.DEFAULT))
                val config   = gson.fromJson(json, HeroConfig::class.java)
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
            if (cachedSha.isEmpty()) {
                // Need SHA before we can push — fetch first
                val fetchResult = fetchConfig()
                if (fetchResult is Result.Error) return fetchResult
            }
            val json      = gson.toJson(config)
            val encoded   = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val request   = GitHubUpdateRequest(
                message = "Update config.json v${config.configVersion}",
                content = encoded,
                sha     = cachedSha
            )
            val response = api.updateFile(
                token  = AUTH_TOKEN,
                owner  = OWNER,
                repo   = REPO,
                path   = FILE_PATH,
                body   = request
            )
            if (response.isSuccessful) {
                // Update cached SHA from response so next push works without re-fetch
                response.body()?.content?.sha?.let { cachedSha = it }
                Result.Success(Unit)
            } else {
                Result.Error("Push failed ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error pushing config")
        }
    }
}

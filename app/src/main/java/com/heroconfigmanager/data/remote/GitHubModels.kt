package com.heroconfigmanager.data.remote

import com.google.gson.annotations.SerializedName

data class GitHubFileResponse(
    @SerializedName("sha")     val sha:     String,
    @SerializedName("content") val content: String,
    @SerializedName("html_url") val htmlUrl: String
)

data class GitHubUpdateRequest(
    @SerializedName("message") val message: String,
    @SerializedName("content") val content: String,
    @SerializedName("sha")     val sha:     String
)

data class GitHubUpdateResponse(
    @SerializedName("content") val content: GitHubFileResponse?
)

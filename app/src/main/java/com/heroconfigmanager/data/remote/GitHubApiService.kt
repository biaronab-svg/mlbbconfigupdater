package com.heroconfigmanager.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface GitHubApiService {

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFile(
        @Header("Authorization")        token:  String,
        @Header("Accept")               accept: String = "application/vnd.github.v3+json",
        @Path("owner")                  owner:  String,
        @Path("repo")                   repo:   String,
        @Path(value = "path", encoded = true) path: String
    ): Response<GitHubFileResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFile(
        @Header("Authorization")        token:  String,
        @Header("Accept")               accept: String = "application/vnd.github.v3+json",
        @Path("owner")                  owner:  String,
        @Path("repo")                   repo:   String,
        @Path(value = "path", encoded = true) path: String,
        @Body                           body:   GitHubUpdateRequest
    ): Response<GitHubUpdateResponse>
}

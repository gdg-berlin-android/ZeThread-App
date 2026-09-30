package de.berlindroid.zethread.data.remote

import de.berlindroid.zethread.data.model.CreateFileRequest
import de.berlindroid.zethread.data.model.CreateFileResponse
import de.berlindroid.zethread.data.model.GitHubUserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface GitHubApiService {

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("X-GitHub-Api-Version") apiVersion: String = "2026-03-10",
        @Body request: CreateFileRequest
    ): Response<CreateFileResponse>

    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String,
        @Header("Authorization") authorization: String? = null,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("X-GitHub-Api-Version") apiVersion: String = "2026-03-10"
    ): Response<GitHubUserResponse>
}

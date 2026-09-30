package de.berlindroid.zethread.data.repository

import android.util.Log
import de.berlindroid.zethread.data.model.CommitInfo
import de.berlindroid.zethread.data.model.ContentInfo
import de.berlindroid.zethread.data.model.CreateFileRequest
import de.berlindroid.zethread.data.model.CreateFileResponse
import de.berlindroid.zethread.data.model.GitHubErrorResponse
import de.berlindroid.zethread.data.model.GitHubUserResponse
import de.berlindroid.zethread.data.remote.GitHubApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

interface GitHubRepository {
    suspend fun createFile(
        owner: String,
        repo: String,
        path: String,
        token: String,
        request: CreateFileRequest
    ): Result<CreateFileResponse>

    suspend fun getUser(
        username: String,
        token: String? = null
    ): Result<GitHubUserResponse>

    companion object {
        operator fun invoke(): GitHubRepository = RealGitHubRepository()
    }
}

data class MockCallRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val endpoint: String,
    val method: String = "PUT",
    val owner: String,
    val repo: String,
    val path: String,
    val branch: String,
    val message: String,
    val authorName: String,
    val authorEmail: String,
    val base64Length: Int,
    val generatedSha: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

class FakeGitHubRepository : GitHubRepository {
    private val _lastCall = MutableStateFlow<MockCallRecord?>(null)
    val lastCall: StateFlow<MockCallRecord?> = _lastCall.asStateFlow()

    override suspend fun createFile(
        owner: String,
        repo: String,
        path: String,
        token: String,
        request: CreateFileRequest
    ): Result<CreateFileResponse> {
        delay(600) // Simulate network latency

        val generatedSha = UUID.randomUUID().toString().replace("-", "") + "00000000"
        val sha40 = generatedSha.take(40)
        val endpoint = "https://api.github.com/repos/$owner/$repo/contents/$path"

        val record = MockCallRecord(
            endpoint = endpoint,
            owner = owner,
            repo = repo,
            path = path,
            branch = request.branch ?: "main",
            message = request.message,
            authorName = request.author.name,
            authorEmail = request.author.email,
            base64Length = request.content.length,
            generatedSha = sha40
        )
        _lastCall.value = record

        val approxKb = (request.content.length * 3) / 4 / 1024
        val previewChars = if (request.content.length > 60) request.content.take(60) + "..." else request.content

        val logBlock = buildString {
            appendLine("╔═══════════════════════════════════════════════════════════════════════════════╗")
            appendLine("║               [ZeThread] FAKE GITHUB REPOSITORY DISPATCH                      ║")
            appendLine("╠═══════════════════════════════════════════════════════════════════════════════╣")
            appendLine("║ Method & URL  : PUT $endpoint")
            appendLine("║ Target Branch : ${request.branch ?: "main"}")
            appendLine("║ Author        : ${request.author.name} <${request.author.email}>")
            appendLine("║ Committer     : ${request.committer.name} <${request.committer.email}>")
            appendLine("║ Commit Message: \"${request.message}\"")
            appendLine("║ Image Payload : ${request.content.length} chars (~$approxKb KB)")
            appendLine("║ Base64 Head   : $previewChars")
            appendLine("║ Simulated SHA : $sha40")
            appendLine("║ Simulated HTTP: 201 Created")
            appendLine("╚═══════════════════════════════════════════════════════════════════════════════╝")
        }

        try {
            Log.i("ZeThread", logBlock)
        } catch (_: Throwable) {
            println(logBlock)
        }

        return Result.success(
            CreateFileResponse(
                content = ContentInfo(
                    name = path.substringAfterLast("/"),
                    path = path,
                    sha = sha40.take(20),
                    size = (request.content.length * 3) / 4,
                    downloadUrl = "https://raw.githubusercontent.com/$owner/$repo/${request.branch ?: "main"}/$path"
                ),
                commit = CommitInfo(
                    sha = sha40,
                    htmlUrl = "https://github.com/$owner/$repo/commit/$sha40"
                )
            )
        )
    }

    override suspend fun getUser(
        username: String,
        token: String?
    ): Result<GitHubUserResponse> {
        delay(200)
        try {
            Log.i("ZeThread", "[ZeThread] Fake GitHub getUser('$username') -> 404 Not Found")
        } catch (_: Throwable) {
            println("[ZeThread] Fake GitHub getUser('$username') -> 404 Not Found")
        }
        return Result.failure(IllegalStateException("User '$username' not found on GitHub (404)"))
    }
}

class RealGitHubRepository(
    private val apiService: GitHubApiService = createDefaultApiService()
) : GitHubRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun createFile(
        owner: String,
        repo: String,
        path: String,
        token: String,
        request: CreateFileRequest
    ): Result<CreateFileResponse> {
        return runCatching {
            val authHeader = if (token.startsWith("Bearer ") || token.startsWith("token ")) {
                token
            } else {
                "Bearer $token"
            }

            val response = apiService.createOrUpdateFile(
                owner = owner,
                repo = repo,
                path = path,
                authorization = authHeader,
                request = request
            )

            if (response.isSuccessful) {
                response.body() ?: throw IllegalStateException("Empty response body from GitHub API")
            } else {
                val errorRaw = response.errorBody()?.string().orEmpty()
                val parsedMessage = runCatching {
                    json.decodeFromString<GitHubErrorResponse>(errorRaw).message
                }.getOrNull()

                val errorMessage = when {
                    !parsedMessage.isNullOrBlank() -> parsedMessage
                    response.code() == 401 -> "Unauthorized: Bad GitHub PAT token."
                    response.code() == 404 -> "Not Found: Repository '$owner/$repo' not found or PAT lacks repo permissions."
                    response.code() == 409 -> "Conflict: File or commit conflict on target branch."
                    response.code() == 422 -> "Validation failed (422): Check repo branch, file path, or author format."
                    else -> "GitHub API Error ${response.code()}: ${response.message()}"
                }
                throw IllegalStateException(errorMessage)
            }
        }
    }

    override suspend fun getUser(
        username: String,
        token: String?
    ): Result<GitHubUserResponse> {
        return runCatching {
            val authHeader = if (!token.isNullOrBlank()) {
                if (token.startsWith("Bearer ") || token.startsWith("token ")) token else "Bearer $token"
            } else null

            val response = apiService.getUser(
                username = username,
                authorization = authHeader
            )

            if (response.isSuccessful) {
                response.body() ?: throw IllegalStateException("Empty response body from GitHub API")
            } else {
                throw IllegalStateException("User '$username' not found on GitHub (${response.code()})")
            }
        }
    }

    companion object {
        fun createDefaultApiService(): GitHubApiService {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS // Avoid logging huge base64 body
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            return retrofit.create(GitHubApiService::class.java)
        }
    }
}

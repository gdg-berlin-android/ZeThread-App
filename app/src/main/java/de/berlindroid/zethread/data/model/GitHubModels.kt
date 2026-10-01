package de.berlindroid.zethread.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFileRequest(
    val message: String,
    val content: String, // Base64 encoded file content
    val author: CommitAuthorIdent,
    val committer: CommitAuthorIdent,
    val branch: String? = null
)

@Serializable
data class CommitAuthorIdent(
    val name: String,
    val email: String
)

@Serializable
data class CreateFileResponse(
    val content: ContentInfo? = null,
    val commit: CommitInfo
)

@Serializable
data class CommitInfo(
    val sha: String,
    @SerialName("html_url")
    val htmlUrl: String? = null
)

@Serializable
data class ContentInfo(
    val name: String,
    val path: String,
    val sha: String? = null,
    val size: Int? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null
)

@Serializable
data class GitHubErrorResponse(
    val message: String? = null,
    @SerialName("documentation_url")
    val documentationUrl: String? = null
)

@Serializable
data class GitHubUserResponse(
    val login: String,
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val bio: String? = null
)

@Serializable
data class PatchMetadata(
    val id: String,
    val author: PatchAuthor,
    val coordinates: PatchCoordinates,
    val note: String? = null,
    val timestamp: String,
    val image: String,
    @SerialName("image_commit_sha")
    val imageCommitSha: String? = null
)

@Serializable
data class PatchAuthor(
    val handle: String,
    val name: String? = null,
    val email: String
)

@Serializable
data class PatchCoordinates(
    val x: Int,
    val y: Int
)


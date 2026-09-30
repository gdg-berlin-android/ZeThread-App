package de.berlindroid.zethread.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.berlindroid.zethread.data.local.AppConfig
import de.berlindroid.zethread.data.local.AppSettingsDataStore
import de.berlindroid.zethread.data.model.CommitAuthorIdent
import de.berlindroid.zethread.data.model.CreateFileRequest
import de.berlindroid.zethread.data.model.PatchAuthor
import de.berlindroid.zethread.data.model.PatchCoordinates
import de.berlindroid.zethread.data.model.PatchMetadata
import de.berlindroid.zethread.data.repository.FakeGitHubRepository
import de.berlindroid.zethread.data.repository.GitHubRepository
import de.berlindroid.zethread.data.repository.MockCallRecord
import de.berlindroid.zethread.data.repository.RealGitHubRepository
import de.berlindroid.zethread.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed interface UserLookupState {
    data object Idle : UserLookupState
    data object Searching : UserLookupState
    data class Found(
        val handle: String,
        val displayName: String?,
        val email: String?,
        val suggestedEmail: String
    ) : UserLookupState
    data class NotFound(val handle: String) : UserLookupState
}

sealed interface TapestryUiState {
    data object Camera : TapestryUiState
    data class PreviewCapture(val bitmap: Bitmap) : TapestryUiState
    data class Form(
        val bitmap: Bitmap,
        val handle: String = "",
        val email: String = "",
        val note: String = "",
        val x: Int = 0,
        val y: Int = 0
    ) : TapestryUiState
    data class Submitting(val terminalLogs: List<String>) : TapestryUiState
    data class Success(val commitHash: String, val htmlUrl: String?) : TapestryUiState
    data class Error(
        val message: String,
        val bitmap: Bitmap,
        val handle: String,
        val email: String,
        val note: String,
        val x: Int = 0,
        val y: Int = 0
    ) : TapestryUiState
}

class TapestryViewModel @JvmOverloads constructor(
    application: Application,
    private val realRepository: GitHubRepository = RealGitHubRepository(),
    private val fakeRepository: FakeGitHubRepository = FakeGitHubRepository()
) : AndroidViewModel(application) {

    companion object {
        private val json = Json { prettyPrint = true }
    }

    private val settingsDataStore = AppSettingsDataStore(application.applicationContext)

    val appConfig: StateFlow<AppConfig> = settingsDataStore.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettingsDataStore.DEFAULT_CONFIG
    )

    val lastMockCall: StateFlow<MockCallRecord?> = fakeRepository.lastCall

    private val _userLookupState = MutableStateFlow<UserLookupState>(UserLookupState.Idle)
    val userLookupState: StateFlow<UserLookupState> = _userLookupState.asStateFlow()

    private var lookupJob: Job? = null

    private val _uiState = MutableStateFlow<TapestryUiState>(TapestryUiState.Camera)
    val uiState: StateFlow<TapestryUiState> = _uiState.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun openSettings() {
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun saveSettings(config: AppConfig) {
        viewModelScope.launch {
            settingsDataStore.saveConfig(config)
            _showSettings.value = false
        }
    }

    fun onHandleChanged(handle: String) {
        val cleanHandle = handle.trim().removePrefix("@")
        lookupJob?.cancel()

        if (cleanHandle.length < 2) {
            _userLookupState.value = UserLookupState.Idle
            return
        }

        lookupJob = viewModelScope.launch {
            delay(400) // Debounce rapid keystrokes
            _userLookupState.value = UserLookupState.Searching
            val config = appConfig.value
            val repository: GitHubRepository = if (config.useMockRepository) fakeRepository else realRepository

            val result = repository.getUser(cleanHandle, config.githubToken.ifBlank { null })
            result.fold(
                onSuccess = { user ->
                    val cleanSlug = cleanHandle.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
                    val suggested = user.email ?: "$cleanSlug@users.noreply.github.com"
                    _userLookupState.value = UserLookupState.Found(
                        handle = cleanHandle,
                        displayName = user.name,
                        email = user.email,
                        suggestedEmail = suggested
                    )
                },
                onFailure = {
                    _userLookupState.value = UserLookupState.NotFound(cleanHandle)
                }
            )
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        _uiState.value = TapestryUiState.PreviewCapture(bitmap)
    }

    fun onRetake() {
        _userLookupState.value = UserLookupState.Idle
        lookupJob?.cancel()
        _uiState.value = TapestryUiState.Camera
    }

    fun onContinueToForm(bitmap: Bitmap) {
        _userLookupState.value = UserLookupState.Idle
        lookupJob?.cancel()
        _uiState.value = TapestryUiState.Form(bitmap = bitmap)
    }

    fun onBackToCamera() {
        _userLookupState.value = UserLookupState.Idle
        lookupJob?.cancel()
        _uiState.value = TapestryUiState.Camera
    }

    fun resetToCamera() {
        _userLookupState.value = UserLookupState.Idle
        lookupJob?.cancel()
        _uiState.value = TapestryUiState.Camera
    }

    fun retryFromError(errorState: TapestryUiState.Error) {
        submitPatch(
            bitmap = errorState.bitmap,
            handle = errorState.handle,
            emailInput = errorState.email,
            note = errorState.note,
            x = errorState.x,
            y = errorState.y
        )
    }

    fun returnToFormFromError(errorState: TapestryUiState.Error) {
        _uiState.value = TapestryUiState.Form(
            bitmap = errorState.bitmap,
            handle = errorState.handle,
            email = errorState.email,
            note = errorState.note,
            x = errorState.x,
            y = errorState.y
        )
    }

    fun submitPatch(
        bitmap: Bitmap,
        handle: String,
        emailInput: String,
        note: String,
        x: Int = 0,
        y: Int = 0
    ) {
        val config = appConfig.value
        val isMock = config.useMockRepository

        if (!isMock && config.githubToken.isBlank()) {
            _uiState.value = TapestryUiState.Error(
                message = "GitHub Personal Access Token (PAT) is not configured. Please open settings (gear icon) and paste your token, or enable Mock Mode.",
                bitmap = bitmap,
                handle = handle,
                email = emailInput,
                note = note,
                x = x,
                y = y
            )
            return
        }

        viewModelScope.launch {
            val logs = mutableListOf<String>()

            fun log(msg: String) {
                logs.add(msg)
                _uiState.value = TapestryUiState.Submitting(logs.toList())
            }

            log("> [1/4] Optimizing and cropping patch image...")
            delay(100)

            log("> [2/4] Encoding bitmap to Base64 JPEG...")
            val base64Image = withContext(Dispatchers.Default) {
                ImageUtils.bitmapToBase64Jpeg(bitmap, quality = 90)
            }
            delay(100)

            val rawHandle = handle.trim().ifEmpty { "anonymous_agent" }
            val cleanHandle = rawHandle.removePrefix("@")
            val authorEmail = emailInput.trim().ifEmpty {
                "${cleanHandle.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")}@users.noreply.github.com"
            }

            val authorIdent = CommitAuthorIdent(
                name = rawHandle,
                email = authorEmail
            )

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = dateFormat.format(Date())
            val safeHandleSlug = cleanHandle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val patchId = "patch_${timestamp}_${safeHandleSlug}"
            val folder = config.pathPrefix.trim().removePrefix("/").removeSuffix("/")
            val patchDir = if (folder.isNotBlank()) "$folder/$patchId" else patchId
            val imagePath = "$patchDir/patch.jpg"
            val metadataPath = "$patchDir/metadata.json"

            val repository: GitHubRepository = if (isMock) fakeRepository else realRepository

            // Call 1: Commit image patch.jpg
            if (isMock) {
                log("> [3/4] [DRY RUN] Uploading patch image to $imagePath...")
            } else {
                log("> [3/4] Uploading patch image to $imagePath...")
            }

            val imageCommitMsg = if (note.isNotBlank()) {
                "feat(tapestry): add patch image for $rawHandle ($patchId)"
            } else {
                "feat(tapestry): add patch image for $rawHandle"
            }

            val imageRequest = CreateFileRequest(
                message = imageCommitMsg,
                content = base64Image,
                author = authorIdent,
                committer = authorIdent,
                branch = config.branch.ifBlank { "main" }
            )

            val imageResult = repository.createFile(
                owner = config.repoOwner,
                repo = config.repoName,
                path = imagePath,
                token = config.githubToken,
                request = imageRequest
            )

            val imageResponse = imageResult.getOrElse { error ->
                _uiState.value = TapestryUiState.Error(
                    message = error.localizedMessage ?: "Failed to upload patch image to GitHub",
                    bitmap = bitmap,
                    handle = handle,
                    email = emailInput,
                    note = note,
                    x = x,
                    y = y
                )
                return@launch
            }

            val imageSha = imageResponse.commit.sha
            val shortImageSha = imageSha.take(7)
            log("> Image committed (SHA: $shortImageSha). Generating metadata @ ($x, $y)...")
            delay(200)

            // Call 2: Commit metadata.json
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val metadata = PatchMetadata(
                id = patchId,
                author = PatchAuthor(
                    handle = rawHandle,
                    name = (userLookupState.value as? UserLookupState.Found)?.displayName,
                    email = authorEmail
                ),
                coordinates = PatchCoordinates(x = x, y = y),
                note = note.trim().ifEmpty { null },
                timestamp = isoDateFormat.format(Date()),
                image = "patch.jpg",
                imageCommitSha = imageSha
            )

            val jsonString = json.encodeToString(metadata)
            val base64Metadata = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val metadataCommitMsg = if (note.isNotBlank()) {
                "feat(tapestry): $note [by $rawHandle @ ($x,$y)]"
            } else {
                "feat(tapestry): add patch metadata for $rawHandle @ ($x,$y)"
            }

            val metadataRequest = CreateFileRequest(
                message = metadataCommitMsg,
                content = base64Metadata,
                author = authorIdent,
                committer = authorIdent,
                branch = config.branch.ifBlank { "main" }
            )

            if (isMock) {
                log("> [4/4] [DRY RUN] Uploading patch metadata to $metadataPath...")
            } else {
                log("> [4/4] Uploading patch metadata to $metadataPath...")
            }

            val metadataResult = repository.createFile(
                owner = config.repoOwner,
                repo = config.repoName,
                path = metadataPath,
                token = config.githubToken,
                request = metadataRequest
            )

            metadataResult.fold(
                onSuccess = { metaResponse ->
                    val finalSha = metaResponse.commit.sha.take(7)
                    if (isMock) {
                        log("> [MOCK] Patch & metadata recorded! SHA: $finalSha")
                    } else {
                        log("> Patch & metadata committed! SHA: $finalSha")
                    }
                    delay(300)
                    _uiState.value = TapestryUiState.Success(
                        commitHash = finalSha,
                        htmlUrl = metaResponse.commit.htmlUrl ?: imageResponse.commit.htmlUrl
                    )
                },
                onFailure = { error ->
                    _uiState.value = TapestryUiState.Error(
                        message = "Image saved ($shortImageSha), but metadata failed: ${error.localizedMessage}",
                        bitmap = bitmap,
                        handle = handle,
                        email = emailInput,
                        note = note,
                        x = x,
                        y = y
                    )
                }
            )
        }
    }
}

package de.berlindroid.zethread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import de.berlindroid.zethread.ui.TapestryUiState
import de.berlindroid.zethread.ui.TapestryViewModel
import de.berlindroid.zethread.ui.screens.CameraScreen
import de.berlindroid.zethread.ui.screens.FeedbackScreen
import de.berlindroid.zethread.ui.screens.MetadataScreen
import de.berlindroid.zethread.ui.screens.SettingsScreen
import de.berlindroid.zethread.ui.theme.TerminalBackground
import de.berlindroid.zethread.ui.theme.ZeThreadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZeThreadTheme {
                ZeThreadApp()
            }
        }
    }
}

@Composable
fun ZeThreadApp(
    viewModel: TapestryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val lastMockCall by viewModel.lastMockCall.collectAsState()
    val userLookupState by viewModel.userLookupState.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasActiveProgress = uiState is TapestryUiState.PreviewCapture || uiState is TapestryUiState.Form

    // Protect progress from accidental system back navigation
    if (!showSettings && hasActiveProgress) {
        BackHandler {
            showDiscardDialog = true
        }
    } else if (uiState is TapestryUiState.Submitting) {
        BackHandler(enabled = true) {
            // Guard in-flight network dispatch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .safeDrawingPadding()
    ) {
        if (showSettings) {
            BackHandler {
                viewModel.closeSettings()
            }
            SettingsScreen(
                currentConfig = config,
                lastMockCall = lastMockCall,
                onBack = viewModel::closeSettings,
                onSave = viewModel::saveSettings
            )
        } else {
            when (val state = uiState) {
                is TapestryUiState.Camera -> {
                    CameraScreen(
                        previewBitmap = null,
                        onPhotoCaptured = viewModel::onPhotoCaptured,
                        onRetake = viewModel::onRetake,
                        onContinue = viewModel::onContinueToForm,
                        onOpenSettings = viewModel::openSettings
                    )
                }
                is TapestryUiState.PreviewCapture -> {
                    CameraScreen(
                        previewBitmap = state.bitmap,
                        onPhotoCaptured = viewModel::onPhotoCaptured,
                        onRetake = { showDiscardDialog = true },
                        onContinue = viewModel::onContinueToForm,
                        onOpenSettings = viewModel::openSettings
                    )
                }
                is TapestryUiState.Form -> {
                    MetadataScreen(
                        bitmap = state.bitmap,
                        initialHandle = state.handle,
                        initialEmail = state.email,
                        initialNote = state.note,
                        initialX = state.x,
                        initialY = state.y,
                        userLookupState = userLookupState,
                        onHandleChanged = viewModel::onHandleChanged,
                        onBack = { showDiscardDialog = true },
                        onSubmit = { handle, email, note, x, y ->
                            viewModel.submitPatch(
                                bitmap = state.bitmap,
                                handle = handle,
                                emailInput = email,
                                note = note,
                                x = x,
                                y = y
                            )
                        },
                        onOpenSettings = viewModel::openSettings
                    )
                }
                is TapestryUiState.Submitting,
                is TapestryUiState.Success -> {
                    FeedbackScreen(
                        state = state,
                        onResetToCamera = viewModel::resetToCamera,
                        onRetry = { },
                        onEditForm = { },
                        onOpenSettings = viewModel::openSettings
                    )
                }
                is TapestryUiState.Error -> {
                    FeedbackScreen(
                        state = state,
                        onResetToCamera = viewModel::resetToCamera,
                        onRetry = { viewModel.retryFromError(state) },
                        onEditForm = { viewModel.returnToFormFromError(state) },
                        onOpenSettings = viewModel::openSettings
                    )
                }
            }
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard Patch?") },
                text = { Text("If you leave now, the captured photo and any entered details will be discarded.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            viewModel.resetToCamera()
                        }
                    ) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep Editing")
                    }
                }
            )
        }
    }
}

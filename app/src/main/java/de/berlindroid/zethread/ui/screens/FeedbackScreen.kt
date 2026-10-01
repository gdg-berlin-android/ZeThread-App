package de.berlindroid.zethread.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.berlindroid.zethread.ui.TapestryUiState
import de.berlindroid.zethread.ui.theme.CodeMonospace
import de.berlindroid.zethread.ui.theme.TerminalAmber
import de.berlindroid.zethread.ui.theme.TerminalBackground
import de.berlindroid.zethread.ui.theme.TerminalCyan
import de.berlindroid.zethread.ui.theme.TerminalGreen
import de.berlindroid.zethread.ui.theme.TerminalRed
import de.berlindroid.zethread.ui.theme.TerminalSurfaceVariant
import de.berlindroid.zethread.ui.theme.TextGlowing
import de.berlindroid.zethread.ui.theme.TextMuted
import de.berlindroid.zethread.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FeedbackScreen(
    state: TapestryUiState,
    onResetToCamera: () -> Unit,
    onRetry: () -> Unit,
    onEditForm: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(24.dp)
    ) {
        when (state) {
            is TapestryUiState.Submitting -> {
                SubmittingView(logs = state.terminalLogs)
            }
            is TapestryUiState.Success -> {
                SuccessView(
                    commitHash = state.commitHash,
                    htmlUrl = state.htmlUrl,
                    onNext = onResetToCamera
                )
            }
            is TapestryUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = onRetry,
                    onEdit = onEditForm,
                    onOpenSettings = onOpenSettings
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun SubmittingView(logs: List<String>) {
    val scrollState = rememberScrollState()

    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = TerminalGreen,
            strokeWidth = 4.dp,
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Submitting to Tapestry...",
            color = TextGlowing,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            color = TerminalSurfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                logs.forEach { line ->
                    Text(
                        text = line,
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessView(
    commitHash: String,
    htmlUrl: String?,
    onNext: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000.milliseconds)
            countdown--
        }
        onNext()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TerminalGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = TerminalGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Patch Merged!",
                color = TextGlowing,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "#$commitHash",
                    color = TerminalGreen,
                    style = CodeMonospace.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
                if (htmlUrl != null) {
                    Text(
                        text = "Staged & pushed to origin/main",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Text(
                text = "Thank you for contributing to the next.app devCon tapestry!",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Resetting in ${countdown}s...",
                color = TextMuted,
                fontSize = 13.sp
            )

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    contentColor = TerminalBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Next Contributor",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TerminalRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = TerminalRed,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Submission Failed",
                color = TerminalRed,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Surface(
                color = TerminalSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Error details:",
                        color = TerminalRed,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        text = message,
                        color = TextGlowing,
                        style = CodeMonospace.copy(fontSize = 13.sp)
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    contentColor = TerminalBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Retry Submission",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TerminalCyan
                    ),
                    border = BorderStroke(1.dp, TerminalCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Edit Details",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TerminalAmber
                    ),
                    border = BorderStroke(1.dp, TerminalAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

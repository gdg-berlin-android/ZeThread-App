package de.berlindroid.zethread.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.berlindroid.zethread.data.local.AppConfig
import de.berlindroid.zethread.data.repository.MockCallRecord
import de.berlindroid.zethread.ui.theme.BorderSubtle
import de.berlindroid.zethread.ui.theme.CodeMonospace
import de.berlindroid.zethread.ui.theme.TerminalAmber
import de.berlindroid.zethread.ui.theme.TerminalBackground
import de.berlindroid.zethread.ui.theme.TerminalGreen
import de.berlindroid.zethread.ui.theme.TerminalSurfaceVariant
import de.berlindroid.zethread.ui.theme.TextGlowing
import de.berlindroid.zethread.ui.theme.TextMuted
import de.berlindroid.zethread.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: AppConfig,
    lastMockCall: MockCallRecord? = null,
    onBack: () -> Unit,
    onSave: (AppConfig) -> Unit
) {
    var owner by remember { mutableStateOf(currentConfig.repoOwner) }
    var repo by remember { mutableStateOf(currentConfig.repoName) }
    var branch by remember { mutableStateOf(currentConfig.branch) }
    var pathPrefix by remember { mutableStateOf(currentConfig.pathPrefix) }
    var token by remember { mutableStateOf(currentConfig.githubToken) }
    var isTokenVisible by remember { mutableStateOf(false) }
    var useMockRepository by remember { mutableStateOf(currentConfig.useMockRepository) }

    fun submitSave() {
        onSave(
            AppConfig(
                repoOwner = owner.trim(),
                repoName = repo.trim(),
                branch = branch.trim().ifEmpty { "main" },
                pathPrefix = pathPrefix.trim(),
                githubToken = token.trim(),
                useMockRepository = useMockRepository
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::submitSave) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Settings",
                            tint = TerminalGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Target GitHub repository and authentication for this booth check-in station.",
                color = TextSecondary,
                fontSize = 14.sp
            )

            // Dry Run / Mock Mode
            Surface(
                color = TerminalSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "Dry Run / Mock Mode",
                            color = if (useMockRepository) TerminalAmber else TextGlowing,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Simulates git commits locally without sending requests to GitHub. Ideal for booth testing without a write token.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Switch(
                        checked = useMockRepository,
                        onCheckedChange = { useMockRepository = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TerminalBackground,
                            checkedTrackColor = TerminalAmber,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Repository Configuration
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Repository Target",
                    color = TerminalGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Repository Owner") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("Repository Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Target Branch") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = pathPrefix,
                    onValueChange = { pathPrefix = it },
                    label = { Text("Path Prefix") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("patches", color = TextMuted, style = CodeMonospace) },
                    colors = fieldColors()
                )
            }

            // GitHub Authentication
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Authentication",
                    color = TerminalGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = {
                        Text(
                            if (useMockRepository) "GitHub PAT Token (Optional in Mock Mode)"
                            else "GitHub PAT Token (contents:write)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle token visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    colors = fieldColors()
                )
            }

            // Diagnostic Log (Mock Mode)
            if (useMockRepository || lastMockCall != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Last Simulated Dispatch",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = TerminalSurfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (lastMockCall != null) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = lastMockCall.formattedTime,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "201 Created",
                                        color = TerminalGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${lastMockCall.method} ${lastMockCall.endpoint}",
                                    color = TextGlowing,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Branch: ${lastMockCall.branch} • SHA: #${lastMockCall.generatedSha.take(7)}",
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Author: ${lastMockCall.authorName} <${lastMockCall.authorEmail}>",
                                    color = TextGlowing,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Message: \"${lastMockCall.message}\"",
                                    color = TerminalGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                val approxKb = (lastMockCall.base64Length * 3) / 4 / 1024
                                Text(
                                    text = "Payload: ${lastMockCall.base64Length} chars (~$approxKb KB)",
                                    color = TerminalAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No simulated dispatches recorded yet.\nSnap a photo and submit in mock mode to test.",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = ::submitSave,
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
                    text = "Save Settings",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerminalGreen,
    unfocusedBorderColor = BorderSubtle,
    focusedTextColor = TextGlowing,
    unfocusedTextColor = TextGlowing,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedLabelColor = TerminalGreen,
    unfocusedLabelColor = TextSecondary
)

package de.berlindroid.zethread.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.berlindroid.zethread.ui.UserLookupState
import de.berlindroid.zethread.ui.theme.BorderSubtle
import de.berlindroid.zethread.ui.theme.CodeMonospace
import de.berlindroid.zethread.ui.theme.TerminalBackground
import de.berlindroid.zethread.ui.theme.TerminalCyan
import de.berlindroid.zethread.ui.theme.TerminalGreen
import de.berlindroid.zethread.ui.theme.TerminalRed
import de.berlindroid.zethread.ui.theme.TextGlowing
import de.berlindroid.zethread.ui.theme.TextMuted
import de.berlindroid.zethread.ui.theme.TextPrimary
import de.berlindroid.zethread.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataScreen(
    bitmap: Bitmap,
    initialHandle: String = "",
    initialEmail: String = "",
    initialNote: String = "",
    initialX: Int = 0,
    initialY: Int = 0,
    userLookupState: UserLookupState = UserLookupState.Idle,
    onHandleChanged: (String) -> Unit = {},
    onBack: () -> Unit,
    onSubmit: (handle: String, email: String, note: String, x: Int, y: Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    var handle by remember { mutableStateOf(initialHandle) }
    var email by remember { mutableStateOf(initialEmail) }
    var note by remember { mutableStateOf(initialNote) }
    var posX by remember { mutableStateOf(initialX.toString()) }
    var posY by remember { mutableStateOf(initialY.toString()) }
    var isHandleError by remember { mutableStateOf(false) }
    var userModifiedEmailManually by remember { mutableStateOf(false) }

    LaunchedEffect(handle) {
        val cleanSlug = handle.trim().removePrefix("@").lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        if (cleanSlug.isNotBlank() && (!userModifiedEmailManually || email.isBlank())) {
            email = "$cleanSlug@users.noreply.github.com"
        }
    }

    LaunchedEffect(userLookupState) {
        if (userLookupState is UserLookupState.Found) {
            if (!userModifiedEmailManually || email.isBlank() || email.endsWith("@users.noreply.github.com")) {
                email = userLookupState.suggestedEmail
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contributor Details",
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
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
                .verticalScroll(scrollState)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Patch Preview row (clean, no boxed cards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Patch Thumbnail",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Patch Staged",
                        color = TerminalGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Ready to stamp git authorship",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Developer Handle
            OutlinedTextField(
                value = handle,
                onValueChange = {
                    val sanitized = it.removePrefix("@")
                    handle = sanitized
                    if (isHandleError && sanitized.isNotBlank()) isHandleError = false
                    onHandleChanged(sanitized)
                },
                label = { Text("Developer Handle *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 15.sp),
                shape = RoundedCornerShape(12.dp),
                prefix = {
                    Text(
                        text = "@",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                },
                placeholder = {
                    Text("username", color = TextMuted, style = CodeMonospace)
                },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TerminalGreen)
                },
                trailingIcon = {
                    when (userLookupState) {
                        is UserLookupState.Searching -> {
                            CircularProgressIndicator(
                                color = TerminalGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        is UserLookupState.Found -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified GitHub User",
                                tint = TerminalGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> Unit
                    }
                },
                isError = isHandleError,
                supportingText = {
                    if (isHandleError) {
                        Text("Developer handle is required", color = TerminalRed)
                    } else {
                        when (userLookupState) {
                            is UserLookupState.Searching -> {
                                Text("Looking up GitHub user...", color = TerminalCyan, fontSize = 12.sp)
                            }
                            is UserLookupState.Found -> {
                                val displayName = userLookupState.displayName
                                val nameText = if (!displayName.isNullOrBlank()) " ($displayName)" else ""
                                val privacyNotice = if (userLookupState.email != null) {
                                    "✓ Found GitHub user$nameText • Public email pre-filled"
                                } else {
                                    "✓ Found GitHub user$nameText • Private email, noreply pre-filled"
                                }
                                Text(privacyNotice, color = TerminalGreen, fontSize = 12.sp)
                            }
                            is UserLookupState.NotFound -> {
                                Text("Handle not verified on GitHub • Fallback email will be used", color = TextMuted, fontSize = 12.sp)
                            }
                            is UserLookupState.Idle -> {
                                Text("Used for commit author name and avatar lookup", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextGlowing,
                    unfocusedTextColor = TextGlowing,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedLabelColor = TerminalGreen,
                    unfocusedLabelColor = TextSecondary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            // GitHub Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    userModifiedEmailManually = true
                },
                label = { Text("GitHub Email (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = CodeMonospace.copy(color = TextGlowing, fontSize = 15.sp),
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text("alex@example.com", color = TextMuted, style = CodeMonospace)
                },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TerminalCyan)
                },
                supportingText = {
                    Text(
                        text = "Link this patch to your GitHub contribution graph. Leave blank to stay anonymous.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerminalCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextGlowing,
                    unfocusedTextColor = TextGlowing,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedLabelColor = TerminalCyan,
                    unfocusedLabelColor = TextSecondary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            // Grid Position (X, Y)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tapestry Grid Position",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = posX,
                        onValueChange = { posX = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Column (X)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = CodeMonospace.copy(
                            color = TextGlowing,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        ),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    val cur = posX.toIntOrNull() ?: 0
                                    if (cur > 0) posX = (cur - 1).toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Column X",
                                    tint = if ((posX.toIntOrNull() ?: 0) > 0) TerminalGreen else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val cur = posX.toIntOrNull() ?: 0
                                    posX = (cur + 1).toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Column X",
                                    tint = TerminalGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextGlowing,
                            unfocusedTextColor = TextGlowing,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedLabelColor = TerminalGreen,
                            unfocusedLabelColor = TextSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = posY,
                        onValueChange = { posY = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Row (Y)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = CodeMonospace.copy(
                            color = TextGlowing,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        ),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    val cur = posY.toIntOrNull() ?: 0
                                    if (cur > 0) posY = (cur - 1).toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Row Y",
                                    tint = if ((posY.toIntOrNull() ?: 0) > 0) TerminalGreen else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val cur = posY.toIntOrNull() ?: 0
                                    posY = (cur + 1).toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Row Y",
                                    tint = TerminalGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextGlowing,
                            unfocusedTextColor = TextGlowing,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedLabelColor = TerminalGreen,
                            unfocusedLabelColor = TextSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }

            // Patch Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Patch Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text("e.g. Stitched during Jetpack Compose Keynote", color = TextMuted)
                },
                leadingIcon = {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = TerminalGreen)
                },
                supportingText = {
                    Text("Appended to the commit message", color = TextMuted, fontSize = 12.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextGlowing,
                    unfocusedTextColor = TextGlowing,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedLabelColor = TerminalGreen,
                    unfocusedLabelColor = TextSecondary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(Modifier.height(4.dp))

            // Primary Action: Merge into Tapestry
            Button(
                onClick = {
                    if (handle.trim().isBlank()) {
                        isHandleError = true
                    } else {
                        focusManager.clearFocus()
                        val parsedX = posX.toIntOrNull() ?: 0
                        val parsedY = posY.toIntOrNull() ?: 0
                        onSubmit(handle.trim(), email.trim(), note.trim(), parsedX, parsedY)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    contentColor = TerminalBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Merge into Tapestry",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

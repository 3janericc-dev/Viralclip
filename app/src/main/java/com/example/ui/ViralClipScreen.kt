package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessingState
import com.example.ui.components.ErrorAlertDialog
import com.example.ui.components.StatusFeedbackCard
import com.example.ui.components.VerticalExoPlayerView
import com.example.ui.components.ViralAnalysisCard
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBackgroundElevated
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryElectric
import com.example.ui.theme.PrimaryElectricBright
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.TertiaryCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViralClipScreen(
    viewModel: ViralClipViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val youtubeUrl by viewModel.youtubeUrl.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val isApiKeyVisible by viewModel.isApiKeyVisible.collectAsState()
    val processingState by viewModel.processingState.collectAsState()
    val renderProgress by viewModel.renderProgress.collectAsState()
    val renderSubtext by viewModel.renderSubtext.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val errorDialogState by viewModel.errorDialogState.collectAsState()

    // Handle toast/snackbar updates
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToastMessage()
        }
    }

    // Explicit error dialog
    errorDialogState?.let { errState ->
        ErrorAlertDialog(
            errorState = errState,
            onDismiss = { viewModel.dismissErrorDialog() },
            onRetry = {
                viewModel.dismissErrorDialog()
                viewModel.startAutoClipPipeline()
            }
        )
    }

    val isBusy = processingState is ProcessingState.FetchingMetadata ||
            processingState is ProcessingState.AnalyzingViralMoments ||
            processingState is ProcessingState.RenderingVideo

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(TertiaryCoral, PrimaryElectric)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ViralClip",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "AI YouTube to 9:16 Shorts",
                                style = MaterialTheme.typography.labelSmall.copy(color = SecondaryCyan)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    if (processingState is ProcessingState.Ready) {
                        IconButton(
                            onClick = { viewModel.resetToInput() },
                            modifier = Modifier.testTag("reset_to_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Clip Another",
                                tint = SecondaryCyan
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Adaptive width wrapper
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ==================== 1. INPUT VIEW ====================
                if (processingState !is ProcessingState.Ready) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_view_card"),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "YouTube Video to Vertical Short",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Paste any YouTube link. Gemini extracts the highest-retention moment into a 9:16 short.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                            )

                            // YouTube URL Input Field
                            Text(
                                text = "YouTube Video Link",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = youtubeUrl,
                                onValueChange = { viewModel.onYoutubeUrlChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("youtube_url_input"),
                                placeholder = {
                                    Text(
                                        "https://www.youtube.com/watch?v=...",
                                        color = TextTertiary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "URL Link",
                                        tint = if (youtubeUrl.isNotBlank()) PrimaryElectricBright else TextTertiary
                                    )
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (youtubeUrl.isNotBlank()) {
                                            IconButton(
                                                onClick = { viewModel.onYoutubeUrlChanged("") },
                                                modifier = Modifier.testTag("clear_url_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear URL",
                                                    tint = TextTertiary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    viewModel.onYoutubeUrlChanged(clip)
                                                }
                                            },
                                            modifier = Modifier.testTag("paste_url_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = SecondaryCyan
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryElectricBright,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next
                                )
                            )

                            // Quick Sample Links Chips
                            Text(
                                text = "Quick Test Samples:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SampleChip("Tech Keynote", "https://www.youtube.com/watch?v=kcnwnc2Fh60") {
                                    viewModel.setSampleUrl(it)
                                }
                                SampleChip("MrBeast Challenge", "https://www.youtube.com/watch?v=0e3GPea1Tyg") {
                                    viewModel.setSampleUrl(it)
                                }
                                SampleChip("Veritasium Physics", "https://www.youtube.com/watch?v=bHIhgxav9LY") {
                                    viewModel.setSampleUrl(it)
                                }
                                SampleChip("Huberman Lab", "https://www.youtube.com/watch?v=gX7bT1P0a74") {
                                    viewModel.setSampleUrl(it)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Gemini API Key Input Field
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Gemini API Key",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Free at aistudio.google.com",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SecondaryCyan
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = geminiApiKey,
                                onValueChange = { viewModel.onGeminiApiKeyChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_api_key_input"),
                                placeholder = {
                                    Text(
                                        "AIzaSy...",
                                        color = TextTertiary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "API Key",
                                        tint = if (geminiApiKey.isNotBlank()) SecondaryCyan else TextTertiary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.toggleApiKeyVisibility() },
                                        modifier = Modifier.testTag("toggle_api_key_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isApiKeyVisible) "Hide Key" else "Show Key",
                                            tint = TextTertiary
                                        )
                                    }
                                },
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    viewModel.startAutoClipPipeline()
                                })
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // "Auto-Clip Short" Primary Button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.startAutoClipPipeline()
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("auto_clip_short_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryElectric,
                                    disabledContainerColor = PrimaryElectric.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Processing Video...",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Auto-Clip Short",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== 2. PROGRESS & STATUS FEEDBACK CARD ====================
                if (isBusy) {
                    Spacer(modifier = Modifier.height(20.dp))
                    StatusFeedbackCard(
                        state = processingState,
                        renderSubtext = renderSubtext,
                        renderPercentage = renderProgress
                    )
                }

                // ==================== 3. VIDEO PREVIEW & EXPORT VIEW ====================
                // (Shown only when valid MP4 URL is ready)
                if (processingState is ProcessingState.Ready) {
                    val clippedShort = (processingState as ProcessingState.Ready).clippedShort

                    Spacer(modifier = Modifier.height(8.dp))

                    // 9:16 Vertical ExoPlayer
                    VerticalExoPlayerView(
                        videoUrl = clippedShort.videoUrl,
                        title = clippedShort.viralMoment.title,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 380.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Viral Analysis Metrics & Reasoning Card
                    ViralAnalysisCard(viralMoment = clippedShort.viralMoment)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Download progress indicator
                    if (isDownloading) {
                        Surface(
                            color = CardBackgroundElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = SecondaryCyan,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Downloading & Preparing Short...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                    if (downloadProgress > 0) {
                                        Text(
                                            text = "$downloadProgress% completed",
                                            style = MaterialTheme.typography.labelSmall.copy(color = SecondaryCyan)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Button 1: "Save to Device" (MediaStore API -> Movies/ViralShorts)
                    Button(
                        onClick = { viewModel.saveToDevice(clippedShort) },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_to_device_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBackgroundElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Save to Device (Movies/ViralShorts)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 2: "Post on YouTube Shorts" (targeted ACTION_SEND intent)
                    Button(
                        onClick = { viewModel.postOnYouTubeShorts(clippedShort) },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("post_youtube_shorts_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TertiaryCoral
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Post on YouTube Shorts",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 3: "Post on TikTok" (targeted ACTION_SEND intent)
                    Button(
                        onClick = { viewModel.postOnTikTok(clippedShort) },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("post_tiktok_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF000000)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25F4EE)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF25F4EE),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Post on TikTok",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Clip Another Video" secondary button
                    OutlinedButton(
                        onClick = { viewModel.resetToInput() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clip_another_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clip Another Video",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleChip(
    label: String,
    url: String,
    onClick: (String) -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .clickable { onClick(url) }
            .testTag("sample_chip_${label.replace(' ', '_').lowercase()}")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                color = PrimaryElectricBright
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

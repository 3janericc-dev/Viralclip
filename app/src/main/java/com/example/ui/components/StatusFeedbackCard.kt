package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProcessingState
import com.example.ui.theme.CardBackgroundElevated
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.PrimaryElectricBright
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TertiaryCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusFeedbackCard(
    state: ProcessingState,
    renderSubtext: String = "",
    renderPercentage: Int = 0,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_feedback_card"),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundElevated),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "AI Shorts Pipeline",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Real-time automated clipping in progress",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                CircularProgressIndicator(
                    color = SecondaryCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indeterminate linear progress bar
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("pipeline_linear_progress"),
                color = PrimaryElectricBright,
                trackColor = DarkBorder
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stage 1: Fetching YouTube Metadata & Transcript
            val stage1Active = state is ProcessingState.FetchingMetadata
            val stage1Done = state is ProcessingState.AnalyzingViralMoments ||
                    state is ProcessingState.RenderingVideo ||
                    state is ProcessingState.Ready

            StageRow(
                stageNumber = 1,
                title = "Stage 1",
                label = "Fetching YouTube Metadata & Transcript...",
                subtext = if (stage1Active) "Parsing timestamps, captions & stream metadata" else null,
                icon = Icons.Default.Subtitles,
                isActive = stage1Active,
                isDone = stage1Done,
                activeAlpha = alphaAnim
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stage 2: Gemini AI identifying viral short moments...
            val stage2Active = state is ProcessingState.AnalyzingViralMoments
            val stage2Done = state is ProcessingState.RenderingVideo || state is ProcessingState.Ready

            StageRow(
                stageNumber = 2,
                title = "Stage 2",
                label = "Gemini AI identifying viral short moments...",
                subtext = if (stage2Active) {
                    (state as? ProcessingState.AnalyzingViralMoments)?.videoTitle ?: "Analyzing retention curves & hook scores"
                } else null,
                icon = Icons.Default.AutoAwesome,
                isActive = stage2Active,
                isDone = stage2Done,
                activeAlpha = alphaAnim
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stage 3: Processing & rendering 9:16 vertical short...
            val stage3Active = state is ProcessingState.RenderingVideo
            val stage3Done = state is ProcessingState.Ready

            StageRow(
                stageNumber = 3,
                title = "Stage 3",
                label = "Processing & rendering 9:16 vertical short...",
                subtext = if (stage3Active) {
                    if (renderSubtext.isNotBlank()) "$renderSubtext ($renderPercentage%)"
                    else "Formatting 9:16 crop & burning dynamic captions"
                } else null,
                icon = Icons.Default.MovieCreation,
                isActive = stage3Active,
                isDone = stage3Done,
                activeAlpha = alphaAnim
            )
        }
    }
}

@Composable
private fun StageRow(
    stageNumber: Int,
    title: String,
    label: String,
    subtext: String?,
    icon: ImageVector,
    isActive: Boolean,
    isDone: Boolean,
    activeAlpha: Float
) {
    val iconColor = when {
        isDone -> SuccessGreen
        isActive -> PrimaryElectricBright
        else -> TextSecondary.copy(alpha = 0.4f)
    }

    val textColor = when {
        isDone -> TextPrimary
        isActive -> TextPrimary
        else -> TextSecondary.copy(alpha = 0.5f)
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> SuccessGreen.copy(alpha = 0.2f)
                        isActive -> PrimaryElectricBright.copy(alpha = 0.2f * activeAlpha)
                        else -> Color.White.copy(alpha = 0.05f)
                    }
                )
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = iconColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            )

            AnimatedVisibility(visible = subtext != null) {
                subtext?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isActive) SecondaryCyan else TextSecondary
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

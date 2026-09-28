package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProcessingState
import com.example.ui.theme.CardBackgroundElevated
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ErrorAlertDialog(
    errorState: ProcessingState.Error,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val stageTitle = when (errorState.stage) {
        ProcessingState.Stage.FETCH_METADATA -> "Stage 1 Failed (YouTube Fetch)"
        ProcessingState.Stage.AI_ANALYSIS -> "Stage 2 Failed (Gemini AI Analysis)"
        ProcessingState.Stage.RENDERING -> "Stage 3 Failed (9:16 Video Rendering)"
        ProcessingState.Stage.DOWNLOAD -> "Download Error"
        ProcessingState.Stage.SHARE -> "Sharing Error"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("error_alert_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = CardBackgroundElevated,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = stageTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = errorState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                if (!errorState.details.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorState.details,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                modifier = Modifier.testTag("error_retry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Process")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("error_dismiss_button")
            ) {
                Text("Dismiss", color = TextPrimary)
            }
        }
    )
}

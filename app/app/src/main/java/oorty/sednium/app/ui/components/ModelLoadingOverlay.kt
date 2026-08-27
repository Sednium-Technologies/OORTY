package oorty.sednium.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import oorty.sednium.app.ui.theme.AnimatedCheckmark
import oorty.sednium.app.ui.theme.ModelLoadingProgress
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.PulsingText
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SpinningIcon

@Composable
fun ModelLoadingOverlay(
    modelName: String,
    progress: Float? = null,
    isSuccess: Boolean = false,
    errorMessage: String? = null,
    onSuccessComplete: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(1500)
            onSuccessComplete()
        }
    }

    Dialog(
        onDismissRequest = {
            if (errorMessage != null) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = errorMessage != null,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, OrangeAlpha.a20, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        errorMessage != null -> {
                            // Error State
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SedniumColors.Red100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = SedniumColors.Red500,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Model Failed to Load",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SedniumColors.Red500
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SedniumColors.Orange)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Orange)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        isSuccess -> {
                            // Success State
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SedniumColors.Green500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedCheckmark(
                                    modifier = Modifier.size(44.dp),
                                    color = SedniumColors.Green500,
                                    strokeWidth = 4.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Model Ready!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SedniumColors.Green500
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Loaded $modelName successfully into native memory.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            // Loading State
                            Box(
                                modifier = Modifier.size(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ModelLoadingProgress(
                                    modifier = Modifier.size(86.dp),
                                    progress = progress,
                                    strokeWidth = 4.5.dp,
                                    color = SedniumColors.Orange
                                )
                                SpinningIcon(
                                    icon = Icons.Filled.Refresh,
                                    tint = SedniumColors.Orange,
                                    size = 32.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            PulsingText(
                                text = "Loading Local Model...",
                                color = SedniumColors.Orange,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = modelName.substringAfterLast("/").substringAfterLast("\\"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Instructional Notice
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OrangeAlpha.a05)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Please wait while weights are mapped to RAM.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = OrangeAlpha.a70,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Do not close the app.",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = SedniumColors.Orange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

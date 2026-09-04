package oorty.sednium.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oorty.sednium.app.navigation.LocalServerStatus
import oorty.sednium.app.ui.theme.LocalSedniumIsDark
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors

/**
 * Editorial top navigation bar with full dark mode support.
 */
@Composable
fun SedniumTopBar(
    title: String,
    subtitle: String,
    localServerStatus: LocalServerStatus? = null,
    showClear: Boolean,
    showExport: Boolean = true,
    onMenuClick: () -> Unit,
    onExportClick: () -> Unit = {},
    onClearClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSessionConfigClick: () -> Unit = {}
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val topBarBg = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.96f) else SedniumColors.Milk.copy(alpha = 0.94f)
    val subtitleColor = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBg)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Chats", tint = accentColor)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSessionConfigClick)
            ) {
                Text(
                    text = title.ifBlank { "Oorty AI" },
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (localServerStatus != null && localServerStatus != LocalServerStatus.UNKNOWN) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val statusColor = when (localServerStatus) {
                            LocalServerStatus.IDLE -> Color(0xFF4CAF50)
                            LocalServerStatus.PROCESSING -> Color(0xFFFFC107)
                            LocalServerStatus.OFFLINE -> Color(0xFFF44336)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                }
            }
        }

        if (showExport) {
            IconButton(onClick = onExportClick) {
                Icon(Icons.Filled.Share, contentDescription = "Export chat", tint = accentColor)
            }
        }
        
        val showClearConfirmDialog = remember { mutableStateOf(false) }

        if (showClearConfirmDialog.value) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog.value = false },
                shape = RoundedCornerShape(12.dp),
                title = { Text("Clear Chat", color = accentColor) },
                text = { Text("Are you sure you want to clear all messages in this chat?", color = if (isDark) SedniumColors.Gray200 else SedniumColors.Gray800) },
                confirmButton = {
                    TextButton(onClick = {
                        onClearClick()
                        showClearConfirmDialog.value = false
                    }) {
                        Text("Clear", color = SedniumColors.Red600)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog.value = false }) {
                        Text("Cancel", color = accentColor)
                    }
                },
                containerColor = if (isDark) Color(0xFF262626) else SedniumColors.Milk
            )
        }

        if (showClear) {
            IconButton(onClick = { showClearConfirmDialog.value = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear chat", tint = accentColor)
            }
        }
        


        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = accentColor)
        }
    }
}

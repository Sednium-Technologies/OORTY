package oorty.sednium.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import oorty.sednium.app.model.Attachment
import oorty.sednium.app.model.AttachmentType
import oorty.sednium.app.model.SavedModelPreset
import oorty.sednium.app.ui.theme.LocalSedniumIsDark
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedRedAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii
import oorty.sednium.app.ui.theme.SpinningIcon

/**
 * Editorial input composer with responsive dark theme support.
 * Empty state features a high-visibility voice mic trigger matching
 * the active send button styling; typing morphs the action to message send.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageComposer(
    input: String,
    onInputChange: (String) -> Unit,
    attachments: List<Attachment>,
    onRemoveAttachment: (Int) -> Unit,
    isLoading: Boolean,
    isPresetMenuOpen: Boolean,
    onTogglePresetMenu: () -> Unit,
    presets: List<SavedModelPreset>,
    activePresetId: String?,
    onSelectPreset: (SavedModelPreset) -> Unit,
    onAttachClick: () -> Unit,
    isListening: Boolean = false,
    onVoiceClick: () -> Unit = {},
    onSend: () -> Unit
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val containerBg = if (isDark) Color(0xFF262626) else SedniumColors.Milk
    val containerBorder = if (isDark) SedniumColors.Gray700 else SedRedAlpha.a30
    val textColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
    val iconTint = if (isDark) SedniumColors.Gray300 else SedniumColors.Orange

    val canSend = (input.isNotBlank() || attachments.isNotEmpty()) && !isLoading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg, RoundedCornerShape(24.dp))
            .border(1.dp, containerBorder, RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        // --- Attachment chips ---
        AnimatedVisibility(visible = attachments.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                attachments.forEachIndexed { idx, att ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(SedniumRadii.sm))
                            .background(if (isDark) SedniumColors.Gray800 else SedRedAlpha.a10)
                            .border(1.dp, if (isDark) SedniumColors.Gray700 else SedRedAlpha.a20, RoundedCornerShape(SedniumRadii.sm))
                            .padding(start = 8.dp, end = 28.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            if (att.type == AttachmentType.IMAGE) "IMG" else att.name.substringAfterLast('.', "TXT").uppercase().take(4),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accentColor
                        )
                        Text(att.name, style = MaterialTheme.typography.bodySmall, color = textColor, maxLines = 1)
                        IconButton(onClick = { onRemoveAttachment(idx) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = iconTint.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onAttachClick, enabled = !isLoading) {
                Icon(Icons.Filled.AttachFile, contentDescription = "Attach", tint = iconTint.copy(alpha = 0.8f))
            }

            Box {
                IconButton(onClick = onTogglePresetMenu, enabled = !isLoading) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "Presets",
                        tint = if (isPresetMenuOpen) accentColor else iconTint.copy(alpha = 0.8f)
                    )
                }
                if (isPresetMenuOpen) {
                    Popup(
                        alignment = Alignment.BottomStart,
                        offset = IntOffset(0, -60),
                        onDismissRequest = onTogglePresetMenu
                    ) {
                        PresetMenu(
                            presets = presets,
                            activePresetId = activePresetId,
                            isDark = isDark,
                            accentColor = accentColor,
                            onSelect = onSelectPreset,
                            onClose = onTogglePresetMenu
                        )
                    }
                }
            }

            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 150.dp)
                    .padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
                enabled = !isLoading,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isLoading) textColor.copy(alpha = 0.5f) else textColor
                ),
                cursorBrush = SolidColor(accentColor),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (input.isEmpty()) {
                            Text(
                                if (isListening) "Listening…" else "Message Oorty…",
                                color = if (isDark) SedniumColors.Gray400 else accentColor.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Dynamic Action Button: When empty, acts as bright Voice Mic button; when typed, acts as Send button
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    SpinningIcon(icon = Icons.Filled.Refresh, tint = SedniumColors.Milk)
                } else if (canSend) {
                    IconButton(onClick = onSend) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = SedniumColors.Milk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onVoiceClick) {
                        Icon(
                            if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isListening) "Stop dictation" else "Voice input",
                            tint = SedniumColors.Milk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetMenu(
    presets: List<SavedModelPreset>,
    activePresetId: String?,
    isDark: Boolean,
    accentColor: Color,
    onSelect: (SavedModelPreset) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(SedniumRadii.lg))
            .background(if (isDark) Color(0xFF262626) else SedniumColors.Milk)
            .border(1.dp, if (isDark) SedniumColors.Gray700 else SedRedAlpha.a20, RoundedCornerShape(SedniumRadii.lg))
            .heightIn(max = 240.dp)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SAVED CONFIGURATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) SedniumColors.Gray400 else SedRedAlpha.a70
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = if (isDark) SedniumColors.Gray400 else SedRedAlpha.a70,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        if (presets.isEmpty()) {
            Text(
                "No saved configurations. Save presets from Settings > Behavior.",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) SedniumColors.Gray400 else SedRedAlpha.a70,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            presets.forEach { preset ->
                val isActive = preset.id == activePresetId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SedniumRadii.sm))
                        .background(if (isActive) (if (isDark) SedniumColors.Gray800 else SedRedAlpha.a10) else Color.Transparent)
                        .clickable { onSelect(preset) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(preset.name, color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(preset.model, color = if (isDark) SedniumColors.Gray400 else SedRedAlpha.a60, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        preset.chatMode.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = SedniumColors.Milk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

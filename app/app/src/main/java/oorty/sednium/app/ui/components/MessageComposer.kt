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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedRedAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii
import oorty.sednium.app.ui.theme.SpinningIcon

/**
 * Editorial pill-shaped input composer with Lucide outline icons.
 * Left to right: Plus (attach) -> Text input -> Mic (live STT) -> Live Mode -> Up-Arrow Send button.
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
    onLiveModeClick: () -> Unit = {},
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
            .background(containerBg, RoundedCornerShape(SedniumRadii.pill))
            .border(1.dp, containerBorder, RoundedCornerShape(SedniumRadii.pill))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // --- Attachment chips ---
        AnimatedVisibility(visible = attachments.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                attachments.forEachIndexed { idx, att ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(SedniumRadii.sm))
                            .background(if (isDark) SedniumColors.Gray800 else SedRedAlpha.a10)
                            .border(1.dp, if (isDark) SedniumColors.Gray700 else SedRedAlpha.a20, RoundedCornerShape(SedniumRadii.sm))
                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            if (att.type == AttachmentType.IMAGE) "IMG" else att.name.substringAfterLast('.', "TXT").uppercase().take(4),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accentColor
                        )
                        Text(att.name, style = MaterialTheme.typography.bodySmall, color = textColor, maxLines = 1)
                        IconButton(onClick = { onRemoveAttachment(idx) }, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = OortyIcons.Close,
                                contentDescription = "Remove",
                                tint = iconTint.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            // 1. Plus Icon (replaces paperclip)
            IconButton(
                onClick = onAttachClick,
                enabled = !isLoading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = OortyIcons.Plus,
                    contentDescription = "Attach / Tools",
                    tint = iconTint.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Presets Shortcut
            Box {
                IconButton(
                    onClick = onTogglePresetMenu,
                    enabled = !isLoading,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = OortyIcons.Bookmark,
                        contentDescription = "Presets",
                        tint = if (isPresetMenuOpen) accentColor else iconTint.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
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

            // 2. Expandable Text Input Field
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 150.dp)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
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

            // 3. Mic Icon (Speech-to-Text)
            IconButton(
                onClick = onVoiceClick,
                enabled = !isLoading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isListening) OortyIcons.MicOff else OortyIcons.Mic,
                    contentDescription = if (isListening) "Stop dictation" else "Voice input",
                    tint = if (isListening) accentColor else iconTint.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // 4. Morphing Action Button: Send Up-Arrow when text is present, Live Mode Waveform when empty
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable(
                        enabled = !isLoading,
                        onClick = {
                            if (canSend) onSend() else onLiveModeClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    SpinningIcon(icon = OortyIcons.Refresh, tint = SedniumColors.Milk, modifier = Modifier.size(18.dp))
                } else if (canSend) {
                    Icon(
                        imageVector = OortyIcons.Send,
                        contentDescription = "Send",
                        tint = SedniumColors.Milk,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = OortyIcons.Waveform,
                        contentDescription = "Live Flow Mode",
                        tint = SedniumColors.Milk,
                        modifier = Modifier.size(18.dp)
                    )
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
            .widthIn(min = 280.dp, max = 340.dp)
            .clip(RoundedCornerShape(SedniumRadii.lg))
            .background(if (isDark) Color(0xFF262626) else SedniumColors.Milk)
            .border(1.dp, if (isDark) SedniumColors.Gray700 else SedRedAlpha.a20, RoundedCornerShape(SedniumRadii.lg))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SAVED CONFIGURATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) SedniumColors.Gray400 else SedRedAlpha.a70,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = OortyIcons.Close,
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
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { preset ->
                    val isActive = preset.id == activePresetId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SedniumRadii.sm))
                            .background(if (isActive) (if (isDark) SedniumColors.Gray800 else SedRedAlpha.a10) else Color.Transparent)
                            .clickable { onSelect(preset) }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

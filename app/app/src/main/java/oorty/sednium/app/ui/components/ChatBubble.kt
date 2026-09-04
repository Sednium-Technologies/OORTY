package oorty.sednium.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import oorty.sednium.app.markdown.MarkdownView
import oorty.sednium.app.model.Attachment
import oorty.sednium.app.model.AttachmentType
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.GenerativeMediaType
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.model.Role
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii
import oorty.sednium.app.ui.theme.ThinkingDots

/**
 * Modern chat bubble layout:
 * - User messages: 4 equally rounded corners, subtle tint, long-press popup (Edit & Copy).
 * - Assistant messages: Transparent background (no bubble), left-aligned full width,
 *   clean thinking toggle, multimodal output views, and post-response action bar.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    msg: ChatMessage,
    providerName: String,
    isDark: Boolean,
    isGenerating: Boolean,
    showPerformanceStats: Boolean = true,
    isSpeaking: Boolean = false,
    onSpeak: (() -> Unit)? = null,
    onImageClick: (String) -> Unit,
    onRetry: (() -> Unit)? = null,
    onEditUserMessage: ((ChatMessage) -> Unit)? = null,
    onBranchChat: ((ChatMessage) -> Unit)? = null,
    onSendToModel: ((ChatMessage, ModelProvider, String) -> Unit)? = null
) {
    val isModel = msg.role == Role.MODEL
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange

    var thoughtExpanded by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }

    if (!isModel) {
        // --- USER MESSAGE: Pinned to the right with rounded capsule ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 40.dp, max = 310.dp)
                    .clip(RoundedCornerShape(SedniumRadii.lg))
                    .background(if (isDark) Color(0xFF261D19) else OrangeAlpha.a10)
                    .border(1.dp, if (isDark) Color(0xFF4A3226) else OrangeAlpha.a30, RoundedCornerShape(SedniumRadii.lg))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showUserMenu = true }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // User Message Context Popup (Milkish background with subtle shadow)
                DropdownMenu(
                    expanded = showUserMenu,
                    onDismissRequest = { showUserMenu = false },
                    modifier = Modifier
                        .background(if (isDark) SedniumColors.Charcoal800 else SedniumColors.Milk, RoundedCornerShape(14.dp))
                        .border(1.dp, if (isDark) SedniumColors.Charcoal600 else OrangeAlpha.a20, RoundedCornerShape(14.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy", color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(OortyIcons.Copy, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            showUserMenu = false
                            clipboardManager.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    if (onEditUserMessage != null) {
                        DropdownMenuItem(
                            text = { Text("Edit & Regenerate", color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange, fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(OortyIcons.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                showUserMenu = false
                                onEditUserMessage(msg)
                            }
                        )
                    }
                }

                Column {
                    if (msg.attachments.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            msg.attachments.forEach { att -> AttachmentPreview(att, isDark, onImageClick) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    } else {
        // --- ASSISTANT MESSAGE: Full-width clean canvas starting from left screen edge ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)
        ) {
            // Assistant Header (Inline mini Oorty badge + Provider badge + performance stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(SedniumRadii.squircle))
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = oorty.sednium.app.R.drawable.logo),
                            contentDescription = "Oorty",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = providerName.ifBlank { msg.modelName ?: "Oorty AI" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                if (showPerformanceStats && msg.tokensPerSecond != null && msg.tokensPerSecond > 0f) {
                    Text(
                        text = "%.1f tok/s • %dms".format(msg.tokensPerSecond, msg.latencyMs ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Thinking / Chain of Thought Block (Compact Tablet / Pill Toggle)
            if (msg.thought != null && msg.thought.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(SedniumRadii.pill))
                        .background(if (isDark) SedniumColors.Charcoal800 else OrangeAlpha.a10)
                        .border(1.dp, if (isDark) SedniumColors.Charcoal700 else OrangeAlpha.a20, RoundedCornerShape(SedniumRadii.pill))
                        .clickable { thoughtExpanded = !thoughtExpanded }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Thought Process",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70
                            )
                            if (msg.isThinking) {
                                ThinkingDots(dotColor = accentColor)
                            }
                            Icon(
                                imageVector = if (thoughtExpanded) OortyIcons.ChevronUp else OortyIcons.ChevronDown,
                                contentDescription = null,
                                tint = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (thoughtExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.thought,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) SedniumColors.Gray400 else SedniumColors.Gray700,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Main Message Content (Markdown for AI)
            if (msg.content.isNotBlank() || isGenerating) {
                MarkdownView(
                    content = msg.content,
                    isDark = isDark
                )
            }

            // Generative Multimodal Output Views
            if (msg.mediaResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                when (msg.mediaResult.type) {
                    GenerativeMediaType.IMAGE -> ImageResultView(
                        result = msg.mediaResult,
                        isDark = isDark,
                        onImageClick = onImageClick
                    )
                    GenerativeMediaType.AUDIO -> AudioResultView(
                        result = msg.mediaResult,
                        isDark = isDark
                    )
                    GenerativeMediaType.VIDEO -> VideoResultView(
                        result = msg.mediaResult,
                        isDark = isDark
                    )
                }
            }

            // User / Model Attachments
            if (msg.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    msg.attachments.forEach { att -> AttachmentPreview(att, isDark, onImageClick) }
                }
            }

            // Post-Response Action Bar for completed assistant messages
            if (!isGenerating && msg.content.isNotBlank()) {
                MessageActionBar(
                    isDark = isDark,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(msg.content))
                        Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, msg.content)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share response via"))
                    },
                    onOpenMore = {
                        showActionSheet = true
                    }
                )
            }
        }
    }

    // Modal Action Sheet for Assistant Message
    if (showActionSheet) {
        MessageActionSheet(
            message = msg,
            isDark = isDark,
            onDismiss = { showActionSheet = false },
            onBranchChat = {
                onBranchChat?.invoke(msg)
            },
            onSendToModel = { prov, modelId ->
                onSendToModel?.invoke(msg, prov, modelId)
            },
            onReadAloud = {
                onSpeak?.invoke()
            },
            onRetry = {
                onRetry?.invoke()
            }
        )
    }
}

@Composable
private fun AttachmentPreview(att: Attachment, isDark: Boolean, onImageClick: (String) -> Unit) {
    if (att.type == AttachmentType.IMAGE) {
        val uri = if (att.data.startsWith("content://") || att.data.startsWith("http")) att.data else "data:${att.mimeType};base64,${att.data}"
        AsyncImage(
            model = uri,
            contentDescription = att.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(SedniumRadii.sm))
                .border(1.dp, if (isDark) SedniumColors.Gray800 else SedniumColors.Gray200, RoundedCornerShape(SedniumRadii.sm))
                .clickable { onImageClick(uri) }
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(SedniumRadii.sm))
                .background(if (isDark) SedniumColors.DarkSurfaceAlt else SedniumColors.White)
                .border(1.dp, if (isDark) SedniumColors.Gray700 else SedniumColors.Gray200, RoundedCornerShape(SedniumRadii.sm))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                att.name.substringAfterLast('.', "FILE").uppercase().take(4),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SedniumColors.Blue600
            )
            Text(att.name, style = MaterialTheme.typography.bodySmall, color = if (isDark) SedniumColors.Gray300 else SedniumColors.Gray700)
        }
    }
}

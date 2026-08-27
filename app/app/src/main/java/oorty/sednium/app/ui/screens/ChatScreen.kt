package oorty.sednium.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.Attachment
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.ChatMode
import oorty.sednium.app.model.PROVIDER_CONFIG
import oorty.sednium.app.model.Role
import oorty.sednium.app.model.SavedModelPreset
import oorty.sednium.app.model.ToolCallState
import oorty.sednium.app.ui.components.ChatBubble
import oorty.sednium.app.ui.components.MessageComposer
import oorty.sednium.app.ui.components.SedniumTopBar
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedRedAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import kotlinx.coroutines.launch

import oorty.sednium.app.navigation.LocalServerStatus
import oorty.sednium.app.voice.rememberVoiceInputController

@Composable
fun ToolActivityView(toolCalls: List<ToolCallState>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = SedniumRadii.md, bottomEnd = SedniumRadii.md))
            .background(SedniumColors.Gray900) // very dark header
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Filled.SmartToy, contentDescription = null, tint = SedniumColors.Orange, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AGENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Orange, fontWeight = FontWeight.Bold)
        }
        toolCalls.forEach { tool ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$ ", color = SedniumColors.Green500, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text(tool.command, color = SedniumColors.Gray300, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                if (tool.isExecuting) {
                    Text("[\\]", color = SedniumColors.Milk, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                } else if (tool.success) {
                    Box(modifier = Modifier.background(SedniumColors.Green500, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("SUCCESS", color = SedniumColors.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(modifier = Modifier.background(SedniumColors.Red600, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("FAILED", color = SedniumColors.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * PAGE 1 / 4 — Chat Screen.
 * Direct port of App.tsx's top-level layout: header, scrollable message
 * list (or empty state), composer, and footer disclaimer caption.
 */
@Composable
fun ChatScreen(
    chatTitle: String,
    settings: AppSettings,
    localServerStatus: LocalServerStatus = LocalServerStatus.UNKNOWN,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isConfigValid: Boolean,
    input: String,
    attachments: List<Attachment>,
    isPresetMenuOpen: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onTogglePresetMenu: () -> Unit,
    onSelectPreset: (SavedModelPreset) -> Unit,
    onMenuClick: () -> Unit,
    onExportClick: () -> Unit,
    onClearClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSessionConfigClick: () -> Unit = {},
    onImageClick: (String) -> Unit
) {
    val providerName = PROVIDER_CONFIG[settings.provider]?.displayName ?: "Unknown"
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var wasLoading by remember { mutableStateOf(isLoading) }

    // Partial results are kept separate from `input` and shown as a dimmed
    // live preview instead of being written into the actual text field —
    // partial guesses get revised mid-sentence by SpeechRecognizer, and
    // committing each revision straight into `input` would make the field
    // visibly jump around while the user is still talking.
    var partialTranscript by remember { mutableStateOf("") }

    val voiceController = rememberVoiceInputController(
        onPartialResult = { partial -> partialTranscript = partial },
        onFinalResult = { transcript ->
            onInputChange(if (input.isBlank()) transcript else "$input $transcript")
            partialTranscript = ""
        },
        onError = { message ->
            partialTranscript = ""
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    )
    
    LaunchedEffect(voiceController.isListening) {
        if (!voiceController.isListening) partialTranscript = ""
    }

    // Haptic feedback when generation completes
    LaunchedEffect(isLoading) {
        if (wasLoading && !isLoading && messages.isNotEmpty() && messages.last().role == Role.MODEL) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
        wasLoading = isLoading
    }
    
    // Auto-scroll logic
    val lastMessage = messages.lastOrNull()
    var isUserScrolling by remember { mutableStateOf(false) }
    
    // Detect manual scrolling to stop auto-scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling = true
        }
    }
    
    // Reset user scrolling state when we are at the bottom
    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward) {
            isUserScrolling = false
        }
    }
    
    LaunchedEffect(messages.size) {
        // Two different intents share this trigger:
        //  - The user just sent a message: pin THEIR new message to the top
        //    of the viewport so the rest of the screen is free for the
        //    incoming response, instead of leaving the new turn buried at
        //    the bottom edge (matches the Gemini-app / Gallery pattern).
        //  - Anything else that changes message count (new chat opened,
        //    a model turn placeholder appended, etc.): only snap to bottom
        //    if the user isn't already mid-scroll reading something else.
        if (messages.isNotEmpty()) {
            val last = messages.last()
            if (last.role == Role.USER) {
                isUserScrolling = false
                listState.animateScrollToItem(messages.size - 1, scrollOffset = 0)
            } else if (!isUserScrolling) {
                listState.scrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(
        lastMessage?.content?.length, 
        lastMessage?.thought?.length, 
        lastMessage?.toolCalls?.size
    ) {
        if (messages.isNotEmpty() && !isUserScrolling) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            
            // Only auto-scroll if we are already near the bottom
            if (totalItems - lastVisibleIndex <= 2) {
                // Use scrollToItem instead of animateScrollToItem during generation to prevent animation interruption jitters
                listState.scrollToItem(messages.size - 1)
            }
        }
    }
    
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            listState.canScrollForward && (totalItems - lastVisibleIndex > 2)
        }
    }

    val activeToolCalls = remember(messages, isLoading) {
        val lastMsg = messages.lastOrNull()
        if (isLoading && lastMsg != null && lastMsg.role == Role.MODEL && lastMsg.toolCalls.isNotEmpty()) {
            lastMsg.toolCalls
        } else {
            emptyList()
        }
    }

    var isFocusMode by remember { mutableStateOf(false) }

    val activeLlama = oorty.sednium.app.api.activeLlamaHelper
    val isGgufLoading by (activeLlama?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) })
    val isGgufLoaded by (activeLlama?.isLoaded?.collectAsState() ?: remember { mutableStateOf(false) })

    if (isGgufLoading) {
        oorty.sednium.app.ui.components.ModelLoadingOverlay(
            modelName = settings.model.ifBlank { "Local GGUF" },
            isSuccess = isGgufLoaded,
            onSuccessComplete = {},
            onDismiss = {}
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                SedniumTopBar(
                    title = chatTitle,
                    subtitle = "$providerName · ${settings.chatMode.name}",
                    localServerStatus = if (settings.provider == oorty.sednium.app.model.ModelProvider.LOCAL) localServerStatus else null,
                    showClear = messages.isNotEmpty(),
                    showExport = messages.isNotEmpty(),
                    isFocusMode = isFocusMode,
                    onMenuClick = onMenuClick,
                    onExportClick = onExportClick,
                    onClearClick = onClearClick,
                    onSettingsClick = onSettingsClick,
                    onSessionConfigClick = onSessionConfigClick,
                    onFocusModeToggle = { isFocusMode = !isFocusMode }
                )
                if (settings.provider == oorty.sednium.app.model.ModelProvider.LOCAL_GGUF) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OrangeAlpha.a05)
                            .padding(horizontal = 16.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SedniumColors.Green500))
                            Text("Local GGUF Mode", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Green500, fontWeight = FontWeight.Bold)
                        }
                        Text("⚡ Limited agentic (<3B)", style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70)
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(12.dp)
            ) {
                val hasImageAttachment = attachments.any { it.type == oorty.sednium.app.model.AttachmentType.IMAGE }

                androidx.compose.animation.AnimatedVisibility(
                    visible = voiceController.isListening && partialTranscript.isNotBlank(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(SedniumRadii.sm))
                            .background(SedRedAlpha.a10)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = null,
                            tint = SedniumColors.SedRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            partialTranscript,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = SedniumColors.SedRed.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = hasImageAttachment && !oorty.sednium.app.model.isLikelyVisionCapable(settings.model),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(SedniumRadii.sm))
                            .background(OrangeAlpha.a10)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = SedniumColors.Orange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "\"${settings.model}\" may not support images — it might ignore the attachment or return an error.",
                            style = MaterialTheme.typography.labelSmall,
                            color = SedniumColors.Orange,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                MessageComposer(
                    input = input,
                    onInputChange = onInputChange,
                    attachments = attachments,
                    onRemoveAttachment = onRemoveAttachment,
                    isLoading = isLoading,
                    isPresetMenuOpen = isPresetMenuOpen,
                    onTogglePresetMenu = onTogglePresetMenu,
                    presets = settings.savedPresets,
                    activePresetId = settings.activePresetId,
                    onSelectPreset = onSelectPreset,
                    onAttachClick = onAttachClick,
                    isListening = voiceController.isListening,
                    onVoiceClick = { voiceController.toggle() },
                    onSend = onSend
                )
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isLoading) "Generating response…"
                        else "Sednium AI may occasionally produce inaccurate, misleading, or\nbeautifully imaginative outputs. Please cross-reference critical data\nindependently.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeAlpha.a40,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isDark = oorty.sednium.app.ui.theme.LocalSedniumIsDark.current
            val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange

            if (messages.isEmpty()) {
                EmptyState(
                    isConfigValid = isConfigValid,
                    providerName = providerName,
                    modelLabel = settings.model,
                    isDark = isDark,
                    accentColor = accentColor,
                    onTapConfigure = onSettingsClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isLast = msg.id == messages.last().id
                        ChatBubble(
                            msg = msg,
                            providerName = providerName,
                            isDark = isDark,
                            isGenerating = isLoading && isLast,
                            showPerformanceStats = settings.showPerformanceStats,
                            onImageClick = onImageClick,
                            onRetry = if (isLast && msg.role == Role.MODEL && !isLoading) onRetry else null
                        )
                    }
                }

                AnimatedVisibility(
                    visible = activeToolCalls.isNotEmpty() && !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ToolActivityView(toolCalls = activeToolCalls)
                }
                
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        containerColor = accentColor,
                        contentColor = SedniumColors.Milk,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                    }
                }
            }
        }
    }
}

/** "Ready to Chat" placeholder shown when the active chat session has no messages yet. */
@Composable
private fun EmptyState(
    isConfigValid: Boolean,
    providerName: String,
    modelLabel: String,
    isDark: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onTapConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(SedniumRadii.lg))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = oorty.sednium.app.R.drawable.logo),
                contentDescription = "Oorty Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Text(
            "Ready to Chat",
            style = MaterialTheme.typography.titleLarge,
            color = if (isDark) SedniumColors.Gray100 else accentColor,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        if (!isConfigValid) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(SedniumRadii.md))
                    .background(if (isDark) Color(0xFF262626) else OrangeAlpha.a10)
                    .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(SedniumRadii.md))
                    .clickable(onClick = onTapConfigure)
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = accentColor)
                Text("Configuration Needed", color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Tap to setup $providerName", color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text(
                "Using ${modelLabel.ifBlank { "Unknown Model" }}.\nStart typing to generate a response.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) SedniumColors.Gray400 else accentColor.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

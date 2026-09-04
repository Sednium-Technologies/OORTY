package oorty.sednium.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.Attachment
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.ChatSession
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.ui.components.ImageViewerOverlay
import oorty.sednium.app.ui.screens.ChatListScreen
import oorty.sednium.app.ui.screens.ChatScreen
import oorty.sednium.app.ui.screens.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

enum class LocalServerStatus {
    OFFLINE, IDLE, PROCESSING, UNKNOWN
}

/**
 * The root composable. Functionally equivalent to App.tsx: a single
 * always-mounted Chat page, with the Chat List sliding in from the left
 * (ModalNavigationDrawer == ChatListDrawer.tsx) and Settings presented as
 * a bottom sheet (ModalBottomSheet == SettingsDrawer.tsx), plus the
 * full-screen ImageViewerOverlay stacked on top of everything (z-[100]
 * in the original).
 *
 * State here is intentionally minimal/in-memory; wire a real
 * ViewModel + Room/DataStore-backed repository for persistence parity
 * with the original's localStorage-based `sednium_settings` /
 * `sednium_chats` keys.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SedniumApp(
    chats: List<ChatSession>,
    currentChatId: String,
    settings: AppSettings,
    mcpServerManager: oorty.sednium.app.mcp.McpServerManager,
    pluginManager: oorty.sednium.app.plugins.PluginManager? = null,
    speechService: oorty.sednium.app.plugins.speech.SpeechService? = null,
    onUpdateSettings: (AppSettings) -> Unit,
    onUpdateSessionConfig: (ChatSession) -> Unit,
    onSelectChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (String) -> Unit,
    onDeleteMultipleChats: (List<String>) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onClearCurrentChat: () -> Unit,
    onSend: (String, List<Attachment>, Boolean) -> Unit,
    onRetry: () -> Unit,
    isLoading: Boolean,
    onOpenPromptLab: () -> Unit = {},
    onBranchChat: ((ChatMessage) -> Unit)? = null,
    onSendToModel: ((ChatMessage, ModelProvider, String) -> Unit)? = null,
    onEditUserMessage: ((ChatMessage) -> Unit)? = null
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var isSettingsOpen by remember { mutableStateOf(false) }
    var showSessionConfig by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }
    var isPresetMenuOpen by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(listOf<Attachment>()) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var exportText by remember { mutableStateOf("") }
    var localServerStatus by remember { mutableStateOf(LocalServerStatus.UNKNOWN) }
    val currentChat = chats.find { it.id == currentChatId } ?: chats.firstOrNull()
    val isConfigValid = settings.model.isNotBlank()

    // Speech & Voice State Collection
    val isListening by (speechService?.isListening?.collectAsState() ?: remember { mutableStateOf(false) })
    val isSpeaking by (speechService?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) })
    val spokenMessageId by (speechService?.spokenMessageId?.collectAsState() ?: remember { mutableStateOf(null) })
    val liveSpokenText by (speechService?.liveSpokenText?.collectAsState() ?: remember { mutableStateOf("") })
    val soundLevel by (speechService?.soundLevel?.collectAsState() ?: remember { mutableStateOf(0f) })

    var lastSpokenMsgId by remember { mutableStateOf<String?>(null) }

    // If in Voice Mode and speech recognized, trigger send
    androidx.compose.runtime.LaunchedEffect(showVoiceMode) {
        if (showVoiceMode && speechService != null) {
            speechService.isContinuousMode = true
            speechService.onSpeechRecognized = { transcript ->
                if (transcript.isNotBlank()) {
                    onSend(transcript, emptyList(), true)
                }
            }
            speechService.onSpeechComplete = {
                // When TTS finished reading the model's response in voice mode, automatically resume listening
                if (showVoiceMode) {
                    speechService.startListening()
                }
            }
            speechService.onInterruption = {
                if (showVoiceMode) {
                    speechService.stopSpeaking()
                    speechService.startListening()
                }
            }
            speechService.startListening()
        } else {
            speechService?.isContinuousMode = false
            speechService?.stopListening()
            speechService?.stopSpeaking()
        }
    }

    // Watchdog to auto-resume listening when in Hands-Free Voice Mode if speech and generation are done
    androidx.compose.runtime.LaunchedEffect(showVoiceMode, isLoading, isSpeaking, isListening) {
        if (showVoiceMode && speechService != null && !isLoading && !isSpeaking && !isListening) {
            kotlinx.coroutines.delay(350)
            if (showVoiceMode && !isLoading && !speechService.isSpeaking.value && !speechService.isListening.value) {
                speechService.isContinuousMode = true
                speechService.startListening()
            }
        }
    }

    // Auto-speak model response when in Hands-Free Voice Mode (fallback for non-streaming turns)
    androidx.compose.runtime.LaunchedEffect(isLoading, showVoiceMode, currentChat?.messages?.size) {
        if (showVoiceMode && speechService != null && !isLoading) {
            val lastMsg = currentChat?.messages?.lastOrNull()
            if (lastMsg != null && lastMsg.role == oorty.sednium.app.model.Role.MODEL && lastMsg.id != lastSpokenMsgId) {
                lastSpokenMsgId = lastMsg.id
                if (lastMsg.content.isNotBlank() && !isSpeaking) {
                    speechService.speakText(
                        text = lastMsg.content,
                        messageId = lastMsg.id,
                        rate = settings.ttsSpeechRate,
                        pitch = settings.ttsPitch,
                        persona = settings.voicePersona
                    )
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(settings.provider, settings.localBaseUrl, isLoading) {
        if (settings.provider != ModelProvider.LOCAL) {
            localServerStatus = LocalServerStatus.UNKNOWN
            return@LaunchedEffect
        }
        if (isLoading) {
            localServerStatus = LocalServerStatus.PROCESSING
            return@LaunchedEffect
        }
        
        while (true) {
            localServerStatus = withContext(Dispatchers.IO) {
                try {
                    val baseUrl = settings.localBaseUrl.removeSuffix("/")
                    // Ollama uses /api/tags or /api/version, standard OpenAI uses /models.
                    // We can just try to connect to the baseUrl.
                    val urlStr = if (baseUrl.endsWith("/v1")) baseUrl.replace("/v1", "/") else baseUrl
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    val code = connection.responseCode
                    if (code in 200..404) LocalServerStatus.IDLE else LocalServerStatus.OFFLINE
                } catch (e: Exception) {
                    LocalServerStatus.OFFLINE
                }
            }
            delay(5000)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(exportText.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val pickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newAttachments = uris.mapNotNull { uri ->
            val typeStr = contentResolver.getType(uri) ?: "application/octet-stream"
            val type = if (typeStr.startsWith("image/")) oorty.sednium.app.model.AttachmentType.IMAGE else oorty.sednium.app.model.AttachmentType.TEXT
            var name = "attachment"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
            
            Attachment(type = type, mimeType = typeStr, data = uri.toString(), name = name)
        }
        attachments = attachments + newAttachments
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val isDark = oorty.sednium.app.ui.theme.LocalSedniumIsDark.current
            ModalDrawerSheet(
                drawerContainerColor = if (isDark) oorty.sednium.app.ui.theme.SedniumColors.DarkBackground else oorty.sednium.app.ui.theme.SedniumColors.Milk
            ) {
                ChatListScreen(
                    chats = chats,
                    currentChatId = currentChatId,
                    onSelectChat = { id -> onSelectChat(id); scope.launch { drawerState.close() } },
                    onNewChat = { onNewChat(); scope.launch { drawerState.close() } },
                    onClose = { scope.launch { drawerState.close() } },
                    onDeleteChat = onDeleteChat,
                    onDeleteMultiple = onDeleteMultipleChats,
                    onRenameChat = onRenameChat,
                    onTogglePin = onTogglePin,
                    onOpenPromptLab = { onOpenPromptLab(); scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatScreen(
                chatTitle = currentChat?.title ?: "Oorty AI",
                settings = settings,
                localServerStatus = localServerStatus,
                messages = currentChat?.messages ?: emptyList(),
                isLoading = isLoading,
                isConfigValid = isConfigValid,
                input = input,
                attachments = attachments,
                isPresetMenuOpen = isPresetMenuOpen,
                currentlySpokenMessageId = spokenMessageId,
                onSpeakMessage = { msg ->
                    if (spokenMessageId == msg.id) {
                        speechService?.stopSpeaking()
                    } else {
                        speechService?.speakText(msg.content, msg.id, settings.ttsSpeechRate, settings.ttsPitch, settings.voicePersona)
                    }
                },
                onOpenVoiceMode = {
                    showVoiceMode = true
                },
                onInputChange = { input = it },
                onSend = {
                    if (input.isNotBlank() || attachments.isNotEmpty()) {
                        onSend(input, attachments, false)
                        input = ""
                        attachments = emptyList()
                    }
                },
                onRetry = onRetry,
                onAttachClick = { pickerLauncher.launch("*/*") },
                onRemoveAttachment = { idx -> attachments = attachments.toMutableList().also { it.removeAt(idx) } },
                onTogglePresetMenu = { isPresetMenuOpen = !isPresetMenuOpen },
                onSelectPreset = { preset ->
                    onUpdateSettings(
                        settings.copy(
                            provider = preset.provider,
                            model = preset.model,
                            chatMode = preset.chatMode,
                            quickSystemInstruction = preset.systemInstruction,
                            activePresetId = preset.id
                        )
                    )
                    isPresetMenuOpen = false
                },
                onBranchChat = onBranchChat,
                onSendToModel = onSendToModel,
                onEditUserMessage = { userMsg ->
                    input = userMsg.content
                    attachments = userMsg.attachments
                    onEditUserMessage?.invoke(userMsg)
                },
                onMenuClick = { scope.launch { drawerState.open() } },
                onExportClick = {
                    val chat = currentChat
                    if (chat != null) {
                        exportText = chat.messages.joinToString("\n\n") { msg ->
                            val roleName = if (msg.role == oorty.sednium.app.model.Role.USER) "You" else "AI"
                            "[$roleName]\n${msg.content}"
                        }
                        val safeTitle = chat.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20).ifBlank { "chat" }
                        val filename = "oorty_export_${safeTitle}.txt"
                        exportLauncher.launch(filename)
                    }
                },
                onClearClick = onClearCurrentChat,
                onSettingsClick = { isSettingsOpen = true },
                onSessionConfigClick = { showSessionConfig = true },
                onImageClick = { url -> selectedImage = url }
            )

            ImageViewerOverlay(
                imageUrl = selectedImage,
                onDismiss = { selectedImage = null }
            ) { url ->
                coil.compose.AsyncImage(
                    model = url, 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }

    var showMcpServers by remember { mutableStateOf(false) }
    var showAddMcpServer by remember { mutableStateOf(false) }
    var showMcpDisclaimer by remember { mutableStateOf(false) }
    var initialMcpName by remember { mutableStateOf("") }
    var initialMcpUrl by remember { mutableStateOf("https://") }
    var initialMcpAuthToken by remember { mutableStateOf<String?>(null) }
    var editingMcpId by remember { mutableStateOf<String?>(null) }

    if (isSettingsOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isSettingsOpen = false },
            sheetState = sheetState,
            containerColor = oorty.sednium.app.ui.theme.SedniumColors.Milk,
            modifier = Modifier.fillMaxSize(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            SettingsScreen(
                settings = settings,
                localServerStatus = localServerStatus,
                mcpServerManager = mcpServerManager,
                pluginManager = pluginManager,
                speechService = speechService,
                onOpenMcpServers = { showMcpServers = true },
                onUpdateSettings = onUpdateSettings,
                onClose = { 
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            isSettingsOpen = false
                        }
                    }
                }
            )
        }
    }

    // --- Onboarding First-Load Plugin Screen ---
    if (!settings.hasCompletedPluginOnboarding && pluginManager != null) {
        oorty.sednium.app.ui.screens.PluginOnboardingScreen(
            pluginManager = pluginManager,
            onCompleteOnboarding = { selectedIds ->
                onUpdateSettings(
                    settings.copy(
                        hasCompletedPluginOnboarding = true,
                        installedPluginIds = settings.installedPluginIds + selectedIds,
                        activePluginIds = settings.activePluginIds + selectedIds
                    )
                )
            },
            onSkip = {
                onUpdateSettings(settings.copy(hasCompletedPluginOnboarding = true))
            }
        )
    }

    // --- Continuous Hands-Free Live Mode Voice Overlay ---
    if (showVoiceMode && speechService != null) {
        val lastModelMessage = currentChat?.messages?.lastOrNull { it.role == oorty.sednium.app.model.Role.MODEL }?.content ?: ""
        oorty.sednium.app.ui.components.LiveModeOverlay(
            isListening = isListening,
            isSpeaking = isSpeaking,
            isLoading = isLoading,
            soundLevel = soundLevel,
            userSaidText = liveSpokenText.ifBlank { input },
            modelResponseText = lastModelMessage,
            onMicClick = {
                if (isSpeaking) {
                    speechService.stopSpeaking()
                    speechService.startListening()
                } else if (isListening) {
                    speechService.isContinuousMode = false
                    speechService.stopListening()
                } else {
                    speechService.isContinuousMode = true
                    speechService.startListening()
                }
            },
            onClose = {
                showVoiceMode = false
                speechService.isContinuousMode = false
                speechService.stopListening()
                speechService.stopSpeaking()
            }
        )
    }

    if (showMcpServers) {
        val mcpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMcpServers = false },
            sheetState = mcpSheetState,
            containerColor = oorty.sednium.app.ui.theme.SedniumColors.SedYellow,
            modifier = Modifier.fillMaxSize(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            oorty.sednium.app.ui.screens.McpServersScreen(
                mcpServerManager = mcpServerManager,
                configs = settings.mcpServers,
                onAddClick = {
                    initialMcpName = ""
                    initialMcpUrl = "https://"
                    initialMcpAuthToken = null
                    editingMcpId = null
                    if (settings.mcpDisclaimerAcknowledged) showAddMcpServer = true else showMcpDisclaimer = true
                },
                onPresetClick = { name, url ->
                    initialMcpName = name
                    initialMcpUrl = url
                    initialMcpAuthToken = null
                    editingMcpId = null
                    if (settings.mcpDisclaimerAcknowledged) showAddMcpServer = true else showMcpDisclaimer = true
                },
                onEditServer = { config ->
                    initialMcpName = config.name
                    initialMcpUrl = config.url
                    initialMcpAuthToken = config.authToken
                    editingMcpId = config.id
                    showAddMcpServer = true
                },
                onToggleServer = { serverId, enabled ->
                    val updatedServers = settings.mcpServers.map { server ->
                        if (server.id == serverId) server.copy(enabled = enabled) else server
                    }
                    onUpdateSettings(settings.copy(mcpServers = updatedServers))
                    if (!enabled) {
                        mcpServerManager.disconnect(serverId)
                    } else {
                        updatedServers.find { it.id == serverId }?.let { mcpServerManager.connect(it) }
                    }
                },
                onRemove = { serverId ->
                    onUpdateSettings(settings.copy(mcpServers = settings.mcpServers.filter { it.id != serverId }))
                    mcpServerManager.disconnect(serverId)
                },
                onReconnectAll = { mcpServerManager.reconnectAll(settings.mcpServers) },
                onToggleTool = { serverId, toolName, enabled ->
                    val updatedServers = settings.mcpServers.map { server ->
                        if (server.id != serverId) return@map server
                        val updatedDisabled = if (enabled) server.disabledTools - toolName else server.disabledTools + toolName
                        server.copy(disabledTools = updatedDisabled)
                    }
                    onUpdateSettings(settings.copy(mcpServers = updatedServers))
                },
                onClose = {
                    scope.launch { mcpSheetState.hide() }.invokeOnCompletion {
                        if (!mcpSheetState.isVisible) {
                            showMcpServers = false
                        }
                    }
                }
            )
        }
    }

    if (showSessionConfig && currentChat != null) {
        oorty.sednium.app.ui.components.SessionConfigDialog(
            session = currentChat,
            globalSettings = settings,
            onDismiss = { showSessionConfig = false },
            onSave = { updatedSession ->
                onUpdateSessionConfig(updatedSession)
                showSessionConfig = false
            }
        )
    }

    if (showAddMcpServer) {
        oorty.sednium.app.ui.components.AddMcpServerDialog(
            initialName = initialMcpName,
            initialUrl = initialMcpUrl,
            initialAuthToken = initialMcpAuthToken,
            onDismiss = { showAddMcpServer = false },
            onAdd = { name, url, authToken ->
                val newConfig = oorty.sednium.app.model.MCPConfig(
                    id = editingMcpId ?: java.util.UUID.randomUUID().toString(),
                    name = name,
                    url = url,
                    authToken = authToken?.takeIf { it.isNotBlank() },
                    disabledTools = settings.mcpServers.find { it.id == editingMcpId }?.disabledTools ?: emptySet(),
                    enabled = settings.mcpServers.find { it.id == editingMcpId }?.enabled ?: true
                )
                val updatedServers = if (editingMcpId != null) {
                    settings.mcpServers.map { if (it.id == editingMcpId) newConfig else it }
                } else {
                    settings.mcpServers + newConfig
                }
                onUpdateSettings(settings.copy(mcpServers = updatedServers))
                if (editingMcpId != null) mcpServerManager.disconnect(editingMcpId!!)
                mcpServerManager.connect(newConfig)
                showAddMcpServer = false
            }
        )
    }

    if (showMcpDisclaimer) {
        oorty.sednium.app.ui.components.McpDisclaimerDialog(
            onDismiss = { showMcpDisclaimer = false },
            onAcknowledge = {
                onUpdateSettings(settings.copy(mcpDisclaimerAcknowledged = true))
                showMcpDisclaimer = false
                showAddMcpServer = true
            }
        )
    }
}

package oorty.sednium.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.ChatSession
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.model.Role
import oorty.sednium.app.model.ToolCallState
import oorty.sednium.app.mcp.*
import oorty.sednium.app.navigation.SedniumApp
import oorty.sednium.app.ui.theme.SedniumTheme
import kotlinx.coroutines.launch
import oorty.sednium.app.api.generateContentStream

/**
 * Runs one full agentic tool-calling round for a user message: builds the
 * right ToolCallingChatClient for the active provider, runs it through
 * ToolCallOrchestrator (send turn -> execute any tool calls via MCP -> feed
 * results back -> repeat, up to ToolCallPolicy.maxIterations), and reports
 * live per-call status via onToolCallsUpdated so the existing
 * ToolActivityView UI lights up exactly the way it already expected to.
 *
 * Only the CURRENT user message's tool-call round-trip gets fully structured
 * history (tool_use/tool_result preserved exactly as each provider needs).
 * Earlier turns from previous messages in this same chat are passed in as
 * plain User/Assistant text — a deliberate simplification, since
 * ChatMessage itself doesn't persist structured tool-call data across app
 * restarts. This doesn't affect correctness of the round Trip happening
 * right now, only whether a model can see the exact tool calls it made many
 * messages ago (it can still see the text content of what happened).
 */
private suspend fun runAgenticTurn(
    mcpServerManager: McpServerManager,
    provider: ModelProvider,
    apiKey: String,
    modelName: String,
    baseUrl: String,
    systemInstruction: String,
    temperature: Float,
    topP: Float,
    topK: Int,
    maxTokens: Int,
    userMessage: String,
    priorHistory: List<ChatMessage>,
    context: android.content.Context,
    vaultIndexer: oorty.sednium.app.vault.VaultIndexer? = null,
    onToolCallsUpdated: (List<ToolCallState>) -> Unit
): String {
    val llmClient: ToolCallingChatClient = when (provider) {
        ModelProvider.GOOGLE -> GeminiToolChatClient(
            apiKey = apiKey, modelName = modelName, systemInstruction = systemInstruction,
            temperature = temperature, topP = topP, topK = topK, maxTokens = maxTokens
        )
        ModelProvider.ANTHROPIC -> AnthropicToolChatClient(
            apiKey = apiKey, modelName = modelName, baseUrl = baseUrl, systemInstruction = systemInstruction,
            temperature = temperature, topP = topP, topK = topK, maxTokens = maxTokens
        )
        ModelProvider.LOCAL_GGUF -> {
            val helper = oorty.sednium.app.api.activeLlamaHelper ?: oorty.sednium.app.api.LlamaHelper(
                context = context,
                uri = oorty.sednium.app.api.activeGgufUri
            )
            LocalGgufToolChatClient(
                llamaHelper = helper,
                systemInstruction = systemInstruction,
                temperature = temperature,
                maxTokens = maxTokens
            )
        }
        ModelProvider.LOCAL_LITERT -> {
            val helper = oorty.sednium.app.api.activeLlamaHelper ?: oorty.sednium.app.api.LlamaHelper(
                context = context,
                uri = oorty.sednium.app.api.activeGgufUri
            )
            LocalGgufToolChatClient(
                llamaHelper = helper,
                systemInstruction = systemInstruction,
                temperature = temperature,
                maxTokens = maxTokens
            )
        }
        else -> OpenAiCompatToolChatClient(
            apiKey = apiKey, modelName = modelName, baseUrl = baseUrl, systemInstruction = systemInstruction,
            temperature = temperature, topP = topP, maxTokens = maxTokens,
            isOpenRouter = provider == ModelProvider.OPENROUTER
        )
    }

    val toolCallStates = linkedMapOf<String, ToolCallState>()

    val orchestrator = ToolCallOrchestrator(
        mcpServers = mcpServerManager,
        llm = llmClient,
        policy = ToolCallPolicy(confirmDestructiveCalls = false),
        vaultIndexer = vaultIndexer,
        context = context,
        onEvent = { event ->
            val update: Pair<String, ToolCallState>? = when (event) {
                is ToolCallEvent.Started -> event.call.callId to ToolCallState(event.call.qualifiedName.substringAfter("::"), isExecuting = true, success = false)
                is ToolCallEvent.Retrying -> event.call.callId to ToolCallState("${event.call.qualifiedName.substringAfter("::")} (retrying)", isExecuting = true, success = false)
                is ToolCallEvent.Succeeded -> event.call.callId to ToolCallState(event.call.qualifiedName.substringAfter("::"), isExecuting = false, success = true)
                is ToolCallEvent.Failed -> event.call.callId to ToolCallState(event.call.qualifiedName.substringAfter("::"), isExecuting = false, success = false)
                is ToolCallEvent.Declined -> event.call.callId to ToolCallState("${event.call.qualifiedName.substringAfter("::")} (declined)", isExecuting = false, success = false)
                else -> null
            }
            if (update != null) {
                toolCallStates[update.first] = update.second
                onToolCallsUpdated(toolCallStates.values.toList())
            }
        }
    )

    val priorTurns: List<LlmChatTurn> = priorHistory.map { msg ->
        if (msg.role == Role.USER) LlmChatTurn.User(msg.content) else LlmChatTurn.Assistant(msg.content)
    }

    return orchestrator.run(userMessage, priorTurns)
}

/**
 * Single-activity host, mirroring the SPA shell index.tsx mounted into.
 * Replace the in-memory `remember { mutableStateOf(...) }` blocks with a
 * SedniumViewModel backed by DataStore (settings) + Room (chat sessions)
 * for true parity with the original's `localStorage` persistence.
 */
class MainActivity : FragmentActivity() {
    private var speechService: oorty.sednium.app.plugins.speech.SpeechService? = null

    override fun onDestroy() {
        super.onDestroy()
        speechService?.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = StorageHelper(this)
        oorty.sednium.app.api.activeAppContext = applicationContext
        oorty.sednium.app.api.LiteRtTitleGen.initialize(this)
        val initialSettings = storage.loadSettings()
        if (initialSettings.ggufModelUri.isNotBlank()) {
            oorty.sednium.app.api.activeGgufUri = android.net.Uri.parse(initialSettings.ggufModelUri)
        }
        val initialChats = storage.loadChats().ifEmpty {
            listOf(
                ChatSession(
                    id = System.currentTimeMillis().toString(),
                    title = "New Chat",
                    messages = emptyList(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        enableEdgeToEdge()
        val speech = oorty.sednium.app.plugins.speech.SpeechService(this)
        speechService = speech
        val pluginManager = oorty.sednium.app.plugins.PluginManager(this)

        setContent {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            var settings by remember { mutableStateOf(initialSettings) }
            var chats by remember { mutableStateOf(initialChats) }
            var currentChatId by remember { mutableStateOf(chats.firstOrNull()?.id ?: System.currentTimeMillis().toString()) }
            var isLoading by remember { mutableStateOf(false) }
            var showPromptLab by remember { mutableStateOf(false) }
            var promptLabOutput by remember { mutableStateOf("") }
            var promptLabRunning by remember { mutableStateOf(false) }

            LaunchedEffect(settings) {
                storage.saveSettings(settings)
                pluginManager.refreshPlugins(settings.installedPluginIds)
            }

            LaunchedEffect(chats) {
                storage.saveChats(chats)
            }

            val mcpServerManager = remember { oorty.sednium.app.mcp.McpServerManager() }

            LaunchedEffect(Unit) {
                // Settings (including mcpServers) are already loaded from
                // SharedPreferences via StorageHelper above; the manager
                // itself is purely in-memory, so without this the servers
                // the user previously added would show as "Error" / blank
                // until they manually hit Reconnect All.
                if (initialSettings.mcpServers.isNotEmpty()) {
                    mcpServerManager.connectSavedServers(initialSettings.mcpServers)
                }
            }

            val isDark = settings.theme == oorty.sednium.app.model.AppTheme.DARK
            androidx.compose.animation.Crossfade(targetState = isDark, label = "ThemeCrossfade") { darkTheme ->
                SedniumTheme(darkTheme = darkTheme, useSerif = settings.useSerifFont) {
                    Surface(modifier = Modifier.fillMaxSize()) {

                    if (showPromptLab) {
                        oorty.sednium.app.ui.screens.PromptLabScreen(
                            isRunning = promptLabRunning,
                            output = promptLabOutput,
                            isDark = settings.theme == oorty.sednium.app.model.AppTheme.DARK,
                            onBack = { showPromptLab = false; promptLabOutput = "" },
                            onRun = { tool, toolInput, toneInstruction ->
                                promptLabOutput = ""
                                promptLabRunning = true
                                scope.launch {
                                    try {
                                         val apiKey = oorty.sednium.app.ui.screens.apiKeyFor(settings)
                                          if (apiKey.isBlank() && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL_GGUF && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL_LITERT && settings.provider != oorty.sednium.app.model.ModelProvider.NONE) {
                                             promptLabOutput = "Error: API Key is missing. Please add it in settings."
                                             return@launch
                                         }
                                        val effectiveSystemPrompt = if (toneInstruction != null) {
                                            "${tool.systemPrompt}\n\n$toneInstruction"
                                        } else {
                                            tool.systemPrompt
                                        }
                                        generateContentStream(
                                            apiKey = apiKey,
                                            modelName = settings.model,
                                            prompt = toolInput,
                                            history = emptyList(),
                                            provider = settings.provider,
                                            baseUrl = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.defaultUrl ?: "",
                                            systemInstruction = effectiveSystemPrompt,
                                            temperature = settings.temperature,
                                            topP = settings.topP,
                                            topK = settings.topK,
                                            maxTokens = settings.maxTokens,
                                            onChunkReceived = { deltaText, _ ->
                                                promptLabOutput += deltaText
                                            }
                                        )
                                    } catch (e: Exception) {
                                        promptLabOutput += "\nError: ${e.message}"
                                    } finally {
                                        promptLabRunning = false
                                    }
                                }
                            },
                            onSendToChat = { tool, toolInput, toolOutput ->
                                val now = System.currentTimeMillis()
                                val fresh = ChatSession(
                                    id = now.toString(),
                                    title = "${tool.label}: ${toolInput.take(30)}",
                                    messages = listOf(
                                        ChatMessage(id = now.toString() + "_u", role = Role.USER, content = toolInput),
                                        ChatMessage(
                                            id = (now + 1).toString() + "_m",
                                            role = Role.MODEL,
                                            content = toolOutput,
                                            modelName = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.displayName ?: settings.model
                                        )
                                    ),
                                    updatedAt = now
                                )
                                chats = listOf(fresh) + chats
                                currentChatId = fresh.id
                                showPromptLab = false
                                promptLabOutput = ""
                            }
                        )
                    } else {
                    SedniumApp(
                        chats = chats,
                        currentChatId = currentChatId,
                        settings = settings,
                        mcpServerManager = mcpServerManager,
                        pluginManager = pluginManager,
                        speechService = speechService,
                        onUpdateSettings = { settings = it },
                        onUpdateSessionConfig = { updatedSession ->
                            chats = chats.map { if (it.id == updatedSession.id) updatedSession else it }
                        },
                        onOpenPromptLab = { showPromptLab = true },
                        onSelectChat = { id -> currentChatId = id },
                        onNewChat = {
                            val fresh = ChatSession(
                                id = System.currentTimeMillis().toString(),
                                title = "New Chat",
                                updatedAt = System.currentTimeMillis()
                            )
                            chats = listOf(fresh) + chats
                            currentChatId = fresh.id
                        },
                        onDeleteChat = { id ->
                            chats = chats.filterNot { it.id == id }
                            if (chats.isEmpty()) {
                                val fresh = ChatSession(System.currentTimeMillis().toString(), "New Chat", updatedAt = System.currentTimeMillis())
                                chats = listOf(fresh)
                            }
                            if (currentChatId == id) currentChatId = chats.first().id
                        },
                        onDeleteMultipleChats = { ids ->
                            chats = chats.filterNot { ids.contains(it.id) }
                            if (chats.isEmpty()) {
                                val fresh = ChatSession(System.currentTimeMillis().toString(), "New Chat", updatedAt = System.currentTimeMillis())
                                chats = listOf(fresh)
                            }
                            if (ids.contains(currentChatId)) currentChatId = chats.first().id
                        },
                        onRenameChat = { id, title ->
                            chats = chats.map { if (it.id == id) it.copy(title = title) else it }
                        },
                        onTogglePin = { id ->
                            chats = chats.map { if (it.id == id) it.copy(isPinned = !it.isPinned) else it }
                        },
                        onClearCurrentChat = {
                            chats = chats.map { if (it.id == currentChatId) it.copy(messages = emptyList()) else it }
                        },
                        onBranchChat = { message ->
                            val chat = chats.find { it.id == currentChatId }
                            if (chat != null) {
                                val idx = chat.messages.indexOfFirst { it.id == message.id }
                                if (idx != -1) {
                                    val branchMessages = chat.messages.take(idx + 1)
                                    val newSessionId = System.currentTimeMillis().toString()
                                    val branched = ChatSession(
                                        id = newSessionId,
                                        title = "Branch: ${chat.title}",
                                        messages = branchMessages,
                                        parentSessionId = chat.id,
                                        forkedFromMessageId = message.id,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    chats = chats + branched
                                    currentChatId = newSessionId
                                    scope.launch { storage.saveChats(chats) }
                                    android.widget.Toast.makeText(this@MainActivity, "Branched to new chat session", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onEditUserMessage = { userMsg ->
                            val chat = chats.find { it.id == currentChatId }
                            if (chat != null) {
                                val idx = chat.messages.indexOfFirst { it.id == userMsg.id }
                                if (idx != -1) {
                                    val kept = chat.messages.take(idx)
                                    chats = chats.map { if (it.id == currentChatId) it.copy(messages = kept) else it }
                                    scope.launch { storage.saveChats(chats) }
                                }
                            }
                        },
                        onSendToModel = { message, prov, modelId ->
                            val chat = chats.find { it.id == currentChatId }
                            if (chat != null) {
                                val msgIdx = chat.messages.indexOfFirst { it.id == message.id }
                                val priorUserMsg = if (msgIdx > 0) {
                                    chat.messages.take(msgIdx).lastOrNull { it.role == Role.USER }
                                } else null
                                val prompt = priorUserMsg?.content ?: message.content
                                val attachments = priorUserMsg?.attachments ?: emptyList()
                                
                                val modelMsgId = (System.currentTimeMillis() + 1).toString()
                                val initialModelMsg = ChatMessage(
                                    id = modelMsgId,
                                    role = Role.MODEL,
                                    content = "",
                                    modelName = "${oorty.sednium.app.model.PROVIDER_CONFIG[prov]?.displayName ?: prov.name} ($modelId)",
                                    isThinking = false
                                )
                                chats = chats.map {
                                    if (it.id == currentChatId) it.copy(
                                        messages = it.messages + initialModelMsg,
                                        updatedAt = System.currentTimeMillis()
                                    ) else it
                                }
                                isLoading = true
                                scope.launch {
                                    try {
                                        val apiKey = oorty.sednium.app.ui.screens.apiKeyForProvider(settings, prov)
                                        val baseUrl = oorty.sednium.app.model.PROVIDER_CONFIG[prov]?.defaultUrl ?: ""
                                        generateContentStream(
                                            apiKey = apiKey,
                                            modelName = modelId,
                                            prompt = prompt,
                                            history = chat.messages.take(msgIdx),
                                            provider = prov,
                                            baseUrl = baseUrl,
                                            systemInstruction = chat.systemInstructionOverride ?: settings.currentSystemInstruction,
                                            temperature = chat.temperatureOverride ?: settings.temperature,
                                            topP = chat.topPOverride ?: settings.topP,
                                            topK = chat.topKOverride ?: settings.topK,
                                            maxTokens = chat.maxTokensOverride ?: settings.maxTokens,
                                            attachments = attachments,
                                            onChunkReceived = { deltaText, deltaThought ->
                                                chats = chats.map { c ->
                                                    if (c.id == currentChatId) {
                                                        val updated = c.messages.map { m ->
                                                            if (m.id == modelMsgId) {
                                                                m.copy(
                                                                    content = m.content + deltaText,
                                                                    thought = if (!deltaThought.isNullOrEmpty()) (m.thought ?: "") + deltaThought else m.thought
                                                                )
                                                            } else m
                                                        }
                                                        c.copy(messages = updated, updatedAt = System.currentTimeMillis())
                                                    } else c
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        val errorMsg = e.message ?: "Failed to generate comparison"
                                        chats = chats.map { c ->
                                            if (c.id == currentChatId) {
                                                val updated = c.messages.map { m ->
                                                    if (m.id == modelMsgId) m.copy(content = "Error: $errorMsg", isError = true) else m
                                                }
                                                c.copy(messages = updated)
                                            } else c
                                        }
                                    } finally {
                                        isLoading = false
                                        storage.saveChats(chats)
                                    }
                                }
                            }
                        },
                        onRetry = {
                            val chat = chats.find { it.id == currentChatId } ?: return@SedniumApp
                            if (chat.messages.isEmpty()) return@SedniumApp
                            
                            // Remove the last model message(s) if any
                            var newMessages = chat.messages
                            while (newMessages.isNotEmpty() && newMessages.last().role == Role.MODEL) {
                                newMessages = newMessages.dropLast(1)
                            }
                            
                            val lastUserMsg = newMessages.lastOrNull { it.role == Role.USER }
                            if (lastUserMsg == null) return@SedniumApp
                            
                            // Remove the last user msg from the history to be sent as the new prompt
                            val historyWithoutLastUser = newMessages.dropLast(1)
                            
                            val modelMsgId = (System.currentTimeMillis() + 1).toString()
                            val initialModelMsg = ChatMessage(
                                id = modelMsgId,
                                role = Role.MODEL,
                                content = "",
                                modelName = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.displayName ?: settings.model,
                                isThinking = false
                            )
                            
                            chats = chats.map {
                                if (it.id == currentChatId) it.copy(
                                    messages = newMessages + initialModelMsg,
                                    updatedAt = System.currentTimeMillis()
                                )
                                else it
                            }

                            isLoading = true
                            scope.launch {
                                 val startTime = System.currentTimeMillis()
                                 var firstTokenTime: Long? = null
                                 var firstThoughtTokenTime: Long? = null
                                 var lastThoughtTokenTime: Long? = null
                                 var firstTextTokenTime: Long? = null
                                 try {
                                      val apiKey = oorty.sednium.app.ui.screens.apiKeyFor(settings)
                                      if (apiKey.isBlank() && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL_GGUF && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL_LITERT && settings.provider != oorty.sednium.app.model.ModelProvider.NONE) throw Exception("API Key is missing.")
                                     val effectiveSystemInstruction = chat.systemInstructionOverride ?: settings.currentSystemInstruction
                                     val effectiveTemp = chat.temperatureOverride ?: settings.temperature
                                     val effectiveTopP = chat.topPOverride ?: settings.topP
                                     val effectiveTopK = chat.topKOverride ?: settings.topK
                                     val effectiveMaxTokens = chat.maxTokensOverride ?: settings.maxTokens
                                     val resolvedBaseUrl = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.defaultUrl ?: ""

                                     val vaultContext = storage.vaultIndexer.getRelevantContext(lastUserMsg.content)
                                     val combinedSystemInstruction = if (vaultContext.isNotBlank()) "$effectiveSystemInstruction\n$vaultContext" else effectiveSystemInstruction

                                     if (settings.enableTools && mcpServerManager.availableTools.isNotEmpty()) {
                                         val finalText = runAgenticTurn(
                                             mcpServerManager = mcpServerManager,
                                             provider = settings.provider,
                                             apiKey = apiKey,
                                             modelName = settings.model,
                                             baseUrl = resolvedBaseUrl,
                                             systemInstruction = combinedSystemInstruction,
                                             temperature = effectiveTemp,
                                             topP = effectiveTopP,
                                             topK = effectiveTopK,
                                             maxTokens = effectiveMaxTokens,
                                             userMessage = lastUserMsg.content,
                                             priorHistory = historyWithoutLastUser,
                                             context = this@MainActivity,
                                             vaultIndexer = storage.vaultIndexer,
                                             onToolCallsUpdated = { states ->
                                                 chats = chats.map { chat ->
                                                     if (chat.id == currentChatId) {
                                                         chat.copy(messages = chat.messages.map { msg ->
                                                             if (msg.id == modelMsgId) msg.copy(toolCalls = states) else msg
                                                         })
                                                     } else chat
                                                 }
                                             }
                                         )
                                         firstTokenTime = System.currentTimeMillis()
                                         chats = chats.map { chat ->
                                             if (chat.id == currentChatId) {
                                                 chat.copy(
                                                     messages = chat.messages.map { msg ->
                                                         if (msg.id == modelMsgId) msg.copy(content = finalText) else msg
                                                     },
                                                     updatedAt = System.currentTimeMillis()
                                                 )
                                             } else chat
                                         }
                                     } else {
                                         generateContentStream(
                                             apiKey = apiKey,
                                             modelName = settings.model,
                                             prompt = lastUserMsg.content,
                                             history = historyWithoutLastUser,
                                             provider = settings.provider,
                                             baseUrl = resolvedBaseUrl,
                                             systemInstruction = effectiveSystemInstruction,
                                             temperature = effectiveTemp,
                                             topP = effectiveTopP,
                                             topK = effectiveTopK,
                                             maxTokens = effectiveMaxTokens,
                                             attachments = lastUserMsg.attachments,
                                             onChunkReceived = { deltaText, deltaThought ->
                                                 val now = System.currentTimeMillis()
                                                 if (firstTokenTime == null && (deltaText.isNotEmpty() || !deltaThought.isNullOrEmpty())) {
                                                     firstTokenTime = now
                                                 }
                                                 if (!deltaThought.isNullOrEmpty()) {
                                                     if (firstThoughtTokenTime == null) firstThoughtTokenTime = now
                                                     lastThoughtTokenTime = now
                                                 }
                                                 if (deltaText.isNotEmpty()) {
                                                     if (firstTextTokenTime == null) firstTextTokenTime = now
                                                 }
                                                 chats = chats.map { chat ->
                                                     if (chat.id == currentChatId) {
                                                         val updatedMessages = chat.messages.map { msg ->
                                                             if (msg.id == modelMsgId) {
                                                                 val updatedThought = if (!deltaThought.isNullOrEmpty()) {
                                                                     (msg.thought ?: "") + deltaThought
                                                                 } else msg.thought
                                                                 val updatedContent = if (deltaText.isNotEmpty()) {
                                                                     msg.content + deltaText
                                                                 } else msg.content
                                                                 msg.copy(
                                                                     content = updatedContent,
                                                                     thought = updatedThought,
                                                                     isThinking = !deltaThought.isNullOrEmpty() && deltaText.isEmpty()
                                                                 )
                                                             } else msg
                                                         }
                                                         chat.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis())
                                                     } else chat
                                                 }
                                             }
                                         )
                                     }
                                     val endTime = System.currentTimeMillis()
                                     val ttft = firstTokenTime ?: endTime
                                     val latency = ttft - startTime
                                     val decodeMs = (endTime - ttft).coerceAtLeast(1)
                                     val ft2 = firstThoughtTokenTime
                                     val thoughtDuration = if (ft2 != null) {
                                         val endOfThought = firstTextTokenTime ?: lastThoughtTokenTime ?: endTime
                                         (endOfThought - ft2).coerceAtLeast(0L)
                                     } else null

                                     chats = chats.map { chat ->
                                         if (chat.id == currentChatId) {
                                             val updatedMessages = chat.messages.map { msg ->
                                                 if (msg.id == modelMsgId) {
                                                     val approxTokens = msg.content.length / 4.0
                                                     msg.copy(
                                                         latencyMs = latency,
                                                         tokensPerSecond = (approxTokens / (decodeMs / 1000.0)).toFloat(),
                                                         thoughtDurationMs = thoughtDuration,
                                                         isThinking = false
                                                     )
                                                 } else msg
                                             }
                                             chat.copy(messages = updatedMessages)
                                         } else chat
                                     }
                                } catch (e: Exception) {
                                    chats = chats.map { chat ->
                                        if (chat.id == currentChatId) {
                                            val updatedMessages = chat.messages.map { msg ->
                                                if (msg.id == modelMsgId) msg.copy(content = "Error: ${e.message}", isError = true) else msg
                                            }
                                            chat.copy(messages = updatedMessages)
                                        } else chat
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        onSend = { text, attachments, isVoiceMode ->
                            val userMsg = ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                role = Role.USER,
                                content = text,
                                attachments = attachments
                            )
                            val modelMsgId = (System.currentTimeMillis() + 1).toString()
                            val initialModelMsg = ChatMessage(
                                id = modelMsgId,
                                role = Role.MODEL,
                                content = "",
                                modelName = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.displayName ?: settings.model,
                                isThinking = false
                            )
                            
                            val currentChatSession = chats.find { it.id == currentChatId }
                            val activeChatHistory = currentChatSession?.messages ?: emptyList()
                            
                            chats = chats.map {
                                if (it.id == currentChatId) it.copy(
                                    messages = it.messages + userMsg + initialModelMsg,
                                    updatedAt = System.currentTimeMillis()
                                )
                                else it
                            }

                            isLoading = true
                            scope.launch {
                                // 1. Direct device action check (e.g. "open whatsapp", "just open whatsapp right now", "turn on flashlight", etc.)
                                val directDeviceResult = oorty.sednium.app.plugins.device.DeviceAutomator.tryHandleDirectIntent(this@MainActivity, text)
                                if (directDeviceResult != null) {
                                    chats = chats.map { chat ->
                                        if (chat.id == currentChatId) {
                                            val updatedMessages = chat.messages.map { msg ->
                                                if (msg.id == modelMsgId) msg.copy(content = directDeviceResult, isThinking = false) else msg
                                            }
                                            chat.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis())
                                        } else chat
                                    }
                                    isLoading = false
                                    storage.saveChats(chats)
                                    return@launch
                                }

                                if (settings.autoGenerateTitle && activeChatHistory.isEmpty()) {
                                    scope.launch {
                                        try {
                                            val titlePrompt = "Summarize the user prompt into a short title of max 4 words. No punctuation, no quotes:\n\n$text"
                                            val generatedTitle = runLocalTitleGenOrFallback(settings, titlePrompt)
                                            if (generatedTitle.isNotBlank()) {
                                                chats = chats.map { if (it.id == currentChatId) it.copy(title = generatedTitle) else it }
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }
                                val startTime = System.currentTimeMillis()
                                var firstTokenTime: Long? = null
                                var firstThoughtTokenTime: Long? = null
                                var lastThoughtTokenTime: Long? = null
                                var firstTextTokenTime: Long? = null
                                try {
                                     val apiKey = oorty.sednium.app.ui.screens.apiKeyFor(settings)
                                     if (apiKey.isBlank() && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL_GGUF && settings.provider != oorty.sednium.app.model.ModelProvider.LOCAL_LITERT && settings.provider != oorty.sednium.app.model.ModelProvider.NONE) {
                                         throw Exception("API Key is missing. Please add it in settings.")
                                     }
                                    val baseSystemInstruction = currentChatSession?.systemInstructionOverride ?: settings.currentSystemInstruction
                                    val nowFormatted = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm:ss a z", java.util.Locale.getDefault()).format(java.util.Date())
                                    val realTimeContext = "Current Real-Time Clock: $nowFormatted (Timezone: ${java.util.TimeZone.getDefault().id})"
                                    val liveModePrefix = """
                                        You are Oorty in Live Voice Conversation mode on an Android device.
                                        $realTimeContext
                                        CRITICAL INSTRUCTIONS:
                                        - You have DIRECT ACCESS to device hardware, installed apps, real-time clock, and web search via Action tags.
                                        - To open/launch any app (e.g. WhatsApp, Chrome, Termux, Spotify, YouTube, Settings, Camera, Maps, etc.), output: `[ACTION: OPEN_APP name="app_name"]`
                                        - To search the web: output `[ACTION: SEARCH query="..."]`
                                        - To open a website/URL: output `[ACTION: OPEN_URL url="https://..."]`
                                        - To turn flashlight on or off: output `[ACTION: FLASHLIGHT enable=true]` or `[ACTION: FLASHLIGHT enable=false]`
                                        - To check device battery: output `[ACTION: BATTERY]`
                                        - To check current time: output `[ACTION: TIME]`
                                        - To copy to clipboard: output `[ACTION: CLIPBOARD text="..."]`
                                        - When asked to perform a device action or search, ALWAYS emit the action tag with a brief friendly spoken confirmation.
                                        - Respond in a natural, direct, warm, concise human speaking tone suitable for voice speech (1 to 2 short sentences).
                                        - Never output long lists, markdown bullet points, tables, code blocks, or step-by-step reasoning.
                                        - Speak directly and conversationally like a real-time voice call.
                                        - Do not use markdown syntax (*, #, _, `, etc.).
                                    """.trimIndent()
                                    val effectiveSystemInstruction = if (isVoiceMode) {
                                        "$liveModePrefix\n\n$baseSystemInstruction"
                                    } else {
                                        """
                                            $baseSystemInstruction
                                            $realTimeContext
                                            Native Device Capabilities:
                                            - To launch an app: [ACTION: OPEN_APP name="app_name"]
                                            - To search web: [ACTION: SEARCH query="..."]
                                            - To open link: [ACTION: OPEN_URL url="https://..."]
                                            - To toggle flashlight: [ACTION: FLASHLIGHT enable=true/false]
                                            - To check battery: [ACTION: BATTERY]
                                            - To check time: [ACTION: TIME]
                                        """.trimIndent()
                                    }
                                    val effectiveTemp = if (isVoiceMode) 0.6f else (currentChatSession?.temperatureOverride ?: settings.temperature)
                                    val effectiveTopP = currentChatSession?.topPOverride ?: settings.topP
                                    val effectiveTopK = currentChatSession?.topKOverride ?: settings.topK
                                    val effectiveMaxTokens = if (isVoiceMode) 512 else (currentChatSession?.maxTokensOverride ?: settings.maxTokens)
                                    val resolvedBaseUrl = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.defaultUrl ?: ""

                                    // Silent Background OCR extraction from image attachments if enabled
                                    var effectivePrompt = text
                                    if (settings.enableSilentOcr && attachments.any { it.type == oorty.sednium.app.model.AttachmentType.IMAGE }) {
                                        val ocrSnippets = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.IMAGE }.map { att ->
                                            oorty.sednium.app.plugins.ocr.OcrEngine.extractTextFromAttachment(
                                                context = this@MainActivity,
                                                mimeType = att.mimeType,
                                                data = att.data,
                                                name = att.name
                                            )
                                        }.filter { it.isNotBlank() }

                                        if (ocrSnippets.isNotEmpty()) {
                                            effectivePrompt = "$effectivePrompt\n\n${ocrSnippets.joinToString("\n")}"
                                        }
                                    }

                                    val vaultContext = storage.vaultIndexer.getRelevantContext(effectivePrompt)
                                    val combinedSystemInstruction = if (vaultContext.isNotBlank()) "$effectiveSystemInstruction\n$vaultContext" else effectiveSystemInstruction

                                    if (isVoiceMode) {
                                        speechService?.startStreamingSpeech(
                                            persona = settings.voicePersona,
                                            rate = settings.ttsSpeechRate,
                                            pitch = settings.ttsPitch
                                        )
                                    }

                                    if (settings.enableTools && mcpServerManager.availableTools.isNotEmpty()) {
                                         // --- Agentic tool-calling path ---
                                         val finalText = runAgenticTurn(
                                             mcpServerManager = mcpServerManager,
                                             provider = settings.provider,
                                             apiKey = apiKey,
                                             modelName = settings.model,
                                             baseUrl = resolvedBaseUrl,
                                             systemInstruction = combinedSystemInstruction,
                                             temperature = effectiveTemp,
                                             topP = effectiveTopP,
                                             topK = effectiveTopK,
                                             maxTokens = effectiveMaxTokens,
                                             userMessage = effectivePrompt,
                                             priorHistory = activeChatHistory,
                                             context = this@MainActivity,
                                             vaultIndexer = storage.vaultIndexer,
                                             onToolCallsUpdated = { states ->
                                                 chats = chats.map { chat ->
                                                     if (chat.id == currentChatId) {
                                                         chat.copy(messages = chat.messages.map { msg ->
                                                             if (msg.id == modelMsgId) msg.copy(toolCalls = states) else msg
                                                         })
                                                     } else chat
                                                 }
                                             }
                                         )
                                         firstTokenTime = System.currentTimeMillis()
                                         if (isVoiceMode && finalText.isNotBlank()) {
                                             speechService?.enqueueStreamChunk(finalText)
                                         }
                                         chats = chats.map { chat ->
                                             if (chat.id == currentChatId) {
                                                 chat.copy(
                                                     messages = chat.messages.map { msg ->
                                                         if (msg.id == modelMsgId) msg.copy(content = finalText) else msg
                                                     },
                                                     updatedAt = System.currentTimeMillis()
                                                 )
                                             } else chat
                                         }
                                    } else {
                                         generateContentStream(
                                             apiKey = apiKey,
                                             modelName = settings.model,
                                             prompt = effectivePrompt,
                                             history = activeChatHistory,
                                             provider = settings.provider,
                                             baseUrl = resolvedBaseUrl,
                                             systemInstruction = combinedSystemInstruction,
                                             temperature = effectiveTemp,
                                             topP = effectiveTopP,
                                             topK = effectiveTopK,
                                             maxTokens = effectiveMaxTokens,
                                             attachments = attachments,
                                            onChunkReceived = { deltaText, deltaThought ->
                                                val now = System.currentTimeMillis()
                                                if (firstTokenTime == null && (deltaText.isNotEmpty() || !deltaThought.isNullOrEmpty())) {
                                                    firstTokenTime = now
                                                }
                                                if (!deltaThought.isNullOrEmpty()) {
                                                    if (firstThoughtTokenTime == null) firstThoughtTokenTime = now
                                                    lastThoughtTokenTime = now
                                                }
                                                if (deltaText.isNotEmpty()) {
                                                    if (firstTextTokenTime == null) firstTextTokenTime = now
                                                    if (isVoiceMode) {
                                                        speechService?.enqueueStreamChunk(deltaText)
                                                    }
                                                }
                                                chats = chats.map { chat ->
                                                    if (chat.id == currentChatId) {
                                                        val newMessages = chat.messages.map { msg ->
                                                             if (msg.id == modelMsgId) {
                                                                 val updatedThought = if (!deltaThought.isNullOrEmpty()) {
                                                                     (msg.thought ?: "") + deltaThought
                                                                 } else msg.thought
                                                                 val updatedContent = if (deltaText.isNotEmpty()) {
                                                                     msg.content + deltaText
                                                                 } else msg.content
                                                                 msg.copy(
                                                                     content = updatedContent,
                                                                     thought = updatedThought,
                                                                     isThinking = !deltaThought.isNullOrEmpty() && deltaText.isEmpty()
                                                                 )
                                                             } else msg
                                                         }
                                                         chat.copy(messages = newMessages, updatedAt = System.currentTimeMillis())
                                                     } else chat
                                                 }
                                             }
                                         )
                                     }
                                     if (isVoiceMode) {
                                         speechService?.finishStreamingSpeech()
                                     }

                                     // Execute any action tags emitted by the model (e.g. [ACTION: OPEN_APP name="whatsapp"])
                                     val completedMsg = chats.find { it.id == currentChatId }?.messages?.find { it.id == modelMsgId }
                                     if (completedMsg != null && completedMsg.content.isNotBlank()) {
                                         val actionResult = oorty.sednium.app.plugins.device.DeviceAutomator.executeActionTags(this@MainActivity, completedMsg.content)
                                         if (actionResult.executedActions.isNotEmpty()) {
                                             val updatedContent = actionResult.cleanText.ifBlank { actionResult.executedActions.first() }
                                             chats = chats.map { chat ->
                                                 if (chat.id == currentChatId) {
                                                     chat.copy(
                                                         messages = chat.messages.map { msg ->
                                                             if (msg.id == modelMsgId) msg.copy(content = updatedContent) else msg
                                                         },
                                                         updatedAt = System.currentTimeMillis()
                                                     )
                                                 } else chat
                                             }
                                         }
                                     }

                                     // --- Performance Insights ---
                                     val endTime = System.currentTimeMillis()
                                     val ttft = firstTokenTime ?: endTime
                                     val latency = ttft - startTime
                                     val decodeMs = (endTime - ttft).coerceAtLeast(1)
                                     val ft2 = firstThoughtTokenTime
                                     val thoughtDuration = if (ft2 != null) {
                                         val endOfThought = firstTextTokenTime ?: lastThoughtTokenTime ?: endTime
                                         (endOfThought - ft2).coerceAtLeast(0L)
                                     } else null

                                     chats = chats.map { chat ->
                                         if (chat.id == currentChatId) {
                                             val newMessages = chat.messages.map { msg ->
                                                 if (msg.id == modelMsgId) {
                                                     val approxTokens = msg.content.length / 4.0
                                                     msg.copy(
                                                         latencyMs = latency,
                                                         tokensPerSecond = (approxTokens / (decodeMs / 1000.0)).toFloat(),
                                                         thoughtDurationMs = thoughtDuration,
                                                         isThinking = false
                                                     )
                                                 } else msg
                                             }
                                             chat.copy(messages = newMessages)
                                         } else chat
                                     }
                                 } catch (e: Exception) {
                                     if (isVoiceMode) {
                                         speechService?.stopStreamingSpeech()
                                         val errorText = "Sorry, I encountered an error: ${e.message ?: "Please try again."}"
                                         speechService?.speakText(
                                             text = errorText,
                                             messageId = modelMsgId,
                                             rate = settings.ttsSpeechRate,
                                             pitch = settings.ttsPitch,
                                             persona = settings.voicePersona
                                         )
                                     }
                                     chats = chats.map { chat ->
                                         if (chat.id == currentChatId) {
                                             val newMessages = chat.messages.map { msg ->
                                                 if (msg.id == modelMsgId) msg.copy(content = msg.content + "\nError: ${e.message}", isError = true) else msg
                                             }
                                             chat.copy(messages = newMessages)
                                         } else chat
                                     }
                                 } finally {
                                     isLoading = false
                                 }
                            }
                        },
                        isLoading = isLoading
                    )
                    }
                }
            }
            }
        }
    }
}

private suspend fun runLocalTitleGenOrFallback(settings: oorty.sednium.app.model.AppSettings, prompt: String): String {
    // 1. Force the active connected API / Local model to generate the title first if configured
    try {
        val apiKey = oorty.sednium.app.ui.screens.apiKeyFor(settings)
        val hasValidConfig = (apiKey.isNotBlank() || settings.provider == oorty.sednium.app.model.ModelProvider.LOCAL || settings.provider == oorty.sednium.app.model.ModelProvider.LOCAL_GGUF || settings.provider == oorty.sednium.app.model.ModelProvider.LOCAL_LITERT) && settings.provider != oorty.sednium.app.model.ModelProvider.NONE

        if (hasValidConfig && settings.model.isNotBlank()) {
            var resultTitle = ""
            oorty.sednium.app.api.generateContentStream(
                apiKey = apiKey,
                modelName = settings.model,
                prompt = "Summarize the user prompt into a short title of max 3 to 4 words. Output ONLY the title text:\n\n$prompt",
                history = emptyList(),
                provider = settings.provider,
                baseUrl = oorty.sednium.app.model.PROVIDER_CONFIG[settings.provider]?.defaultUrl ?: "",
                systemInstruction = "You are a concise title generator. Summarize the user message into a short title of 3 to 4 words max. Return ONLY the title text with no punctuation, no quotes, no introductory words, and no markdown.",
                onChunkReceived = { chunk, _ -> resultTitle += chunk }
            )
            val cleaned = resultTitle.trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .removePrefix("Title:")
                .removePrefix("Title")
                .removePrefix("#")
                .trim()
                .trimEnd('.', '!', '?')

            if (cleaned.isNotBlank() && cleaned.length in 2..40 && !cleaned.contains("\n")) {
                return cleaned
            }
        }
    } catch (e: Exception) {}

    // 2. Fallback to on-device LiteRT title generator
    try {
        val localTitle = oorty.sednium.app.api.LiteRtTitleGen.generateTitle(prompt)
        if (localTitle.isNotBlank() && localTitle != "Local Title") return localTitle
    } catch (e: Exception) {}

    // 3. Fallback heuristic: First 4 words
    return prompt.trim().split(Regex("\\s+")).take(4).joinToString(" ").take(28)
}

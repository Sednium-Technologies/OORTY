package oorty.sednium.app.model

import kotlinx.serialization.Serializable

/**
 * Core domain models for Sednium Local Spaces.
 * Direct Kotlin port of the original `types.ts`.
 */

@Serializable enum class Role { USER, MODEL }

@Serializable enum class ChatMode { QUICK, THINKING, CODING }

@Serializable enum class ModelProvider {
    NONE, GOOGLE, OPENAI, ANTHROPIC, XAI, GROQ, OPENROUTER, NVIDIA, LOCAL, CUSTOM, LOCAL_GGUF, ROSETTE
}

@Serializable enum class AppTheme { LIGHT, DARK }

@Serializable enum class AttachmentType { IMAGE, TEXT }

@Serializable data class Attachment(
    val type: AttachmentType,
    val mimeType: String,
    val data: String,      // base64 (no prefix) for images, raw text for text files
    val name: String
)

@Serializable data class ToolCallState(
    val command: String,
    val isExecuting: Boolean,
    val success: Boolean
)

@Serializable data class Citation(
    val id: Int,
    val title: String,
    val domain: String,
    val url: String
)

@Serializable data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val modelName: String? = null,
    val isError: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val thought: String? = null,
    val isThinking: Boolean = false,
    val toolCalls: List<ToolCallState> = emptyList(),
    val citations: List<Citation> = emptyList(),
    // Performance Insights — populated once a model turn finishes streaming.
    // latencyMs = time from request sent to first token received (TTFT).
    // tokensPerSecond is approximate: providers don't return exact token
    // counts on a per-chunk basis over these streaming APIs, so it's
    // estimated from response character count (~4 chars/token, a common
    // rough heuristic for English text) divided by decode time. Treat it as
    // a relative "which provider/model felt faster" signal, not a precise
    // figure.
    val latencyMs: Long? = null,
    val tokensPerSecond: Float? = null
)

@Serializable data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    // Per-session generation overrides. Null means "inherit the current
    // global AppSettings value" — these only kick in once a user explicitly
    // sets them via the session config dialog (gear icon in the chat
    // TopBar), and only affect this one chat.
    val temperatureOverride: Float? = null,
    val topPOverride: Float? = null,
    val topKOverride: Int? = null,
    val maxTokensOverride: Int? = null,
    val systemInstructionOverride: String? = null
)

@Serializable data class MCPConfig(
    val id: String,
    val name: String,
    val url: String,
    val authToken: String? = null,
    // Names of tools (unqualified, as reported by tools/list) that the user has
    // explicitly disabled for this server. Disabled tools are filtered out of
    // McpServerManager.availableTools and therefore never offered to the model.
    val disabledTools: Set<String> = emptySet(),
    val enabled: Boolean = true
)

enum class McpConnectionStatus {
    CONNECTING, CONNECTED, ERROR
}

@Serializable data class SavedModelPreset(
    val id: String,
    val name: String,
    val provider: ModelProvider,
    val model: String,
    val chatMode: ChatMode,
    val systemInstruction: String
)

@Serializable data class AppSettings(
    val theme: AppTheme = AppTheme.LIGHT,
    val provider: ModelProvider = ModelProvider.NONE,

    val chatMode: ChatMode = ChatMode.QUICK,
    val enableTools: Boolean = true,
    val mcpServers: List<MCPConfig> = emptyList(),
    val mcpDisclaimerAcknowledged: Boolean = false,
    val skills: List<Skill> = emptyList(), // Changed to Skill to be serializable safely
    val savedPresets: List<SavedModelPreset> = emptyList(),
    val activePresetId: String? = null,

    val model: String = "",
    val quickSystemInstruction: String = "You are Oorty, a helpful and concise AI assistant.",
    val thinkingSystemInstruction: String = "You are Oorty, an elite AI. Think step-by-step and show your reasoning.",
    val codingSystemInstruction: String = "You are Oorty, a world-class software architect. Write exceptionally clean, robust, secure, and highly optimized code.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val maxTokens: Int = 4096,

    val enableHistory: Boolean = true,
    val showPerformanceStats: Boolean = true,
    val historyLimit: Int = 50,
    val autoGenerateTitle: Boolean = true,

    val googleApiKey: String = "",
    val openaiApiKey: String = "",
    val anthropicApiKey: String = "",
    val xaiApiKey: String = "",
    val groqApiKey: String = "",
    val openRouterApiKey: String = "",
    val nvidiaApiKey: String = "",
    val rosetteApiKey: String = "",

    val localBaseUrl: String = "http://localhost:11434/v1",
    val customApiKey: String = "",
    val ggufModelUri: String = "",
    val ggufModelPath: String = ""
) {
    val currentSystemInstruction: String
        get() = when (chatMode) {
            ChatMode.QUICK -> quickSystemInstruction
            ChatMode.THINKING -> thinkingSystemInstruction
            ChatMode.CODING -> codingSystemInstruction
        }
}

@Serializable data class GgufModelInfo(
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val estimatedParamsBillion: Float = 1.0f,
    val quantType: String = "Q4_K_M"
)

@Serializable data class Skill(val id: String, val name: String, val content: String)


enum class ModelIconType {
    TEXT, CODE, AGENT, IMAGE, VIDEO, AUTO, LIGHTNING
}

data class ModelOption(val id: String, val label: String, val icon: ModelIconType = ModelIconType.TEXT)

// `ModelIconType.IMAGE` above is a UI category (speed/tier/specialty), not a
// reliable capability flag — most flagship models across every provider
// here (Gemini, GPT, Claude, Grok, etc.) actually DO support image input
// despite being tagged LIGHTNING/AGENT/CODE for speed instead. Rather than
// try to maintain an allowlist of "known vision models" that goes stale the
// moment a provider ships a new one, this defaults to assuming vision
// support and only flags the handful of model families that are clearly
// NOT multimodal (audio transcription, embeddings, moderation, TTS). It's
// used purely to show a soft warning before sending, never to block the
// attach button or the send action — we genuinely can't be certain, and
// the provider's own error response is still the ground truth.
private val KNOWN_NON_VISION_PATTERNS = listOf("whisper", "embed", "moderation", "-tts", "rerank")

fun isLikelyVisionCapable(modelId: String): Boolean {
    val lower = modelId.lowercase()
    return KNOWN_NON_VISION_PATTERNS.none { lower.contains(it) }
}

data class ProviderInfo(val displayName: String, val defaultUrl: String, val apiLink: String = "", val popularModels: List<ModelOption> = emptyList())

/** Mirrors PROVIDER_CONFIG from constants.ts */
val PROVIDER_CONFIG: Map<ModelProvider, ProviderInfo> = mapOf(
    ModelProvider.NONE to ProviderInfo(
        "Select Provider", 
        "", 
        "",
        emptyList()
    ),
    ModelProvider.ROSETTE to ProviderInfo(
        "Rosette API", 
        "https://api.rosette.com/rest/v1/", 
        "https://developer.rosette.com/",
        listOf(
            ModelOption("rosette-language", "Language Identification", ModelIconType.TEXT),
            ModelOption("rosette-entities", "Entity Extraction", ModelIconType.AGENT),
            ModelOption("rosette-sentiment", "Sentiment Analysis", ModelIconType.TEXT),
            ModelOption("rosette-morphology", "Morphological Analysis", ModelIconType.CODE)
        )
    ),
    ModelProvider.GOOGLE to ProviderInfo(
        "Google Gemini", 
        "https://generativelanguage.googleapis.com", 
        "https://aistudio.google.com/app/apikey", 
        listOf(
            ModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", ModelIconType.LIGHTNING),
            ModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", ModelIconType.AGENT),
            ModelOption("gemini-3.1-flash-live", "Gemini 3.1 Flash Live", ModelIconType.LIGHTNING),
            ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", ModelIconType.LIGHTNING),
            ModelOption("gemma-4-31b", "Gemma 4 31B", ModelIconType.CODE)
        )
    ),
    ModelProvider.OPENAI to ProviderInfo(
        "OpenAI", 
        "https://api.openai.com/v1", 
        "https://platform.openai.com/api-keys",
        listOf(
            ModelOption("gpt-5.4-thinking", "GPT-5.4 Thinking", ModelIconType.AGENT),
            ModelOption("gpt-5.4-pro", "GPT-5.4 Pro", ModelIconType.CODE),
            ModelOption("gpt-5.4-standard", "GPT-5.4 (Standard)", ModelIconType.AUTO),
            ModelOption("gpt-5.4-mini", "GPT-5.4 Mini / Nano", ModelIconType.LIGHTNING),
            ModelOption("gpt-oss-120b", "gpt-oss-120b", ModelIconType.CODE)
        )
    ),
    ModelProvider.ANTHROPIC to ProviderInfo(
        "Anthropic Claude", 
        "https://api.anthropic.com/v1", 
        "https://console.anthropic.com/settings/keys",
        listOf(
            ModelOption("claude-fable-5", "Claude Fable 5", ModelIconType.AGENT),
            ModelOption("claude-opus-4.8", "Claude Opus 4.8", ModelIconType.TEXT),
            ModelOption("claude-sonnet-4.6", "Claude Sonnet 4.6", ModelIconType.CODE),
            ModelOption("claude-haiku-4.5", "Claude Haiku 4.5", ModelIconType.LIGHTNING)
        )
    ),
    ModelProvider.XAI to ProviderInfo(
        "xAI Grok", 
        "https://api.x.ai/v1", 
        "https://console.x.ai/",
        listOf(
            ModelOption("grok-4.3", "Grok 4.3", ModelIconType.AGENT),
            ModelOption("grok-4.20-reasoning", "Grok 4.20 Reasoning", ModelIconType.AGENT),
            ModelOption("grok-build-0.1", "grok-build-0.1", ModelIconType.CODE),
            ModelOption("grok-4.20-non-reasoning", "Grok 4.20 Non-Reasoning", ModelIconType.LIGHTNING),
            ModelOption("grok-1.5-vision", "Grok 1.5 Vision", ModelIconType.IMAGE)
        )
    ),
    ModelProvider.GROQ to ProviderInfo(
        "Groq", 
        "https://api.groq.com/openai/v1", 
        "https://console.groq.com/keys",
        listOf(
            ModelOption("llama-4-scout-17b", "Llama 4 Scout 17B", ModelIconType.LIGHTNING),
            ModelOption("llama-3.3-70b", "Llama 3.3 70B", ModelIconType.LIGHTNING),
            ModelOption("qwen-3-32b", "Qwen 3 32B", ModelIconType.CODE),
            ModelOption("kimi-k2-instruct", "Kimi K2 Instruct", ModelIconType.AGENT),
            ModelOption("whisper-large-v3", "Whisper Large V3", ModelIconType.VIDEO)
        )
    ),
    ModelProvider.OPENROUTER to ProviderInfo(
        "OpenRouter", 
        "https://openrouter.ai/api/v1", 
        "https://openrouter.ai/keys",
        listOf(
            ModelOption("owl-alpha", "Owl Alpha", ModelIconType.AGENT),
            ModelOption("nex-n2-pro", "Nex-N2-Pro", ModelIconType.AGENT),
            ModelOption("laguna-m.1", "Laguna M.1 (Poolside)", ModelIconType.CODE),
            ModelOption("laguna-xs.2", "Laguna XS.2 (Poolside)", ModelIconType.CODE),
            ModelOption("deepseek-v4-flash", "DeepSeek V4 Flash", ModelIconType.LIGHTNING)
        )
    ),
    ModelProvider.NVIDIA to ProviderInfo(
        "NVIDIA NIM", 
        "https://integrate.api.nvidia.com/v1", 
        "https://build.nvidia.com/explore/discover",
        listOf(
            ModelOption("nemotron-3-super-120b", "Nemotron 3 Super 120B", ModelIconType.AGENT),
            ModelOption("nemotron-3-ultra-550b", "Nemotron 3 Ultra 550B", ModelIconType.AGENT),
            ModelOption("nemotron-3-nano-omni-30b", "Nemotron 3 Nano Omni 30B", ModelIconType.IMAGE),
            ModelOption("nemotron-nano-12b-v2-vl", "Nemotron Nano 12B v2 VL", ModelIconType.VIDEO),
            ModelOption("cosmos3-nano", "Cosmos3-Nano", ModelIconType.VIDEO)
        )
    ),
    ModelProvider.LOCAL to ProviderInfo("Local Server", "http://localhost:11434/v1"),
    ModelProvider.CUSTOM to ProviderInfo("Custom Endpoint", ""),
    ModelProvider.LOCAL_GGUF to ProviderInfo(
        "Local GGUF",
        "",
        "",
        listOf(
            ModelOption("Qwen/Qwen2.5-0.5B-Instruct-GGUF", "Qwen 2.5 0.5B (0.4 GB)", ModelIconType.LIGHTNING),
            ModelOption("meta-llama/Llama-3.2-1B-Instruct-GGUF", "Llama 3.2 1B (0.7 GB)", ModelIconType.AGENT),
            ModelOption("google/gemma-2-2b-it-GGUF", "Gemma 2 2B (1.5 GB)", ModelIconType.AGENT),
            ModelOption("microsoft/Phi-3-mini-4k-instruct-gguf", "Phi 3 Mini 3.8B (2.2 GB)", ModelIconType.CODE)
        )
    )
)

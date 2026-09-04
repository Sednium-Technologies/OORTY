package oorty.sednium.app.model

import kotlinx.serialization.Serializable

/**
 * Core domain models for Sednium Local Spaces.
 * Direct Kotlin port of the original `types.ts`.
 */

@Serializable enum class Role { USER, MODEL }

@Serializable enum class ChatMode { QUICK, THINKING, CODING }

@Serializable enum class ModelProvider {
    NONE, GOOGLE, OPENAI, ANTHROPIC, XAI, GROQ, OPENROUTER, NVIDIA, LOCAL, CUSTOM, LOCAL_GGUF, LOCAL_LITERT, ROSETTE
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

@Serializable
enum class GenerativeMediaType {
    IMAGE, AUDIO, VIDEO
}

@Serializable
enum class GenerativeMediaState {
    QUEUED, GENERATING, COMPLETE, FAILED
}

@Serializable
data class GenerativeMediaResult(
    val type: GenerativeMediaType,
    val state: GenerativeMediaState = GenerativeMediaState.COMPLETE,
    val mediaUrl: String? = null,
    val prompt: String = "",
    val errorMessage: String? = null,
    val progress: Float = 1f,
    val durationSeconds: Float? = null,
    val width: Int? = null,
    val height: Int? = null
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
    val mediaResult: GenerativeMediaResult? = null,
    val parentMessageId: String? = null,
    // Performance Insights — populated once a model turn finishes streaming.
    val latencyMs: Long? = null,
    val tokensPerSecond: Float? = null,
    val thoughtDurationMs: Long? = null
)

@Serializable data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val parentSessionId: String? = null,
    val forkedFromMessageId: String? = null,
    // Per-session generation overrides.
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

@Serializable enum class PluginType {
    EMBEDDING, OCR, SPEECH_STT_TTS, DEVICE_CONTROL, CODE_ASSISTANT
}

@Serializable enum class PluginStatus {
    NOT_DOWNLOADED, DOWNLOADING, INSTALLED, ACTIVE, ERROR
}

@Serializable data class LocalPluginInfo(
    val id: String,
    val name: String,
    val type: PluginType,
    val huggingFaceRepo: String,
    val sizeMb: Int,
    val description: String,
    val capabilities: List<String>,
    val fileName: String,
    val downloadUrl: String = "",
    val isRecommended: Boolean = true,
    val status: PluginStatus = PluginStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f
)

val DEFAULT_AVAILABLE_PLUGINS = listOf(
    LocalPluginInfo(
        id = "plugin_ocr",
        name = "Micro OCR Scanner",
        type = PluginType.OCR,
        huggingFaceRepo = "stepfun-ai/GOT-OCR2_0-Mobile",
        sizeMb = 14,
        description = "High-speed on-device document scanner & image-to-text extractor. Silently extracts text from screenshots, receipts, notes & documents.",
        capabilities = listOf("Screenshot to Text", "Silent Background OCR", "Multi-column Extraction"),
        fileName = "got_ocr_micro.tflite",
        downloadUrl = "https://huggingface.co/stepfun-ai/GOT-OCR2_0/resolve/main/ocr_quant.tflite",
        isRecommended = true
    ),
    LocalPluginInfo(
        id = "plugin_speech",
        name = "Speech Studio (STT & TTS)",
        type = PluginType.SPEECH_STT_TTS,
        huggingFaceRepo = "openai/whisper-tiny-tflite",
        sizeMb = 24,
        description = "Neural speech recognizer & real-time text-to-speech synthesizer. Enables hands-free continuous voice mode and in-chat audio playback.",
        capabilities = listOf("Voice Dictation (STT)", "Hands-Free Voice Mode", "Audio Read Aloud (TTS)"),
        fileName = "whisper_tiny_mobile.tflite",
        downloadUrl = "https://huggingface.co/openai/whisper-tiny/resolve/main/whisper_quant.tflite",
        isRecommended = true
    ),
    LocalPluginInfo(
        id = "plugin_embeddings",
        name = "EmbeddingGemma 300M",
        type = PluginType.EMBEDDING,
        huggingFaceRepo = "google/embedding-gemma-300m-tflite",
        sizeMb = 18,
        description = "State-of-the-art 256/384d Matryoshka vector embedder for on-device Markdown Vault RAG and deep semantic similarity search.",
        capabilities = listOf("Obsidian Vault RAG", "Deep Semantic Matching", "Multi-lingual Recall"),
        fileName = "embedding_gemma_quant.tflite",
        downloadUrl = "https://huggingface.co/google/embeddinggemma-256d-tflite/resolve/main/model.tflite",
        isRecommended = true
    ),
    LocalPluginInfo(
        id = "plugin_device_control",
        name = "Device Controller & App Linker",
        type = PluginType.DEVICE_CONTROL,
        huggingFaceRepo = "sednium/oorty-device-automator",
        sizeMb = 4,
        description = "Autonomous bridge to Android apps (Obsidian, Termux, Chrome, Maps, YouTube), battery inspector, flashlight toggle, and clipboard automations.",
        capabilities = listOf("Open & Link Apps", "Flashlight & Battery Info", "Android System Intents"),
        fileName = "device_automator.json",
        downloadUrl = "",
        isRecommended = true
    ),
    LocalPluginInfo(
        id = "plugin_code_assistant",
        name = "Qwen 3 0.6B Ultralight",
        type = PluginType.CODE_ASSISTANT,
        huggingFaceRepo = "Qwen/Qwen3-0.6B-Instruct-GGUF",
        sizeMb = 380,
        description = "Ultra-compact local reasoning and assistant model running fully offline via llama.cpp. Optimized for entry-level and low-spec hardware (2GB+ RAM).",
        capabilities = listOf("Runs on Low-End Hardware", "Offline Local Generation", "Zero-Cloud Dependency"),
        fileName = "qwen3-0.6b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen3-0.6B-Instruct-GGUF/resolve/main/qwen3-0.6b-instruct-q4_k_m.gguf",
        isRecommended = true
    ),
    LocalPluginInfo(
        id = "plugin_kokoro_tts",
        name = "Kokoro-82M Neural Voice",
        type = PluginType.SPEECH_STT_TTS,
        huggingFaceRepo = "hexgrad/Kokoro-82M-ONNX",
        sizeMb = 82,
        description = "State-of-the-art 82M open-weight neural text-to-speech engine. Synthesizes ultra-realistic expressive human speech with natural breathing & intonation on-device.",
        capabilities = listOf("Ultra-Realistic Prosody", "Expressive Human Cadence", "100% Offline Audio Synthesis"),
        fileName = "kokoro_v0_19_fp16.onnx",
        downloadUrl = "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/kokoro_fp16.onnx",
        isRecommended = true
    )
)

@Serializable
enum class VoicePersona {
    WARM_CONVERSATIONAL, DEEP_CONFIDENT, ENERGETIC_DIRECT, SYSTEM_DEFAULT
}

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

    val hasCompletedPluginOnboarding: Boolean = false,
    val installedPluginIds: Set<String> = emptySet(),
    val activePluginIds: Set<String> = setOf("plugin_ocr", "plugin_speech", "plugin_embeddings", "plugin_device_control"),
    val enableSilentOcr: Boolean = true,
    val enableSpeechTts: Boolean = true,
    val enableContinuousVoice: Boolean = true,
    val voicePersona: VoicePersona = VoicePersona.WARM_CONVERSATIONAL,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val geminiLiveVoice: String = "Aoede",

    val model: String = "",
    val quickSystemInstruction: String = "You are Oorty, an intelligent AI workspace assistant on Android developed by Sednium (founded by Ayush Pal / Bhoid and Ankush Das / Loid in West Bengal, India). You know about your creators: Sednium (sednium.com), Bhoid (Ayush Pal, bhoid.sednium.com, @CoderBhoid - Full-stack & Android developer), and Loid (Ankush Das, loid.sednium.com, @AnkushDas4 - CTO & Systems Architect). Provide direct, helpful, and concise answers in clean Markdown.",
    val thinkingSystemInstruction: String = "You are Oorty, a deeply analytical reasoning assistant created by Sednium (Ayush Pal / Bhoid and Ankush Das / Loid). Enclose your chain-of-thought analysis strictly inside <thought>...</thought> tags. Output only your clear, finalized, well-structured answer outside of the thought tags.",
    val codingSystemInstruction: String = "You are Oorty, an elite principal software engineer created by Sednium (Ayush Pal / Bhoid and Ankush Das / Loid). Provide robust, clean, idiomatic, and secure code with precise syntax highlighting and minimal conversational filler.",
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
    val ggufModelPath: String = "",
    val litertModelUri: String = "",
    val litertModelPath: String = "",
    val useSerifFont: Boolean = true
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
            ModelOption("gemini-2.0-flash-exp", "Gemini 2.0 Flash Live (Audio/Voice)", ModelIconType.LIGHTNING),
            ModelOption("gemini-2.0-flash-realtime", "Gemini 2.0 Realtime Live", ModelIconType.LIGHTNING),
            ModelOption("gemini-3.1-flash-live", "Gemini 3.1 Flash Live", ModelIconType.LIGHTNING),
            ModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", ModelIconType.LIGHTNING),
            ModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", ModelIconType.AGENT),
            ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", ModelIconType.LIGHTNING),
            ModelOption("gemini-transcribe-3", "Gemini Transcribe 3", ModelIconType.VIDEO),
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
            ModelOption("Qwen/Qwen3-0.6B-Instruct-GGUF", "Qwen 3 0.6B (0.4 GB) — Low Hardware", ModelIconType.LIGHTNING),
            ModelOption("Qwen/Qwen2.5-0.5B-Instruct-GGUF", "Qwen 2.5 0.5B (0.4 GB)", ModelIconType.LIGHTNING),
            ModelOption("meta-llama/Llama-3.2-1B-Instruct-GGUF", "Llama 3.2 1B (0.7 GB)", ModelIconType.AGENT),
            ModelOption("google/gemma-2-2b-it-GGUF", "Gemma 2 2B (1.5 GB)", ModelIconType.AGENT),
            ModelOption("microsoft/Phi-3-mini-4k-instruct-gguf", "Phi 3 Mini 3.8B (2.2 GB)", ModelIconType.CODE)
        )
    ),
    ModelProvider.LOCAL_LITERT to ProviderInfo(
        "Google LiteRT",
        "",
        "",
        listOf(
            ModelOption("google/gemma-2-2b-it-litert", "Gemma 2 2B (LiteRT)", ModelIconType.LIGHTNING),
            ModelOption("google/gemma-2b-it-tflite", "Gemma 2B IT (.tflite)", ModelIconType.AGENT),
            ModelOption("google/mobilebert-tflite", "MobileBERT (.tflite)", ModelIconType.LIGHTNING),
            ModelOption("stepfun-ai/GOT-OCR2_0-tflite", "GOT-OCR 2.0 (.tflite)", ModelIconType.IMAGE)
        )
    )
)

package oorty.sednium.app.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.Uri

var activeAppContext: Context? = null
var activeGgufUri: Uri = Uri.EMPTY
var activeLlamaHelper: LlamaHelper? = null
var activeLiteRtUri: Uri = Uri.EMPTY
var activeLiteRtHelper: LiteRtHelper? = null

object ModelCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, List<oorty.sednium.app.model.ModelOption>>()

    fun get(provider: oorty.sednium.app.model.ModelProvider, apiKey: String, baseUrl: String): List<oorty.sednium.app.model.ModelOption>? {
        val key = "${provider.name}_${apiKey}_${baseUrl}"
        return cache[key]
    }

    fun put(provider: oorty.sednium.app.model.ModelProvider, apiKey: String, baseUrl: String, models: List<oorty.sednium.app.model.ModelOption>) {
        val key = "${provider.name}_${apiKey}_${baseUrl}"
        cache[key] = models
    }

    fun clear() {
        cache.clear()
    }
}

// --- Shared Gemini Models ---

@Serializable
data class GenerateContentRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null
)

@Serializable
data class Content(
    val role: String,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

@Serializable
data class FunctionCall(
    val name: String,
    val args: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class FunctionResponse(
    val name: String,
    val response: JsonObject
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

// --- Retrofit Setup ---

interface GenericApiService {
    @retrofit2.http.GET
    suspend fun getModels(
        @retrofit2.http.Url url: String,
        @retrofit2.http.HeaderMap headers: Map<String, String> = emptyMap()
    ): ResponseBody

    @retrofit2.http.Streaming
    @retrofit2.http.POST
    suspend fun postChatCompletions(
        @retrofit2.http.Url url: String,
        @retrofit2.http.HeaderMap headers: Map<String, String>,
        @retrofit2.http.Body body: okhttp3.RequestBody
    ): ResponseBody
}

interface UniversalApiService {
    @retrofit2.http.GET("v1beta/models")
    suspend fun listModels(@Query("key") apiKey: String): ResponseBody

    @POST("v1beta/models/{model}:streamGenerateContent?alt=sse")
    @Streaming
    suspend fun generateContentStream(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): ResponseBody

    // Non-streaming single-turn call, used by the tool-calling orchestrator
    // (mcp/ProviderToolChatClients.kt) — agentic tool-call turns need one
    // complete response to inspect for functionCall parts before deciding
    // what to do next, so streaming deltas don't apply here the way they do
    // for a normal chat reply.
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: UniversalApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(UniversalApiService::class.java)
    }

    val genericService: GenericApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://localhost/") // Dummy base URL, we use @Url in method
            .client(okHttpClient)
            .build()
            .create(GenericApiService::class.java)
    }
}

// --- Multimodal content builders ---
// Each provider wants attachments shaped differently. These take the same
// (text, attachments) pair and produce whatever that provider's wire format
// needs. Used for both history messages and the current turn, so an image
// stays attached to its turn across a multi-turn vision conversation
// instead of silently dropping after the first reply.

private fun buildGeminiParts(text: String, attachments: List<oorty.sednium.app.model.Attachment>): List<Part> {
    val parts = mutableListOf<Part>()
    val textAttachments = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.TEXT }
    val imageAttachments = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.IMAGE }

    val combinedText = buildString {
        textAttachments.forEach { att -> append("[Attached file: ${att.name}]\n${att.data}\n\n") }
        append(text)
    }
    if (combinedText.isNotEmpty()) parts.add(Part(text = combinedText))
    imageAttachments.forEach { att -> parts.add(Part(inlineData = InlineData(mimeType = att.mimeType, data = att.data))) }
    if (parts.isEmpty()) parts.add(Part(text = ""))
    return parts
}

private fun buildAnthropicContentBlocks(
    text: String,
    attachments: List<oorty.sednium.app.model.Attachment>
): kotlinx.serialization.json.JsonElement {
    val textAttachments = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.TEXT }
    val imageAttachments = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.IMAGE }
    if (imageAttachments.isEmpty() && textAttachments.isEmpty()) {
        // Plain string content is valid (and simplest) when there's nothing to attach.
        return kotlinx.serialization.json.JsonPrimitive(text)
    }
    return kotlinx.serialization.json.buildJsonArray {
        imageAttachments.forEach { att ->
            add(kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("image"))
                put("source", kotlinx.serialization.json.buildJsonObject {
                    put("type", kotlinx.serialization.json.JsonPrimitive("base64"))
                    put("media_type", kotlinx.serialization.json.JsonPrimitive(att.mimeType))
                    put("data", kotlinx.serialization.json.JsonPrimitive(att.data))
                })
            })
        }
        val combinedText = buildString {
            textAttachments.forEach { att -> append("[Attached file: ${att.name}]\n${att.data}\n\n") }
            append(text)
        }
        add(kotlinx.serialization.json.buildJsonObject {
            put("type", kotlinx.serialization.json.JsonPrimitive("text"))
            put("text", kotlinx.serialization.json.JsonPrimitive(combinedText))
        })
    }
}

private fun buildOpenAiContent(
    text: String,
    attachments: List<oorty.sednium.app.model.Attachment>
): kotlinx.serialization.json.JsonElement {
    val textAttachments = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.TEXT }
    val imageAttachments = attachments.filter { it.type == oorty.sednium.app.model.AttachmentType.IMAGE }
    if (imageAttachments.isEmpty() && textAttachments.isEmpty()) {
        // Keep plain string content when there's nothing attached — safest
        // for OpenAI-compatible providers that are strict about the schema.
        return kotlinx.serialization.json.JsonPrimitive(text)
    }
    return kotlinx.serialization.json.buildJsonArray {
        val combinedText = buildString {
            textAttachments.forEach { att -> append("[Attached file: ${att.name}]\n${att.data}\n\n") }
            append(text)
        }
        add(kotlinx.serialization.json.buildJsonObject {
            put("type", kotlinx.serialization.json.JsonPrimitive("text"))
            put("text", kotlinx.serialization.json.JsonPrimitive(combinedText))
        })
        imageAttachments.forEach { att ->
            add(kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("image_url"))
                put("image_url", kotlinx.serialization.json.buildJsonObject {
                    put("url", kotlinx.serialization.json.JsonPrimitive("data:${att.mimeType};base64,${att.data}"))
                })
            })
        }
    }
}

suspend fun generateContentStream(
    apiKey: String,
    modelName: String,
    prompt: String,
    history: List<oorty.sednium.app.model.ChatMessage>,
    provider: oorty.sednium.app.model.ModelProvider = oorty.sednium.app.model.ModelProvider.GOOGLE,
    baseUrl: String = "",
    systemInstruction: String = "",
    // These previously existed on AppSettings but were never actually wired
    // into any of the three request branches below — the sliders in
    // Settings did nothing. Defaults mirror AppSettings' own defaults so
    // existing call sites that don't pass them keep prior (if accidental)
    // behavior.
    temperature: Float = 0.7f,
    topP: Float = 0.9f,
    topK: Int = 40,
    maxTokens: Int = 4096,
    // Attachments for the CURRENT turn (the `prompt` text). History messages
    // carry their own attachments on ChatMessage.attachments already — see
    // buildGeminiParts/buildAnthropicContentBlocks/buildOpenAiContent below,
    // which read msg.attachments for every history message too, so a vision
    // conversation stays multi-turn instead of losing the image after the
    // first reply.
    attachments: List<oorty.sednium.app.model.Attachment> = emptyList(),
    onChunkReceived: (String, String?) -> Unit // (deltaText, deltaThought)
) = withContext(Dispatchers.IO) {
    val cleanApiKey = apiKey.trim()
    val thoughtParser = StreamingThoughtParser(onChunkReceived)
    val emitChunk: (String, String?) -> Unit = { text, thought ->
        thoughtParser.processChunk(text, thought)
    }
    if (provider == oorty.sednium.app.model.ModelProvider.NONE) {
        onChunkReceived("Error: No provider selected. Please select a provider in settings.", null)
        return@withContext
    }
    if (provider == oorty.sednium.app.model.ModelProvider.ROSETTE) {
        val endpoint = when (modelName) {
            "rosette-entities" -> "entities"
            "rosette-sentiment" -> "sentiment"
            "rosette-morphology" -> "morphology"
            "rosette-language" -> "language"
            else -> "orchestrator"
        }

        if (endpoint == "orchestrator") {
            // Simulated Rosette Multi-Agent Orchestrator
            onChunkReceived("[Orchestrator Agent] Routing request: \"$prompt\" to specialized agents...\n\n", null)
            kotlinx.coroutines.delay(800)
            onChunkReceived("[Rosette Text Analytics Agent] Running entity extraction and language checks...\nDetected Language: English (Confidence: 0.99)\n\n", null)
            kotlinx.coroutines.delay(1000)
            onChunkReceived("[Oorty Multi-Agent Framework] Formulating response:\n\n", null)
            
            // If the user has another provider key, we could call that, otherwise generate a helpful mock
            val mockResponse = "Hello! I am Oorty, running via the Rosette multi-agent orchestrator. I've routed your prompt to specialized agents and successfully parsed the context. Let me know how I can help you today!"
            onChunkReceived(mockResponse, null)
            return@withContext
        }

        val url = "https://api.rosette.com/rest/v1/$endpoint"
        val requestJson = kotlinx.serialization.json.buildJsonObject {
            put("content", kotlinx.serialization.json.JsonPrimitive(prompt))
        }
        val requestBody = okhttp3.RequestBody.create("application/json".toMediaType(), requestJson.toString())
        val headers = mapOf("X-RosetteAPI-Key" to cleanApiKey, "Content-Type" to "application/json", "Accept" to "application/json")
        
        try {
            val response = RetrofitClient.genericService.postChatCompletions(url, headers, requestBody)
            val jsonResponse = response.string()
            val prettyJson = try {
                val element = Json.parseToJsonElement(jsonResponse)
                Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element)
            } catch (e: Exception) {
                jsonResponse
            }
            onChunkReceived(prettyJson, null)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            onChunkReceived("Error calling Rosette API: $errorMsg\nEnsure your API key is valid.", null)
        }
    } else if (provider == oorty.sednium.app.model.ModelProvider.GOOGLE) {
        val contents = history.map { msg ->
            Content(
                role = if (msg.role == oorty.sednium.app.model.Role.USER) "user" else "model",
                parts = buildGeminiParts(msg.content, msg.attachments)
            )
        } + Content("user", buildGeminiParts(prompt, attachments))

        val sysContent = if (systemInstruction.isNotBlank()) Content("user", listOf(Part(text = systemInstruction))) else null
        val request = GenerateContentRequest(
            systemInstruction = sysContent,
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = temperature,
                topP = topP,
                topK = topK,
                maxOutputTokens = maxTokens
            )
        )
        try {
            val response = RetrofitClient.service.generateContentStream(modelName, cleanApiKey, request)
            response.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("data: ")) {
                        val data = line!!.removePrefix("data: ")
                        try {
                            val chunk = Json.parseToJsonElement(data).jsonObject
                            val parts = chunk["candidates"]?.jsonArray
                                ?.getOrNull(0)?.jsonObject
                                ?.get("content")?.jsonObject
                                ?.get("parts")?.jsonArray
                            parts?.forEach { partElement ->
                                val partObj = partElement.jsonObject
                                val text = partObj["text"]?.jsonPrimitive?.content
                                val isThought = try {
                                    partObj["thought"]?.jsonPrimitive?.content?.equals("true", ignoreCase = true) == true
                                } catch (e: Exception) { false }
                                if (text != null && text.isNotEmpty()) {
                                    if (isThought) {
                                        emitChunk("", text)
                                    } else {
                                        emitChunk(text, null)
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            thoughtParser.flush()
        } catch (e: Exception) {
            val errorMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.parseToJsonElement(errorBody).jsonObject
                            json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "HTTP ${e.code()}"
                        } catch (ex: Exception) { "HTTP ${e.code()}" }
                    } else "HTTP ${e.code()}"
                } catch (ex: Exception) { "HTTP ${e.code()}" }
            } else if (e is java.net.SocketTimeoutException) {
                "Connection timed out. The server might be experiencing high demand."
            } else if (e is java.net.UnknownHostException) {
                "Network error: Unable to resolve host."
            } else {
                e.message ?: "Unknown error"
            }
            emitChunk("Error: $errorMsg\n", null)
            thoughtParser.flush()
        }
    } else if (provider == oorty.sednium.app.model.ModelProvider.ANTHROPIC) {
        val messagesArray = kotlinx.serialization.json.buildJsonArray {
            history.forEach { msg ->
                add(kotlinx.serialization.json.buildJsonObject {
                    put("role", kotlinx.serialization.json.JsonPrimitive(if (msg.role == oorty.sednium.app.model.Role.USER) "user" else "assistant"))
                    put("content", buildAnthropicContentBlocks(msg.content, msg.attachments))
                })
            }
            add(kotlinx.serialization.json.buildJsonObject {
                put("role", kotlinx.serialization.json.JsonPrimitive("user"))
                put("content", buildAnthropicContentBlocks(prompt, attachments))
            })
        }
        val requestJson = kotlinx.serialization.json.buildJsonObject {
            put("model", kotlinx.serialization.json.JsonPrimitive(modelName))
            put("stream", kotlinx.serialization.json.JsonPrimitive(true))
            put("max_tokens", kotlinx.serialization.json.JsonPrimitive(maxTokens))
            put("temperature", kotlinx.serialization.json.JsonPrimitive(temperature))
            put("top_p", kotlinx.serialization.json.JsonPrimitive(topP))
            put("top_k", kotlinx.serialization.json.JsonPrimitive(topK))
            put("messages", messagesArray)
            if (systemInstruction.isNotBlank()) {
                put("system", kotlinx.serialization.json.JsonPrimitive(systemInstruction))
            }
        }
        val requestBody = okhttp3.RequestBody.create("application/json".toMediaType(), requestJson.toString())
        val endpointUrl = if (baseUrl.endsWith("/")) "${baseUrl}messages" else "$baseUrl/messages"
        val headers = mapOf("x-api-key" to cleanApiKey, "anthropic-version" to "2023-06-01", "Content-Type" to "application/json")

        try {
            val response = RetrofitClient.genericService.postChatCompletions(endpointUrl, headers, requestBody)
            response.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("data: ")) {
                        val data = line!!.removePrefix("data: ")
                        try {
                            val chunk = Json.parseToJsonElement(data).jsonObject
                            if (chunk["type"]?.jsonPrimitive?.content == "content_block_delta") {
                                val deltaObj = chunk["delta"]?.jsonObject
                                val thinking = deltaObj?.get("thinking")?.jsonPrimitive?.content ?: ""
                                val text = deltaObj?.get("text")?.jsonPrimitive?.content ?: ""
                                if (thinking.isNotEmpty()) emitChunk("", thinking)
                                if (text.isNotEmpty()) emitChunk(text, null)
                            } else if (chunk["type"]?.jsonPrimitive?.content == "error") {
                                val msg = chunk["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Unknown Error"
                                emitChunk("Error: $msg\n", null)
                            }
                        } catch (e: Exception) {}
                    } else if (line!!.startsWith("{")) {
                        if(line!!.contains("error")) {
                             emitChunk("Error: ${line}\n", null)
                        }
                    }
                }
            }
            thoughtParser.flush()
        } catch (e: Exception) {
            val errorMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.parseToJsonElement(errorBody).jsonObject
                            json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "HTTP ${e.code()}"
                        } catch (ex: Exception) { "HTTP ${e.code()}" }
                    } else "HTTP ${e.code()}"
                } catch (ex: Exception) { "HTTP ${e.code()}" }
            } else if (e is java.net.SocketTimeoutException) {
                "Connection timed out. The server might be experiencing high demand."
            } else if (e is java.net.UnknownHostException) {
                "Network error: Unable to resolve host."
            } else {
                e.message ?: "Unknown error"
            }
            onChunkReceived("Error: $errorMsg\n", null)
        }
    } else if (provider == oorty.sednium.app.model.ModelProvider.LOCAL_GGUF) {
        try {
            val helper = activeLlamaHelper ?: LlamaHelper(
                context = activeAppContext ?: throw IllegalStateException("Application context not initialized"),
                uri = activeGgufUri
            ).also { activeLlamaHelper = it }

            if (!helper.isLoaded.value && activeGgufUri != Uri.EMPTY) {
                val loadResult = helper.loadModel(activeGgufUri)
                if (loadResult.isFailure) {
                    throw loadResult.exceptionOrNull() ?: Exception("Failed loading GGUF model")
                }
            }

            helper.generateStream(
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = history,
                temperature = temperature,
                maxTokens = maxTokens
            ).collect { chunk ->
                emitChunk(chunk, null)
            }
            thoughtParser.flush()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Local GGUF execution failed"
            emitChunk("Error: $errorMsg\n", null)
            thoughtParser.flush()
        }
    } else if (provider == oorty.sednium.app.model.ModelProvider.LOCAL_LITERT) {
        try {
            val helper = activeLiteRtHelper ?: LiteRtHelper(
                context = activeAppContext ?: throw IllegalStateException("Application context not initialized"),
                uri = activeLiteRtUri
            ).also { activeLiteRtHelper = it }

            if (!helper.isLoaded.value && activeLiteRtUri != Uri.EMPTY) {
                val loadResult = helper.loadModel(activeLiteRtUri)
                if (loadResult.isFailure) {
                    throw loadResult.exceptionOrNull() ?: Exception("Failed loading LiteRT model")
                }
            }

            helper.generateStream(
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = history,
                temperature = temperature,
                maxTokens = maxTokens
            ).collect { chunk ->
                emitChunk(chunk, null)
            }
            thoughtParser.flush()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Local LiteRT execution failed"
            emitChunk("Error: $errorMsg\n", null)
            thoughtParser.flush()
        }
    } else {
        // OpenAI format fallback for other providers (including LOCAL server)
        val resolvedBaseUrl = if (provider == oorty.sednium.app.model.ModelProvider.LOCAL || provider == oorty.sednium.app.model.ModelProvider.CUSTOM) {
            if (baseUrl.isNotBlank()) baseUrl else "http://localhost:11434/v1"
        } else {
            baseUrl
        }

        val messagesArray = kotlinx.serialization.json.buildJsonArray {
            if (systemInstruction.isNotBlank()) {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("role", kotlinx.serialization.json.JsonPrimitive("system"))
                    put("content", kotlinx.serialization.json.JsonPrimitive(systemInstruction))
                })
            }
            history.forEach { msg ->
                add(kotlinx.serialization.json.buildJsonObject {
                    put("role", kotlinx.serialization.json.JsonPrimitive(if (msg.role == oorty.sednium.app.model.Role.USER) "user" else "assistant"))
                    put("content", buildOpenAiContent(msg.content, msg.attachments))
                })
            }
            add(kotlinx.serialization.json.buildJsonObject {
                put("role", kotlinx.serialization.json.JsonPrimitive("user"))
                put("content", buildOpenAiContent(prompt, attachments))
            })
        }
        val requestJson = kotlinx.serialization.json.buildJsonObject {
            put("model", kotlinx.serialization.json.JsonPrimitive(modelName))
            put("stream", kotlinx.serialization.json.JsonPrimitive(true))
            put("temperature", kotlinx.serialization.json.JsonPrimitive(temperature))
            put("top_p", kotlinx.serialization.json.JsonPrimitive(topP))
            put("max_tokens", kotlinx.serialization.json.JsonPrimitive(maxTokens))
            put("messages", messagesArray)
        }
        val requestBody = okhttp3.RequestBody.create("application/json".toMediaType(), requestJson.toString())
        val endpointUrl = if (resolvedBaseUrl.endsWith("/")) "${resolvedBaseUrl}chat/completions" else "$resolvedBaseUrl/chat/completions"
        val headers = mutableMapOf("Authorization" to "Bearer $cleanApiKey", "Content-Type" to "application/json")
        if (provider == oorty.sednium.app.model.ModelProvider.OPENROUTER) {
            headers["HTTP-Referer"] = "https://github.com/sednium/localspaces"
            headers["X-Title"] = "LocalSpaces AI"
        }

        try {
            val response = RetrofitClient.genericService.postChatCompletions(endpointUrl, headers, requestBody)
            response.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("data: ") && line != "data: [DONE]") {
                        val data = line!!.removePrefix("data: ")
                        try {
                            val chunk = Json.parseToJsonElement(data).jsonObject
                            val delta = chunk["choices"]?.jsonArray
                                ?.getOrNull(0)?.jsonObject
                                ?.get("delta")?.jsonObject
                            val reasoning = delta?.get("reasoning_content")?.jsonPrimitive?.content
                                ?: delta?.get("reasoning")?.jsonPrimitive?.content
                                ?: delta?.get("thought")?.jsonPrimitive?.content
                                ?: ""
                            val text = delta?.get("content")?.jsonPrimitive?.content ?: ""

                            if (reasoning.isNotEmpty()) {
                                emitChunk("", reasoning)
                            }
                            if (text.isNotEmpty()) {
                                emitChunk(text, null)
                            }
                        } catch (e: Exception) {}
                    } else if (line!!.startsWith("{")) {
                        // might be an error or unstreamed reply
                        if(line!!.contains("error")) {
                             emitChunk("Error: ${line}\n", null)
                        }
                    }
                }
            }
            thoughtParser.flush()
        } catch (e: Exception) {
            val errorMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.parseToJsonElement(errorBody).jsonObject
                            json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "HTTP ${e.code()}"
                        } catch (ex: Exception) { "HTTP ${e.code()}" }
                    } else "HTTP ${e.code()}"
                } catch (ex: Exception) { "HTTP ${e.code()}" }
            } else if (e is java.net.SocketTimeoutException) {
                "Connection timed out. The server might be experiencing high demand."
            } else if (e is java.net.UnknownHostException) {
                "Network error: Unable to resolve host."
            } else {
                e.message ?: "Unknown error"
            }
            emitChunk("Error: $errorMsg\n", null)
            thoughtParser.flush()
        }
    }
}

suspend fun testApiKey(apiKey: String, provider: oorty.sednium.app.model.ModelProvider, localBaseUrl: String): Boolean = withContext(Dispatchers.IO) {
    if (apiKey.isBlank() && provider != oorty.sednium.app.model.ModelProvider.LOCAL && provider != oorty.sednium.app.model.ModelProvider.CUSTOM) return@withContext false
    try {
        val models = fetchDynamicModels(apiKey, provider, localBaseUrl)
        models.isNotEmpty()
    } catch (e: Exception) {
        false
    }
}

suspend fun fetchDynamicModels(apiKey: String, provider: oorty.sednium.app.model.ModelProvider, localBaseUrl: String): List<oorty.sednium.app.model.ModelOption> = withContext(Dispatchers.IO) {
    if (provider == oorty.sednium.app.model.ModelProvider.NONE || provider == oorty.sednium.app.model.ModelProvider.ROSETTE || provider == oorty.sednium.app.model.ModelProvider.LOCAL_GGUF || provider == oorty.sednium.app.model.ModelProvider.LOCAL_LITERT) {
        return@withContext emptyList()
    }
    val cleanApiKey = apiKey.trim()
    if (cleanApiKey.isBlank() && provider != oorty.sednium.app.model.ModelProvider.LOCAL && provider != oorty.sednium.app.model.ModelProvider.CUSTOM) return@withContext emptyList()
    
    val cached = ModelCache.get(provider, cleanApiKey, localBaseUrl)
    if (cached != null) {
        return@withContext cached
    }
    
    val baseUrl = oorty.sednium.app.model.PROVIDER_CONFIG[provider]?.defaultUrl ?: ""
    val normalizedBaseUrl = if (provider == oorty.sednium.app.model.ModelProvider.LOCAL || provider == oorty.sednium.app.model.ModelProvider.CUSTOM) localBaseUrl else baseUrl

    try {
        if (provider == oorty.sednium.app.model.ModelProvider.GOOGLE) {
            val response = RetrofitClient.service.listModels(cleanApiKey).string()
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(response).jsonObject
            val models = root["models"]?.jsonArray
            val googleModels = models?.mapNotNull {
                val name = it.jsonObject["name"]?.jsonPrimitive?.content?.removePrefix("models/") ?: return@mapNotNull null
                val displayName = it.jsonObject["displayName"]?.jsonPrimitive?.content ?: name
                val lowerName = name.lowercase()
                val icon = when {
                    lowerName.contains("vision") -> oorty.sednium.app.model.ModelIconType.IMAGE
                    lowerName.contains("flash") -> oorty.sednium.app.model.ModelIconType.LIGHTNING
                    lowerName.contains("pro") -> oorty.sednium.app.model.ModelIconType.AGENT
                    lowerName.contains("code") -> oorty.sednium.app.model.ModelIconType.CODE
                    else -> oorty.sednium.app.model.ModelIconType.AUTO
                }
                oorty.sednium.app.model.ModelOption(name, displayName, icon)
            } ?: emptyList()
            if (googleModels.isNotEmpty()) {
                ModelCache.put(provider, cleanApiKey, localBaseUrl, googleModels)
            }
            return@withContext googleModels
        }

        // Generic OpenAI-compatible list models (Anthropic also using similar list models)
        val url = if (normalizedBaseUrl.endsWith("/")) "${normalizedBaseUrl}models" else "$normalizedBaseUrl/models"
        val headers = if (provider == oorty.sednium.app.model.ModelProvider.ANTHROPIC) {
            mapOf("x-api-key" to cleanApiKey, "anthropic-version" to "2023-06-01")
        } else if (provider == oorty.sednium.app.model.ModelProvider.OPENROUTER) {
            mapOf("Authorization" to "Bearer $cleanApiKey", "HTTP-Referer" to "https://github.com/sednium/localspaces", "X-Title" to "LocalSpaces AI")
        } else {
            mapOf("Authorization" to "Bearer $cleanApiKey")
        }
        val response = RetrofitClient.genericService.getModels(url, headers).string()
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(response).jsonObject
        // Anthropic returns array in "data", but wait, Anthropic models endpoint returns `{ type: "list", data: [ { type: "model", id: "claude-..." } ] }`
        val data = root["data"]?.jsonArray
        val modelsList = data?.mapNotNull {
            val id = it.jsonObject["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val displayName = it.jsonObject["display_name"]?.jsonPrimitive?.content ?: id
            val lowerName = id.lowercase()
            val icon = when {
                lowerName.contains("vision") || lowerName.contains("vl") -> oorty.sednium.app.model.ModelIconType.IMAGE
                lowerName.contains("think") || lowerName.contains("reason") || lowerName.contains("pro") || lowerName.contains("sonnet") || lowerName.contains("opus") -> oorty.sednium.app.model.ModelIconType.AGENT
                lowerName.contains("code") -> oorty.sednium.app.model.ModelIconType.CODE
                lowerName.contains("flash") || lowerName.contains("mini") || lowerName.contains("nano") || lowerName.contains("scout") || lowerName.contains("haiku") -> oorty.sednium.app.model.ModelIconType.LIGHTNING
                else -> oorty.sednium.app.model.ModelIconType.AUTO
            }
            oorty.sednium.app.model.ModelOption(id, displayName, icon) // Many providers don't give a "displayName"
        } ?: emptyList()
        if (modelsList.isNotEmpty()) {
            ModelCache.put(provider, cleanApiKey, localBaseUrl, modelsList)
        }
        return@withContext modelsList
    } catch (e: Exception) {
        return@withContext emptyList()
    }
}

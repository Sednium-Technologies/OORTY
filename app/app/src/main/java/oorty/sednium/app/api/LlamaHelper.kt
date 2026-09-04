package oorty.sednium.app.api

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.GgufModelInfo
import oorty.sednium.app.model.Role
import org.nehuatl.llamacpp.LlamaHelper as NativeLlamaHelper
import java.io.File

/**
 * Native llama.cpp GGUF runner for Oorty.
 * Directly interfaces with native llama.cpp bindings (librnllama) without mocks or simulated delays.
 */
class LlamaHelper(
    private val context: Context,
    private val uri: Uri? = null
) {
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _tokensPerSecond = MutableStateFlow(0f)
    val tokensPerSecond: StateFlow<Float> = _tokensPerSecond.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _modelInfo = MutableStateFlow<GgufModelInfo?>(null)
    val modelInfo: StateFlow<GgufModelInfo?> = _modelInfo.asStateFlow()

    private var nativeInstance: NativeLlamaHelper? = null
    private var nativeScope: CoroutineScope? = null
    private val sharedFlow = MutableSharedFlow<NativeLlamaHelper.LLMEvent>(extraBufferCapacity = 64)

    init {
        if (uri != null && uri != Uri.EMPTY) {
            parseUriInfo(uri)
        }
    }

    private fun parseUriInfo(targetUri: Uri) {
        val fileName = targetUri.lastPathSegment?.substringAfterLast("/") ?: "local_model.gguf"
        var fileSize = 0L
        try {
            if (targetUri.scheme == "content") {
                context.contentResolver.openFileDescriptor(targetUri, "r")?.use {
                    fileSize = it.statSize
                }
            } else {
                val file = File(targetUri.path ?: targetUri.toString())
                if (file.exists()) {
                    fileSize = file.length()
                }
            }
        } catch (e: Exception) {
            fileSize = 0L
        }

        val estimatedParams = when {
            fileSize > 3L * 1024 * 1024 * 1024 -> 3.8f
            fileSize > 1.5 * 1024 * 1024 * 1024 -> 2.0f
            fileSize > 600L * 1024 * 1024 -> 1.0f
            else -> 0.5f
        }

        _modelInfo.value = GgufModelInfo(
            fileName = fileName,
            fileSizeBytes = fileSize,
            estimatedParamsBillion = estimatedParams,
            quantType = if (fileName.contains("q4", ignoreCase = true)) "Q4_K_M" else "Q8_0"
        )
    }

    suspend fun loadModel(targetUri: Uri = uri ?: Uri.EMPTY): Result<Boolean> = withContext(Dispatchers.IO) {
        if (targetUri == Uri.EMPTY) {
            val err = "No valid GGUF model file URI provided"
            _errorMessage.value = err
            _isLoading.value = false
            _isLoaded.value = false
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        _isLoading.value = true
        _isLoaded.value = false
        _errorMessage.value = null
        _loadProgress.value = 0.05f

        try {
            parseUriInfo(targetUri)
            _loadProgress.value = 0.15f

            // Release any previously loaded model instance
            unloadModel()

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            nativeScope = scope

            val native = try {
                NativeLlamaHelper(
                    contentResolver = context.contentResolver,
                    scope = scope,
                    sharedFlow = sharedFlow
                )
            } catch (e: Throwable) {
                _isLoading.value = false
                _isLoaded.value = false
                val msg = "Native llama.cpp engine initialization failed: ${e.message}"
                _errorMessage.value = msg
                return@withContext Result.failure(Exception(msg, e))
            }
            nativeInstance = native

            val resolvedUriString = if (targetUri.scheme == null) {
                Uri.fromFile(File(targetUri.path ?: targetUri.toString())).toString()
            } else {
                targetUri.toString()
            }

            _loadProgress.value = 0.35f

            val loadDeferred = CompletableDeferred<Boolean>()
            val loadListenerJob = scope.launch {
                sharedFlow.collect { event ->
                    when (event) {
                        is NativeLlamaHelper.LLMEvent.Loaded -> {
                            _loadProgress.value = 1.0f
                            if (!loadDeferred.isCompleted) loadDeferred.complete(true)
                        }
                        is NativeLlamaHelper.LLMEvent.Error -> {
                            if (!loadDeferred.isCompleted) {
                                loadDeferred.completeExceptionally(Exception(event.message))
                            }
                        }
                        else -> {}
                    }
                }
            }

            native.load(
                path = resolvedUriString,
                contextLength = 2048,
                mmprojPath = null,
                loaded = { durationMs ->
                    _loadProgress.value = 1.0f
                    if (!loadDeferred.isCompleted) loadDeferred.complete(true)
                }
            )

            // Wait up to 120s for memory mapping and allocation
            try {
                withTimeout(120_000) {
                    loadDeferred.await()
                }
            } catch (e: Exception) {
                loadListenerJob.cancel()
                throw Exception("GGUF model allocation failed or timed out: ${e.message}", e)
            } finally {
                loadListenerJob.cancel()
            }

            _isLoaded.value = true
            _isLoading.value = false
            Result.success(true)
        } catch (e: Throwable) {
            _isLoaded.value = false
            _isLoading.value = false
            val msg = e.message ?: "Failed to load GGUF model into memory"
            _errorMessage.value = msg
            Result.failure(e)
        }
    }

    fun generateStream(
        prompt: String,
        systemInstruction: String = "",
        history: List<ChatMessage> = emptyList(),
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<String> = callbackFlow {
        val helper = nativeInstance
        if (helper == null || !_isLoaded.value) {
            val err = _errorMessage.value ?: "Local GGUF model is not loaded into RAM. Please load a model first."
            close(IllegalStateException(err))
            return@callbackFlow
        }

        // Format prompt using standard ChatML format for local instruction-tuned GGUF models
        val fullPrompt = buildString {
            if (systemInstruction.isNotBlank()) {
                append("<|im_start|>system\n")
                append(systemInstruction.trim())
                append("<|im_end|>\n")
            }
            history.forEach { msg ->
                val roleName = if (msg.role == Role.USER) "user" else "assistant"
                append("<|im_start|>$roleName\n")
                append(msg.content.trim())
                append("<|im_end|>\n")
            }
            append("<|im_start|>user\n")
            append(prompt.trim())
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }

        val startTime = System.currentTimeMillis()
        var emittedTokens = 0

        val collectorJob = launch {
            sharedFlow.collect { event ->
                when (event) {
                    is NativeLlamaHelper.LLMEvent.Started -> {
                        _tokensPerSecond.value = 0f
                    }
                    is NativeLlamaHelper.LLMEvent.Ongoing -> {
                        trySend(event.word)
                        emittedTokens = event.tokenCount
                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                        if (elapsedSec > 0.05f) {
                            _tokensPerSecond.value = emittedTokens / elapsedSec
                        }
                    }
                    is NativeLlamaHelper.LLMEvent.Done -> {
                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                        if (elapsedSec > 0f && emittedTokens > 0) {
                            _tokensPerSecond.value = emittedTokens / elapsedSec
                        }
                        close()
                    }
                    is NativeLlamaHelper.LLMEvent.Error -> {
                        close(Exception(event.message))
                    }
                    else -> {}
                }
            }
        }

        try {
            helper.predict(
                prompt = fullPrompt,
                imagePath = null,
                partialCompletion = true
            )
        } catch (e: Throwable) {
            close(e)
        }

        awaitClose {
            collectorJob.cancel()
            try {
                helper.stopPrediction()
            } catch (ignored: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    fun unloadModel() {
        try {
            nativeInstance?.abort()
            nativeInstance?.release()
        } catch (ignored: Exception) {}
        nativeInstance = null
        nativeScope?.cancel()
        nativeScope = null
        _isLoaded.value = false
        _isLoading.value = false
        _loadProgress.value = 0f
        _tokensPerSecond.value = 0f
    }
}

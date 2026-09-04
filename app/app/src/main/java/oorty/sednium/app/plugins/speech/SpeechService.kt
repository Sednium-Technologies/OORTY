package oorty.sednium.app.plugins.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oorty.sednium.app.model.VoicePersona
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phonetic speech pre-processor that transforms Markdown, code blocks,
 * currency, percentages, decimals, and acronyms into natural spoken phonetics.
 */
object PhoneticNormalizer {
    fun normalizeForSpeech(text: String): String {
        if (text.isBlank()) return ""
        return text
            // Strip code blocks and inline code
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("`[^`]*`"), "")
            // Strip markdown formatting symbols
            .replace(Regex("[#*_~\\[\\]()<>{}]"), " ")
            // Strip URLs
            .replace(Regex("https?://\\S+"), " web link ")
            // Currency normalization ($50 -> fifty dollars, $25.50 -> 25 dollars and 50 cents)
            .replace(Regex("\\$(\\d+)(?:\\.(\\d{2}))?")) { match ->
                val dollars = match.groupValues[1]
                val cents = match.groupValues.getOrNull(2)
                if (!cents.isNullOrBlank() && cents != "00") {
                    "$dollars dollars and $cents cents"
                } else {
                    "$dollars dollars"
                }
            }
            // Percentage normalization (80% -> 80 percent)
            .replace(Regex("(\\d+)%")) { "${it.groupValues[1]} percent" }
            // Common abbreviations & acronyms
            .replace(Regex("\\bAPI\\b", RegexOption.IGNORE_CASE), "A P I")
            .replace(Regex("\\bAI\\b", RegexOption.IGNORE_CASE), "A I")
            .replace(Regex("\\be\\.g\\.", RegexOption.IGNORE_CASE), "for example")
            .replace(Regex("\\bi\\.e\\.", RegexOption.IGNORE_CASE), "that is")
            .replace(Regex("\\bvs\\b\\.?", RegexOption.IGNORE_CASE), "versus")
            .replace(Regex("\\betc\\b\\.?", RegexOption.IGNORE_CASE), "etcetera")
            // Decimal numbers (3.14 -> 3 point 14)
            .replace(Regex("(\\d+)\\.(\\d+)")) { "${it.groupValues[1]} point ${it.groupValues[2]}" }
            // Clean up extra whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

class SpeechService(private val context: Context) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _spokenMessageId = MutableStateFlow<String?>(null)
    val spokenMessageId: StateFlow<String?> = _spokenMessageId.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    var isContinuousMode: Boolean = false
    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechComplete: (() -> Unit)? = null
    var onInterruption: (() -> Unit)? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // --- Sentence-Streaming TTS state ---
    private val streamBuffer = StringBuilder()
    private val activeUtteranceCount = AtomicInteger(0)
    private var isFirstStreamingChunk = true
    private var isStreamingActive = false
    private var lastPersona: VoicePersona = VoicePersona.WARM_CONVERSATIONAL
    private var speechStartTime: Long = 0L

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.getDefault()
            applyVoicePersona(lastPersona)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    stopListening()
                }
                override fun onDone(utteranceId: String?) {
                    val remaining = activeUtteranceCount.decrementAndGet()
                    if (remaining <= 0 && !isStreamingActive) {
                        _isSpeaking.value = false
                        _spokenMessageId.value = null
                        mainHandler.postDelayed({
                            if (!_isSpeaking.value) {
                                onSpeechComplete?.invoke()
                            }
                        }, 300L)
                    }
                }
                override fun onError(utteranceId: String?) {
                    val remaining = activeUtteranceCount.decrementAndGet()
                    if (remaining <= 0 && !isStreamingActive) {
                        _isSpeaking.value = false
                        _spokenMessageId.value = null
                        mainHandler.postDelayed({
                            if (!_isSpeaking.value) {
                                onSpeechComplete?.invoke()
                            }
                        }, 300L)
                    }
                }
            })
        }
    }

    /**
     * Dynamically configures the TTS engine with high-definition neural voices,
     * cadence, speech rate, and pitch matching the chosen persona.
     */
    fun applyVoicePersona(persona: VoicePersona, baseRate: Float = 1.0f, basePitch: Float = 1.0f) {
        lastPersona = persona
        if (!isTtsReady || tts == null) return
        val availableVoices: Set<Voice> = try { tts?.voices ?: emptySet() } catch (e: Exception) { emptySet() }

        when (persona) {
            VoicePersona.WARM_CONVERSATIONAL -> {
                // Aoede / Serena style: Melodic, friendly, natural intonation
                tts?.setSpeechRate(baseRate * 1.03f)
                tts?.setPitch(basePitch * 1.04f)
                val bestVoice = availableVoices.find { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("female") || name.contains("sfg") || name.contains("en-us-x-sfg") || name.contains("aoede") || name.contains("neural2-f") || name.contains("f-local")) &&
                    !voice.isNetworkConnectionRequired
                } ?: availableVoices.find { it.quality >= Voice.QUALITY_HIGH && it.locale.language == Locale.getDefault().language }
                if (bestVoice != null) tts?.voice = bestVoice
            }
            VoicePersona.DEEP_CONFIDENT -> {
                // Charon / Onyx style: Grounded, calm, resonant cadence
                tts?.setSpeechRate(baseRate * 0.98f)
                tts?.setPitch(basePitch * 0.88f)
                val bestVoice = availableVoices.find { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("male") || name.contains("iom") || name.contains("en-us-x-iom") || name.contains("charon") || name.contains("neural2-d") || name.contains("m-local")) &&
                    !voice.isNetworkConnectionRequired
                } ?: availableVoices.find { it.quality >= Voice.QUALITY_HIGH && it.locale.language == Locale.getDefault().language }
                if (bestVoice != null) tts?.voice = bestVoice
            }
            VoicePersona.ENERGETIC_DIRECT -> {
                // Puck / Echo style: Expressive, upbeat, snappy tempo
                tts?.setSpeechRate(baseRate * 1.10f)
                tts?.setPitch(basePitch * 1.02f)
                val bestVoice = availableVoices.find { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("puck") || name.contains("tlg") || name.contains("neural2-a") || name.contains("a-network")) &&
                    !voice.isNetworkConnectionRequired
                } ?: availableVoices.find { it.quality >= Voice.QUALITY_HIGH && it.locale.language == Locale.getDefault().language }
                if (bestVoice != null) tts?.voice = bestVoice
            }
            VoicePersona.SYSTEM_DEFAULT -> {
                tts?.setSpeechRate(baseRate)
                tts?.setPitch(basePitch)
            }
        }
    }

    // ==========================================
    // SENTENCE-STREAMING TTS PIPELINE (<300ms TTFW)
    // ==========================================

    fun startStreamingSpeech(persona: VoicePersona, rate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isTtsReady) return
        speechStartTime = System.currentTimeMillis()
        stopListening()
        applyVoicePersona(persona, rate, pitch)
        synchronized(streamBuffer) {
            streamBuffer.clear()
        }
        activeUtteranceCount.set(0)
        isFirstStreamingChunk = true
        isStreamingActive = true
    }

    fun enqueueStreamChunk(deltaText: String) {
        if (!isTtsReady || !isStreamingActive || deltaText.isBlank()) return
        synchronized(streamBuffer) {
            streamBuffer.append(deltaText)
            val currentText = streamBuffer.toString()

            // Check for sentence or clause boundaries: . ? ! \n or comma after >= 5 words
            val boundaryMatch = findSentenceBoundary(currentText)
            if (boundaryMatch != null) {
                val sentence = currentText.substring(0, boundaryMatch).trim()
                val remainder = currentText.substring(boundaryMatch).trimStart()
                streamBuffer.clear()
                streamBuffer.append(remainder)

                if (sentence.isNotBlank()) {
                    speakSentenceChunk(sentence)
                }
            }
        }
    }

    fun finishStreamingSpeech() {
        if (!isTtsReady || !isStreamingActive) return
        isStreamingActive = false
        val remainingText = synchronized(streamBuffer) {
            val rem = streamBuffer.toString().trim()
            streamBuffer.clear()
            rem
        }
        if (remainingText.isNotBlank()) {
            speakSentenceChunk(remainingText)
        } else if (activeUtteranceCount.get() <= 0) {
            _isSpeaking.value = false
            mainHandler.postDelayed({
                if (!_isSpeaking.value) {
                    onSpeechComplete?.invoke()
                }
            }, 300L)
        }
    }

    private fun findSentenceBoundary(text: String): Int? {
        val delimiters = listOf(".", "?", "!", "\n")
        val words = text.trim().split(Regex("\\s+"))

        // Priority 1: Full sentence punctuation
        for (i in text.indices) {
            val char = text[i].toString()
            if (delimiters.contains(char) && i >= 8) {
                return i + 1
            }
        }

        // Priority 2: Clause punctuation (comma / semicolon) if we have enough words (5+)
        if (words.size >= 5) {
            for (i in text.indices) {
                if ((text[i] == ',' || text[i] == ';') && i >= 12) {
                    return i + 1
                }
            }
        }

        // Fallback for very long single clauses without punctuation
        if (words.size >= 12) {
            val spaceIdx = text.indexOf(" ", 35)
            if (spaceIdx != -1) return spaceIdx + 1
        }

        return null
    }

    private fun speakSentenceChunk(sentence: String) {
        val cleanSpeech = PhoneticNormalizer.normalizeForSpeech(sentence)
        if (cleanSpeech.isBlank()) return

        val queueMode = if (isFirstStreamingChunk) {
            isFirstStreamingChunk = false
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        activeUtteranceCount.incrementAndGet()
        _isSpeaking.value = true
        val utteranceId = "stream_utt_${System.currentTimeMillis()}_${activeUtteranceCount.get()}"
        tts?.speak(cleanSpeech, queueMode, null, utteranceId)
    }

    fun stopStreamingSpeech() {
        isStreamingActive = false
        synchronized(streamBuffer) {
            streamBuffer.clear()
        }
        stopSpeaking()
    }

    // ==========================================
    // STANDARD TTS & SPEECH RECOGNITION
    // ==========================================

    fun speakText(text: String, messageId: String? = null, rate: Float = 1.0f, pitch: Float = 1.0f, persona: VoicePersona = VoicePersona.WARM_CONVERSATIONAL) {
        if (!isTtsReady || text.isBlank()) return
        // Prevent stuttering/restarting if already actively speaking the exact same message
        if (messageId != null && _spokenMessageId.value == messageId && _isSpeaking.value) {
            return
        }
        speechStartTime = System.currentTimeMillis()
        stopListening()
        applyVoicePersona(persona, rate, pitch)
        try {
            _spokenMessageId.value = messageId
            _isSpeaking.value = true
            val cleanSpeech = PhoneticNormalizer.normalizeForSpeech(text).take(3000)
            activeUtteranceCount.set(1)
            tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, messageId ?: "utterance_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            _isSpeaking.value = false
            _spokenMessageId.value = null
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {}
        finally {
            _isSpeaking.value = false
            _spokenMessageId.value = null
            activeUtteranceCount.set(0)
            isStreamingActive = false
        }
    }

    fun startListening() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _liveSpokenText.value = "Speech recognition not available on this device"
                return@post
            }
            if (_isListening.value || _isSpeaking.value) return@post

            try {
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.destroy()
                } catch (e: Exception) {}

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                if (isContinuousMode && !_isSpeaking.value) {
                    mainHandler.postDelayed({
                        if (isContinuousMode && !_isSpeaking.value && !_isListening.value) {
                            startListening()
                        }
                    }, 300L)
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {}
            finally {
                _isListening.value = false
                _soundLevel.value = 0f
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                // If TTS is speaking, only interrupt if after 3-second cooldown
                val elapsed = System.currentTimeMillis() - speechStartTime
                if (_isSpeaking.value && elapsed > 3000L) {
                    stopStreamingSpeech()
                    onInterruption?.invoke()
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _soundLevel.value = level
                // Do NOT interrupt on raw RMS level (speaker sound creates high RMS)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _soundLevel.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _soundLevel.value = 0f
                if (isContinuousMode && !_isSpeaking.value) {
                    val delay = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 100L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        SpeechRecognizer.ERROR_CLIENT -> 300L
                        else -> 250L
                    }
                    mainHandler.postDelayed({
                        if (isContinuousMode && !_isSpeaking.value && !_isListening.value) {
                            startListening()
                        }
                    }, delay)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _soundLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull() ?: ""
                if (recognized.isNotBlank()) {
                    _liveSpokenText.value = recognized
                    onSpeechRecognized?.invoke(recognized)
                } else if (isContinuousMode && !_isSpeaking.value) {
                    mainHandler.postDelayed({
                        if (isContinuousMode && !_isSpeaking.value && !_isListening.value) {
                            startListening()
                        }
                    }, 150)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    _liveSpokenText.value = partial
                    if (_isSpeaking.value) {
                        stopStreamingSpeech()
                        onInterruption?.invoke()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        try {
            stopStreamingSpeech()
            mainHandler.post {
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.destroy()
                } catch (e: Exception) {}
                speechRecognizer = null
            }
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {}
    }
}

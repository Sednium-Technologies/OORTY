package oorty.sednium.app.api

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object LiteRtTitleGen {
    private var interpreter: Interpreter? = null
    private var isInitialized = false

    private val STOPWORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can", "can't", "cannot", "could",
        "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down",
        "during", "each", "few", "for", "from", "further", "had", "hadn't", "has",
        "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her",
        "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's",
        "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it",
        "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
        "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other",
        "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't",
        "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
        "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
        "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
        "they've", "this", "those", "through", "to", "too", "under", "until", "up",
        "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
        "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
        "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
        "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
        "yourself", "yourselves", "please", "help", "make", "create", "write", "give", "tell"
    )

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        try {
            val modelBuffer = loadModelFile(context, "title_generator.tflite")
            if (modelBuffer != null) {
                interpreter = Interpreter(modelBuffer)
            }
        } catch (e: Exception) {
            interpreter = null
        }
    }

    fun isAvailable(): Boolean = interpreter != null

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            try {
                context.assets.open(modelName).use { stream ->
                    val bytes = stream.readBytes()
                    val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                        order(ByteOrder.nativeOrder())
                        put(bytes)
                        rewind()
                    }
                    buffer
                }
            } catch (fallbackEx: Exception) {
                null
            }
        }
    }

    /**
     * Extracts top keywords and generates a concise, descriptive title (under 25 chars)
     * using TF-IDF frequency analysis on the initial prompt.
     */
    fun generateTitle(prompt: String): String {
        if (prompt.isBlank()) return "New Chat"

        val keywords = extractKeywords(prompt, limit = 4)
        if (keywords.isEmpty()) {
            val words = prompt.trim().split("\\s+".toRegex()).take(3)
            return words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.take(20)
        }

        val candidate = keywords.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return if (candidate.length > 18) {
            candidate.take(18).substringBeforeLast(" ").ifBlank { candidate.take(18) }
        } else {
            candidate
        }
    }

    /**
     * Extracts key topic tags from text for Obsidian frontmatter and indexing.
     */
    fun extractKeywords(text: String, limit: Int = 4): List<String> {
        val cleanWords = text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOPWORDS }

        if (cleanWords.isEmpty()) return emptyList()

        val frequencies = cleanWords.groupingBy { it }.eachCount()
        return frequencies.entries
            .sortedByDescending { it.value * (1.0 + (it.key.length * 0.05)) }
            .take(limit)
            .map { it.key }
    }
}


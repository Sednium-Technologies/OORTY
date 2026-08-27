package oorty.sednium.app.vault

import kotlin.math.sqrt

object EmbeddingEngine {
    private const val VECTOR_DIM = 384

    fun embed(text: String): FloatArray {
        val vector = FloatArray(VECTOR_DIM)
        if (text.isBlank()) return vector

        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return vector

        // Word-level hash distribution with positional decay
        for ((index, token) in tokens.withIndex()) {
            val positionWeight = 1.0f / (1.0f + (index * 0.03f))
            val hash1 = (token.hashCode() and 0x7FFFFFFF) % VECTOR_DIM
            val hash2 = ((token.reversed().hashCode()) and 0x7FFFFFFF) % VECTOR_DIM

            vector[hash1] += 1.5f * positionWeight
            vector[hash2] += 0.8f * positionWeight

            // Subword / char n-grams (3-grams) for robust typo and morphological matching
            if (token.length >= 3) {
                for (i in 0..token.length - 3) {
                    val trigram = token.substring(i, i + 3)
                    val triHash = ((trigram.hashCode() xor (i * 31)) and 0x7FFFFFFF) % VECTOR_DIM
                    vector[triHash] += 0.4f * positionWeight
                }
            }
        }

        // L2 Unit Normalization
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v)
        }
        val norm = sqrt(sumSquares).toFloat()

        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector
    }

    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0f
        var dot = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(-1.0f, 1.0f)
    }
}

package oorty.sednium.app.vault

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultIndexer(private val chatVault: ChatVault) {

    private val cachedEntries = mutableListOf<ChatVaultEntry>()
    private var isIndexed = false

    suspend fun updateIndex() = withContext(Dispatchers.IO) {
        val loaded = chatVault.loadAllChats()
        synchronized(cachedEntries) {
            cachedEntries.clear()
            cachedEntries.addAll(loaded)
            isIndexed = true
        }
    }

    suspend fun search(query: String, limit: Int = 5): List<ChatVaultEntry> = withContext(Dispatchers.Default) {
        if (!isIndexed) {
            updateIndex()
        }

        val entriesSnapshot = synchronized(cachedEntries) { cachedEntries.toList() }
        if (entriesSnapshot.isEmpty() || query.isBlank()) return@withContext emptyList()

        val queryVec = EmbeddingEngine.embed(query)
        val queryLower = query.lowercase().trim()

        val scored = entriesSnapshot.map { entry ->
            val sim = entry.embedding?.let { EmbeddingEngine.cosineSimilarity(queryVec, it) } ?: 0f

            // Keyword boost for exact title or tag matches
            var boost = 0f
            if (entry.title.lowercase().contains(queryLower)) boost += 0.35f
            if (entry.tags.any { it.lowercase().contains(queryLower) }) boost += 0.25f
            if (entry.contentPreview.lowercase().contains(queryLower)) boost += 0.15f

            val totalScore = (sim * 0.6f) + boost
            entry to totalScore
        }

        scored.filter { it.second > 0.12f }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    suspend fun getRelevantContext(userMessage: String, limit: Int = 3): String {
        if (userMessage.length < 5) return ""
        val matches = search(userMessage, limit)
        if (matches.isEmpty()) return ""

        return buildString {
            append("\n[Relevant memory from your past chats on this device:]\n")
            matches.forEach { entry ->
                append("• Chat: \"${entry.title}\" (tags: ${entry.tags.joinToString(", ")})\n")
                append("  Snippet: ${entry.contentPreview.take(160)}...\n")
            }
            append("[Use the above context if relevant to the query, or proceed naturally.]\n\n")
        }
    }

    fun getChatCount(): Int = synchronized(cachedEntries) { cachedEntries.size }
}

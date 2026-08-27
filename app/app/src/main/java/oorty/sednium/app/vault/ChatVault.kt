package oorty.sednium.app.vault

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oorty.sednium.app.api.LiteRtTitleGen
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.ChatSession
import oorty.sednium.app.model.Role
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatVault(private val context: Context) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

    private fun getVaultDirectory(): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val oortyDir = File(docsDir, "Oorty/chats")
        if (!oortyDir.exists()) {
            oortyDir.mkdirs()
        }
        return if (oortyDir.exists()) oortyDir else File(context.filesDir, "Oorty/chats").apply { mkdirs() }
    }

    private fun slugify(title: String, id: String): String {
        val clean = title.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .take(40)
            .trim('-')
        return if (clean.isNotBlank()) "$clean-${id.take(6)}.md" else "chat-${id.take(8)}.md"
    }

    suspend fun saveChat(session: ChatSession, settings: AppSettings): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = slugify(session.title, session.id)
            val keywords = LiteRtTitleGen.extractKeywords(
                text = session.messages.joinToString(" ") { it.content },
                limit = 4
            )

            // Estimate total tokens (~4 chars per token heuristic)
            val totalChars = session.messages.sumOf { it.content.length }
            val totalTokensEst = (totalChars / 4).coerceAtLeast(1)

            val modelName = session.messages.lastOrNull { it.role == Role.MODEL }?.modelName ?: settings.model.ifBlank { "default" }
            val providerName = settings.provider.name

            val markdownContent = buildString {
                append("---\n")
                append("id: \"${session.id}\"\n")
                append("title: \"${session.title.replace("\"", "\\\"")}\"\n")
                append("created: \"${isoFormat.format(Date(session.createdAt))}\"\n")
                append("updated: \"${isoFormat.format(Date(session.updatedAt))}\"\n")
                append("model: \"$modelName\"\n")
                append("provider: \"$providerName\"\n")
                append("total_tokens_est: $totalTokensEst\n")
                append("tags: [${keywords.joinToString(", ") { "\"$it\"" }}]\n")
                append("message_count: ${session.messages.size}\n")
                append("has_attachments: ${session.messages.any { it.attachments.isNotEmpty() }}\n")
                append("---\n\n")

                append("# ${session.title}\n\n")

                session.messages.forEach { msg ->
                    val timestampStr = timeFormat.format(Date(session.updatedAt))
                    if (msg.role == Role.USER) {
                        append("## 🧑 User — $timestampStr\n")
                        append("${msg.content}\n\n")
                    } else {
                        val tokenEst = (msg.content.length / 4).coerceAtLeast(1)
                        val latencyStr = if (msg.latencyMs != null) " | ${(msg.latencyMs / 1000f)}s TTFT" else ""
                        val speedStr = if (msg.tokensPerSecond != null && msg.tokensPerSecond > 0) " | ${"%.1f".format(msg.tokensPerSecond)} tok/s" else ""
                        val usedModel = msg.modelName ?: modelName

                        append("## 🤖 Oorty ($usedModel) — $timestampStr | ~$tokenEst tokens$latencyStr$speedStr\n")
                        if (msg.thought != null && msg.thought.isNotBlank()) {
                            append("> [!NOTE] Thinking Process\n")
                            append("> ${msg.thought.replace("\n", "\n> ")}\n\n")
                        }
                        append("${msg.content}\n\n")
                    }
                }
            }

            // Write to disk / Documents
            val targetDir = getVaultDirectory()
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(markdownContent.toByteArray())
            }

            // Also register in MediaStore if Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Oorty/chats")
                    }
                    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val uri = resolver.insert(collection, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream ->
                            stream.write(markdownContent.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    // Direct file fallback was already written
                }
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadAllChats(): List<ChatVaultEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ChatVaultEntry>()
        val dir = getVaultDirectory()
        if (!dir.exists() || !dir.isDirectory) return@withContext entries

        val files = dir.listFiles { f -> f.extension.equals("md", ignoreCase = true) } ?: emptyArray()

        for (file in files) {
            try {
                val text = file.readText()
                val entry = parseMarkdownVaultFile(file.absolutePath, text)
                if (entry != null) {
                    entries.add(entry)
                }
            } catch (e: Exception) {}
        }
        entries.sortedByDescending { it.updated }
    }

    private fun parseMarkdownVaultFile(filePath: String, content: String): ChatVaultEntry? {
        if (!content.startsWith("---")) return null
        val frontmatterEnd = content.indexOf("---", 3)
        if (frontmatterEnd == -1) return null

        val frontmatter = content.substring(3, frontmatterEnd)
        val body = content.substring(frontmatterEnd + 3).trim()

        var id = ""
        var title = "Untitled"
        var created = System.currentTimeMillis()
        var updated = System.currentTimeMillis()
        var model = "default"
        var provider = "LOCAL"
        var totalTokens = 0
        var tags = listOf<String>()
        var messageCount = 0
        var hasAttachments = false

        frontmatter.lines().forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim().removeSurrounding("\"")
                when (key) {
                    "id" -> id = value
                    "title" -> title = value
                    "model" -> model = value
                    "provider" -> provider = value
                    "total_tokens_est" -> totalTokens = value.toIntOrNull() ?: 0
                    "message_count" -> messageCount = value.toIntOrNull() ?: 0
                    "has_attachments" -> hasAttachments = value.toBoolean()
                    "tags" -> {
                        tags = value.removePrefix("[").removeSuffix("]")
                            .split(",")
                            .map { it.trim().removeSurrounding("\"") }
                            .filter { it.isNotBlank() }
                    }
                }
            }
        }

        if (id.isBlank()) id = File(filePath).nameWithoutExtension

        val preview = body.lines().filter { !it.startsWith("#") && it.isNotBlank() }.joinToString(" ").take(220)
        val embedding = EmbeddingEngine.embed("$title ${tags.joinToString(" ")} $preview")

        return ChatVaultEntry(
            id = id,
            title = title,
            created = created,
            updated = updated,
            model = model,
            provider = provider,
            totalTokensEst = totalTokens,
            tags = tags,
            messageCount = messageCount,
            hasAttachments = hasAttachments,
            contentPreview = preview,
            filePath = filePath,
            embedding = embedding
        )
    }

    suspend fun deleteChat(id: String): Boolean = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory()
        val files = dir.listFiles() ?: return@withContext false
        for (f in files) {
            if (f.name.contains(id.take(6))) {
                f.delete()
                return@withContext true
            }
        }
        false
    }
}

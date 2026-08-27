package oorty.sednium.app.mcp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import oorty.sednium.app.vault.VaultIndexer

object VaultRecallTool {

    const val QUALIFIED_NAME = "builtin::recall_from_vault"

    fun getToolDefinition(): Tool = Tool(
        name = "recall_from_vault",
        description = "Search your on-device Markdown chat vault for previous conversations, solutions, and notes.",
        inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {
                putJsonObject("query") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("The search query or keywords to locate past discussions"))
                }
                putJsonObject("limit") {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Max number of past chats to recall (default: 4)"))
                }
            }
            put("required", buildJsonArray {
                add(JsonPrimitive("query"))
            })
        }
    )

    suspend fun execute(vaultIndexer: VaultIndexer, arguments: JsonObject): CallToolResult {
        val query = (arguments["query"] as? JsonPrimitive)?.content ?: ""
        val limit = (arguments["limit"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 4

        if (query.isBlank()) {
            return CallToolResult(
                content = listOf(ContentBlock.Text("Query was empty. No chats searched.")),
                isError = true
            )
        }

        val results = vaultIndexer.search(query, limit)
        if (results.isEmpty()) {
            return CallToolResult(
                content = listOf(ContentBlock.Text("No previous conversations found matching \"$query\" in your local vault.")),
                isError = false
            )
        }

        val text = buildString {
            append("Found ${results.size} matching past conversation(s) in local Markdown vault:\n\n")
            results.forEachIndexed { i, entry ->
                append("### ${i + 1}. ${entry.title}\n")
                append("- **Tags:** ${entry.tags.joinToString(", ")}\n")
                append("- **Model:** ${entry.model} (${entry.provider})\n")
                append("- **Excerpt:** ${entry.contentPreview}\n\n")
            }
        }

        return CallToolResult(
            content = listOf(ContentBlock.Text(text)),
            isError = false
        )
    }
}

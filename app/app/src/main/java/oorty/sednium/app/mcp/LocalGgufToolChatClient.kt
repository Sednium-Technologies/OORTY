package oorty.sednium.app.mcp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import oorty.sednium.app.api.LlamaHelper

class LocalGgufToolChatClient(
    private val llamaHelper: LlamaHelper,
    private val systemInstruction: String = "",
    private val temperature: Float = 0.7f,
    private val maxTokens: Int = 2048
) : ToolCallingChatClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun send(history: List<LlmChatTurn>, tools: List<LlmTool>): LlmTurnResult {
        val toolsPrompt = if (tools.isNotEmpty()) {
            buildString {
                append("\n\nYou have access to the following tools:\n")
                tools.forEach { tool ->
                    append("- ${tool.qualifiedName}: ${tool.description ?: "No description"}\n")
                    append("  Parameters: ${tool.parameters}\n")
                }
                append("\nTo invoke a tool, output ONLY a JSON object formatted as:\n")
                append("```json\n")
                append("{\"call\": \"tool_name\", \"arguments\": { ... }}\n")
                append("```\n")
                append("If no tool is needed, respond naturally with plain text.")
            }
        } else ""

        val effectiveSystemPrompt = systemInstruction + toolsPrompt

        val promptBuilder = StringBuilder()
        history.forEach { turn ->
            when (turn) {
                is LlmChatTurn.User -> promptBuilder.append("User: ${turn.text}\n")
                is LlmChatTurn.Assistant -> promptBuilder.append("Assistant: ${turn.text}\n")
                is LlmChatTurn.AssistantToolCalls -> {
                    promptBuilder.append("Assistant [Action]: ${turn.calls.joinToString { it.qualifiedName }}\n")
                }
                is LlmChatTurn.ToolResult -> {
                    promptBuilder.append("Tool Output (${turn.qualifiedName}): ${turn.content}\n")
                }
            }
        }

        val lastUserTurn = history.filterIsInstance<LlmChatTurn.User>().lastOrNull()?.text ?: ""

        val responseBuilder = StringBuilder()
        llamaHelper.generateStream(
            prompt = lastUserTurn,
            systemInstruction = effectiveSystemPrompt,
            temperature = temperature,
            maxTokens = maxTokens
        ).collect { chunk ->
            responseBuilder.append(chunk)
        }

        val responseText = responseBuilder.toString().trim()

        // Inspect for tool invocation JSON
        val toolCall = parseToolCallJson(responseText, tools)
        return if (toolCall != null) {
            LlmTurnResult.ToolCalls(
                calls = listOf(toolCall),
                assistantPreface = "Executing tool ${toolCall.qualifiedName}..."
            )
        } else {
            LlmTurnResult.FinalText(responseText)
        }
    }

    private fun parseToolCallJson(text: String, availableTools: List<LlmTool>): LlmToolCall? {
        try {
            val jsonBlock = when {
                text.contains("```json") -> text.substringAfter("```json").substringBefore("```").trim()
                text.contains("```") -> text.substringAfter("```").substringBefore("```").trim()
                text.startsWith("{") && text.endsWith("}") -> text
                else -> null
            } ?: return null

            val parsed = json.parseToJsonElement(jsonBlock).jsonObject
            val toolName = (parsed["call"] ?: parsed["tool"] ?: parsed["name"])?.toString()?.removeSurrounding("\"") ?: return null
            val args = (parsed["arguments"] ?: parsed["args"] ?: JsonObject(emptyMap())).jsonObject

            val matchedTool = availableTools.firstOrNull {
                it.qualifiedName.equals(toolName, ignoreCase = true) ||
                it.qualifiedName.substringAfter("::").equals(toolName, ignoreCase = true)
            }

            val qualified = matchedTool?.qualifiedName ?: toolName
            val callId = "local_call_${System.currentTimeMillis() % 10000}"

            return LlmToolCall(
                callId = callId,
                qualifiedName = qualified,
                arguments = args
            )
        } catch (e: Exception) {
            return null
        }
    }
}

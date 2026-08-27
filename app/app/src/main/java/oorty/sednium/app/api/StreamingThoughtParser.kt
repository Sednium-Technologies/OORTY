package oorty.sednium.app.api

/**
 * Real-time stream demuxer for LLM responses.
 * Detects and extracts `<think>...</think>` blocks from text streams (e.g. Qwen, DeepSeek),
 * routing internal reasoning to [onOutput] as (deltaText="", deltaThought=...),
 * and normal text as (deltaText=..., deltaThought=null).
 */
class StreamingThoughtParser(
    private val onOutput: (deltaText: String, deltaThought: String?) -> Unit
) {
    private var inThinkTag = false
    private val buffer = StringBuilder()

    fun processChunk(rawText: String, rawThought: String?) {
        if (!rawThought.isNullOrEmpty()) {
            onOutput("", rawThought)
        }
        if (rawText.isEmpty()) return

        buffer.append(rawText)
        var str = buffer.toString()
        buffer.clear()

        while (str.isNotEmpty()) {
            if (!inThinkTag) {
                val thinkStartIdx = str.indexOf("<think>")
                if (thinkStartIdx != -1) {
                    val before = str.substring(0, thinkStartIdx)
                    if (before.isNotEmpty()) {
                        onOutput(before, null)
                    }
                    inThinkTag = true
                    str = str.substring(thinkStartIdx + "<think>".length)
                } else {
                    val partialTag = listOf("<", "<t", "<th", "<thi", "<thin", "<think")
                        .findLast { str.endsWith(it) }
                    if (partialTag != null) {
                        val safeText = str.substring(0, str.length - partialTag.length)
                        if (safeText.isNotEmpty()) onOutput(safeText, null)
                        buffer.append(partialTag)
                        break
                    } else {
                        onOutput(str, null)
                        break
                    }
                }
            } else {
                val thinkEndIdx = str.indexOf("</think>")
                if (thinkEndIdx != -1) {
                    val thoughtPart = str.substring(0, thinkEndIdx)
                    if (thoughtPart.isNotEmpty()) {
                        onOutput("", thoughtPart)
                    }
                    inThinkTag = false
                    str = str.substring(thinkEndIdx + "</think>".length)
                } else {
                    val partialTag = listOf("<", "</", "</t", "</th", "</thi", "</thin", "</think")
                        .findLast { str.endsWith(it) }
                    if (partialTag != null) {
                        val safeThought = str.substring(0, str.length - partialTag.length)
                        if (safeThought.isNotEmpty()) onOutput("", safeThought)
                        buffer.append(partialTag)
                        break
                    } else {
                        onOutput("", str)
                        break
                    }
                }
            }
        }
    }

    fun flush() {
        val remaining = buffer.toString()
        if (remaining.isNotEmpty()) {
            if (inThinkTag) {
                onOutput("", remaining)
            } else {
                onOutput(remaining, null)
            }
            buffer.clear()
        }
    }
}

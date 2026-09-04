package oorty.sednium.app.api

/**
 * Real-time stream demuxer for LLM responses.
 * Detects and extracts `<think>...</think>`, `<thought>...</thought>`, and `<reasoning>...</reasoning>` blocks
 * from text streams, routing internal reasoning to [onOutput] as (deltaText="", deltaThought=...),
 * and normal text as (deltaText=..., deltaThought=null).
 */
class StreamingThoughtParser(
    private val onOutput: (deltaText: String, deltaThought: String?) -> Unit
) {
    private var inThinkTag = false
    private var activeCloseTag = "</think>"
    private val buffer = StringBuilder()

    private val openTags = listOf("<think>", "<thought>", "<reasoning>")
    private val closeTags = listOf("</think>", "</thought>", "</reasoning>")

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
                // Find earliest opening tag
                var earliestIdx = -1
                var matchingOpenTag = ""
                var matchingCloseTag = ""

                for (tag in openTags) {
                    val idx = str.indexOf(tag)
                    if (idx != -1 && (earliestIdx == -1 || idx < earliestIdx)) {
                        earliestIdx = idx
                        matchingOpenTag = tag
                        matchingCloseTag = when (tag) {
                            "<thought>" -> "</thought>"
                            "<reasoning>" -> "</reasoning>"
                            else -> "</think>"
                        }
                    }
                }

                if (earliestIdx != -1) {
                    val before = str.substring(0, earliestIdx)
                    if (before.isNotEmpty()) {
                        onOutput(before, null)
                    }
                    inThinkTag = true
                    activeCloseTag = matchingCloseTag
                    str = str.substring(earliestIdx + matchingOpenTag.length)
                } else {
                    // Check if string ends with a potential partial open tag
                    val partialCandidates = openTags.flatMap { tag ->
                        (1 until tag.length).map { tag.substring(0, it) }
                    }.distinct().sortedByDescending { it.length }

                    val foundPartial = partialCandidates.firstOrNull { str.endsWith(it) }
                    if (foundPartial != null) {
                        val safeText = str.substring(0, str.length - foundPartial.length)
                        if (safeText.isNotEmpty()) onOutput(safeText, null)
                        buffer.append(foundPartial)
                        break
                    } else {
                        onOutput(str, null)
                        break
                    }
                }
            } else {
                val closeIdx = str.indexOf(activeCloseTag)
                if (closeIdx != -1) {
                    val thoughtPart = str.substring(0, closeIdx)
                    if (thoughtPart.isNotEmpty()) {
                        onOutput("", thoughtPart)
                    }
                    inThinkTag = false
                    str = str.substring(closeIdx + activeCloseTag.length)
                } else {
                    val partialCloseCandidates = (1 until activeCloseTag.length)
                        .map { activeCloseTag.substring(0, it) }
                        .sortedByDescending { it.length }

                    val foundPartial = partialCloseCandidates.firstOrNull { str.endsWith(it) }
                    if (foundPartial != null) {
                        val safeThought = str.substring(0, str.length - foundPartial.length)
                        if (safeThought.isNotEmpty()) onOutput("", safeThought)
                        buffer.append(foundPartial)
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

package oorty.sednium.app.vault

import android.net.Uri

data class ChatVaultEntry(
    val id: String,
    val title: String,
    val created: Long,
    val updated: Long,
    val model: String,
    val provider: String,
    val totalTokensEst: Int,
    val tags: List<String> = emptyList(),
    val messageCount: Int = 0,
    val hasAttachments: Boolean = false,
    val contentPreview: String = "",
    val filePath: String = "",
    val uri: Uri? = null,
    val embedding: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChatVaultEntry
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

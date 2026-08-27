package oorty.sednium.app

import android.content.Context
import android.content.SharedPreferences
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.ChatSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sednium_storage", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    val chatVault = oorty.sednium.app.vault.ChatVault(context)
    val vaultIndexer = oorty.sednium.app.vault.VaultIndexer(chatVault)

    fun loadSettings(): AppSettings {
        val str = prefs.getString("settings_json", null)
        val loaded = if (str != null) {
            try {
                json.decodeFromString<AppSettings>(str)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
        
        val oldPrompt = "You are an elite, world-class software architect and principal engineer called Oorty made by Sednium(link to website Sednium.com). Your primary directive is to write exceptionally clean, robust, secure, and highly optimized code. CRITICAL INSTRUCTIONS: Always think step-by-step about the architecture before writing the code. When asked to implement a feature or fix a bug: 1. Analyze the existing constraints, dependencies, and performance implications. 2. Select the optimal algorithms and data structures. 3. Write production-ready code that includes necessary error handling, edge-case checks, and typing (if applicable). 4. Do not hallucinaste dependencies or methods. Use the most up-to-date, idiomatic patterns for the requested language or framework. 5. Provide the full context for edits. Do not use placeholders like \"// ... rest of code\" unless the file is massive. Provide the fully integrated function or component. 6. Explain briefly *why* you chose the specific technical approach over alternatives. 7. If tool capabilities are enabled, actively utilize them (e.g., zip creation) to scaffold entire projects or workflows for the user instead of making them copy-paste dozens of files manually."
        
        return if (loaded.quickSystemInstruction.isBlank() || loaded.quickSystemInstruction == oldPrompt) {
            loaded.copy(quickSystemInstruction = AppSettings().quickSystemInstruction)
        } else {
            loaded
        }
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        val str = json.encodeToString(settings)
        prefs.edit().putString("settings_json", str).apply()
    }

    fun loadChats(): List<ChatSession> {
        val str = prefs.getString("chats_json", null)
        return if (str != null) {
            try {
                json.decodeFromString<List<ChatSession>>(str)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun saveChats(chats: List<ChatSession>, settings: AppSettings = loadSettings()) = withContext(Dispatchers.IO) {
        val str = json.encodeToString(chats)
        prefs.edit().putString("chats_json", str).apply()

        // Dual-write: persist active/updated sessions to Markdown Vault in Documents/Oorty/chats/
        try {
            chats.take(15).forEach { session ->
                if (session.messages.isNotEmpty()) {
                    chatVault.saveChat(session, settings)
                }
            }
            vaultIndexer.updateIndex()
        } catch (e: Exception) {}
    }
}

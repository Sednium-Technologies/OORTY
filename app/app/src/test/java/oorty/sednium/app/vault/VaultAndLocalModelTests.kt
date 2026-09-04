package oorty.sednium.app.vault

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import oorty.sednium.app.api.LiteRtTitleGen
import oorty.sednium.app.api.LlamaHelper
import oorty.sednium.app.mcp.LocalGgufToolChatClient
import oorty.sednium.app.mcp.LlmChatTurn
import oorty.sednium.app.mcp.LlmTool
import oorty.sednium.app.mcp.LlmTurnResult
import oorty.sednium.app.mcp.VaultRecallTool
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.ChatSession
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.model.Role
import oorty.sednium.app.util.HardwareChecker
import oorty.sednium.app.util.HardwareFit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultAndLocalModelTests {

    private lateinit var context: Context
    private lateinit var chatVault: ChatVault
    private lateinit var vaultIndexer: VaultIndexer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        chatVault = ChatVault(context)
        vaultIndexer = VaultIndexer(chatVault)
    }

    @Test
    fun testTitleGeneratorAndKeywordExtraction() {
        val prompt = "How do I implement a RecyclerView with DiffUtil in Kotlin?"
        val title = LiteRtTitleGen.generateTitle(prompt)
        assertFalse("Title should not be empty", title.isBlank())
        assertTrue("Title should be concise (under 25 chars)", title.length < 25)

        val keywords = LiteRtTitleGen.extractKeywords(prompt)
        assertTrue("Keywords should extract meaningful terms", keywords.contains("recyclerview") || keywords.contains("diffutil") || keywords.contains("kotlin"))
    }

    @Test
    fun testHardwareCheckerAssessment() {
        val totalRam = HardwareChecker.getTotalRamMb(context)
        assertTrue("Total RAM must be positive", totalRam > 0)

        val fitComfortable = HardwareChecker.assessModelFit(400, 2048)
        assertEquals(HardwareFit.COMFORTABLE, fitComfortable)

        val fitTight = HardwareChecker.assessModelFit(1500, 2048)
        assertEquals(HardwareFit.TIGHT, fitTight)

        val fitDangerous = HardwareChecker.assessModelFit(3500, 2048)
        assertEquals(HardwareFit.DANGEROUS, fitDangerous)
    }

    @Test
    fun testChatVaultSaveAndLoadMarkdown() = runBlocking {
        val session = ChatSession(
            id = "test-session-12345",
            title = "Kotlin Flow Architecture",
            messages = listOf(
                ChatMessage(id = "msg1", role = Role.USER, content = "Explain Kotlin StateFlow vs SharedFlow"),
                ChatMessage(
                    id = "msg2",
                    role = Role.MODEL,
                    content = "StateFlow is a state-holder observable flow that emits the current and new state updates to its collectors.",
                    modelName = "Qwen2.5-0.5B",
                    latencyMs = 450L,
                    tokensPerSecond = 24.5f
                )
            ),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val settings = AppSettings(
            provider = ModelProvider.LOCAL_GGUF,
            model = "Qwen2.5-0.5B"
        )

        val saveResult = chatVault.saveChat(session, settings)
        assertTrue("Vault save must succeed", saveResult.isSuccess)

        val savedPath = saveResult.getOrNull()
        assertNotNull(savedPath)
        val file = File(savedPath!!)
        assertTrue("Saved file must exist on disk", file.exists())

        val fileContent = file.readText()
        assertTrue("File must start with YAML frontmatter", fileContent.startsWith("---"))
        assertTrue("File must contain title", fileContent.contains("title: \"Kotlin Flow Architecture\""))
        assertTrue("File must contain model info", fileContent.contains("model: \"Qwen2.5-0.5B\""))
        assertTrue("File must contain user heading", fileContent.contains("## 🧑 User"))
        assertTrue("File must contain Oorty heading", fileContent.contains("## 🤖 Oorty (Qwen2.5-0.5B)"))

        // Load all chats and verify parsing
        val loadedEntries = chatVault.loadAllChats()
        assertTrue("Loaded entries should not be empty", loadedEntries.isNotEmpty())
        val found = loadedEntries.find { it.id == "test-session-12345" }
        assertNotNull("Must find the saved session in vault index", found)
        assertEquals("Kotlin Flow Architecture", found!!.title)
    }

    @Test
    fun testEmbeddingEngineAndSemanticSearch() = runBlocking {
        val v1 = EmbeddingEngine.embed("Kotlin Android Jetpack Compose UI")
        val v2 = EmbeddingEngine.embed("Android Jetpack Compose layout and widgets")
        val v3 = EmbeddingEngine.embed("Cooking spaghetti bolognese pasta recipe")

        val simRelated = EmbeddingEngine.cosineSimilarity(v1, v2)
        val simUnrelated = EmbeddingEngine.cosineSimilarity(v1, v3)

        assertTrue("Related queries must have higher similarity", simRelated > simUnrelated)
        assertTrue("Related similarity should be positive", simRelated > 0.3f)
    }

    @Test
    fun testVaultRecallToolExecution() = runBlocking {
        val session = ChatSession(
            id = "vault-recall-test",
            title = "Database Room Setup",
            messages = listOf(
                ChatMessage(id = "m1", role = Role.USER, content = "How do I configure Room Database with KSP?"),
                ChatMessage(id = "m2", role = Role.MODEL, content = "Add androidx.room:room-runtime and use ksp to compile entities.")
            )
        )
        chatVault.saveChat(session, AppSettings())
        vaultIndexer.updateIndex()

        val args = kotlinx.serialization.json.buildJsonObject {
            put("query", kotlinx.serialization.json.JsonPrimitive("Room Database"))
        }

        val result = VaultRecallTool.execute(vaultIndexer, args)
        assertFalse("Vault recall should succeed", result.isError)
        val firstBlock = result.content.first() as oorty.sednium.app.mcp.ContentBlock.Text
        assertTrue("Result should contain relevant snippet", firstBlock.text.contains("Database Room Setup"))
    }

    @Test
    fun testLocalGgufToolChatClientFallback() = runBlocking {
        val helper = LlamaHelper(context)
        val client = LocalGgufToolChatClient(
            llamaHelper = helper,
            systemInstruction = "You are a test assistant"
        )

        val history = listOf(LlmChatTurn.User("Hello there!"))
        val turnResult = client.send(history, emptyList())

        assertTrue("Client should return a valid result without crashing", turnResult is LlmTurnResult.FinalText || turnResult is LlmTurnResult.ToolCalls)
    }
}

package oorty.sednium.app.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import oorty.sednium.app.model.ModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalEngineZeroMockTests {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testLlamaHelperContainsNoCannedMockResponses() {
        val helperSourceFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/LlamaHelper.kt")
        val content = helperSourceFile.readText()

        // Verify all legacy hardcoded canned response mocks have been eliminated
        assertFalse("Must not contain canned 'strawberry' response", content.contains("strawberry has 3 'r's"))
        assertFalse("Must not contain canned 'hello' greeting", content.contains("Hello! I am your local AI assistant"))
        assertFalse("Must not contain canned 'Received on-device request' mock", content.contains("Received on-device request:"))
        assertFalse("Must not contain fake delay loop mocks", content.contains("delay(150)") && content.contains("mockResponse"))
        
        // Verify native bindings are hooked to org.nehuatl.llamacpp
        assertTrue("Must bind to org.nehuatl.llamacpp.LlamaHelper", content.contains("org.nehuatl.llamacpp.LlamaHelper"))
    }

    @Test
    fun testLiteRtHelperImplementationExistsAndIntegratesWithEdgeRuntime() {
        val liteRtSourceFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/LiteRtHelper.kt")
        assertTrue("LiteRtHelper.kt must exist", liteRtSourceFile.exists())
        val content = liteRtSourceFile.readText()

        assertTrue("Must reference LiteRT / TFLite runtime", content.contains("org.tensorflow.lite.Interpreter"))
        assertTrue("Must track memory load state", content.contains("isLoaded") && content.contains("isLoading"))
    }

    @Test
    fun testModelProviderEnumIncludesBothGgufAndLiteRt() {
        val providers = ModelProvider.values()
        assertTrue("ModelProvider must contain LOCAL_GGUF", providers.contains(ModelProvider.LOCAL_GGUF))
        assertTrue("ModelProvider must contain LOCAL_LITERT", providers.contains(ModelProvider.LOCAL_LITERT))
    }

    @Test
    fun testLlamaHelperInitialStateIsUnloaded() = runBlocking {
        val helper = LlamaHelper(context)
        assertFalse("Initial isLoaded state must be false", helper.isLoaded.value)
        assertFalse("Initial isLoading state must be false", helper.isLoading.value)
        assertEquals("Initial loadProgress must be 0", 0f, helper.loadProgress.value, 0.01f)
    }

    @Test
    fun testLiteRtHelperInitialStateIsUnloaded() = runBlocking {
        val helper = LiteRtHelper(context)
        assertFalse("Initial isLoaded state must be false", helper.isLoaded.value)
        assertFalse("Initial isLoading state must be false", helper.isLoading.value)
        assertEquals("Initial loadProgress must be 0", 0f, helper.loadProgress.value, 0.01f)
    }
}

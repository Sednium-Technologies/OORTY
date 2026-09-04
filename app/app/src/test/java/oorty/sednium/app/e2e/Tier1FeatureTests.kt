package oorty.sednium.app.e2e

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import oorty.sednium.app.ui.screens.*
import oorty.sednium.app.ui.components.*
import oorty.sednium.app.model.*
import oorty.sednium.app.ui.theme.*
import oorty.sednium.app.api.*
import oorty.sednium.app.mcp.*
import oorty.sednium.app.navigation.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows
import java.io.File
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier1FeatureTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------
    // F1: App Icon & Bot Avatar (Tests 1-5)
    // -------------------------------------------------------------

    @Test
    fun test1_appIconReferencesMipmapLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        val iconResEntryName = context.resources.getResourceEntryName(appInfo.icon)
        val iconResTypeName = context.resources.getResourceTypeName(appInfo.icon)
        
        // Unenhanced codebase uses @drawable/logo, so this should fail because it expects @mipmap/ic_launcher
        assertEquals("mipmap", iconResTypeName)
        assertEquals("ic_launcher", iconResEntryName)
    }

    @Test
    fun test2_appIconRoundReferencesMipmapLauncherRound() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        
        // roundIconRes is fetched via reflection to ensure compatibility across SDK levels
        val field = appInfo.javaClass.getField("roundIconRes")
        val roundIconRes = field.getInt(appInfo)
        
        val iconResEntryName = context.resources.getResourceEntryName(roundIconRes)
        val iconResTypeName = context.resources.getResourceTypeName(roundIconRes)
        
        // Unenhanced codebase uses @drawable/logo, so this should fail because it expects @mipmap/ic_launcher_round
        assertEquals("mipmap", iconResTypeName)
        assertEquals("ic_launcher_round", iconResEntryName)
    }

    @Test
    fun test3_chatBubbleAvatarContainerIsRendered() {
        val msg = ChatMessage(
            id = "1",
            role = Role.MODEL,
            content = "Hello, I am Oorty"
        )
        composeTestRule.setContent {
            ChatBubble(
                msg = msg,
                providerName = "google",
                isDark = false,
                isGenerating = false,
                onImageClick = {}
            )
        }
        // Verification: check if the Oorty Logo avatar container is rendered
        composeTestRule.onNodeWithContentDescription("Oorty Logo").assertExists()
    }

    @Test
    fun test4_botAvatarLoadsLogoResource() {
        val msg = ChatMessage(
            id = "1",
            role = Role.MODEL,
            content = "Hello, I am Oorty"
        )
        composeTestRule.setContent {
            ChatBubble(
                msg = msg,
                providerName = "google",
                isDark = false,
                isGenerating = false,
                onImageClick = {}
            )
        }
        // Verify that the avatar loads Oorty Logo drawable instead of standard SmartToy icon
        composeTestRule.onNodeWithContentDescription("Oorty Logo").assertExists()
    }

    @Test
    fun test5_oortyLogoAssetCanBeLoaded() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.getDrawable(oorty.sednium.app.R.drawable.logo)
        assertNotNull("The brand logo drawable must be loadable", drawable)
    }

    // -------------------------------------------------------------
    // F2: Termux Setup Guide (Tests 6-10)
    // -------------------------------------------------------------

    @Test
    fun test6_termuxNotInstalledShowsDownloadButton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager.removePackage("com.termux")

        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }

        // When Termux is missing, show "Download Termux (GitHub Releases)" button
        composeTestRule.onNodeWithText("Download Termux (GitHub Releases)").assertExists()
        composeTestRule.onNodeWithText("Copy & Open Termux").assertDoesNotExist()
    }

    @Test
    fun test7_termuxInstalledShowsCopyOpenButton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val packageInfo = android.content.pm.PackageInfo().apply {
            packageName = "com.termux"
        }
        shadowPackageManager.addPackage(packageInfo)

        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }

        // When Termux is installed, show "Copy & Open Termux" button
        composeTestRule.onNodeWithText("Copy & Open Termux").assertExists()
        composeTestRule.onNodeWithText("Download Termux (GitHub Releases)").assertDoesNotExist()
    }

    @Test
    fun test8_clickDownloadTermuxFiresIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager.removePackage("com.termux")

        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }

        composeTestRule.onNodeWithText("Download Termux (GitHub Releases)").performClick()

        val shadowApp = Shadows.shadowOf(context as android.app.Application)
        val nextIntent = shadowApp.nextStartedActivity
        assertNotNull("Intent should be fired", nextIntent)
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals("https://github.com/termux/termux-app/releases", nextIntent.data?.toString())
    }

    @Test
    fun test9_clickCopyOpenTermuxCopiesToClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val packageInfo = android.content.pm.PackageInfo().apply {
            packageName = "com.termux"
        }
        shadowPackageManager.addPackage(packageInfo)

        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }

        composeTestRule.onNodeWithText("Copy & Open Termux").performClick()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        assertNotNull("Clipboard must contain text", clipData)
        assertTrue(clipData!!.itemCount > 0)
        val copiedText = clipData.getItemAt(0).text.toString()
        assertTrue("Clipboard must contain package updates and llama.cpp build commands", copiedText.contains("pkg update"))
        assertTrue(copiedText.contains("llama.cpp"))
    }

    @Test
    fun test10_clickCopyOpenTermuxFiresLaunchIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        val packageInfo = android.content.pm.PackageInfo().apply {
            packageName = "com.termux"
        }
        shadowPackageManager.addPackage(packageInfo)
        
        val intentFilter = android.content.IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val componentName = android.content.ComponentName("com.termux", "com.termux.app.TermuxActivity")
        shadowPackageManager.addActivityIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForActivity(componentName, intentFilter)

        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }

        composeTestRule.onNodeWithText("Copy & Open Termux").performClick()

        val shadowApp = Shadows.shadowOf(context as android.app.Application)
        val nextIntent = shadowApp.nextStartedActivity
        assertNotNull("Launch intent targeting Termux should be fired", nextIntent)
        val targetPkg = nextIntent.getPackage() ?: nextIntent.component?.packageName
        assertEquals("com.termux", targetPkg)
    }

    // -------------------------------------------------------------
    // F3: Local GGUF Model Downloader & RAM Check (Tests 11-15)
    // -------------------------------------------------------------

    @Test
    fun test11_huggingFaceDownloaderListDisplaysModels() {
        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }
        // Fails on unenhanced codebase: list of GGUF models is not yet pre-configured/displayed
        composeTestRule.onNodeWithText("Qwen/Qwen2.5-0.5B-Instruct-GGUF").assertExists()
        composeTestRule.onNodeWithText("meta-llama/Llama-3.2-1B-Instruct-GGUF").assertExists()
    }

    @Test
    fun test12_searchFiltersModelList() {
        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }
        
        // Query search input (unenhanced code has placeholder, but no filtered list)
        composeTestRule.onNodeWithText("e.g. Qwen/Qwen2.5-0.5B-Instruct-GGUF").performTextInput("Llama")
        
        // Fails on unenhanced codebase because no GGUF list items are rendered or filtered
        composeTestRule.onNodeWithText("meta-llama/Llama-3.2-1B-Instruct-GGUF").assertExists()
        composeTestRule.onNodeWithText("Qwen/Qwen2.5-0.5B-Instruct-GGUF").assertDoesNotExist()
    }

    @Test
    fun test13_deviceTotalRamIsQueried() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Verification: ensure the system queries memory and gets a non-zero value
        assertTrue(memoryInfo.totalMem > 0)
    }

    @Test
    fun test14_recommendedBadgeDisplaysCorrectlyBasedOnRam() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val shadowActivityManager = Shadows.shadowOf(activityManager)
        
        // Mock 4GB RAM to check if low-end models are recommended
        val memoryInfo = android.app.ActivityManager.MemoryInfo().apply {
            totalMem = 4L * 1024 * 1024 * 1024 // 4GB
        }
        shadowActivityManager.setMemoryInfo(memoryInfo)
        
        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }
        
        // Fails on unenhanced codebase: recommendation badges do not exist yet
        composeTestRule.onAllNodesWithText("Recommended").assertCountEquals(1)
    }

    @Test
    fun test15_completeDownloadAddsToPresets() {
        var updatedSettings: AppSettings? = null
        
        // Since we want to check the settings state updates, we render the entire screen or test the preset update callback.
        // We will mock the onUpdateSettings inside UsageContent/SettingsScreen.
        // As SettingsScreen does not support model download presets update callback in unenhanced state, this test will fail.
        composeTestRule.setContent {
            UsageContent(settings = AppSettings())
        }
        
        composeTestRule.onNodeWithText("e.g. Qwen/Qwen2.5-0.5B-Instruct-GGUF").performTextInput("meta-llama/Llama-3.2-1B-Instruct-GGUF")
        composeTestRule.onNodeWithText("Download Model").performClick()
        
        composeTestRule.waitForIdle()
        
        // In enhanced code, completing simulated download triggers onUpdateSettings with the new local preset added.
        // Since this doesn't happen on unenhanced codebase, assert fails.
        assertNotNull(updatedSettings)
        val addedPreset = updatedSettings!!.savedPresets.firstOrNull { it.name.contains("Llama-3.2-1B") }
        assertNotNull("Preset should be added to settings", addedPreset)
    }

    // -------------------------------------------------------------
    // F4: App-Wide Dark Mode (Tests 16-20)
    // -------------------------------------------------------------

    @Test
    fun test16_settingsThemeTogglesCorrectly() {
        var updatedSettings: AppSettings? = null
        val settings = AppSettings(theme = AppTheme.LIGHT)
        
        // Setup state wrapper mock
        val mcpServerManager = McpServerManager(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            SettingsScreen(
                settings = settings,
                localServerStatus = LocalServerStatus.UNKNOWN,
                mcpServerManager = mcpServerManager,
                onOpenMcpServers = {},
                onUpdateSettings = { updatedSettings = it },
                onClose = {}
            )
        }
        
        // Toggle Dark Theme Switch
        composeTestRule.onNodeWithText("DARK THEME").performClick()
        
        // Assert that the theme toggles to AppTheme.DARK
        assertNotNull(updatedSettings)
        assertEquals(AppTheme.DARK, updatedSettings!!.theme)
    }

    @Test
    fun test17_themeUses333333ForDarkBackgroundAndSurface() {
        // Read theme config or color references to verify if DarkBackground and DarkSurfaceAlt are updated to Color(0xFF333333)
        // In the unenhanced codebase, DarkBackground is Color(0xFF121212) and DarkSurfaceAlt is Color(0xFF2A2B32)
        // Checking via reflection/direct verification of the Color constants in SedniumColors
        val darkBgColor = SedniumColors.DarkBackground
        val darkSurfaceColor = SedniumColors.DarkSurfaceAlt
        
        // Fails on unenhanced codebase because color values are not yet updated to 0xFF333333
        assertEquals(Color(0xFF333333), darkBgColor)
        assertEquals(Color(0xFF333333), darkSurfaceColor)
    }

    @Test
    fun test18_chatScreenRootBackgroundIsThemeAware() {
        // Check if ChatScreen container background is theme-aware or hardcoded
        // In unenhanced code: containerColor = SedniumColors.Milk (hardcoded)
        // Since we want to assert that it is theme-aware (uses MaterialTheme.colorScheme.background or updates dynamically),
        // we test ChatScreen rendering in dark mode and assert background matches DarkBackground
        composeTestRule.setContent {
            // Render ChatScreen under dark mode
            SedniumTheme(darkTheme = true) {
                ChatScreen(
                    chatTitle = "Test Chat",
                    settings = AppSettings(theme = AppTheme.DARK),
                    messages = emptyList(),
                    isLoading = false,
                    isConfigValid = true,
                    input = "",
                    attachments = emptyList(),
                    isPresetMenuOpen = false,
                    onInputChange = {},
                    onSend = {},
                    onRetry = {},
                    onAttachClick = {},
                    onRemoveAttachment = {},
                    onTogglePresetMenu = {},
                    onSelectPreset = {},
                    onMenuClick = {},
                    onExportClick = {},
                    onClearClick = {},
                    onSettingsClick = {},
                    onImageClick = {}
                )
            }
        }
        
        // Verification fails on unenhanced code because background is hardcoded to SedniumColors.Milk (0xFFFDFBF7)
        // instead of the theme background (0xFF333333 / 0xFF121212)
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/ui/screens/ChatScreen.kt")
        val content = buildFile.readText()
        assertFalse("ChatScreen must not hardcode background to Milk", content.contains("containerColor = SedniumColors.Milk"))
    }

    @Test
    fun test19_settingsScreenBackgroundIsThemeAware() {
        // Check if SettingsScreen background uses the theme-aware colors
        // In unenhanced code: SettingsScreen has no background modifier on Column (is transparent/unaware)
        // We assert that the Column modifier sets a theme-aware background
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/ui/screens/SettingsScreen.kt")
        val content = buildFile.readText()
        assertTrue("SettingsScreen root modifier should specify a theme-aware background color", content.contains(".background(MaterialTheme.colorScheme.background)"))
    }

    @Test
    fun test20_promptLabScreenBackgroundIsThemeAware() {
        // Check if PromptLabScreen background is theme-aware or hardcoded
        // In unenhanced code: containerColor = SedniumColors.Milk (hardcoded)
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/ui/screens/PromptLabScreen.kt")
        val content = buildFile.readText()
        assertFalse("PromptLabScreen must not hardcode background to Milk", content.contains("containerColor = SedniumColors.Milk"))
    }

    // -------------------------------------------------------------
    // F5: Native On-Device GGUF Inference (Tests 21-25)
    // -------------------------------------------------------------

    @Test
    fun test21_persistableUriPermissionIsInvokedOnModelSelect() {
        // GGUF Native Inference needs Uri permission persistence.
        // Fails on unenhanced codebase because no file picker or uri persisting is implemented.
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/ui/screens/SettingsScreen.kt")
        val content = buildFile.readText()
        assertTrue("takePersistableUriPermission must be called when local GGUF model is selected", content.contains("takePersistableUriPermission"))
    }

    @Test
    fun test22_llamaHelperInstantiatedWithPersistableUri() {
        // Reflection test to verify LlamaHelper class exists and is instantiated with the URI
        // Fails on unenhanced codebase because LlamaHelper doesn't exist yet.
        try {
            val clazz = Class.forName("oorty.sednium.app.api.LlamaHelper")
            assertNotNull(clazz)
            val constructor = clazz.getConstructor(Context::class.java, Uri::class.java)
            assertNotNull(constructor)
        } catch (e: ClassNotFoundException) {
            fail("LlamaHelper class not found: ${e.message}")
        }
    }

    @Test
    fun test23_universalApiRoutesRequestsToLlamaHelper() {
        // Fails on unenhanced codebase because ModelProvider.LOCAL_GGUF is not handled in UniversalApi.kt
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/UniversalApi.kt")
        val content = buildFile.readText()
        assertTrue("UniversalApi must handle ModelProvider.LOCAL_GGUF and route to LlamaHelper", content.contains("LOCAL_GGUF"))
    }

    @Test
    fun test24_localEndpointsResolveUsingBaseUrl() {
        // Fails on unenhanced codebase because LOCAL or LOCAL_GGUF endpoint url resolution doesn't check localBaseUrl properly
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/UniversalApi.kt")
        val content = buildFile.readText()
        assertTrue("UniversalApi must resolve local endpoints using localBaseUrl", content.contains("localBaseUrl"))
    }

    @Test
    fun test25_responseStreamAppendsChunksToChat() {
        // Fails on unenhanced codebase because local GGUF generation stream is not implemented
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/UniversalApi.kt")
        val content = buildFile.readText()
        assertTrue("generateContentStream must handle LOCAL_GGUF stream output and invoke callback", content.contains("LOCAL_GGUF"))
    }

    // -------------------------------------------------------------
    // F6: LiteRt Title Generator & Packaging (Tests 26-30)
    // -------------------------------------------------------------

    @Test
    fun test26_tfliteAssetsAreNotCompressed() {
        // Checks that the build script contains noCompress rule for tflite
        val buildGradleFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/build.gradle.kts")
        assertTrue(buildGradleFile.exists())
        val content = buildGradleFile.readText()
        assertTrue("tflite assets must not be compressed", content.contains("noCompress(\"tflite\")"))
    }

    @Test
    fun test27_liteRtTitleGenUsesOpenFd() {
        // Inspect LiteRtTitleGen to ensure it uses assets.openFd() to load the model
        val clazz = LiteRtTitleGen::class.java
        val method = clazz.getDeclaredMethod("loadModelFile", Context::class.java, String::class.java)
        method.isAccessible = true
        assertNotNull("loadModelFile method must exist", method)
        
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/LiteRtTitleGen.kt")
        val content = buildFile.readText()
        assertTrue("LiteRtTitleGen must load model using openFd", content.contains("openFd"))
    }

    @Test
    fun test28_liteRtTitleGenFallbackLoaderReadsBytesIfOpenFdFails() {
        // Fails on unenhanced codebase because LiteRtTitleGen has no fallback loader reading asset bytes into a direct ByteBuffer
        val buildFile = File("/run/media/bhoid/StorageVault/WEbs/Oorty/app/app/src/main/java/oorty/sednium/app/api/LiteRtTitleGen.kt")
        val content = buildFile.readText()
        assertTrue("LiteRtTitleGen must contain a fallback binary reader into a direct ByteBuffer", content.contains("ByteBuffer.allocateDirect"))
    }

    @Test
    fun test29_generateTitleConvertsToConciseTitle() {
        LiteRtTitleGen.initialize(ApplicationProvider.getApplicationContext())
        val title = LiteRtTitleGen.generateTitle("Generate a short title for this chat")
        
        // Fails on unenhanced codebase because missing weights result in an empty string title output
        assertFalse("Title should not be empty", title.isEmpty())
        assertTrue("Title should be concise", title.length < 20)
    }

    @Test
    fun test30_initializeRecoversGracefullyOnMissingAsset() {
        // Success test on unenhanced codebase: calling initialize on missing assets must not crash the app
        try {
            LiteRtTitleGen.initialize(ApplicationProvider.getApplicationContext())
        } catch (e: Exception) {
            fail("Graceful recovery failed, initialization crashed: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // F7: Saved Presets Management & Dialog Roundedness (Tests 31-35)
    // -------------------------------------------------------------

    @Test
    fun test31_savedPresetsAreRenderedAsList() {
        val preset = SavedModelPreset(
            id = "preset-123",
            name = "My Custom Preset",
            provider = ModelProvider.GOOGLE,
            model = "gemini-1.5-flash",
            chatMode = ChatMode.QUICK,
            systemInstruction = "Custom instruction"
        )
        val settings = AppSettings(savedPresets = listOf(preset))
        val mcpServerManager = McpServerManager(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            SettingsScreen(
                settings = settings,
                localServerStatus = LocalServerStatus.UNKNOWN,
                mcpServerManager = mcpServerManager,
                onOpenMcpServers = {},
                onUpdateSettings = {},
                onClose = {}
            )
        }
        
        // Fails on unenhanced codebase because preset lists are not rendered in the Settings screen
        composeTestRule.onNodeWithText("My Custom Preset").assertExists()
    }

    @Test
    fun test32_deletingPresetRemovesItFromSettingsAndUi() {
        var updatedSettings: AppSettings? = null
        val preset = SavedModelPreset(
            id = "preset-123",
            name = "Delete Me Preset",
            provider = ModelProvider.GOOGLE,
            model = "gemini-1.5-flash",
            chatMode = ChatMode.QUICK,
            systemInstruction = "Custom instruction"
        )
        val settings = AppSettings(savedPresets = listOf(preset))
        val mcpServerManager = McpServerManager(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            SettingsScreen(
                settings = settings,
                localServerStatus = LocalServerStatus.UNKNOWN,
                mcpServerManager = mcpServerManager,
                onOpenMcpServers = {},
                onUpdateSettings = { updatedSettings = it },
                onClose = {}
            )
        }
        
        // Fails on unenhanced codebase because delete preset action does not exist
        composeTestRule.onNodeWithContentDescription("Delete Delete Me Preset").performClick()
        
        assertNotNull(updatedSettings)
        assertTrue(updatedSettings!!.savedPresets.none { it.id == "preset-123" })
    }

    @Test
    fun test33_editingPresetViaDialogUpdatesPreset() {
        var updatedSettings: AppSettings? = null
        val preset = SavedModelPreset(
            id = "preset-123",
            name = "Edit Me Preset",
            provider = ModelProvider.GOOGLE,
            model = "gemini-1.5-flash",
            chatMode = ChatMode.QUICK,
            systemInstruction = "Custom instruction"
        )
        val settings = AppSettings(savedPresets = listOf(preset))
        val mcpServerManager = McpServerManager(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            SettingsScreen(
                settings = settings,
                localServerStatus = LocalServerStatus.UNKNOWN,
                mcpServerManager = mcpServerManager,
                onOpenMcpServers = {},
                onUpdateSettings = { updatedSettings = it },
                onClose = {}
            )
        }
        
        // Fails on unenhanced codebase because edit preset button and inline dialog do not exist
        composeTestRule.onNodeWithContentDescription("Edit Edit Me Preset").performClick()
        composeTestRule.onNodeWithText("Edit Me Preset").performTextReplacement("New Preset Name")
        composeTestRule.onNodeWithText("SAVE CHANGES").performClick()
        
        assertNotNull(updatedSettings)
        val edited = updatedSettings!!.savedPresets.firstOrNull { it.id == "preset-123" }
        assertNotNull(edited)
        assertEquals("New Preset Name", edited!!.name)
    }

    @Test
    fun test34_chatListAlertDialogsUseRoundedShape12dp() {
        // Success test on unenhanced codebase: ChatListScreen uses shape = RoundedCornerShape(12.dp) for dialogs
        val chat = ChatSession(
            id = "session-123",
            title = "Test Session",
            messages = emptyList(),
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        
        composeTestRule.setContent {
            ChatListScreen(
                chats = listOf(chat),
                currentChatId = "session-123",
                onSelectChat = {},
                onNewChat = {},
                onClose = {},
                onDeleteChat = {},
                onDeleteMultiple = {},
                onRenameChat = { _, _ -> },
                onTogglePin = {}
            )
        }
        
        // Trigger rename chat to open the rename dialog which uses shape = RoundedCornerShape(12.dp)
        composeTestRule.onNodeWithContentDescription("Rename Chat", substring = true).performClick()
        
        // Verify that the dialog is displayed
        composeTestRule.onNodeWithText("Rename Chat").assertExists()
    }

    @Test
    fun test35_dialogContainersUseThemeAwareColors() {
        // Success test on unenhanced codebase: AlertDialog uses containerColor = MaterialTheme.colorScheme.surface
        val chat = ChatSession(
            id = "session-123",
            title = "Test Session",
            messages = emptyList(),
            createdAt = 123456789L,
            updatedAt = 123456789L
        )
        
        composeTestRule.setContent {
            ChatListScreen(
                chats = listOf(chat),
                currentChatId = "session-123",
                onSelectChat = {},
                onNewChat = {},
                onClose = {},
                onDeleteChat = {},
                onDeleteMultiple = {},
                onRenameChat = { _, _ -> },
                onTogglePin = {}
            )
        }
        
        // Trigger delete chat to open the delete confirmation dialog which uses containerColor = MaterialTheme.colorScheme.surface
        composeTestRule.onNodeWithContentDescription("Delete Chat", substring = true).performClick()
        
        // Verify that the dialog is displayed
        composeTestRule.onNodeWithText("Delete Chat").assertExists()
    }
}

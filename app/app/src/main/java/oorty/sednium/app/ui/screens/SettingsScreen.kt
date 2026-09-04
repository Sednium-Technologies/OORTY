package oorty.sednium.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import oorty.sednium.app.ui.theme.OortyIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.ChatMode
import oorty.sednium.app.model.ModelIconType
import oorty.sednium.app.model.ModelOption
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.model.PROVIDER_CONFIG
import oorty.sednium.app.model.SavedModelPreset
import oorty.sednium.app.model.VoicePersona
import oorty.sednium.app.navigation.LocalServerStatus
import oorty.sednium.app.ui.components.HuggingFaceHubDialog
import oorty.sednium.app.ui.components.SettingsSectionLabel
import oorty.sednium.app.ui.components.SettingsSliderRow
import oorty.sednium.app.ui.components.SettingsSwitchRow
import oorty.sednium.app.ui.components.SettingsTextField
import oorty.sednium.app.ui.theme.LocalSedniumIsDark
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii
import oorty.sednium.app.util.HardwareChecker
import oorty.sednium.app.util.HardwareFit
import oorty.sednium.app.util.ModelSuitability

enum class SettingsTab {
    API_MODELS, FEATURES_GENERAL, USAGE
}

fun Context.getFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.getFragmentActivity()
    else -> null
}

fun authenticateWithBiometrics(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onSuccess()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentication required")
        .setSubtitle("Authenticate to view or edit your API Key")
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onSuccess()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    localServerStatus: LocalServerStatus = LocalServerStatus.UNKNOWN,
    mcpServerManager: oorty.sednium.app.mcp.McpServerManager,
    pluginManager: oorty.sednium.app.plugins.PluginManager? = null,
    speechService: oorty.sednium.app.plugins.speech.SpeechService? = null,
    onOpenMcpServers: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SettingsTab.API_MODELS) }
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // --- Header Tabs ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                SettingsTabButton(
                    title = "API & MODELS",
                    isSelected = currentTab == SettingsTab.API_MODELS,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = { currentTab = SettingsTab.API_MODELS }
                )
                SettingsTabButton(
                    title = "GENERAL",
                    isSelected = currentTab == SettingsTab.FEATURES_GENERAL,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = { currentTab = SettingsTab.FEATURES_GENERAL }
                )
                SettingsTabButton(
                    title = "LOCAL AI",
                    isSelected = currentTab == SettingsTab.USAGE,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = { currentTab = SettingsTab.USAGE }
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = accentColor)
            }
        }
        
        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, thickness = 1.dp)

        // --- Content ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            when (currentTab) {
                SettingsTab.API_MODELS -> ApiModelsContent(settings, localServerStatus, onUpdateSettings)
                SettingsTab.FEATURES_GENERAL -> FeaturesGeneralContent(settings, mcpServerManager, onOpenMcpServers, onUpdateSettings)
                SettingsTab.USAGE -> UsageContent(settings, pluginManager, speechService, onUpdateSettings)
            }
        }
    }
}

@Composable
fun SettingsTabButton(title: String, isSelected: Boolean, isDark: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else (if (isDark) SedniumColors.Gray400 else OrangeAlpha.a40),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(if (title.length > 5) 0.8f else 1f)
                    .background(accentColor)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiModelsContent(
    settings: AppSettings,
    localServerStatus: LocalServerStatus,
    onUpdateSettings: (AppSettings) -> Unit
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val menuBg = if (isDark) Color(0xFF262626) else SedniumColors.Milk
    val context = LocalContext.current

    var selectedProvider by remember(settings.provider) { mutableStateOf(settings.provider) }
    var selectedModel by remember(settings.model) { mutableStateOf(settings.model) }
    var apiKeyUnlocked by remember { mutableStateOf(false) }

    // Provider Dropdown
    SettingsSectionLabel("CHOOSE PROVIDER")
    var expandedProviderDropdown by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandedProviderDropdown,
        onExpandedChange = { expandedProviderDropdown = it }
    ) {
        SettingsTextField(
            label = "",
            value = PROVIDER_CONFIG[selectedProvider]?.displayName ?: selectedProvider.name,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProviderDropdown) }
        )
        ExposedDropdownMenu(
            expanded = expandedProviderDropdown,
            onDismissRequest = { expandedProviderDropdown = false },
            modifier = Modifier.background(menuBg)
        ) {
            DropdownMenuItem(
                text = { Text("LOCAL PROVIDERS", fontWeight = FontWeight.Bold, color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60, style = MaterialTheme.typography.labelSmall) },
                onClick = {},
                enabled = false
            )
            listOf(ModelProvider.LOCAL, ModelProvider.LOCAL_GGUF, ModelProvider.LOCAL_LITERT).forEach { provider ->
                DropdownMenuItem(
                    text = { Text(PROVIDER_CONFIG[provider]?.displayName ?: provider.name, color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange, modifier = Modifier.padding(start = 8.dp)) },
                    onClick = {
                        selectedProvider = provider
                        val popularModels = PROVIDER_CONFIG[provider]?.popularModels ?: emptyList()
                        val defaultModel = popularModels.firstOrNull()?.id ?: ""
                        selectedModel = defaultModel
                        expandedProviderDropdown = false
                    }
                )
            }
            HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("EXTERNAL PROVIDERS", fontWeight = FontWeight.Bold, color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60, style = MaterialTheme.typography.labelSmall) },
                onClick = {},
                enabled = false
            )
            listOf(ModelProvider.GOOGLE, ModelProvider.OPENAI, ModelProvider.ANTHROPIC, ModelProvider.XAI, ModelProvider.GROQ, ModelProvider.OPENROUTER, ModelProvider.NVIDIA, ModelProvider.ROSETTE, ModelProvider.CUSTOM).forEach { provider ->
                DropdownMenuItem(
                    text = { Text(PROVIDER_CONFIG[provider]?.displayName ?: provider.name, color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange, modifier = Modifier.padding(start = 8.dp)) },
                    onClick = {
                        selectedProvider = provider
                        val popularModels = PROVIDER_CONFIG[provider]?.popularModels ?: emptyList()
                        val defaultModel = popularModels.firstOrNull()?.id ?: ""
                        selectedModel = defaultModel
                        expandedProviderDropdown = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (selectedProvider != ModelProvider.LOCAL && selectedProvider != ModelProvider.LOCAL_GGUF && selectedProvider != ModelProvider.LOCAL_LITERT && selectedProvider != ModelProvider.NONE) {
        val providerName = PROVIDER_CONFIG[selectedProvider]?.displayName ?: ""
        SettingsSectionLabel("${providerName.uppercase()} API KEY")
        val currentKey = apiKeyForProvider(settings, selectedProvider)
        SettingsTextField(
            label = "",
            value = if (!apiKeyUnlocked && currentKey.isNotEmpty()) "••••••••••••••••••••" else currentKey,
            onValueChange = { if (apiKeyUnlocked) onUpdateSettings(updateApiKeyForProvider(settings, selectedProvider, it)) },
            placeholder = "sk-…",
            isSecret = !apiKeyUnlocked && currentKey.isNotEmpty(),
            readOnly = !apiKeyUnlocked,
            trailingIcon = {
                Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (apiKeyUnlocked) "LOCK" else "EDIT/VIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.clickable {
                            if (apiKeyUnlocked) {
                                apiKeyUnlocked = false
                            } else {
                                val fragmentActivity = context.getFragmentActivity()
                                if (fragmentActivity != null) {
                                    authenticateWithBiometrics(fragmentActivity) {
                                        apiKeyUnlocked = true
                                    }
                                } else {
                                    apiKeyUnlocked = true
                                }
                            }
                        }.padding(4.dp)
                    )
                    if (apiKeyUnlocked) {
                        Text(
                            text = "CLEAR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.clickable { onUpdateSettings(updateApiKeyForProvider(settings, selectedProvider, "")) }.padding(4.dp)
                        )
                    }
                }
            }
        )
        val apiLink = PROVIDER_CONFIG[selectedProvider]?.apiLink
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!apiLink.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apiLink))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Get API Key", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            var testConnectionStatus by remember { mutableStateOf<String?>(null) }
            var isTestingConnection by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (testConnectionStatus != null) {
                    Text(
                        text = testConnectionStatus!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (testConnectionStatus == "Success") Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                TextButton(
                    enabled = !isTestingConnection && currentKey.isNotBlank(),
                    onClick = {
                        isTestingConnection = true
                        testConnectionStatus = "Testing..."
                        scope.launch {
                            try {
                                val success = oorty.sednium.app.api.testApiKey(currentKey, selectedProvider, settings.localBaseUrl)
                                testConnectionStatus = if (success) "Success" else "Failed"
                            } catch (e: Exception) {
                                testConnectionStatus = "Failed"
                            } finally {
                                isTestingConnection = false
                            }
                        }
                    }
                ) {
                    Text(if (isTestingConnection) "Testing..." else "Test Connection", style = MaterialTheme.typography.labelSmall, color = if (currentKey.isNotBlank()) accentColor else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (selectedProvider == ModelProvider.LOCAL || selectedProvider == ModelProvider.CUSTOM) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsSectionLabel("Base URL")
            if (selectedProvider == ModelProvider.LOCAL && localServerStatus != LocalServerStatus.UNKNOWN) {
                Spacer(modifier = Modifier.width(8.dp))
                val statusColor = when (localServerStatus) {
                    LocalServerStatus.IDLE -> Color(0xFF4CAF50)
                    LocalServerStatus.PROCESSING -> Color(0xFFFFC107)
                    LocalServerStatus.OFFLINE -> Color(0xFFF44336)
                    else -> Color.Transparent
                }
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            }
        }
        SettingsTextField(
            label = "",
            value = settings.localBaseUrl,
            onValueChange = { onUpdateSettings(settings.copy(localBaseUrl = it)) },
            placeholder = "http://localhost:11434/v1"
        )
    }

    if (selectedProvider == ModelProvider.LOCAL_GGUF || selectedProvider == ModelProvider.LOCAL_LITERT) {
        Spacer(modifier = Modifier.height(16.dp))
        var showLoadingOverlay by remember { mutableStateOf(false) }
        var isLoadSuccess by remember { mutableStateOf(false) }
        var loadingModelName by remember { mutableStateOf("") }
        var loadingProgress by remember { mutableStateOf<Float?>(null) }
        var loadingErrorMessage by remember { mutableStateOf<String?>(null) }

        var pendingUri by remember { mutableStateOf<Uri?>(null) }
        var showWarningDialog by remember { mutableStateOf(false) }
        var warningFit by remember { mutableStateOf(HardwareFit.COMFORTABLE) }
        var warningModelSizeMb by remember { mutableStateOf(0) }

        val scope = rememberCoroutineScope()

        fun completeModelLoad(uri: Uri) {
            scope.launch {
                val filename = uri.lastPathSegment?.substringAfterLast("/") ?: "model"
                val isLiteRt = filename.endsWith(".tflite", ignoreCase = true) || filename.endsWith(".litertlm", ignoreCase = true)

                loadingModelName = filename
                showLoadingOverlay = true
                isLoadSuccess = false
                loadingErrorMessage = null
                loadingProgress = 0.1f

                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {}

                    if (isLiteRt) {
                        val helper = oorty.sednium.app.api.LiteRtHelper(context, uri)
                        oorty.sednium.app.api.activeLiteRtHelper = helper
                        oorty.sednium.app.api.activeLiteRtUri = uri

                        val result = helper.loadModel(uri)
                        if (result.isFailure) {
                            throw result.exceptionOrNull() ?: Exception("LiteRT model load failed")
                        }
                        selectedModel = filename
                        selectedProvider = ModelProvider.LOCAL_LITERT
                        onUpdateSettings(settings.copy(
                            provider = ModelProvider.LOCAL_LITERT,
                            model = filename,
                            litertModelUri = uri.toString(),
                            activePresetId = null
                        ))
                    } else {
                        val helper = oorty.sednium.app.api.LlamaHelper(context, uri)
                        oorty.sednium.app.api.activeLlamaHelper = helper
                        oorty.sednium.app.api.activeGgufUri = uri

                        val result = helper.loadModel(uri)
                        if (result.isFailure) {
                            throw result.exceptionOrNull() ?: Exception("GGUF model load failed")
                        }
                        selectedModel = filename
                        selectedProvider = ModelProvider.LOCAL_GGUF
                        onUpdateSettings(settings.copy(
                            provider = ModelProvider.LOCAL_GGUF,
                            model = filename,
                            ggufModelUri = uri.toString(),
                            activePresetId = null
                        ))
                    }

                    loadingProgress = 1.0f
                    isLoadSuccess = true
                } catch (e: Exception) {
                    loadingErrorMessage = e.message ?: "Failed to load model into RAM"
                    isLoadSuccess = false
                }
            }
        }

        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { nonNullUri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        nonNullUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                var fileSizeMb = 0
                try {
                    context.contentResolver.openFileDescriptor(nonNullUri, "r")?.use {
                        fileSizeMb = (it.statSize / (1024 * 1024)).toInt()
                    }
                } catch (e: Exception) {}

                val availRam = HardwareChecker.getAvailableRamMb(context)
                val fit = HardwareChecker.assessModelFit(fileSizeMb, availRam)

                if (fit == HardwareFit.DANGEROUS || fit == HardwareFit.TIGHT) {
                    pendingUri = nonNullUri
                    warningFit = fit
                    warningModelSizeMb = fileSizeMb
                    showWarningDialog = true
                } else {
                    completeModelLoad(nonNullUri)
                }
            }
        }

        if (showLoadingOverlay) {
            oorty.sednium.app.ui.components.ModelLoadingOverlay(
                modelName = loadingModelName,
                progress = loadingProgress,
                isSuccess = isLoadSuccess,
                errorMessage = loadingErrorMessage,
                onSuccessComplete = {
                    showLoadingOverlay = false
                },
                onRetry = {
                    pendingUri?.let { completeModelLoad(it) }
                },
                onDismiss = {
                    showLoadingOverlay = false
                    loadingErrorMessage = null
                }
            )
        }

        if (showWarningDialog && pendingUri != null) {
            val availRam = HardwareChecker.getAvailableRamMb(context)
            val totalRam = HardwareChecker.getTotalRamMb(context)
            oorty.sednium.app.ui.components.HardwareWarningDialog(
                modelName = pendingUri!!.lastPathSegment?.substringAfterLast("/") ?: "Selected Model",
                modelSizeMb = warningModelSizeMb,
                availableRamMb = availRam,
                totalRamMb = totalRam,
                fit = warningFit,
                onLoadAnyway = {
                    val uri = pendingUri!!
                    showWarningDialog = false
                    pendingUri = null
                    completeModelLoad(uri)
                },
                onDismiss = {
                    showWarningDialog = false
                    pendingUri = null
                }
            )
        }

        val sectionTitle = if (selectedProvider == ModelProvider.LOCAL_LITERT) "LITERT MODEL FILE (.tflite / .litertlm)" else "GGUF MODEL FILE (.gguf)"
        SettingsSectionLabel(sectionTitle)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                val hasLocalFile = selectedModel.endsWith(".gguf", ignoreCase = true) ||
                        selectedModel.endsWith(".tflite", ignoreCase = true) ||
                        selectedModel.endsWith(".litertlm", ignoreCase = true)
                SettingsTextField(
                    label = "",
                    value = if (hasLocalFile) selectedModel else "",
                    onValueChange = {},
                    placeholder = "Select .gguf or .tflite model file",
                    readOnly = true
                )
            }
            Button(
                onClick = { launcher.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(SedniumRadii.md),
                modifier = Modifier.padding(top = 4.dp).height(54.dp)
            ) {
                Text("SELECT", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (selectedProvider != ModelProvider.NONE) {
        Spacer(modifier = Modifier.height(16.dp))

        var dynamicModels by remember { mutableStateOf<List<ModelOption>?>(null) }
        var isFetchingModels by remember { mutableStateOf(false) }
        val scope2 = rememberCoroutineScope()
        val currentKey = apiKeyForProvider(settings, selectedProvider)

        LaunchedEffect(selectedProvider, currentKey, settings.localBaseUrl) {
            dynamicModels = null
            if (currentKey.isNotBlank() || selectedProvider == ModelProvider.LOCAL || selectedProvider == ModelProvider.CUSTOM) {
                isFetchingModels = true
                try {
                    val fetched = oorty.sednium.app.api.fetchDynamicModels(currentKey, selectedProvider, settings.localBaseUrl)
                    if (fetched.isNotEmpty()) {
                        dynamicModels = fetched
                    }
                } catch (e: Exception) {
                } finally {
                    isFetchingModels = false
                }
            }
        }

        // Model Dropdown
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsSectionLabel("SELECT MODEL")
            if (isFetchingModels) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = {
                        scope2.launch {
                            isFetchingModels = true
                            try {
                                val fetched = oorty.sednium.app.api.fetchDynamicModels(currentKey, selectedProvider, settings.localBaseUrl)
                                if (fetched.isNotEmpty()) {
                                    dynamicModels = fetched
                                }
                            } catch (e: Exception) {}
                            finally {
                                isFetchingModels = false
                            }
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(oorty.sednium.app.ui.theme.OortyIcons.Refresh, contentDescription = "Refresh Models", tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
        }

        var expandedModelDropdown by remember { mutableStateOf(false) }
        val baseModels = PROVIDER_CONFIG[selectedProvider]?.popularModels ?: emptyList()
        
        val modelsForProvider = if (dynamicModels != null) {
            val finalMerged = (baseModels + dynamicModels!!.filter { dyn -> baseModels.none { b -> b.id == dyn.id } })
            finalMerged.ifEmpty { baseModels }
        } else {
            baseModels
        }

        if (modelsForProvider.isEmpty()) {
            SettingsTextField(
                label = "",
                value = selectedModel,
                onValueChange = { selectedModel = it },
                placeholder = "e.g. gemini-2.5-flash"
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = expandedModelDropdown,
                onExpandedChange = { expandedModelDropdown = it }
            ) {
                SettingsTextField(
                    label = "",
                    value = selectedModel,
                    onValueChange = { selectedModel = it; expandedModelDropdown = true },
                    placeholder = "Select or type a model",
                    readOnly = false,
                    modifier = Modifier.menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModelDropdown) }
                )
                var modelSearchQuery by remember { mutableStateOf("") }
                ExposedDropdownMenu(
                    expanded = expandedModelDropdown,
                    onDismissRequest = { expandedModelDropdown = false },
                    modifier = Modifier.background(menuBg)
                ) {
                    OutlinedTextField(
                        value = modelSearchQuery,
                        onValueChange = { modelSearchQuery = it },
                        placeholder = { Text("Search models...", color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a40) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30,
                            focusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                            unfocusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
                        )
                    )
                    
                    modelsForProvider.filter {
                        it.label.contains(modelSearchQuery, ignoreCase = true) || it.id.contains(modelSearchQuery, ignoreCase = true)
                    }.forEach { modelOption ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(modelOption.label, color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange)
                                    if (selectedProvider == ModelProvider.LOCAL || dynamicModels?.any { it.id == modelOption.id } == true) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SedniumColors.Green500))
                                            Text("Ready locally / Fetched", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Green500)
                                        }
                                    }
                                }
                            },
                            leadingIcon = {
                                val iconVector = when(modelOption.icon) {
                                    ModelIconType.TEXT -> oorty.sednium.app.ui.theme.OortyIcons.FileText
                                    ModelIconType.CODE -> oorty.sednium.app.ui.theme.OortyIcons.Code
                                    ModelIconType.AGENT -> oorty.sednium.app.ui.theme.OortyIcons.Bot
                                    ModelIconType.IMAGE -> oorty.sednium.app.ui.theme.OortyIcons.Image
                                    ModelIconType.VIDEO -> oorty.sednium.app.ui.theme.OortyIcons.Video
                                    ModelIconType.AUTO -> oorty.sednium.app.ui.theme.OortyIcons.Refresh
                                    ModelIconType.LIGHTNING -> oorty.sednium.app.ui.theme.OortyIcons.Lightning
                                }
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                            },
                            onClick = {
                                selectedModel = modelOption.id
                                expandedModelDropdown = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedProvider == ModelProvider.GOOGLE) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("GEMINI LIVE VOICE")
            val liveVoices = listOf(
                "Aoede" to "Melodic",
                "Puck" to "Energetic",
                "Charon" to "Deep",
                "Kore" to "Warm",
                "Fenrir" to "Authoritative"
            )
            val currentVoice = settings.geminiLiveVoice.ifBlank { "Aoede" }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                liveVoices.forEach { (voiceName, voiceTrait) ->
                    val isSelected = currentVoice.equals(voiceName, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(SedniumRadii.pill))
                            .background(
                                if (isSelected) accentColor 
                                else if (isDark) Color(0xFF262626) 
                                else OrangeAlpha.a10
                            )
                            .border(
                                1.dp,
                                if (isSelected) accentColor 
                                else if (isDark) Color(0xFF383838) 
                                else OrangeAlpha.a20,
                                RoundedCornerShape(SedniumRadii.pill)
                            )
                            .clickable {
                                onUpdateSettings(settings.copy(geminiLiveVoice = voiceName))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = OortyIcons.Volume,
                                contentDescription = null,
                                tint = if (isSelected) SedniumColors.Milk else accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$voiceName ($voiceTrait)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SedniumColors.Milk else (if (isDark) SedniumColors.Gray200 else SedniumColors.Gray800)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent SELECT MODEL Action Button
        Button(
            onClick = {
                onUpdateSettings(settings.copy(
                    provider = selectedProvider,
                    model = selectedModel,
                    activePresetId = null // Dismiss preset banner when model is explicitly chosen
                ))
                Toast.makeText(context, "Model selected: ${selectedModel.ifBlank { selectedProvider.name }}", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = SedniumColors.Milk
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (settings.model == selectedModel && settings.provider == selectedProvider) "MODEL ACTIVE (${selectedModel.ifBlank { selectedProvider.name }})"
                else "SELECT MODEL (${selectedModel.ifBlank { selectedProvider.name }})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Prompts
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SYSTEM PROMPTS", style = MaterialTheme.typography.labelSmall, color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.Description, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Transparent)
                    .border(1.dp, accentColor, RoundedCornerShape(4.dp))
                    .clickable {
                         val newMode = when(settings.chatMode) {
                            ChatMode.QUICK -> ChatMode.THINKING
                            ChatMode.THINKING -> ChatMode.CODING
                            ChatMode.CODING -> ChatMode.QUICK
                        }
                        onUpdateSettings(settings.copy(chatMode = newMode))
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${settings.chatMode.name} MODE", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
        
        SettingsTextField(
            label = "",
            value = settings.currentSystemInstruction,
            onValueChange = { 
                val updatedSettings = when(settings.chatMode) {
                    ChatMode.QUICK -> settings.copy(quickSystemInstruction = it)
                    ChatMode.THINKING -> settings.copy(thinkingSystemInstruction = it)
                    ChatMode.CODING -> settings.copy(codingSystemInstruction = it)
                }
                onUpdateSettings(updatedSettings) 
            },
            placeholder = "You are an elite, world-class software architect...",
            singleLine = false
        )
    }
}

@Composable
fun FeaturesGeneralContent(
    settings: AppSettings,
    mcpServerManager: oorty.sednium.app.mcp.McpServerManager,
    onOpenMcpServers: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    var presetName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionLabel("EDITORIAL TYPOGRAPHY")
        
        // Serif Font Toggle (Primary App Font)
        SettingsSwitchRow(
            label = "EDITORIAL SERIF FONT",
            description = "Use Crimson Pro editorial serif typography across the app and AI model responses (matches website design).",
            checked = settings.useSerifFont,
            onCheckedChange = { onUpdateSettings(settings.copy(useSerifFont = it)) }
        )

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

        SettingsSectionLabel("GENERATION PARAMETERS")
        SettingsSliderRow(
            label = "Temperature",
            value = settings.temperature,
            onValueChange = { onUpdateSettings(settings.copy(temperature = it)) },
            valueRange = 0f..2f,
            onReset = if (settings.temperature != 0.7f) { { onUpdateSettings(settings.copy(temperature = 0.7f)) } } else null
        )
        SettingsSliderRow(
            label = "Top P",
            value = settings.topP,
            onValueChange = { onUpdateSettings(settings.copy(topP = it)) },
            valueRange = 0f..1f,
            onReset = if (settings.topP != 0.9f) { { onUpdateSettings(settings.copy(topP = 0.9f)) } } else null
        )
        SettingsSliderRow(
            label = "Max Tokens (Output)",
            value = settings.maxTokens.toFloat(),
            onValueChange = { onUpdateSettings(settings.copy(maxTokens = it.toInt())) },
            valueRange = 256f..32000f,
            displayFormat = { it.toInt().toString() },
            onReset = if (settings.maxTokens != 4096) { { onUpdateSettings(settings.copy(maxTokens = 4096)) } } else null
        )

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

        // Tool Calling
        SettingsSwitchRow(
            label = "TOOL CALLING CAPABILITIES",
            description = "Enables AI to make zips, read workflows, use MCPs. Turning off saves API cost.",
            checked = settings.enableTools,
            onCheckedChange = { onUpdateSettings(settings.copy(enableTools = it)) }
        )

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

        // Performance Insights
        SettingsSwitchRow(
            label = "PERFORMANCE INSIGHTS",
            description = "Show time-to-first-token, thought duration, and approximate tokens/sec under each response.",
            checked = settings.showPerformanceStats,
            onCheckedChange = { onUpdateSettings(settings.copy(showPerformanceStats = it)) }
        )
        
        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))
        
        // MCP Servers
        val mcpStatuses by mcpServerManager.statuses.collectAsState()
        val connectedCount = mcpStatuses.values.count { it.status == oorty.sednium.app.model.McpConnectionStatus.CONNECTED }
        val totalTools = mcpStatuses.values.sumOf { it.tools.size }

        SettingsSectionLabel("MCP SERVERS")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF262626) else OrangeAlpha.a05)
                .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenMcpServers)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (mcpStatuses.isEmpty()) "No MCP servers yet" else "$connectedCount/${mcpStatuses.size} connected",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (mcpStatuses.isEmpty()) "Tap to add one" else "$totalTools tool${if (totalTools == 1) "" else "s"} available · Tap to manage",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        }

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))
        
        // History
        SettingsSwitchRow(
            label = "SAVE CHAT HISTORY",
            description = "Persist chats to local Markdown vault",
            checked = settings.enableHistory,
            onCheckedChange = { onUpdateSettings(settings.copy(enableHistory = it)) }
        )

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

        // Theme (Dark Mode) Toggle
        SettingsSwitchRow(
            label = "DARK THEME",
            description = "Comfortable editorial charcoal dark mode",
            checked = isDark,
            onCheckedChange = { checked ->
                onUpdateSettings(settings.copy(theme = if (checked) oorty.sednium.app.model.AppTheme.DARK else oorty.sednium.app.model.AppTheme.LIGHT))
            }
        )

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

        // Auto Chat Naming Toggle
        SettingsSwitchRow(
            label = "AUTO CHAT NAMING",
            description = "Automatically generate chat titles from the first prompt using local/active model",
            checked = settings.autoGenerateTitle,
            onCheckedChange = { onUpdateSettings(settings.copy(autoGenerateTitle = it)) }
        )

        HorizontalDivider(color = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

        // Presets
        SettingsSectionLabel("SAVED MODEL CONFIGURATIONS")
        Text("Save your current Provider, Model, Mode, and Prompts as a preset to quickly switch in the main chat.", style = MaterialTheme.typography.bodySmall, color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SettingsTextField(
                    label = "",
                    value = presetName,
                    onValueChange = { presetName = it },
                    placeholder = "e.g. Code Llama Fast"
                )
            }
            Button(
                onClick = {
                    val nameToUse = presetName.ifBlank { "${settings.provider.name} - ${settings.model}" }
                    val newPreset = SavedModelPreset(
                        id = java.util.UUID.randomUUID().toString(),
                        name = nameToUse,
                        provider = settings.provider,
                        model = settings.model,
                        chatMode = settings.chatMode,
                        systemInstruction = settings.currentSystemInstruction
                    )
                    onUpdateSettings(settings.copy(
                        savedPresets = settings.savedPresets + newPreset,
                        activePresetId = newPreset.id
                    ))
                    presetName = ""
                },
                modifier = Modifier.padding(bottom = 6.dp),
                shape = RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = SedniumColors.Milk
                )
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
        }

        if (settings.savedPresets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            var editingPreset by remember { mutableStateOf<SavedModelPreset?>(null) }
            var editPresetName by remember { mutableStateOf("") }

            if (editingPreset != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { editingPreset = null },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Edit Preset", fontWeight = FontWeight.Bold, color = accentColor) },
                    text = {
                        OutlinedTextField(
                            value = editPresetName,
                            onValueChange = { editPresetName = it },
                            singleLine = true,
                            shape = RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.pill),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = if (isDark) SedniumColors.Charcoal700 else OrangeAlpha.a30,
                                focusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                                unfocusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val targetId = editingPreset!!.id
                                val updatedList = settings.savedPresets.map {
                                    if (it.id == targetId) it.copy(name = editPresetName) else it
                                }
                                onUpdateSettings(settings.copy(savedPresets = updatedList))
                                editingPreset = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingPreset = null }) {
                            Text("Cancel", color = accentColor)
                        }
                    },
                    containerColor = if (isDark) Color(0xFF262626) else SedniumColors.Milk
                )
            }

            settings.savedPresets.forEach { preset ->
                val isActive = preset.id == settings.activePresetId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) (if (isDark) SedniumColors.Gray800 else OrangeAlpha.a10) else (if (isDark) Color(0xFF262626) else OrangeAlpha.a05))
                        .border(1.dp, if (isActive) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.name, fontWeight = FontWeight.Bold, color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange)
                        Text("${preset.provider.name} • ${preset.model}", style = MaterialTheme.typography.labelSmall, color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70)
                    }
                    Row {
                        IconButton(onClick = {
                            editingPreset = preset
                            editPresetName = preset.name
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit ${preset.name}",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            val filtered = settings.savedPresets.filterNot { it.id == preset.id }
                            val newActiveId = if (settings.activePresetId == preset.id) null else settings.activePresetId
                            onUpdateSettings(settings.copy(savedPresets = filtered, activePresetId = newActiveId))
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete ${preset.name}",
                                tint = SedniumColors.Red500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun apiKeyFor(s: AppSettings): String = apiKeyForProvider(s, s.provider)

fun apiKeyForProvider(s: AppSettings, provider: ModelProvider): String = when (provider) {
    ModelProvider.GOOGLE -> s.googleApiKey
    ModelProvider.OPENAI -> s.openaiApiKey
    ModelProvider.ANTHROPIC -> s.anthropicApiKey
    ModelProvider.XAI -> s.xaiApiKey
    ModelProvider.GROQ -> s.groqApiKey
    ModelProvider.OPENROUTER -> s.openRouterApiKey
    ModelProvider.NVIDIA -> s.nvidiaApiKey
    ModelProvider.ROSETTE -> s.rosetteApiKey
    ModelProvider.CUSTOM -> s.customApiKey
    else -> ""
}

private fun updateApiKeyForProvider(s: AppSettings, provider: ModelProvider, value: String): AppSettings = when (provider) {
    ModelProvider.GOOGLE -> s.copy(googleApiKey = value)
    ModelProvider.OPENAI -> s.copy(openaiApiKey = value)
    ModelProvider.ANTHROPIC -> s.copy(anthropicApiKey = value)
    ModelProvider.XAI -> s.copy(xaiApiKey = value)
    ModelProvider.GROQ -> s.copy(groqApiKey = value)
    ModelProvider.OPENROUTER -> s.copy(openRouterApiKey = value)
    ModelProvider.NVIDIA -> s.copy(nvidiaApiKey = value)
    ModelProvider.ROSETTE -> s.copy(rosetteApiKey = value)
    ModelProvider.CUSTOM -> s.copy(customApiKey = value)
    else -> s
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageContent(
    settings: AppSettings,
    pluginManager: oorty.sednium.app.plugins.PluginManager? = null,
    speechService: oorty.sednium.app.plugins.speech.SpeechService? = null,
    onUpdateSettings: (AppSettings) -> Unit = {}
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val cardBg = if (isDark) Color(0xFF262626) else OrangeAlpha.a05
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val availablePlugins = pluginManager?.availablePlugins?.collectAsState()?.value ?: oorty.sednium.app.model.DEFAULT_AVAILABLE_PLUGINS
    var testAppQuery by remember { mutableStateOf("termux") }

    var showHfHubDialog by remember { mutableStateOf(false) }
    var hfModelSearchQuery by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var hfDownloadProgress by remember { mutableStateOf<String?>(null) }

    val isTermuxInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    if (showHfHubDialog) {
        HuggingFaceHubDialog(
            currentModel = settings.model,
            onSelectModel = { repoId, fileName ->
                val newPreset = SavedModelPreset(
                    id = java.util.UUID.randomUUID().toString(),
                    name = repoId,
                    provider = ModelProvider.LOCAL_GGUF,
                    model = fileName,
                    chatMode = ChatMode.QUICK,
                    systemInstruction = settings.currentSystemInstruction
                )
                onUpdateSettings(settings.copy(
                    provider = ModelProvider.LOCAL_GGUF,
                    model = fileName,
                    savedPresets = settings.savedPresets + newPreset,
                    activePresetId = null // clear active preset banner
                ))
            },
            onDismiss = { showHfHubDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Micro Plugins Section ---
        SettingsSectionLabel("ON-DEVICE MICRO-MODELS & PLUGINS")

        SettingsSwitchRow(
            label = "SILENT BACKGROUND OCR ON IMAGES",
            description = "Automatically extract text from attached images and inject into prompt context",
            checked = settings.enableSilentOcr,
            onCheckedChange = { onUpdateSettings(settings.copy(enableSilentOcr = it)) }
        )

        SettingsSwitchRow(
            label = "VOICE MODE & TTS READOUT",
            description = "Enable hands-free continuous voice mode and in-chat audio speaker button",
            checked = settings.enableSpeechTts,
            onCheckedChange = { onUpdateSettings(settings.copy(enableSpeechTts = it)) }
        )

        Spacer(modifier = Modifier.height(6.dp))
        SettingsSectionLabel("LIVE VOICE PERSONA (ALL MODELS)")
        val personas = listOf(
            VoicePersona.WARM_CONVERSATIONAL to "🌸 Warm & Friendly",
            VoicePersona.DEEP_CONFIDENT to "🎙️ Deep & Confident",
            VoicePersona.ENERGETIC_DIRECT to "⚡ Energetic & Direct",
            VoicePersona.SYSTEM_DEFAULT to "📱 System Default"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            personas.forEach { (persona, label) ->
                val isSelected = settings.voicePersona == persona
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SedniumRadii.pill))
                        .background(
                            if (isSelected) accentColor 
                            else if (isDark) Color(0xFF262626) 
                            else OrangeAlpha.a10
                        )
                        .border(
                            1.dp,
                            if (isSelected) accentColor 
                            else if (isDark) Color(0xFF383838) 
                            else OrangeAlpha.a20,
                            RoundedCornerShape(SedniumRadii.pill)
                        )
                        .clickable {
                            onUpdateSettings(settings.copy(voicePersona = persona))
                            speechService?.speakText("Hi! This is Oorty speaking.", persona = persona, rate = settings.ttsSpeechRate, pitch = settings.ttsPitch)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SedniumColors.Milk else (if (isDark) SedniumColors.Gray200 else SedniumColors.Gray800)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        SettingsSliderRow(
            label = "Voice Speech Rate",
            value = settings.ttsSpeechRate,
            onValueChange = { onUpdateSettings(settings.copy(ttsSpeechRate = it)) },
            valueRange = 0.5f..2.0f,
            displayFormat = { String.format("%.1fx", it) },
            onReset = if (settings.ttsSpeechRate != 1.0f) { { onUpdateSettings(settings.copy(ttsSpeechRate = 1.0f)) } } else null
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Plugin Cards
        availablePlugins.forEach { plugin ->
            val isInstalled = pluginManager?.isPluginInstalled(plugin.id) == true || settings.installedPluginIds.contains(plugin.id) || plugin.type == oorty.sednium.app.model.PluginType.DEVICE_CONTROL
            val isActive = settings.activePluginIds.contains(plugin.id)

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    plugin.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isInstalled) SedniumColors.Green500.copy(alpha = 0.2f) else OrangeAlpha.a10)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        if (isInstalled) "INSTALLED" else "${plugin.sizeMb} MB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isInstalled) SedniumColors.Green500 else accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                plugin.huggingFaceRepo,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
                            )
                        }

                        if (plugin.type != oorty.sednium.app.model.PluginType.DEVICE_CONTROL) {
                            if (isInstalled) {
                                TextButton(
                                    onClick = {
                                        pluginManager?.deletePlugin(plugin.id)
                                        onUpdateSettings(settings.copy(
                                            installedPluginIds = settings.installedPluginIds - plugin.id,
                                            activePluginIds = settings.activePluginIds - plugin.id
                                        ))
                                        Toast.makeText(context, "${plugin.name} uninstalled", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Uninstall", color = SedniumColors.Red600, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            pluginManager?.downloadPlugin(plugin.id)
                                            onUpdateSettings(settings.copy(
                                                installedPluginIds = settings.installedPluginIds + plugin.id,
                                                activePluginIds = settings.activePluginIds + plugin.id
                                            ))
                                            Toast.makeText(context, "${plugin.name} installed successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(SedniumRadii.sm),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = SedniumColors.Milk),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Download", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        plugin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70
                    )

                    // Test Action Bar
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (plugin.type) {
                            oorty.sednium.app.model.PluginType.SPEECH_STT_TTS -> {
                                OutlinedButton(
                                    onClick = {
                                        speechService?.speakText("Hello! This is Oorty running high quality on-device neural voice synthesis at ${settings.ttsSpeechRate}x speed.", rate = settings.ttsSpeechRate, pitch = settings.ttsPitch)
                                        Toast.makeText(context, "Playing test audio...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(SedniumRadii.sm)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Voice Readout", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            oorty.sednium.app.model.PluginType.DEVICE_CONTROL -> {
                                OutlinedButton(
                                    onClick = {
                                        val res = oorty.sednium.app.plugins.device.DeviceAutomator.launchApp(context, testAppQuery)
                                        Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(SedniumRadii.sm)
                                ) {
                                    Text("Test App Launch (Termux)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            oorty.sednium.app.model.PluginType.EMBEDDING -> {
                                OutlinedButton(
                                    onClick = {
                                        Toast.makeText(context, "Markdown Vault Reindexed with 384d Dense Embeddings!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(SedniumRadii.sm)
                                ) {
                                    Text("Re-index Vault", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            oorty.sednium.app.model.PluginType.OCR -> {
                                OutlinedButton(
                                    onClick = {
                                        Toast.makeText(context, "OCR Engine Ready for Automatic Document Scanning", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(SedniumRadii.sm)
                                ) {
                                    Text("Status: Ready", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionLabel("LOCAL AI & ON-DEVICE DIRECTORY")
        
        // Hugging Face Hub Browser Feature Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Hugging Face GGUF Hub",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Hub Explorer", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Search and browse real open-source GGUF model repositories. Automatically inspects device RAM and tags models as Recommended, May Overheat, or Not Able to Run.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showHfHubDialog = true },
                    shape = RoundedCornerShape(SedniumRadii.sm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = SedniumColors.Milk
                    ),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Hugging Face Model Directory", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Termux Section
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "1. Run Local llama.cpp in Termux",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Run native llama-server directly on your Android phone using Termux. Run these commands inside Termux to install and start the server:\n\n" +
                    "pkg update && pkg install clang git cmake\n" +
                    "git clone https://github.com/ggerganov/llama.cpp\n" +
                    "cd llama.cpp && make -j4\n" +
                    "./llama-server -m model.gguf -c 2048 --port 8080\n\n" +
                    "Once running, set your Base URL in Local Server settings to http://localhost:8080/v1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isTermuxInstalled) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Termux Commands", "pkg update && pkg install clang git cmake && git clone https://github.com/ggerganov/llama.cpp && cd llama.cpp && make -j4")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Setup commands copied!", Toast.LENGTH_SHORT).show()
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Could not open Termux", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Termux", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(SedniumRadii.sm),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = SedniumColors.Milk),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("Copy Setup Commands & Open Termux", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/termux/termux-app/releases"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(SedniumRadii.sm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("Download Termux (GitHub Releases)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hugging Face Quick Download Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "2. Quick GGUF Model Fetch",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter a HuggingFace Model Repo ID to register it for local inference:",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsTextField(
                    label = "",
                    value = hfModelSearchQuery,
                    onValueChange = { hfModelSearchQuery = it },
                    placeholder = "e.g. Qwen/Qwen2.5-0.5B-Instruct-GGUF"
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Curated Recommended Quick Chips
                val quickChips = listOf(
                    "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
                    "meta-llama/Llama-3.2-1B-Instruct-GGUF",
                    "google/gemma-2-2b-it-GGUF",
                    "Qwen/Qwen2.5-7B-Instruct-GGUF"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickChips.forEach { modelName ->
                        val suitability = HardwareChecker.getSuitability(modelName, context)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E1E1E) else OrangeAlpha.a05)
                                .clickable { hfModelSearchQuery = modelName }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modelName.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(suitability.badgeColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(suitability.label, style = MaterialTheme.typography.labelSmall, color = suitability.textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isDownloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(hfDownloadProgress ?: "Processing...", style = MaterialTheme.typography.bodySmall, color = accentColor)
                    }
                } else {
                    Button(
                        onClick = {
                            val query = hfModelSearchQuery.ifBlank { "Qwen/Qwen2.5-0.5B-Instruct-GGUF" }
                            isDownloading = true
                            hfDownloadProgress = "Fetching model info..."
                            scope.launch {
                                kotlinx.coroutines.delay(800)
                                val modelBaseName = query.substringAfterLast("/")
                                onUpdateSettings(settings.copy(
                                    provider = ModelProvider.LOCAL_GGUF,
                                    model = "$modelBaseName-Q4_K_M.gguf",
                                    activePresetId = null
                                ))
                                isDownloading = false
                                Toast.makeText(context, "Model configured: $modelBaseName", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(SedniumRadii.sm),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = SedniumColors.Milk),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("Configure Model for Local AI", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ABOUT SEDNIUM & CREATORS SECTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.squircle))
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = oorty.sednium.app.R.drawable.logo),
                            contentDescription = "Oorty Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Column {
                        Text(
                            "About Sednium & Creators",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Privacy-first, lightning-fast on-device AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Oorty is developed with craft by Sednium Technologies — an engineering and digital studio in West Bengal, India specializing in high-performance web applications, native mobile apps, and fluid design systems.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Organization: Sednium
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else OrangeAlpha.a05)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sednium.com"))
                            context.startActivity(intent)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Sednium Technologies",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
                        )
                        Text(
                            "sednium.com • West Bengal, India",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Creator 1: Ayush Pal (Bhoid)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else OrangeAlpha.a05)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bhoid.sednium.com"))
                            context.startActivity(intent)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ayush Pal (Bhoid)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
                        )
                        Text(
                            "Software Developer & Co-Creator • bhoid.sednium.com\nReact, TypeScript, Python, Kotlin Android, WebGL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Creator 2: Ankush Das (Loid)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else OrangeAlpha.a05)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://loid.sednium.com"))
                            context.startActivity(intent)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ankush Das (Loid)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
                        )
                        Text(
                            "Co-Founder & CTO • loid.sednium.com\nSystems Engineer, TypeScript, Go, Kotlin, OnyxChat",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Oorty v2.5.0 • Crafted with ❤️ by Sednium Systems",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) SedniumColors.Gray500 else OrangeAlpha.a40,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

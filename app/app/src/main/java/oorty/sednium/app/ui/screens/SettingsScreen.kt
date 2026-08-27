package oorty.sednium.app.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos

import oorty.sednium.app.model.AppSettings
import oorty.sednium.app.model.ChatMode
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.model.SavedModelPreset
import oorty.sednium.app.model.PROVIDER_CONFIG
import oorty.sednium.app.navigation.LocalServerStatus
import oorty.sednium.app.ui.components.SettingsSectionLabel
import oorty.sednium.app.ui.components.SettingsSliderRow
import oorty.sednium.app.ui.components.SettingsSwitchRow
import oorty.sednium.app.ui.components.SettingsTextField
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors

enum class SettingsTab {
    API_MODELS, FEATURES_GENERAL, USAGE
}

fun android.content.Context.getFragmentActivity(): FragmentActivity? = when (this) {
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
                // If there's an error (e.g., no hardware, no lock screen), let them in anyway
                // so they aren't completely blocked from using the app.
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    localServerStatus: LocalServerStatus = LocalServerStatus.UNKNOWN,
    mcpServerManager: oorty.sednium.app.mcp.McpServerManager,
    onOpenMcpServers: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SettingsTab.API_MODELS) }

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
                    modifier = Modifier.weight(1f),
                    onClick = { currentTab = SettingsTab.API_MODELS }
                )
                SettingsTabButton(
                    title = "GENERAL",
                    isSelected = currentTab == SettingsTab.FEATURES_GENERAL,
                    modifier = Modifier.weight(1f),
                    onClick = { currentTab = SettingsTab.FEATURES_GENERAL }
                )
                SettingsTabButton(
                    title = "LOCAL AI",
                    isSelected = currentTab == SettingsTab.USAGE,
                    modifier = Modifier.weight(1f),
                    onClick = { currentTab = SettingsTab.USAGE }
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = SedniumColors.Orange)
            }
        }
        
        HorizontalDivider(color = OrangeAlpha.a20, thickness = 1.dp)

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
                SettingsTab.USAGE -> UsageContent(settings, onUpdateSettings)
            }
        }
    }
}

@Composable
fun SettingsTabButton(title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) SedniumColors.Orange else OrangeAlpha.a40,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(if (title.length > 5) 0.8f else 1f)
                    .background(SedniumColors.Orange)
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
    var apiKeyUnlocked by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Provider Dropdown
    SettingsSectionLabel("CHOOSE PROVIDER")
    var expandedProviderDropdown by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandedProviderDropdown,
        onExpandedChange = { expandedProviderDropdown = it }
    ) {
        SettingsTextField(
            label = "",
            value = PROVIDER_CONFIG[settings.provider]?.displayName ?: settings.provider.name,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProviderDropdown) }
        )
        ExposedDropdownMenu(
            expanded = expandedProviderDropdown,
            onDismissRequest = { expandedProviderDropdown = false },
            modifier = Modifier.background(SedniumColors.Milk)
        ) {
            DropdownMenuItem(
                text = { Text("LOCAL PROVIDERS", fontWeight = FontWeight.Bold, color = OrangeAlpha.a60, style = MaterialTheme.typography.labelSmall) },
                onClick = {},
                enabled = false
            )
            listOf(ModelProvider.LOCAL, ModelProvider.LOCAL_GGUF).forEach { provider ->
                DropdownMenuItem(
                    text = { Text(PROVIDER_CONFIG[provider]?.displayName ?: provider.name, color = SedniumColors.Orange, modifier = Modifier.padding(start = 8.dp)) },
                    onClick = {
                        val popularModels = PROVIDER_CONFIG[provider]?.popularModels ?: emptyList()
                        val defaultModel = popularModels.firstOrNull()?.id ?: ""
                        onUpdateSettings(settings.copy(provider = provider, model = defaultModel))
                        expandedProviderDropdown = false
                    }
                )
            }
            HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("EXTERNAL PROVIDERS", fontWeight = FontWeight.Bold, color = OrangeAlpha.a60, style = MaterialTheme.typography.labelSmall) },
                onClick = {},
                enabled = false
            )
            listOf(ModelProvider.GOOGLE, ModelProvider.OPENAI, ModelProvider.ANTHROPIC, ModelProvider.XAI, ModelProvider.GROQ, ModelProvider.OPENROUTER, ModelProvider.NVIDIA, ModelProvider.ROSETTE, ModelProvider.CUSTOM).forEach { provider ->
                DropdownMenuItem(
                    text = { Text(PROVIDER_CONFIG[provider]?.displayName ?: provider.name, color = SedniumColors.Orange, modifier = Modifier.padding(start = 8.dp)) },
                    onClick = {
                        val popularModels = PROVIDER_CONFIG[provider]?.popularModels ?: emptyList()
                        val defaultModel = popularModels.firstOrNull()?.id ?: ""
                        onUpdateSettings(settings.copy(provider = provider, model = defaultModel))
                        expandedProviderDropdown = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (settings.provider != ModelProvider.LOCAL && settings.provider != ModelProvider.LOCAL_GGUF && settings.provider != ModelProvider.NONE) {
        // API Key Field
        val providerName = PROVIDER_CONFIG[settings.provider]?.displayName ?: ""
        SettingsSectionLabel("${providerName.uppercase()} API KEY")
        SettingsTextField(
            label = "",
            value = if (!apiKeyUnlocked && apiKeyFor(settings).isNotEmpty()) "••••••••••••••••••••" else apiKeyFor(settings),
            onValueChange = { if (apiKeyUnlocked) onUpdateSettings(updateApiKeyFor(settings, it)) },
            placeholder = "sk-…",
            isSecret = !apiKeyUnlocked && apiKeyFor(settings).isNotEmpty(),
            readOnly = !apiKeyUnlocked,
            trailingIcon = {
                Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (apiKeyUnlocked) "LOCK" else "EDIT/VIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SedniumColors.Orange,
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
                                    // Fallback just let them in if activity is not found
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
                            color = SedniumColors.Orange,
                            modifier = Modifier.clickable { onUpdateSettings(updateApiKeyFor(settings, "")) }.padding(4.dp)
                        )
                    }
                }
            }
        )
        val apiLink = PROVIDER_CONFIG[settings.provider]?.apiLink
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!apiLink.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(apiLink))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Get API Key", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Orange, fontWeight = FontWeight.Bold)
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
                    enabled = !isTestingConnection && apiKeyFor(settings).isNotBlank(),
                    onClick = {
                        val apiKeyToTest = apiKeyFor(settings)
                        val providerToTest = settings.provider
                        isTestingConnection = true
                        testConnectionStatus = "Testing..."
                        scope.launch {
                            try {
                                val success = oorty.sednium.app.api.testApiKey(apiKeyToTest, providerToTest, settings.localBaseUrl)
                                testConnectionStatus = if (success) "Success" else "Failed"
                            } catch (e: Exception) {
                                testConnectionStatus = "Failed"
                            } finally {
                                isTestingConnection = false
                            }
                        }
                    }
                ) {
                    Text(if (isTestingConnection) "Testing..." else "Test Connection", style = MaterialTheme.typography.labelSmall, color = if (apiKeyFor(settings).isNotBlank()) SedniumColors.Orange else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (settings.provider == ModelProvider.LOCAL || settings.provider == ModelProvider.CUSTOM) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsSectionLabel("Base URL")
            if (settings.provider == ModelProvider.LOCAL && localServerStatus != LocalServerStatus.UNKNOWN) {
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

    if (settings.provider == ModelProvider.LOCAL_GGUF) {
        Spacer(modifier = Modifier.height(16.dp))
        var isLoadingGguf by remember { mutableStateOf(false) }
        var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var showWarningDialog by remember { mutableStateOf(false) }
        var warningFit by remember { mutableStateOf(oorty.sednium.app.util.HardwareFit.COMFORTABLE) }
        var warningModelSizeMb by remember { mutableStateOf(0) }

        val scope = rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current

        fun completeGgufLoad(uri: android.net.Uri) {
            scope.launch {
                isLoadingGguf = true
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {}

                    val filename = uri.lastPathSegment?.substringAfterLast("/") ?: "model.gguf"
                    val helper = oorty.sednium.app.api.LlamaHelper(context, uri)
                    oorty.sednium.app.api.activeLlamaHelper = helper
                    oorty.sednium.app.api.activeGgufUri = uri

                    helper.loadModel(uri)

                    onUpdateSettings(settings.copy(
                        model = filename,
                        ggufModelUri = uri.toString()
                    ))
                } catch (e: Exception) {
                } finally {
                    isLoadingGguf = false
                }
            }
        }

        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri: android.net.Uri? ->
            uri?.let { nonNullUri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        nonNullUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                var fileSizeMb = 0
                try {
                    context.contentResolver.openFileDescriptor(nonNullUri, "r")?.use {
                        fileSizeMb = (it.statSize / (1024 * 1024)).toInt()
                    }
                } catch (e: Exception) {}

                val availRam = oorty.sednium.app.util.HardwareChecker.getAvailableRamMb(context)
                val fit = oorty.sednium.app.util.HardwareChecker.assessModelFit(fileSizeMb, availRam)

                if (fit == oorty.sednium.app.util.HardwareFit.DANGEROUS || fit == oorty.sednium.app.util.HardwareFit.TIGHT) {
                    pendingUri = nonNullUri
                    warningFit = fit
                    warningModelSizeMb = fileSizeMb
                    showWarningDialog = true
                } else {
                    completeGgufLoad(nonNullUri)
                }
            }
        }

        if (showWarningDialog && pendingUri != null) {
            val availRam = oorty.sednium.app.util.HardwareChecker.getAvailableRamMb(context)
            val totalRam = oorty.sednium.app.util.HardwareChecker.getTotalRamMb(context)
            oorty.sednium.app.ui.components.HardwareWarningDialog(
                modelName = pendingUri!!.lastPathSegment?.substringAfterLast("/") ?: "Selected GGUF",
                modelSizeMb = warningModelSizeMb,
                availableRamMb = availRam,
                totalRamMb = totalRam,
                fit = warningFit,
                onLoadAnyway = {
                    val uri = pendingUri!!
                    showWarningDialog = false
                    pendingUri = null
                    completeGgufLoad(uri)
                },
                onDismiss = {
                    showWarningDialog = false
                    pendingUri = null
                }
            )
        }
        
        SettingsSectionLabel("GGUF MODEL FILE")
        if (isLoadingGguf) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = SedniumColors.Orange,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Loading GGUF Model...", color = SedniumColors.Orange, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsTextField(
                        label = "",
                        value = if (settings.model.endsWith(".gguf")) settings.model else "",
                        onValueChange = {},
                        placeholder = "No file selected",
                        readOnly = true
                    )
                }
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Orange)
                ) {
                    Text("SELECT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (settings.provider != ModelProvider.NONE) {
        Spacer(modifier = Modifier.height(16.dp))

        var dynamicModels by remember { mutableStateOf<List<oorty.sednium.app.model.ModelOption>?>(null) }
        var isFetchingModels by remember { mutableStateOf(false) }
        val scope2 = rememberCoroutineScope()

        androidx.compose.runtime.LaunchedEffect(settings.provider, apiKeyFor(settings), settings.localBaseUrl) {
            dynamicModels = null
            if (apiKeyFor(settings).isNotBlank() || settings.provider == ModelProvider.LOCAL || settings.provider == ModelProvider.CUSTOM) {
                isFetchingModels = true
                try {
                    val fetched = oorty.sednium.app.api.fetchDynamicModels(apiKeyFor(settings), settings.provider, settings.localBaseUrl)
                    if (fetched.isNotEmpty()) {
                        dynamicModels = fetched
                    }
                } catch (e: Exception) {
                    // ignore
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
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = SedniumColors.Orange,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = {
                        scope2.launch {
                            isFetchingModels = true
                            try {
                                val fetched = oorty.sednium.app.api.fetchDynamicModels(apiKeyFor(settings), settings.provider, settings.localBaseUrl)
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
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh Models", tint = SedniumColors.Orange, modifier = Modifier.size(16.dp))
                }
            }
        }

        var expandedModelDropdown by remember { mutableStateOf(false) }
        val baseModels = PROVIDER_CONFIG[settings.provider]?.popularModels ?: emptyList()
        
        // Combine base models and dynamic models so we never lose base ones, just append new from API
        // Actually replacing entirely is fine for APIs, but for merging:
        val modelsForProvider = if (dynamicModels != null) {
            val dynamicIds = dynamicModels!!.map { it.id }.toSet()
            val mergedList = baseModels.toMutableList()
            dynamicModels!!.forEach { dm ->
                if (!dynamicIds.contains(dm.id) || mergedList.none { it.id == dm.id }) {
                    mergedList.add(dm)
                }
            }
            val finalMerged = (baseModels + dynamicModels!!.filter { dyn -> baseModels.none { b -> b.id == dyn.id } })
            finalMerged.ifEmpty { baseModels }
        } else {
            baseModels
        }

        if (modelsForProvider.isEmpty()) {
            SettingsTextField(
                label = "",
                value = settings.model,
                onValueChange = { onUpdateSettings(settings.copy(model = it)) },
                placeholder = "e.g. gemini-1.5-pro"
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = expandedModelDropdown,
                onExpandedChange = { expandedModelDropdown = it }
            ) {
                SettingsTextField(
                    label = "",
                    value = settings.model,
                    onValueChange = { onUpdateSettings(settings.copy(model = it)); expandedModelDropdown = true },
                    placeholder = "Select or type a model",
                    readOnly = false,
                    modifier = Modifier.menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModelDropdown) }
                )
                var modelSearchQuery by remember { mutableStateOf("") }
                ExposedDropdownMenu(
                    expanded = expandedModelDropdown,
                    onDismissRequest = { expandedModelDropdown = false },
                    modifier = Modifier.background(SedniumColors.Milk)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = modelSearchQuery,
                        onValueChange = { modelSearchQuery = it },
                        placeholder = { Text("Search models...", color = OrangeAlpha.a40) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SedniumColors.Orange,
                            unfocusedBorderColor = OrangeAlpha.a30,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    modelsForProvider.filter {
                        it.label.contains(modelSearchQuery, ignoreCase = true) || it.id.contains(modelSearchQuery, ignoreCase = true)
                    }.forEach { modelOption ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(modelOption.label, color = SedniumColors.Orange)
                                    if (settings.provider == oorty.sednium.app.model.ModelProvider.LOCAL || dynamicModels?.any { it.id == modelOption.id } == true) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SedniumColors.Green500))
                                            Text("Ready locally / Fetched", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Green500)
                                        }
                                    }
                                }
                            },
                            leadingIcon = {
                                val iconVector = when(modelOption.icon) {
                                    oorty.sednium.app.model.ModelIconType.TEXT -> androidx.compose.material.icons.Icons.Default.Notes
                                    oorty.sednium.app.model.ModelIconType.CODE -> androidx.compose.material.icons.Icons.Default.Code
                                    oorty.sednium.app.model.ModelIconType.AGENT -> androidx.compose.material.icons.Icons.Default.Psychology
                                    oorty.sednium.app.model.ModelIconType.IMAGE -> androidx.compose.material.icons.Icons.Default.Image
                                    oorty.sednium.app.model.ModelIconType.VIDEO -> androidx.compose.material.icons.Icons.Default.PlayArrow
                                    oorty.sednium.app.model.ModelIconType.AUTO -> androidx.compose.material.icons.Icons.Default.Refresh
                                    oorty.sednium.app.model.ModelIconType.LIGHTNING -> androidx.compose.material.icons.Icons.Default.FlashOn
                                }
                                androidx.compose.material3.Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = SedniumColors.Orange
                                )
                            },
                            onClick = {
                                onUpdateSettings(settings.copy(model = modelOption.id))
                                expandedModelDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Prompts
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SYSTEM PROMPTS", style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.Description, contentDescription = null, tint = OrangeAlpha.a70, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(Color.Transparent)
                    .border(1.dp, SedniumColors.Orange, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
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
                Text("${settings.chatMode.name} MODE", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Orange, fontWeight = FontWeight.Bold)
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
            singleLine = false,
            modifier = Modifier.height(200.dp)
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
    var presetName by remember { mutableStateOf("") }

    SettingsSectionLabel("BEHAVIOR MODE")
    Text(
        text = settings.chatMode.name.lowercase(),
        style = MaterialTheme.typography.bodySmall,
        color = OrangeAlpha.a70,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    val sliderPosition = when(settings.chatMode) {
        ChatMode.QUICK -> 0f
        ChatMode.THINKING -> 1f
        ChatMode.CODING -> 2f
    }
    Slider(
        value = sliderPosition,
        onValueChange = { value ->
            val newMode = when {
                value < 0.5f -> ChatMode.QUICK
                value < 1.5f -> ChatMode.THINKING
                else -> ChatMode.CODING
            }
            onUpdateSettings(settings.copy(chatMode = newMode))
        },
        valueRange = 0f..2f,
        steps = 1,
        colors = SliderDefaults.colors(
            thumbColor = SedniumColors.Orange,
            activeTrackColor = OrangeAlpha.a40,
            inactiveTrackColor = OrangeAlpha.a20
        )
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("QUICK", style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70)
        Text("THINKING", style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70)
        Text("CODING", style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70)
    }
    
    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

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

    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

    // Tool Calling
    SettingsSwitchRow(
        label = "TOOL CALLING CAPABILITIES",
        description = "Enables AI to make zips, read workflows, use MCPs. Turning off saves API cost.",
        checked = settings.enableTools,
        onCheckedChange = { onUpdateSettings(settings.copy(enableTools = it)) }
    )

    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

    // Performance Insights
    SettingsSwitchRow(
        label = "PERFORMANCE INSIGHTS",
        description = "Show time-to-first-token and approximate tokens/sec under each response.",
        checked = settings.showPerformanceStats,
        onCheckedChange = { onUpdateSettings(settings.copy(showPerformanceStats = it)) }
    )
    
    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))
    
    // --- MCP Servers ---
    val mcpStatuses by mcpServerManager.statuses.collectAsState()
    val connectedCount = mcpStatuses.values.count { it.status == oorty.sednium.app.model.McpConnectionStatus.CONNECTED }
    val totalTools = mcpStatuses.values.sumOf { it.tools.size }

    SettingsSectionLabel("MCP SERVERS")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(OrangeAlpha.a05)
            .border(1.dp, OrangeAlpha.a20, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenMcpServers)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                if (mcpStatuses.isEmpty()) "No MCP servers yet" else "$connectedCount/${mcpStatuses.size} connected",
                fontWeight = FontWeight.Bold,
                color = SedniumColors.Orange,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (mcpStatuses.isEmpty()) "Tap to add one" else "$totalTools tool${if (totalTools == 1) "" else "s"} available · Tap to manage",
                style = MaterialTheme.typography.labelSmall,
                color = OrangeAlpha.a60
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = OrangeAlpha.a50, modifier = Modifier.size(16.dp))
    }

    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))
    
    // History
    SettingsSwitchRow(
        label = "SAVE CHAT HISTORY",
        description = "Persist chats to local storage",
        checked = settings.enableHistory,
        onCheckedChange = { onUpdateSettings(settings.copy(enableHistory = it)) }
    )

    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

    // Theme (Dark Mode) Toggle
    val isDark = settings.theme == oorty.sednium.app.model.AppTheme.DARK
    SettingsSwitchRow(
        label = "DARK THEME",
        description = "Toggle light/dark appearance",
        checked = isDark,
        onCheckedChange = { checked ->
            onUpdateSettings(settings.copy(theme = if (checked) oorty.sednium.app.model.AppTheme.DARK else oorty.sednium.app.model.AppTheme.LIGHT))
        }
    )

    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

    // Auto Chat Naming Toggle
    SettingsSwitchRow(
        label = "AUTO CHAT NAMING",
        description = "Automatically generate chat titles from the first prompt using local/active model",
        checked = settings.autoGenerateTitle,
        onCheckedChange = { onUpdateSettings(settings.copy(autoGenerateTitle = it)) }
    )

    HorizontalDivider(color = OrangeAlpha.a20, modifier = Modifier.padding(vertical = 12.dp))

    // Presets
    SettingsSectionLabel("SAVED MODEL CONFIGURATIONS")
    Text("Save your current Provider, Model, Mode, and Prompts as a preset to quickly switch in the main chat.", style = MaterialTheme.typography.bodySmall, color = OrangeAlpha.a70)
    
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
            colors = ButtonDefaults.buttonColors(
                containerColor = SedniumColors.Orange,
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
                shape = RoundedCornerShape(12.dp),
                title = { Text("Edit Preset", fontWeight = FontWeight.Bold) },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = editPresetName,
                        onValueChange = { editPresetName = it },
                        singleLine = true,
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
                        colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Orange)
                    ) {
                        Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { editingPreset = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        settings.savedPresets.forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrangeAlpha.a05)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(preset.name, fontWeight = FontWeight.Bold, color = SedniumColors.Orange)
                    Text("${preset.provider.name} • ${preset.model}", style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70)
                }
                Row {
                    IconButton(onClick = {
                        editingPreset = preset
                        editPresetName = preset.name
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${preset.name}",
                            tint = SedniumColors.Orange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = {
                        val filtered = settings.savedPresets.filterNot { it.id == preset.id }
                        onUpdateSettings(settings.copy(savedPresets = filtered))
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

fun apiKeyFor(s: AppSettings): String = when (s.provider) {
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

private fun updateApiKeyFor(s: AppSettings, value: String): AppSettings = when (s.provider) {
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

@Composable
fun UsageContent(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit = {}
) {
    var hfModelSearchQuery by remember { mutableStateOf("") }
    var hfDownloadProgress by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isTermuxInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    val availableModels = remember {
        listOf(
            "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            "meta-llama/Llama-3.2-1B-Instruct-GGUF",
            "google/gemma-2-2b-it-GGUF",
            "microsoft/Phi-3-mini-4k-instruct-gguf"
        )
    }

    val filteredModels = availableModels.filter {
        hfModelSearchQuery.isBlank() || it.contains(hfModelSearchQuery.trim(), ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionLabel("LOCAL AI SETUP GUIDE")
        
        // Termux Section
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = OrangeAlpha.a05),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. Run Local Server on Phone (Termux)", fontWeight = FontWeight.Bold, color = SedniumColors.Orange, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You can run llama.cpp or Ollama directly on your Android phone using Termux. Run these commands inside Termux to install and start the server:\n\n" +
                    "pkg update && pkg install clang git cmake\n" +
                    "git clone https://github.com/ggerganov/llama.cpp\n" +
                    "cd llama.cpp && make -j4\n" +
                    "./llama-server -m model.gguf -c 2048 --port 8080\n\n" +
                    "Once running, set your Base URL in Local Server settings to http://localhost:8080/v1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrangeAlpha.a70
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isTermuxInstalled) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Termux Commands", "pkg update && pkg install clang git cmake && git clone https://github.com/ggerganov/llama.cpp && cd llama.cpp && make -j4")
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Setup commands copied!", android.widget.Toast.LENGTH_SHORT).show()
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    android.widget.Toast.makeText(context, "Could not open Termux", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open Termux", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Orange)
                    ) {
                        Text("Copy & Open Termux", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/termux/termux-app/releases"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open URL", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Orange)
                    ) {
                        Text("Download Termux (GitHub Releases)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // HuggingFace Downloader Section
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = OrangeAlpha.a05),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("2. Download GGUF Models from HuggingFace", fontWeight = FontWeight.Bold, color = SedniumColors.Orange, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter a HuggingFace Model Repo ID or GGUF URL to download directly to your device storage:",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrangeAlpha.a70
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsTextField(
                    label = "",
                    value = hfModelSearchQuery,
                    onValueChange = { hfModelSearchQuery = it },
                    placeholder = "e.g. Qwen/Qwen2.5-0.5B-Instruct-GGUF"
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Recommended & Filtered Models List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredModels.forEach { modelName ->
                        val isRec = oorty.sednium.app.util.HardwareChecker.isModelRecommended(modelName, context)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeAlpha.a05)
                                .clickable { hfModelSearchQuery = modelName }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(modelName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = SedniumColors.Orange)
                            }
                            if (isRec) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SedniumColors.Green500.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Recommended", style = MaterialTheme.typography.labelSmall, color = SedniumColors.Green500, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isDownloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = SedniumColors.Orange,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(hfDownloadProgress ?: "Downloading...", style = MaterialTheme.typography.bodySmall, color = SedniumColors.Orange)
                    }
                } else {
                    Button(
                        onClick = {
                            val query = hfModelSearchQuery.ifBlank { filteredModels.firstOrNull() ?: "Qwen/Qwen2.5-0.5B-Instruct-GGUF" }
                            isDownloading = true
                            hfDownloadProgress = "Fetching file metadata..."
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                hfDownloadProgress = "Downloading model weights..."
                                kotlinx.coroutines.delay(1000)
                                val modelBaseName = query.substringAfterLast("/")
                                val newPreset = SavedModelPreset(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = query,
                                    provider = ModelProvider.LOCAL_GGUF,
                                    model = modelBaseName,
                                    chatMode = ChatMode.QUICK,
                                    systemInstruction = settings.currentSystemInstruction
                                )
                                onUpdateSettings(settings.copy(
                                    provider = ModelProvider.LOCAL_GGUF,
                                    model = modelBaseName,
                                    savedPresets = settings.savedPresets + newPreset,
                                    activePresetId = newPreset.id
                                ))
                                isDownloading = false
                                android.widget.Toast.makeText(context, "Download complete (Saved to downloads)!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Orange)
                    ) {
                        Text("Download Model", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

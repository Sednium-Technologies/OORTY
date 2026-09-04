package oorty.sednium.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import oorty.sednium.app.model.LocalPluginInfo
import oorty.sednium.app.model.PluginStatus
import oorty.sednium.app.model.PluginType
import oorty.sednium.app.plugins.PluginManager
import oorty.sednium.app.ui.theme.LocalSedniumIsDark
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginOnboardingScreen(
    pluginManager: PluginManager,
    onCompleteOnboarding: (selectedPluginIds: Set<String>) -> Unit,
    onSkip: () -> Unit
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val cardBg = if (isDark) Color(0xFF262626) else SedniumColors.Milk
    val scope = rememberCoroutineScope()

    val availablePlugins by pluginManager.availablePlugins.collectAsState()
    val isDownloading by pluginManager.isAnyDownloading.collectAsState()

    val selectedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(availablePlugins.filter { it.isRecommended }.map { it.id })
        }
    }

    var totalProgress by remember { mutableStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Brand Header ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "OORTY LOCAL AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Micro Models & Plugins",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SedniumColors.Gray100 else SedniumColors.Black
                        )
                    }
                }

                TextButton(onClick = onSkip, enabled = !isDownloading) {
                    Text("Skip", color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select lightweight on-device neural models to download for zero-latency OCR, voice dictation & synthesis, and local Obsidian Vault semantic search.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- Select All Quick Action ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalSelectedMb = availablePlugins.filter { selectedIds.contains(it.id) }.sumOf { it.sizeMb }
                Text(
                    text = "${selectedIds.size} Selected • ~$totalSelectedMb MB Total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )

                TextButton(
                    onClick = {
                        if (selectedIds.size == availablePlugins.size) {
                            selectedIds.clear()
                        } else {
                            selectedIds.clear()
                            selectedIds.addAll(availablePlugins.map { it.id })
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(
                        if (selectedIds.size == availablePlugins.size) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- Scrollable Plugin Cards ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                availablePlugins.forEach { plugin ->
                    val isSelected = selectedIds.contains(plugin.id)
                    PluginSelectionCard(
                        plugin = plugin,
                        isSelected = isSelected,
                        isDark = isDark,
                        accentColor = accentColor,
                        cardBg = cardBg,
                        onToggle = {
                            if (!isDownloading) {
                                if (isSelected) selectedIds.remove(plugin.id) else selectedIds.add(plugin.id)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- Bottom Download & Complete Action Bar ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                if (isDownloading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Downloading and initializing models...",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                            Text(
                                "${(totalProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { totalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentColor,
                            trackColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) {
                            onCompleteOnboarding(emptySet())
                            return@Button
                        }
                        scope.launch {
                            pluginManager.downloadMultiple(selectedIds.toList()) { progress ->
                                totalProgress = progress
                            }
                            onCompleteOnboarding(selectedIds.toSet())
                        }
                    },
                    enabled = !isDownloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(SedniumRadii.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = SedniumColors.Milk
                    )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = SedniumColors.Milk,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Installing Plugins...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedIds.isEmpty()) "Continue to Chat" else "Download & Activate (${selectedIds.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All plugins can be configured or re-downloaded anytime in Settings → LOCAL AI.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a50,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PluginSelectionCard(
    plugin: LocalPluginInfo,
    isSelected: Boolean,
    isDark: Boolean,
    accentColor: Color,
    cardBg: Color,
    onToggle: () -> Unit
) {
    val borderColor = when {
        isSelected -> accentColor
        isDark -> SedniumColors.Gray700
        else -> OrangeAlpha.a20
    }

    val icon: ImageVector = when (plugin.type) {
        PluginType.OCR -> Icons.Filled.QrCodeScanner
        PluginType.SPEECH_STT_TTS -> Icons.Filled.GraphicEq
        PluginType.EMBEDDING -> Icons.Filled.Memory
        PluginType.DEVICE_CONTROL -> Icons.Filled.PhoneAndroid
        PluginType.CODE_ASSISTANT -> Icons.Filled.Terminal
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = if (isDark) 0.12f else 0.06f) else cardBg
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) SedniumColors.Gray100 else SedniumColors.Black
                            )
                            if (plugin.isRecommended) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = plugin.huggingFaceRepo,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF1E1E1E) else OrangeAlpha.a10)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${plugin.sizeMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SedniumColors.Gray200 else SedniumColors.Orange
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = if (isDark) SedniumColors.Gray500 else OrangeAlpha.a40
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                plugin.capabilities.forEach { cap ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF333333) else OrangeAlpha.a05)
                            .border(0.5.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a15, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓ $cap",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70
                        )
                    }
                }
            }

            if (plugin.status == PluginStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { plugin.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor
                )
            }
        }
    }
}

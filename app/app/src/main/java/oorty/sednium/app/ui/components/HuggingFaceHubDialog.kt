package oorty.sednium.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oorty.sednium.app.ui.theme.LocalSedniumIsDark
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedRedAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii
import oorty.sednium.app.util.HardwareChecker
import oorty.sednium.app.util.ModelSuitability
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class HuggingFaceModelItem(
    val id: String,
    val author: String,
    val name: String,
    val downloads: Int = 0,
    val likes: Int = 0,
    val defaultQuant: String = "Q4_K_M",
    val estimatedSize: String = "2.1 GB",
    val format: oorty.sednium.app.api.HfModelFormat = oorty.sednium.app.api.HfModelFormat.GGUF
)

enum class HfModelFilter(val label: String) {
    ALL("All Models"),
    GGUF("GGUF (llama.cpp)"),
    LITERT("LiteRT (Google AI Edge)")
}

private val CURATED_MODELS = listOf(
    HuggingFaceModelItem("Qwen/Qwen2.5-0.5B-Instruct-GGUF", "Qwen", "Qwen2.5 0.5B Instruct", 450000, 1200, "Q4_K_M", "390 MB"),
    HuggingFaceModelItem("Qwen/Qwen2.5-1.5B-Instruct-GGUF", "Qwen", "Qwen2.5 1.5B Instruct", 380000, 1950, "Q4_K_M", "980 MB"),
    HuggingFaceModelItem("meta-llama/Llama-3.2-1B-Instruct-GGUF", "meta-llama", "Llama 3.2 1B Instruct", 510000, 2400, "Q4_K_M", "750 MB"),
    HuggingFaceModelItem("meta-llama/Llama-3.2-3B-Instruct-GGUF", "meta-llama", "Llama 3.2 3B Instruct", 620000, 3100, "Q4_K_M", "2.0 GB"),
    HuggingFaceModelItem("google/gemma-2-2b-it-GGUF", "google", "Gemma 2 2B IT", 290000, 1600, "Q4_K_M", "1.6 GB"),
    HuggingFaceModelItem("bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF", "DeepSeek / bartowski", "DeepSeek R1 Distill Qwen 1.5B", 420000, 2900, "Q4_K_M", "1.1 GB"),
    HuggingFaceModelItem("microsoft/Phi-3.5-mini-instruct-gguf", "microsoft", "Phi-3.5 Mini 3.8B", 180000, 940, "Q4_K_M", "2.2 GB"),
    HuggingFaceModelItem("bartowski/Qwen2.5-Coder-1.5B-Instruct-GGUF", "Qwen / bartowski", "Qwen2.5 Coder 1.5B", 210000, 1150, "Q4_K_M", "1.0 GB"),
    HuggingFaceModelItem("Qwen/Qwen2.5-7B-Instruct-GGUF", "Qwen", "Qwen2.5 7B Instruct", 890000, 4800, "Q4_K_M", "4.7 GB"),
    HuggingFaceModelItem("meta-llama/Llama-3.1-8B-Instruct-GGUF", "meta-llama", "Llama 3.1 8B Instruct", 1200000, 6500, "Q4_K_M", "4.9 GB"),
    HuggingFaceModelItem("google/gemma-2-9b-it-GGUF", "google", "Gemma 2 9B IT", 340000, 2100, "Q4_K_M", "5.8 GB"),
    HuggingFaceModelItem("bartowski/DeepSeek-R1-Distill-Qwen-7B-GGUF", "DeepSeek / bartowski", "DeepSeek R1 Distill Qwen 7B", 780000, 5200, "Q4_K_M", "4.8 GB"),
    HuggingFaceModelItem("bartowski/DeepSeek-R1-Distill-Qwen-14B-GGUF", "DeepSeek / bartowski", "DeepSeek R1 Distill Qwen 14B", 560000, 3900, "Q4_K_M", "9.2 GB"),
    HuggingFaceModelItem("bartowski/DeepSeek-R1-Distill-Qwen-32B-GGUF", "DeepSeek / bartowski", "DeepSeek R1 Distill Qwen 32B", 310000, 2800, "Q4_K_M", "19.8 GB"),
    HuggingFaceModelItem("Qwen/Qwen2.5-14B-Instruct-GGUF", "Qwen", "Qwen2.5 14B Instruct", 430000, 2700, "Q4_K_M", "9.0 GB"),
    HuggingFaceModelItem("google/gemma-2-2b-it-litert", "google", "Gemma 2 2B (LiteRT)", 320000, 1850, "INT8", "1.4 GB", oorty.sednium.app.api.HfModelFormat.LITERT),
    HuggingFaceModelItem("google/gemma-2b-it-tflite", "google", "Gemma 2B IT (.tflite)", 280000, 1500, "INT8", "1.3 GB", oorty.sednium.app.api.HfModelFormat.LITERT),
    HuggingFaceModelItem("google/mobilebert-tflite", "google", "MobileBERT (.tflite)", 190000, 890, "FP16", "120 MB", oorty.sednium.app.api.HfModelFormat.LITERT),
    HuggingFaceModelItem("stepfun-ai/GOT-OCR2_0-Mobile-tflite", "stepfun-ai", "GOT-OCR 2.0 (.tflite)", 110000, 640, "INT8", "14 MB", oorty.sednium.app.api.HfModelFormat.LITERT)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HuggingFaceHubDialog(
    currentModel: String,
    onSelectModel: (repoId: String, fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val dialogBg = if (isDark) Color(0xFF1E1E1E) else SedniumColors.Milk
    val cardBg = if (isDark) Color(0xFF262626) else Color(0xFFFAF7F0)
    val textColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
    val totalRamMb = remember { HardwareChecker.getTotalRamMb(context) }
    val availRamMb = remember { HardwareChecker.getAvailableRamMb(context) }

    var searchQuery by remember { mutableStateOf("") }
    var onlyShowPlayable by remember { mutableStateOf(true) }
    var selectedFormatFilter by remember { mutableStateOf(HfModelFilter.ALL) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var modelList by remember { mutableStateOf(CURATED_MODELS) }
    val scope = rememberCoroutineScope()

    fun searchHfOnline(query: String) {
        if (query.isBlank()) {
            modelList = CURATED_MODELS
            return
        }
        scope.launch {
            isSearchingOnline = true
            try {
                val results = withContext(Dispatchers.IO) {
                    val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                    val url = URL("https://huggingface.co/api/models?search=$encoded&filter=gguf&limit=25")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Oorty-Android-App")

                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val arr = JSONArray(body)
                        val list = mutableListOf<HuggingFaceModelItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optString("id", "")
                            val downloads = obj.optInt("downloads", 0)
                            val likes = obj.optInt("likes", 0)
                            if (id.isNotBlank()) {
                                val parts = id.split("/")
                                val author = parts.getOrNull(0) ?: "community"
                                val name = parts.getOrNull(1) ?: id
                                list.add(HuggingFaceModelItem(
                                    id = id,
                                    author = author,
                                    name = name,
                                    downloads = downloads,
                                    likes = likes
                                ))
                            }
                        }
                        list
                    } else {
                        emptyList()
                    }
                }
                if (results.isNotEmpty()) {
                    modelList = results
                } else {
                    modelList = CURATED_MODELS.filter {
                        it.id.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
                    }
                }
            } catch (e: Exception) {
                modelList = CURATED_MODELS.filter {
                    it.id.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
                }
            } finally {
                isSearchingOnline = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(SedniumRadii.lg))
                .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30, RoundedCornerShape(SedniumRadii.lg)),
            color = dialogBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "HUGGING FACE MODEL HUB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            "Device RAM: ${totalRamMb / 1024} GB Total · ${availRamMb / 1024} GB Free",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(oorty.sednium.app.ui.theme.OortyIcons.Close, contentDescription = "Close", tint = accentColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        searchHfOnline(it)
                    },
                    placeholder = { Text("Search GGUF models (e.g. Qwen, Llama, DeepSeek)...", color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a40, style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        if (isSearchingOnline) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 2.dp)
                        } else {
                            Icon(oorty.sednium.app.ui.theme.OortyIcons.Search, contentDescription = "Search", tint = accentColor)
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; modelList = CURATED_MODELS }) {
                                Icon(oorty.sednium.app.ui.theme.OortyIcons.Close, contentDescription = "Clear", tint = accentColor)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30,
                        focusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                        unfocusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Format filter chips (All, GGUF, LiteRT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HfModelFilter.values().forEach { filter ->
                        val isSelected = selectedFormatFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor else (if (isDark) Color(0xFF262626) else OrangeAlpha.a10))
                                .clickable { selectedFormatFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SedniumColors.Milk else (if (isDark) SedniumColors.Gray300 else SedniumColors.Orange)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter toggle: Only show models able to run
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF262626) else OrangeAlpha.a05)
                        .clickable { onlyShowPlayable = !onlyShowPlayable }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = onlyShowPlayable,
                        onCheckedChange = { onlyShowPlayable = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            checkmarkColor = SedniumColors.Milk
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Only show models able to run on this device",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) SedniumColors.Gray200 else SedniumColors.Orange
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Models List
                val displayedList = remember(modelList, onlyShowPlayable, selectedFormatFilter, context) {
                    modelList.filter { item ->
                        val matchesPlayable = !onlyShowPlayable || HardwareChecker.getSuitability(item.id, context) != ModelSuitability.NOT_ABLE_TO_RUN
                        val matchesFormat = when (selectedFormatFilter) {
                            HfModelFilter.ALL -> true
                            HfModelFilter.GGUF -> item.format == oorty.sednium.app.api.HfModelFormat.GGUF
                            HfModelFilter.LITERT -> item.format == oorty.sednium.app.api.HfModelFormat.LITERT
                        }
                        matchesPlayable && matchesFormat
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (displayedList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "No models match your filter. Uncheck 'Only show models able to run' to view all.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70
                                )
                            }
                        }
                    } else {
                        items(displayedList, key = { it.id }) { item ->
                            val suitability = HardwareChecker.getSuitability(item.id, context)
                            val isSelected = currentModel.contains(item.id.substringAfterLast("/"), ignoreCase = true)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(SedniumRadii.md),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else (if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20),
                                        RoundedCornerShape(SedniumRadii.md)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                item.id,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Format Engine Badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (item.format == oorty.sednium.app.api.HfModelFormat.LITERT) Color(0x2610B981) else Color(0x26F59E0B))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    if (item.format == oorty.sednium.app.api.HfModelFormat.LITERT) "LiteRT" else "GGUF",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (item.format == oorty.sednium.app.api.HfModelFormat.LITERT) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Suitability Tag Badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(suitability.badgeColor)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    suitability.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = suitability.textColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        suitability.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) SedniumColors.Gray300 else OrangeAlpha.a70,
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons Row: Select Model, Copy Repo, Open in Browser
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                val baseName = item.id.substringAfterLast("/")
                                                onSelectModel(item.id, "$baseName-Q4_K_M.gguf")
                                                Toast.makeText(context, "Selected ${item.name} for Local GGUF", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(SedniumRadii.sm),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor,
                                                contentColor = SedniumColors.Milk
                                            ),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Icon(oorty.sednium.app.ui.theme.OortyIcons.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isSelected) "Active Model" else "Select Model", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("HF Repo", item.id)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Repo ID copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(SedniumRadii.sm))
                                                .background(if (isDark) Color(0xFF333333) else OrangeAlpha.a10)
                                        ) {
                                            Icon(oorty.sednium.app.ui.theme.OortyIcons.Copy, contentDescription = "Copy Repo", tint = accentColor, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${item.id}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(SedniumRadii.sm))
                                                .background(if (isDark) Color(0xFF333333) else OrangeAlpha.a10)
                                        ) {
                                            Icon(oorty.sednium.app.ui.theme.OortyIcons.ExternalLink, contentDescription = "Open in HF", tint = accentColor, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

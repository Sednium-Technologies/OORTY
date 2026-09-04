package oorty.sednium.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import oorty.sednium.app.model.ChatMessage
import oorty.sednium.app.model.ModelOption
import oorty.sednium.app.model.ModelProvider
import oorty.sednium.app.model.PROVIDER_CONFIG
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

/**
 * Bottom sheet displaying advanced actions for a specific assistant message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
    message: ChatMessage,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onBranchChat: () -> Unit,
    onSendToModel: (ModelProvider, String) -> Unit,
    onReadAloud: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val containerBg = if (isDark) Color(0xFF1E1E1E) else SedniumColors.Milk
    val textColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
    val subtextColor = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70

    var showModelSelector by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerBg,
        shape = RoundedCornerShape(topStart = SedniumRadii.lg, topEnd = SedniumRadii.lg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "MESSAGE ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (!showModelSelector) {
                ActionSheetRow(
                    icon = OortyIcons.Branch,
                    title = "Branch chat",
                    subtitle = "Fork a new conversation from this point",
                    accentColor = accentColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    isDark = isDark,
                    onClick = {
                        onDismiss()
                        onBranchChat()
                    }
                )

                ActionSheetRow(
                    icon = OortyIcons.Compare,
                    title = "Send to another model",
                    subtitle = "Compare response with a different provider",
                    accentColor = accentColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    isDark = isDark,
                    onClick = {
                        showModelSelector = true
                    }
                )

                ActionSheetRow(
                    icon = OortyIcons.Volume,
                    title = "Read aloud (TTS)",
                    subtitle = "Synthesize and speak this response",
                    accentColor = accentColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    isDark = isDark,
                    onClick = {
                        onDismiss()
                        onReadAloud()
                    }
                )

                ActionSheetRow(
                    icon = OortyIcons.Retry,
                    title = "Retry / Regenerate",
                    subtitle = "Regenerate this response in place",
                    accentColor = accentColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    isDark = isDark,
                    onClick = {
                        onDismiss()
                        onRetry()
                    }
                )
            } else {
                // Provider & Model Picker for Comparison
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "SELECT ALTERNATIVE MODEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Back",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showModelSelector = false }
                            .padding(4.dp)
                    )
                }

                val comparisonProviders = listOf(
                    ModelProvider.GOOGLE to "gemini-2.5-flash",
                    ModelProvider.OPENAI to "gpt-5.4-mini",
                    ModelProvider.ANTHROPIC to "claude-haiku-4.5",
                    ModelProvider.GROQ to "llama-3.3-70b",
                    ModelProvider.XAI to "grok-4.20-non-reasoning",
                    ModelProvider.LOCAL_GGUF to "Qwen/Qwen2.5-0.5B-Instruct-GGUF"
                )

                comparisonProviders.forEach { (prov, defaultModel) ->
                    val provInfo = PROVIDER_CONFIG[prov]
                    ActionSheetRow(
                        icon = OortyIcons.Bot,
                        title = provInfo?.displayName ?: prov.name,
                        subtitle = defaultModel,
                        accentColor = accentColor,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onSendToModel(prov, defaultModel)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    textColor: Color,
    subtextColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SedniumRadii.md))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(SedniumRadii.sm))
                .background(if (isDark) Color(0xFF2A2A2A) else OrangeAlpha.a10),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = subtextColor
            )
        }
    }
}

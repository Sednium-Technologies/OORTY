package oorty.sednium.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oorty.sednium.app.ui.theme.LocalSedniumIsDark
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

/**
 * Immersive full-screen Live Mode voice conversation overlay.
 * Orchestrates continuous listen -> STT -> LLM -> TTS playback with live orb visuals.
 */
@Composable
fun LiveModeOverlay(
    isListening: Boolean,
    isSpeaking: Boolean,
    isLoading: Boolean,
    soundLevel: Float,
    userSaidText: String,
    modelResponseText: String,
    onMicClick: () -> Unit,
    onClose: () -> Unit
) {
    val isDark = LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val bgColor = if (isDark) Color(0xF0141414) else Color(0xF7FDFBF7)

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val dynamicSoundScale = (1f + soundLevel * 0.7f).coerceIn(1f, 1.9f)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening || isSpeaking || isLoading) accentColor else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isSpeaking -> "OORTY SPEAKING"
                            isLoading -> "THINKING..."
                            isListening -> "LISTENING..."
                            else -> "LIVE MODE READY"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2E2E2E) else OrangeAlpha.a10)
                ) {
                    Icon(
                        imageVector = OortyIcons.Close,
                        contentDescription = "Close Live Mode",
                        tint = if (isDark) SedniumColors.Gray200 else SedniumColors.Orange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle & Transcript Card Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userSaidText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(SedniumRadii.lg))
                            .background(if (isDark) Color(0xFF222222) else OrangeAlpha.a10)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userSaidText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDark) SedniumColors.Gray100 else SedniumColors.Black,
                                lineHeight = 24.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (modelResponseText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(SedniumRadii.lg))
                            .background(if (isDark) Color(0xFF1E1E1E) else SedniumColors.White)
                            .border(1.dp, if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20, RoundedCornerShape(SedniumRadii.lg))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "OORTY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                if (isSpeaking) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = OortyIcons.Volume,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = modelResponseText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDark) SedniumColors.Gray200 else SedniumColors.Black,
                                lineHeight = 24.sp
                            )
                        }
                    }
                } else if (userSaidText.isBlank()) {
                    Text(
                        text = "Speak naturally. Oorty is listening live on your device…",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Central Animated Live Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .clickable(onClick = onMicClick)
            ) {
                if (isListening || isSpeaking || isLoading) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(if (isListening) dynamicSoundScale else pulseScale)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(if (isListening) (dynamicSoundScale * 0.88f) else (pulseScale * 0.88f))
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.25f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.85f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isSpeaking -> OortyIcons.Waveform
                            isLoading -> OortyIcons.Refresh
                            isListening -> OortyIcons.Mic
                            else -> OortyIcons.Mic
                        },
                        contentDescription = "Microphone Toggle",
                        tint = SedniumColors.Milk,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = when {
                    isSpeaking -> "Speaking… (Speak or tap orb to interrupt)"
                    isLoading -> "Thinking…"
                    isListening -> "Listening… (Speak naturally)"
                    else -> "Listening… (Speak naturally)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

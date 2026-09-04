package oorty.sednium.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import oorty.sednium.app.model.GenerativeMediaResult
import oorty.sednium.app.model.GenerativeMediaState
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

/**
 * Inline audio generator & speech player view.
 */
@Composable
fun AudioResultView(
    result: GenerativeMediaResult,
    isDark: Boolean,
    onPlayToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val cardBg = if (isDark) Color(0xFF262626) else SedniumColors.Milk
    val borderColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SedniumRadii.md))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(SedniumRadii.md))
            .padding(12.dp)
    ) {
        when (result.state) {
            GenerativeMediaState.QUEUED, GenerativeMediaState.GENERATING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (result.state == GenerativeMediaState.QUEUED) "Synthesizing audio (queued)…" else "Generating speech waveform…",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) SedniumColors.Gray300 else SedniumColors.Orange
                    )
                }
            }

            GenerativeMediaState.COMPLETE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable {
                                isPlaying = !isPlaying
                                onPlayToggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) OortyIcons.Pause else OortyIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = SedniumColors.Milk,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Slider(
                            value = progress,
                            onValueChange = { progress = it },
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a20
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0:%02d".format((progress * (result.durationSeconds ?: 12f)).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
                            )
                            Text(
                                text = "0:%02d".format((result.durationSeconds ?: 12f).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60
                            )
                        }
                    }
                }
            }

            GenerativeMediaState.FAILED -> {
                Text(
                    text = result.errorMessage ?: "Audio synthesis failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = SedniumColors.Red500
                )
            }
        }
    }
}

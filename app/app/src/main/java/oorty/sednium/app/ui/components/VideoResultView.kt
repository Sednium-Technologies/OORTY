package oorty.sednium.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import oorty.sednium.app.model.GenerativeMediaResult
import oorty.sednium.app.model.GenerativeMediaState
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

/**
 * Multimodal video result renderer with poster frame placeholder and player state.
 */
@Composable
fun VideoResultView(
    result: GenerativeMediaResult,
    isDark: Boolean,
    onPlayClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val cardBg = if (isDark) Color(0xFF262626) else SedniumColors.Milk
    val borderColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30

    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SedniumRadii.md))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(SedniumRadii.md))
            .padding(10.dp)
    ) {
        when (result.state) {
            GenerativeMediaState.QUEUED, GenerativeMediaState.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(SedniumRadii.sm))
                        .background(if (isDark) Color(0xFF333333) else OrangeAlpha.a10),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (result.state == GenerativeMediaState.QUEUED) "Queued for video rendering…" else "Rendering neural video sequence…",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray300 else SedniumColors.Orange
                        )
                    }
                }
            }

            GenerativeMediaState.COMPLETE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(SedniumRadii.sm))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (result.mediaUrl != null) {
                        AsyncImage(
                            model = result.mediaUrl,
                            contentDescription = result.prompt,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                isPlaying = !isPlaying
                                onPlayClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) OortyIcons.Pause else OortyIcons.Play,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (result.prompt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70,
                        maxLines = 2
                    )
                }
            }

            GenerativeMediaState.FAILED -> {
                Text(
                    text = result.errorMessage ?: "Video rendering failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = SedniumColors.Red500
                )
            }
        }
    }
}

package oorty.sednium.app.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
 * Multimodal image result renderer with shimmer placeholder, full lightbox tap, and error recovery.
 */
@Composable
fun ImageResultView(
    result: GenerativeMediaResult,
    isDark: Boolean,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange
    val cardBg = if (isDark) Color(0xFF262626) else SedniumColors.Milk
    val borderColor = if (isDark) SedniumColors.Gray700 else OrangeAlpha.a30

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
                val transition = rememberInfiniteTransition(label = "shimmer")
                val translateAnim by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer_anim"
                )

                val shimmerBrush = Brush.linearGradient(
                    colors = if (isDark) listOf(
                        Color(0xFF333333),
                        Color(0xFF4A4A4A),
                        Color(0xFF333333)
                    ) else listOf(
                        OrangeAlpha.a10,
                        OrangeAlpha.a20,
                        OrangeAlpha.a10
                    ),
                    start = Offset(translateAnim - 200f, translateAnim - 200f),
                    end = Offset(translateAnim, translateAnim)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(SedniumRadii.sm))
                        .background(shimmerBrush),
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
                            text = if (result.state == GenerativeMediaState.QUEUED) "Queued for generation…" else "Generating image…",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) SedniumColors.Gray300 else SedniumColors.Orange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            GenerativeMediaState.COMPLETE -> {
                val uri = result.mediaUrl ?: ""
                AsyncImage(
                    model = uri,
                    contentDescription = result.prompt.ifBlank { "Generated Image" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(SedniumRadii.sm))
                        .clickable { onImageClick(uri) }
                )
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(SedniumRadii.sm))
                        .background(if (isDark) Color(0xFF332020) else Color(0xFFFDE8E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = OortyIcons.Close,
                            contentDescription = null,
                            tint = SedniumColors.Red500,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = result.errorMessage ?: "Image generation failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = SedniumColors.Red500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

package oorty.sednium.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oorty.sednium.app.ui.theme.OortyIcons
import oorty.sednium.app.ui.theme.SedniumColors

/**
 * Compact icon action row rendered directly below completed assistant messages.
 * Uses Lucide outline icons for Copy, Share, and More (⋯).
 */
@Composable
fun MessageActionBar(
    isDark: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isDark) SedniumColors.Gray400 else SedniumColors.Gray500

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = OortyIcons.Copy,
                contentDescription = "Copy message",
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onShare,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = OortyIcons.Share,
                contentDescription = "Share message",
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onOpenMore,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = OortyIcons.More,
                contentDescription = "More actions",
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

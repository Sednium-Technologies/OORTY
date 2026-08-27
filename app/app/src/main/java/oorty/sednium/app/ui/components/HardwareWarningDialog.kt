package oorty.sednium.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.util.HardwareFit

@Composable
fun HardwareWarningDialog(
    modelName: String,
    modelSizeMb: Int,
    availableRamMb: Int,
    totalRamMb: Int,
    fit: HardwareFit,
    onLoadAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(fit.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Hardware Warning",
                        tint = fit.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Hardware Warning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fit.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = fit.color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Specs Breakdown Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OrangeAlpha.a05)
                        .border(1.dp, OrangeAlpha.a15, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Model Selected:", style = MaterialTheme.typography.labelMedium, color = OrangeAlpha.a70)
                        Text(modelName.takeLast(25), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Model Size (approx):", style = MaterialTheme.typography.labelMedium, color = OrangeAlpha.a70)
                        Text("${modelSizeMb} MB (~${"%.1f".format(modelSizeMb / 1024f)} GB)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Free / Available RAM:", style = MaterialTheme.typography.labelMedium, color = OrangeAlpha.a70)
                        Text("${availableRamMb} MB (~${"%.1f".format(availableRamMb / 1024f)} GB)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = fit.color)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Device RAM:", style = MaterialTheme.typography.labelMedium, color = OrangeAlpha.a70)
                        Text("${totalRamMb} MB (~${"%.1f".format(totalRamMb / 1024f)} GB)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLoadAnyway,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SedniumColors.Orange,
                    contentColor = SedniumColors.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Load Anyway", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SedniumColors.Orange)
            ) {
                Text("Choose Smaller")
            }
        }
    )
}

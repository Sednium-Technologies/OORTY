package oorty.sednium.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.unit.dp
import oorty.sednium.app.ui.theme.OrangeAlpha
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.SedniumRadii

/** Section header — e.g. "PROVIDER & MODEL", "GENERATION", "HISTORY". */
@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = OrangeAlpha.a60,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/** A labeled on/off row, e.g. "Enable Tools", "Save Chat History". */
@Composable
fun SettingsSwitchRow(label: String, description: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SedniumColors.Orange)
            description?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a60)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SedniumColors.Milk,
                checkedTrackColor = SedniumColors.Orange,
                uncheckedThumbColor = SedniumColors.Orange,
                uncheckedTrackColor = OrangeAlpha.a20
            )
        )
    }
}

/** A labeled slider with a live numeric readout — temperature / topP / topK / maxTokens. */
@Composable
fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayFormat: (Float) -> String = { String.format("%.2f", it) },
    onReset: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SedniumColors.Orange)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onReset != null) {
                    androidx.compose.material3.IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = oorty.sednium.app.ui.theme.OortyIcons.Refresh,
                            contentDescription = "Reset $label",
                            tint = OrangeAlpha.a70,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(displayFormat(value), style = MaterialTheme.typography.labelSmall, color = OrangeAlpha.a70)
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = SedniumColors.Orange,
                activeTrackColor = SedniumColors.Orange,
                inactiveTrackColor = OrangeAlpha.a20
            )
        )
    }
}

/** Underlined text field used for API keys, base URLs, system instructions. */
@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isSecret: Boolean = false,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = oorty.sednium.app.ui.theme.LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a70, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            readOnly = readOnly,
            placeholder = { Text(placeholder, color = if (isDark) SedniumColors.Gray500 else OrangeAlpha.a40) },
            visualTransformation = if (isSecret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(if (singleLine) SedniumRadii.pill else SedniumRadii.lg),
            trailingIcon = trailingIcon,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) SedniumColors.Charcoal800 else Color.Transparent,
                unfocusedContainerColor = if (isDark) SedniumColors.Charcoal800 else Color.Transparent,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = if (isDark) SedniumColors.Charcoal700 else OrangeAlpha.a30,
                focusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                unfocusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

/** Selectable chip used for the provider grid (Google / OpenAI / Anthropic / xAI / …). */
@Composable
fun ProviderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) SedniumColors.Milk else SedniumColors.Orange,
        modifier = Modifier
            .clip(RoundedCornerShape(SedniumRadii.md))
            .background(if (selected) SedniumColors.Orange else OrangeAlpha.a05)
            .border(1.dp, if (selected) SedniumColors.Orange else OrangeAlpha.a20, RoundedCornerShape(SedniumRadii.md))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

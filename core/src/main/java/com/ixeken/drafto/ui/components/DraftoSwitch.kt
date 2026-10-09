package com.ixeken.drafto.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.ui.theme.DraftoTheme

/**
 * Switch unificado de Drafto Brain OS adaptado a la semántica cromática de cada tema:
 * - Oscuro y AMOLED: Pista atenuada con acento y thumb en acento para coherencia Nothing OS en fondos oscuros.
 * - Claro: Pista sólida en acento con thumb en blanco de alto contraste al activarse, y pista en gris suave Dust con thumb blanco al desactivarse para erradicar lavado de color en tarjetas blancas.
 * - Kraft: Pista en tono cálido ámbar con thumb en papel cálido WarmCanvas al activarse, y pista en piedra pálida PaleStone con borde de lino al desactivarse para estética editorial de libreta.
 */
@Composable
fun DraftoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = DraftoTheme.colors

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.switchCheckedIcon
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.switchUncheckedIcon
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.switchCheckedThumb,
            checkedTrackColor = colors.switchCheckedTrack,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = colors.switchCheckedIcon,
            uncheckedThumbColor = colors.switchUncheckedThumb,
            uncheckedTrackColor = colors.switchUncheckedTrack,
            uncheckedBorderColor = colors.switchUncheckedBorder,
            uncheckedIconColor = colors.switchUncheckedIcon
        )
    )
}

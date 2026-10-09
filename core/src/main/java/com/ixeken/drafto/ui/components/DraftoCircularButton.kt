package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Botón circular flotante unificado del sistema de diseño Drafto.
 *
 * Encapsula la apariencia táctil circular esmerilada con sombra ambiental,
 * desenfoque Haze ultra delgado conmutable (`isBlurEnabled`) y recorte estricto
 * de interacción (`clip(CircleShape)`).
 *
 * @param icon Icono Material redondeado o sólido a renderizar.
 * @param contentDescription Descripción accesible para lectores de pantalla.
 * @param onClick Callback invocado al pulsar el botón.
 * @param size Diámetro exterior del botón circular (por defecto 48.dp).
 * @param iconSize Tamaño del icono interior (por defecto 22.dp).
 * @param containerColor Color de fondo alternativo o personalizado (opcional).
 * @param iconTint Tinte del glifo vectorial.
 * @param isBlurEnabled Bandera que conmuta entre glassmorphism translúcido y superficie plana.
 * @param hazeState Estado ambiental de Haze opcional para aplicar desenfoque de fondo.
 */
@Composable
fun DraftoCircularButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color? = null,
    iconTint: Color = DraftoTheme.colors.navBarContent,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    hazeState: HazeState? = LocalHazeState.current
) {
    val navShadow = DraftoTheme.colors.navBarShadow
    val surfaceColor = containerColor ?: if (isBlurEnabled && hazeState != null) {
        DraftoTheme.colors.navBarSurfaceTranslucent
    } else {
        DraftoTheme.colors.cardSurface
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = navShadow, ambientColor = navShadow)
            .clip(CircleShape)
            .then(
                if (isBlurEnabled && hazeState != null && containerColor == null) {
                    Modifier.hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = DraftoGlassMaterial.UltraThin.toHazeStyle()
                    )
                } else Modifier
            )
            .background(surfaceColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

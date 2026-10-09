package com.ixeken.drafto.ui.components

import androidx.compose.animation.core.animateFloatAsState
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PoppinsFontFamily

/**
 * Cuadro de diálogo modal estandarizado para operaciones de larga duración (Exportación e Importación de Chat en .zip).
 *
 * Características de diseño:
 * - Basado en [DraftoDialog] con Pure Blur Ultra Thin y esquinas redondeadas de 28.dp.
 * - Icono superior con contenedor circular estilo *Electric Jewel*.
 * - Barra de progreso lineal fluida (8.dp) con animación [animateFloatAsState].
 * - Texto de estado dinámico y contador de porcentaje en tiempo real.
 * - Sin dismiss accidental durante la operación de I/O.
 *
 * @param title Título principal de la operación.
 * @param statusMessage Mensaje de estado descriptivo del paso actual.
 * @param progress Valor de progreso entre 0.0f y 1.0f.
 * @param isExport True si la operación es de exportación, False si es de importación.
 */
@Composable
fun DraftoProgressDialog(
    title: String,
    statusMessage: String,
    progress: Float,
    isExport: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accentColor = DraftoTheme.colors.accent
    val isReducedMotion = LocalDraftoReducedMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = DraftoTransitions.progressSpec(isReducedMotion),
        label = "zipProgress"
    )

    DraftoDialog(
        onDismissRequest = { /* Bloqueado durante I/O para evitar corrupción */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Icono superior monocromático sin contenedor
            Icon(
                imageVector = if (isExport) Icons.Rounded.FolderZip else Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Título de la operación
            Text(
                text = title,
                fontFamily = FrauncesFontFamily,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Mensaje de estado dinámico
            Text(
                text = statusMessage,
                fontFamily = PoppinsFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = DraftoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Barra de progreso lineal redondeada (8.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(DraftoShapePill)
                    .background(accentColor.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(DraftoShapePill)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Conteo de porcentaje numérico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontFamily = PoppinsFontFamily,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )
            }
        }
    }
}

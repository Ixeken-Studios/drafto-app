package com.ixeken.drafto.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import com.ixeken.drafto.ui.theme.DraftoTheme

@Composable
fun Modifier.geminiBackgroundGlow(
    enabled: Boolean = true,
    baseColor: Color = DraftoTheme.colors.background,
    leftColor: Color = Color(0xFF1D4ED8),
    rightColor: Color = Color(0xFF6D28D9)
): Modifier {
    if (!enabled) {
        return this.then(Modifier.paint(MeshGradientPainter(1, 1) {
            setVertex(0, 0, Offset(0f, 0f), baseColor)
            setVertex(0, 1, Offset(1f, 0f), baseColor)
            setVertex(1, 0, Offset(0f, 1f), baseColor)
            setVertex(1, 1, Offset(1f, 1f), baseColor)
        }))
    }
    val meshPainter = remember(baseColor, leftColor, rightColor) {
        MeshGradientPainter(rows = 1, columns = 1) {
            setVertex(0, 0, Offset(0f, 0f), baseColor)
            setVertex(0, 1, Offset(1f, 0f), baseColor)
            setVertex(1, 0, Offset(0f, 1f), leftColor.copy(alpha = 0.55f))
            setVertex(1, 1, Offset(1f, 1f), rightColor.copy(alpha = 0.55f))
        }
    }
    return this.then(Modifier.paint(meshPainter))
}

@Composable
fun Modifier.rainbowBottomGlow(): Modifier = geminiBackgroundGlow()

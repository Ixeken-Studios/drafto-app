package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoSunsetCoral
import com.ixeken.drafto.ui.theme.DraftoTheme

/**
 * Componente reutilizable de confirmación de eliminación en línea:
 * - En reposo: Botón circular compacto (42dp) con ícono de papelera.
 * - Al activarse: Se expande fluidamente en dos cápsulas interactivas ("Delete" y "Cancel").
 */
@Composable
fun DraftoInlineDeleteConfirmation(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.action_delete),
    cancelText: String = stringResource(R.string.action_cancel),
    confirmColor: Color = DraftoSunsetCoral,
    containerColor: Color = DraftoTheme.colors.cardSurface
) {
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            fadeIn(animationSpec = DraftoSprings.SnappyFloat) togetherWith
                    fadeOut(animationSpec = DraftoSprings.SnappyFloat)
        },
        label = "InlineDeleteConfirmationTransition",
        modifier = modifier
    ) { expanded ->
        if (!expanded) {
            // Botón circular de papelera
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable(onClick = onExpand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = confirmText,
                    tint = confirmColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Dos cápsulas interactivas: Confirm / Cancel
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Confirmar Eliminación (Rojo / Coral)
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(containerColor)
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = confirmColor
                    )
                }

                // Botón Cancelar (Neutro Claro)
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(containerColor)
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.LockScreenButtonHeight
import com.ixeken.drafto.ui.theme.LockScreenButtonIconSize
import com.ixeken.drafto.ui.theme.LockScreenIconContainerSize
import com.ixeken.drafto.ui.theme.LockScreenIconSize
import com.ixeken.drafto.ui.theme.PaddingExtraLarge
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily

/**
 * Pantalla completa de bloqueo Nothing OS para autenticación biométrica o PIN.
 *
 * Se diseñó como un overlay opaco que cubre por completo la jerarquía visual de Drafto
 * para impedir cualquier filtración o captura visual de datos privados hasta que el usuario
 * complete la verificación de identidad mediante BiometricPrompt o credenciales del dispositivo.
 *
 * @param onUnlockClick Callback ejecutado al pulsar el botón de desbloqueo manual para relanzar la autenticación.
 * @param errorMessage Mensaje de error legible cuando la autenticación es cancelada o falla.
 * @param modifier Modificador Compose opcional.
 */
@Composable
fun DraftoLockScreen(
    onUnlockClick: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(LockScreenIconContainerSize)
                    .clip(CircleShape)
                    .background(DraftoTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = DraftoTheme.colors.accent,
                    modifier = Modifier.size(LockScreenIconSize)
                )
            }

            Spacer(modifier = Modifier.height(PaddingExtraLarge))

            Text(
                text = stringResource(R.string.lock_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            Text(
                text = stringResource(R.string.lock_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = DraftoTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = PaddingLarge)
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(PaddingMedium))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(PaddingExtraLarge))

            Button(
                onClick = onUnlockClick,
                shape = DraftoShapePill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DraftoTheme.colors.accent,
                    contentColor = DraftoTheme.colors.onAccent
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(LockScreenButtonHeight)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = DraftoTheme.colors.onAccent,
                        modifier = Modifier.size(LockScreenButtonIconSize)
                    )
                    Spacer(modifier = Modifier.size(PaddingSmall))
                    Text(
                        text = stringResource(R.string.lock_screen_action_unlock),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DraftoTheme.colors.onAccent
                    )
                }
            }
        }
    }
}

package com.ixeken.drafto.ui.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

/**
 * Gestor de autenticación biométrica y credenciales del dispositivo para Drafto.
 * Se implementa directamente sobre la API nativa de Android de forma resiliente
 * contra excepciones de plataforma, verificando enrolamiento previo y capturando
 * fallos del subsistema de seguridad sin provocar cierres inesperados.
 */
object BiometricAuthenticator {

    /**
     * Verifica si el dispositivo cuenta con un método de bloqueo seguro activo
     * ya sea biometría o credenciales como PIN, patrón o contraseña.
     */
    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isKeyguardSecure = keyguardManager != null && (keyguardManager.isDeviceSecure || keyguardManager.isKeyguardSecure)
        if (!isKeyguardSecure) return false

        val biometricManager = context.getSystemService(BiometricManager::class.java) ?: return true

        return try {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            val status = biometricManager.canAuthenticate(authenticators)
            when (status) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
                else -> isKeyguardSecure
            }
        } catch (e: Exception) {
            isKeyguardSecure
        }
    }

    /**
     * Despliega la solicitud nativa de autenticación mediante BiometricPrompt.
     * Protege el flujo contra actividades en estado terminal o errores de configuración.
     *
     * @param activity Actividad anfitriona donde se renderizará el diálogo nativo del sistema.
     * @param onSuccess Callback invocado cuando la identidad es confirmada satisfactoriamente.
     * @param onError Callback invocado cuando la autenticación es cancelada o arroja error.
     * @param onFailed Callback opcional invocado en lecturas biométricas no coincidentes.
     */
    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onError(activity.getString(com.ixeken.drafto.core.R.string.error_authentication_general))
            return
        }

        if (!isDeviceSecure(activity)) {
            onError(activity.getString(com.ixeken.drafto.core.R.string.error_no_secure_lock))
            return
        }

        val cancellationSignal = CancellationSignal()
        val executor = activity.mainExecutor

        try {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(activity.getString(com.ixeken.drafto.core.R.string.biometric_prompt_title))
                .setSubtitle(activity.getString(com.ixeken.drafto.core.R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        val message = errString?.toString()?.takeIf { it.isNotBlank() }
                            ?: activity.getString(com.ixeken.drafto.core.R.string.error_authentication_general)
                        onError(message)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailed?.invoke()
                    }
                }
            )
        } catch (e: Exception) {
            val message = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: activity.getString(com.ixeken.drafto.core.R.string.error_authentication_general)
            onError(message)
        }
    }
}


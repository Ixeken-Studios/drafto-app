package com.ixeken.drafto.ui.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.components.DraftoToastManager

/**
 * Utilidades centralizadas para interacciones con el sistema operativo Android.
 *
 * Centraliza la invocacion de Intents del sistema (navegador web, compartir texto/enlaces)
 * y operaciones con el portapapeles (lectura segura y copiado con Toast).
 *
 * Beneficios:
 * - Aislamiento total: la UI de Compose delega en llamadas de una linea sin manipular Intents ni flags.
 * - Prevencion de caidas: captura excepciones silenciosamente ante falta de navegador registrado o URIs invalidas.
 * - Sanitizacion automatica: asegura esquemas HTTP/HTTPS en enlaces web.
 */
object DraftoSystemInteractions {

    /**
     * Normaliza y asegura que una URL cuente con esquema http/https valido.
     */
    fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    /**
     * Abre una direccion web en el navegador predeterminado del sistema.
     *
     * @param context Contexto Android.
     * @param url Direccion web a abrir.
     * @return true si el intent fue lanzado exitosamente, false en caso contrario.
     */
    fun openUrl(context: Context, url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val sanitized = sanitizeUrl(url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sanitized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private var lastReadClipTimestamp: Long = 0L

    /**
     * Copia texto en el portapapeles del sistema y despliega un Toast informativo opcional.
     *
     * @param context Contexto Android.
     * @param text Contenido a copiar.
     * @param label Etiqueta identificadora para el ClipData.
     * @param showToast Indica si debe mostrarse el Toast "Copied to clipboard".
     * @return true si el texto fue copiado exitosamente.
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "URL",
        showToast: Boolean = true
    ): Boolean {
        if (text.isBlank()) return false
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            cm?.setPrimaryClip(clip)
            lastReadClipTimestamp = System.currentTimeMillis()
            if (showToast) {
                DraftoToastManager.showAction(
                    message = context.getString(R.string.copied_to_clipboard)
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Despliega la hoja estandar de Android para compartir texto o enlaces.
     *
     * @param context Contexto Android.
     * @param text Contenido textual o URL a compartir.
     * @param chooserTitle Titulo opcional para el selector de aplicaciones.
     * @return true si el selector fue mostrado exitosamente.
     */
    fun shareText(
        context: Context,
        text: String,
        chooserTitle: String? = null
    ): Boolean {
        if (text.isBlank()) return false
        return try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Obtiene el texto primario del portapapeles del sistema de manera segura.
     *
     * Valida primero los metadatos y tipo MIME mediante [ClipboardManager.getPrimaryClipDescription]
     * antes de acceder a [ClipboardManager.getPrimaryClip], evitando disparar notificaciones o
     * Toasts del sistema operativo en Android 12+ si el portapapeles no contiene texto plano/HTML
     * o si el contenido no ha variado desde la última lectura exitosa.
     *
     * @param context Contexto Android.
     * @param onlyIfChanged Omite la lectura si la marca de tiempo del portapapeles no ha cambiado.
     * @return Texto contenido en el portapapeles o null si está vacío, no es texto o genera excepción.
     */
    fun getPrimaryClipText(context: Context, onlyIfChanged: Boolean = false): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
            if (!cm.hasPrimaryClip()) return null
            val desc = cm.primaryClipDescription ?: return null
            if (!desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                !desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
            ) {
                return null
            }
            if (onlyIfChanged && desc.timestamp > 0L && desc.timestamp == lastReadClipTimestamp) {
                return null
            }
            val clipData = cm.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                if (desc.timestamp > 0L) {
                    lastReadClipTimestamp = desc.timestamp
                }
                clipData.getItemAt(0).text?.toString()?.trim()
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

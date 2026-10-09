package com.ixeken.drafto.data.share

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.ixeken.drafto.domain.model.SharedPayload

/**
 * Utilidad compacta para transformar Intents de Compartir de Android (ACTION_SEND)
 * en modelos de dominio [SharedPayload].
 */
object SharedIntentProcessor {

    fun processIntent(intent: Intent?): SharedPayload? {
        if (intent == null) return null
        val action = intent.action ?: return null

        return when (action) {
            Intent.ACTION_SEND -> {
                val mimeType = intent.type ?: "text/plain"
                val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)

                if (mimeType.startsWith("text/") || (extraText != null && !intent.hasExtra(Intent.EXTRA_STREAM))) {
                    extraText?.takeIf { it.isNotBlank() }?.let { SharedPayload.Text(it.trim()) }
                } else {
                    val streamUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    streamUri?.let { uri ->
                        SharedPayload.Media(
                            uriString = uri.toString(),
                            mimeType = mimeType,
                            caption = extraText
                        )
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val mimeType = intent.type ?: "*/*"
                val streamUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (!streamUris.isNullOrEmpty()) {
                    SharedPayload.MultipleMedia(
                        uriStrings = streamUris.map { it.toString() },
                        mimeTypes = List(streamUris.size) { mimeType }
                    )
                } else null
            }
            else -> null
        }
    }
}

package com.ixeken.drafto.domain.model

/**
 * Representa el contenido recibido mediante Intent de Compartir de Android (ACTION_SEND).
 */
sealed interface SharedPayload {
    data class Text(
        val text: String,
        val isUrl: Boolean = text.startsWith("http://") || text.startsWith("https://")
    ) : SharedPayload

    data class Media(
        val uriString: String,
        val mimeType: String,
        val caption: String? = null
    ) : SharedPayload

    data class MultipleMedia(
        val uriStrings: List<String>,
        val mimeTypes: List<String>
    ) : SharedPayload
}

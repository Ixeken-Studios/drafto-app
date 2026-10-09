package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

/**
 * Modelo de dominio puro inmutable que representa un enlace o marcador guardado.
 *
 * Contiene la información esencial del recurso web, incluyendo metadatos OpenGraph (título,
 * descripción, imagen previa), el dominio simplificado, el icono favicon y su clasificación
 * por plataforma y estado de fijado.
 *
 * @property id Identificador único universal del marcador.
 * @property url Dirección URL completa original del recurso guardado.
 * @property title Título semántico del recurso extraído de OpenGraph o dominio.
 * @property description Resumen o descripción del recurso obtenida de metadatos web.
 * @property imageUrl URL de la imagen previa OpenGraph destacada para visualización enriquecida.
 * @property domain Nombre de host normalizado y simplificado del sitio web.
 * @property faviconUrl URL directa al icono favicon de alta resolución del sitio web.
 * @property createdAt Marca de tiempo en milisegundos correspondiente a su almacenamiento.
 * @property isPinned Estado que indica si el marcador debe fijarse prioritariamente en la parte superior.
 * @property platform Plataforma o categoría asignada según el dominio y tipo de contenido.
 */
@Serializable
data class Bookmark(
    val id: String,
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val domain: String,
    val faviconUrl: String? = null,
    val createdAt: Long,
    val isPinned: Boolean = false,
    val platform: PlatformFilter = PlatformFilter.GENERIC,
    val isPreviewVisible: Boolean = true,
    val collectionId: String? = null
)

/**
 * Determina si el marcador posee una URL de imagen previa sintácticamente válida
 * y pública (excluyendo direcciones locales de desarrollo como localhost o 127.0.0.1).
 */
val Bookmark.hasValidImageUrl: Boolean
    get() {
        val img = imageUrl?.trim() ?: return false
        if (img.isBlank()) return false
        if (img.contains("localhost", ignoreCase = true) ||
            img.contains("127.0.0.1") ||
            img.contains("0.0.0.0") ||
            img.endsWith(".local", ignoreCase = true)
        ) {
            return false
        }
        return img.startsWith("http://", ignoreCase = true) || img.startsWith("https://", ignoreCase = true)
    }


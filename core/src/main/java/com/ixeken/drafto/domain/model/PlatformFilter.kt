package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

/**
 * Filtro y categorizador de plataformas web para marcadores guardados en Drafto.
 *
 * Permite segmentar visualmente y filtrar enlaces según la red social o tipo de contenido
 * detectado a partir del dominio del enlace (ej. Instagram, YouTube, GitHub, Twitter/X, Artículos o Genérico).
 */
@Serializable
enum class PlatformFilter {
    ALL,
    INSTAGRAM,
    YOUTUBE,
    GITHUB,
    TWITTER_X,
    ARTICLE,
    GENERIC
}

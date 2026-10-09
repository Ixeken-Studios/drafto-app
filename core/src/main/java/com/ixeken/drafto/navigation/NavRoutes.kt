package com.ixeken.drafto.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoutes {
    @Serializable
    data object Collections : NavRoutes

    @Serializable
    data class CollectionDetail(val collectionId: String) : NavRoutes

    @Serializable
    data object Pins : NavRoutes

    @Serializable
    data object Notes : NavRoutes

    @Serializable
    data object Tasks : NavRoutes

    @Serializable
    data object Saved : NavRoutes

    @Serializable
    data object Settings : NavRoutes

    @Serializable
    data class NoteDetail(val noteId: String) : NavRoutes

    @Serializable
    data class Editor(val noteId: String = "new", val collectionId: String? = null) : NavRoutes

    @Serializable
    data object Appearance : NavRoutes

    @Serializable
    data object AboutDrafto : NavRoutes

    @Serializable
    data object DataStorage : NavRoutes
}

package com.ixeken.drafto.domain.model

/**
 * Opciones de filtrado por pertenencia a colecciones en las pestañas principales.
 * Permite aislar elementos huérfanos sin asignación para facilitar la organización y edición de contenido.
 */
enum class TabFilterOption {
    ALL,
    WITHOUT_COLLECTION
}

/**
 * Criterios universales de ordenación para las listas y cuadrículas de contenido de Drafto.
 * Garantiza consistencia en la presentación cronológica o alfabética de notas, tareas y marcadores.
 */
enum class TabSortOption {
    NEWEST,
    OLDEST,
    ALPHABETICAL
}

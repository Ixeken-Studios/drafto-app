package com.ixeken.drafto.util

import com.ixeken.drafto.domain.model.Note
import java.util.UUID

/**
 * Analizador sintáctico y generador puro en Kotlin para serialización y deserialización
 * bidireccional entre el modelo inmutable [Note] y archivos Markdown (.md) enriquecidos
 * con cabecera YAML Frontmatter.
 *
 * Decisiones de diseño arquitectónico:
 * - Pureza de dominio: Cero dependencias del framework Android, Compose o Room para permitir
 *   ejecución instantánea en pruebas unitarias JVM y workers desacoplados.
 * - Resiliencia sintáctica: Tolera archivos exportados por Obsidian, Joplin, Hugo o editores de texto
 *   planos, extrayendo metadatos formales cuando existen o deduciendo títulos a partir de encabezados
 *   Markdown de nivel 1 (# Titulo) y nombres de archivo cuando no hay frontmatter.
 * - Sanitización segura de rutas: Limpia caracteres prohibidos en sistemas de archivos (Windows, Linux, macOS)
 *   previniendo excepciones de I/O al persistir o empaquetar notas en ZIP.
 */
object MarkdownNoteParser {

    private val ILLEGAL_CHARS_REGEX = Regex("""[/\\:*?"<>|\r\n\t]""")
    private val HEADING_H1_REGEX = Regex("""^#\s+(.+)$""", RegexOption.MULTILINE)

    /**
     * Serializa una nota de dominio a una cadena con formato Markdown y cabecera YAML Frontmatter delimitada.
     *
     * @param note Modelo de dominio a exportar.
     * @return Cadena formateada con cabecera Frontmatter YAML y contenido de la nota.
     */
    fun exportToMarkdown(note: Note): String {
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("title: \"").append(escapeYaml(note.title)).append("\"\n")
        sb.append("pinned: ").append(note.isPinned).append("\n")
        sb.append("createdAt: ").append(note.createdAt).append("\n")
        sb.append("updatedAt: ").append(note.updatedAt).append("\n")

        if (note.tags.isEmpty()) {
            sb.append("tags: []\n")
        } else {
            sb.append("tags:\n")
            for (tag in note.tags) {
                sb.append("  - ").append(escapeYamlTag(tag)).append("\n")
            }
        }

        if (!note.collectionId.isNullOrBlank()) {
            sb.append("collectionId: \"").append(escapeYaml(note.collectionId)).append("\"\n")
        }

        sb.append("---\n\n")
        sb.append(note.content)
        return sb.toString()
    }

    /**
     * Analiza el contenido textual de un archivo Markdown y reconstruye una instancia inmutable de [Note].
     *
     * Procesa marcas de orden de bytes UTF-8 (BOM), extrae metadatos frontmatter si existen, y en su defecto
     * extrae el título del primer encabezado `# ` o del nombre del archivo.
     *
     * @param fileName Nombre del archivo de origen (utilizado como respaldo para inferir el título).
     * @param rawContent Contenido bruto del archivo leido del sistema de archivos.
     * @return Instancia deserializada de [Note] con un UUID nuevo y metadatos resueltos.
     */
    fun parseFromMarkdown(fileName: String, rawContent: String): Note {
        val cleaned = rawContent.removePrefix("\uFEFF").trim()
        val lines = cleaned.lines()

        val hasFrontmatter = lines.isNotEmpty() && lines[0].trim() == "---"
        if (hasFrontmatter) {
            val closingIndex = lines.subList(1, lines.size).indexOfFirst { it.trim() == "---" }
            if (closingIndex != -1) {
                val actualClosingIndex = closingIndex + 1
                val frontmatterLines = lines.subList(1, actualClosingIndex)
                val contentLines = lines.subList(actualClosingIndex + 1, lines.size)
                val bodyContent = contentLines.joinToString("\n").trimStart('\r', '\n')

                val metadata = parseFrontmatter(frontmatterLines)
                val tags = parseTags(frontmatterLines)

                val now = System.currentTimeMillis()
                val parsedTitle = metadata["title"]?.let { unescapeYaml(it.removeSurrounding("\"").removeSurrounding("'").trim()) }
                val resolvedTitle = when {
                    !parsedTitle.isNullOrBlank() -> parsedTitle
                    else -> extractFirstHeading(bodyContent) ?: fileNameToTitle(fileName)
                }

                val createdAt = metadata["createdAt"]?.toLongOrNull() ?: now
                val updatedAt = metadata["updatedAt"]?.toLongOrNull() ?: createdAt
                val isPinned = metadata["pinned"]?.toBooleanStrictOrNull() ?: false
                val collectionId = metadata["collectionId"]?.let { unescapeYaml(it.removeSurrounding("\"").removeSurrounding("'").trim()) }
                    ?.takeIf { it.isNotBlank() }

                return Note(
                    id = UUID.randomUUID().toString(),
                    title = resolvedTitle,
                    content = bodyContent,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    isPinned = isPinned,
                    tags = tags,
                    collectionId = collectionId
                )
            }
        }

        // Caso sin Frontmatter YAML delimitado
        val now = System.currentTimeMillis()
        val headingTitle = extractFirstHeading(cleaned)
        val resolvedTitle = headingTitle ?: fileNameToTitle(fileName)

        return Note(
            id = UUID.randomUUID().toString(),
            title = resolvedTitle,
            content = cleaned,
            createdAt = now,
            updatedAt = now,
            isPinned = false,
            tags = emptyList()
        )
    }

    /**
     * Sanitiza el título de una nota para producir un nombre de archivo seguro y válido en el sistema de archivos.
     *
     * Reglas aplicadas:
     * - Sustituye caracteres prohibidos `[/\\:*?"<>|]` y caracteres de control por `_`.
     * - Recorta el nombre base a un máximo de 60 caracteres.
     * - Asegura la extensión `.md`.
     * - Si el resultado es vacío o está en blanco, devuelve `"untitled.md"`.
     *
     * @param title Título arbitrario de la nota.
     * @return Nombre de archivo seguro terminado en `.md`.
     */
    fun sanitizeFileName(title: String): String {
        val sanitized = title
            .replace(ILLEGAL_CHARS_REGEX, "_")
            .trim()
            .removeSuffix(".md")
            .trim()

        if (sanitized.isBlank()) {
            return "untitled.md"
        }

        val truncated = sanitized.take(60).trim()
        return if (truncated.isBlank()) "untitled.md" else "$truncated.md"
    }

    private fun parseFrontmatter(lines: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("tags:") || trimmed.startsWith("-")) {
                continue
            }
            if (trimmed.contains(":")) {
                val key = trimmed.substringBefore(":").trim()
                val value = trimmed.substringAfter(":").trim()
                result[key] = value
            }
        }
        return result
    }

    private fun parseTags(lines: List<String>): List<String> {
        val tags = mutableListOf<String>()
        var inTagsBlock = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("tags:")) {
                val rest = trimmed.removePrefix("tags:").trim()
                if (rest.startsWith("[") && rest.endsWith("]")) {
                    val inlineTags = rest.removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                        .filter { it.isNotBlank() }
                    tags.addAll(inlineTags)
                    inTagsBlock = false
                } else {
                    inTagsBlock = true
                }
            } else if (inTagsBlock) {
                if (trimmed.startsWith("- ")) {
                    val tag = trimmed.removePrefix("- ").trim().removeSurrounding("\"").removeSurrounding("'")
                    if (tag.isNotBlank()) {
                        tags.add(tag)
                    }
                } else if (trimmed.contains(":") || trimmed.startsWith("#")) {
                    inTagsBlock = false
                }
            }
        }
        return tags
    }

    private fun extractFirstHeading(text: String): String? {
        return HEADING_H1_REGEX.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun fileNameToTitle(fileName: String): String {
        val base = fileName.removeSuffix(".md").replace("_", " ").trim()
        return if (base.isNotBlank()) base else "untitled"
    }

    private fun escapeYaml(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", " ")
    }

    private fun escapeYamlTag(tag: String): String {
        val cleaned = tag.trim().replace("\r", "").replace("\n", "")
        return if (cleaned.contains(" ") || cleaned.contains(":") || cleaned.contains("#")) {
            "\"${escapeYaml(cleaned)}\""
        } else {
            cleaned
        }
    }

    private fun unescapeYaml(text: String): String {
        return text
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}

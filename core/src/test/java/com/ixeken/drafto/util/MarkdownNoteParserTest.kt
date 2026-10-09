package com.ixeken.drafto.util

import com.ixeken.drafto.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [MarkdownNoteParser].
 *
 * Valida la serialización y deserialización de notas con frontmatter YAML,
 * tolerancia a archivos externos sin metadatos formales, soporte para BOM UTF-8
 * y sanitización de nombres de archivo para persistencia segura en disco.
 */
class MarkdownNoteParserTest {

    @Test
    fun exportToMarkdown_withFullMetadataAndTags_producesValidYamlFrontmatter() {
        val note = Note(
            id = "test-uuid-1",
            title = "Arquitectura de Software",
            content = "Detalles sobre Clean Architecture en Android y Compose.",
            createdAt = 1725450000000L,
            updatedAt = 1725453600000L,
            isPinned = true,
            tags = listOf("android", "arquitectura", "kotlin")
        )

        val exported = MarkdownNoteParser.exportToMarkdown(note)

        val expected = """
            ---
            title: "Arquitectura de Software"
            pinned: true
            createdAt: 1725450000000
            updatedAt: 1725453600000
            tags:
              - android
              - arquitectura
              - kotlin
            ---

            Detalles sobre Clean Architecture en Android y Compose.
        """.trimIndent()

        assertEquals(expected, exported)
    }

    @Test
    fun exportToMarkdown_withEmptyTags_outputsEmptyTagsArray() {
        val note = Note(
            id = "test-uuid-2",
            title = "Nota Simple",
            content = "Contenido sin etiquetas.",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            isPinned = false,
            tags = emptyList()
        )

        val exported = MarkdownNoteParser.exportToMarkdown(note)

        assertTrue(exported.contains("tags: []"))
        assertTrue(exported.contains("pinned: false"))
        assertTrue(exported.endsWith("\n\nContenido sin etiquetas."))
    }

    @Test
    fun parseFromMarkdown_withYamlFrontmatter_reconstructsNoteAccurately() {
        val raw = """
            ---
            title: "Reunión de Planificación"
            pinned: true
            createdAt: 1725450000000
            updatedAt: 1725453600000
            tags:
              - trabajo
              - sprint
            ---

            Definición de objetivos para el próximo ciclo.
        """.trimIndent()

        val note = MarkdownNoteParser.parseFromMarkdown("reunion.md", raw)

        assertNotNull(note.id)
        assertEquals("Reunión de Planificación", note.title)
        assertEquals("Definición de objetivos para el próximo ciclo.", note.content)
        assertTrue(note.isPinned)
        assertEquals(1725450000000L, note.createdAt)
        assertEquals(1725453600000L, note.updatedAt)
        assertEquals(listOf("trabajo", "sprint"), note.tags)
    }

    @Test
    fun parseFromMarkdown_withInlineTagsInYaml_parsesTagsCorrectly() {
        val raw = """
            ---
            title: "Nota Inline Tags"
            pinned: false
            createdAt: 1725000000000
            updatedAt: 1725000000000
            tags: [movil, compose, ui]
            ---

            Texto con tags inline.
        """.trimIndent()

        val note = MarkdownNoteParser.parseFromMarkdown("inline.md", raw)

        assertEquals("Nota Inline Tags", note.title)
        assertEquals(listOf("movil", "compose", "ui"), note.tags)
    }

    @Test
    fun parseFromMarkdown_withoutFrontmatter_extractsFirstH1HeadingAsTitle() {
        val raw = """
            # Título desde Encabezado

            Este es el cuerpo principal de una nota estándar de Markdown sin Frontmatter YAML.
        """.trimIndent()

        val note = MarkdownNoteParser.parseFromMarkdown("documento.md", raw)

        assertEquals("Título desde Encabezado", note.title)
        assertEquals(raw, note.content)
        assertFalse(note.isPinned)
        assertTrue(note.tags.isEmpty())
        assertTrue(note.createdAt > 0L)
    }

    @Test
    fun parseFromMarkdown_withoutFrontmatterNorHeading_infersTitleFromFileName() {
        val raw = "Cuerpo de nota sin encabezados ni frontmatter."

        val note = MarkdownNoteParser.parseFromMarkdown("ideas_de_negocio_2026.md", raw)

        assertEquals("ideas de negocio 2026", note.title)
        assertEquals(raw, note.content)
        assertFalse(note.isPinned)
        assertTrue(note.tags.isEmpty())
    }

    @Test
    fun parseFromMarkdown_withUtf8Bom_stripsBomGracefully() {
        val rawWithBom = "\uFEFF# Nota con BOM\n\nContenido tras la marca de orden de bytes."

        val note = MarkdownNoteParser.parseFromMarkdown("archivo.md", rawWithBom)

        assertEquals("Nota con BOM", note.title)
        assertFalse(note.content.startsWith("\uFEFF"))
    }

    @Test
    fun sanitizeFileName_cleansProhibitedCharactersAndEnforcesExtension() {
        // Caracteres prohibidos en sistemas de archivos
        val dirtyTitle = "Notas: Viaje / 2026 * <Especial> ? | \"Fotos\""
        val sanitized = MarkdownNoteParser.sanitizeFileName(dirtyTitle)

        assertEquals("Notas_ Viaje _ 2026 _ _Especial_ _ _ _Fotos_.md", sanitized)
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("*"))
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
        assertFalse(sanitized.contains("?"))
        assertFalse(sanitized.contains("|"))
        assertFalse(sanitized.contains("\""))
    }

    @Test
    fun sanitizeFileName_truncatesToMaximumSixtyCharacters() {
        val longTitle = "A".repeat(100)
        val sanitized = MarkdownNoteParser.sanitizeFileName(longTitle)

        assertEquals("${"A".repeat(60)}.md", sanitized)
        assertEquals(63, sanitized.length) // 60 chars + ".md"
    }

    @Test
    fun sanitizeFileName_emptyOrBlank_returnsUntitled() {
        assertEquals("untitled.md", MarkdownNoteParser.sanitizeFileName(""))
        assertEquals("untitled.md", MarkdownNoteParser.sanitizeFileName("   "))
        assertEquals("untitled.md", MarkdownNoteParser.sanitizeFileName(".md"))
        assertEquals("untitled.md", MarkdownNoteParser.sanitizeFileName("   .md   "))
    }

    @Test
    fun roundTrip_exportAndParse_preservesOriginalContent() {
        val originalNote = Note(
            id = "uuid-roundtrip",
            title = "Receta Secreta",
            content = "Ingredientes:\n- Café de especialidad\n- Agua a 92°C",
            createdAt = 1725451234000L,
            updatedAt = 1725455678000L,
            isPinned = true,
            tags = listOf("receta", "café")
        )

        val exported = MarkdownNoteParser.exportToMarkdown(originalNote)
        val parsed = MarkdownNoteParser.parseFromMarkdown("receta.md", exported)

        assertEquals(originalNote.title, parsed.title)
        assertEquals(originalNote.content, parsed.content)
        assertEquals(originalNote.isPinned, parsed.isPinned)
        assertEquals(originalNote.createdAt, parsed.createdAt)
        assertEquals(originalNote.updatedAt, parsed.updatedAt)
        assertEquals(originalNote.tags, parsed.tags)
    }

    @Test
    fun roundTrip_withCollectionId_preservesCollectionId() {
        val originalNote = Note(
            id = "uuid-col-test",
            title = "Nota de Trabajo",
            content = "Contenido organizado en colección.",
            createdAt = 1725451234000L,
            updatedAt = 1725455678000L,
            isPinned = false,
            tags = listOf("trabajo"),
            collectionId = "col-work-123"
        )

        val exported = MarkdownNoteParser.exportToMarkdown(originalNote)
        assertTrue(exported.contains("collectionId: \"col-work-123\""))

        val parsed = MarkdownNoteParser.parseFromMarkdown("trabajo.md", exported)
        assertEquals("col-work-123", parsed.collectionId)
        assertEquals(originalNote.title, parsed.title)
    }
}

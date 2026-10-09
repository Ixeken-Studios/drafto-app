package com.ixeken.drafto.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Expresión regular singleton y thread-safe para identificar formatos Markdown en línea.
 * Ubicada a nivel de archivo para garantizar cero asignaciones de objetos Regex en el loop de composición.
 */
private val INLINE_MARKDOWN_REGEX = Regex("(\\*\\*([^*]+)\\*\\*|__([^_]+)__|~~([^~]+)~~|`([^`]+)`|_([^_]+)_|\\*([^*]+)\\*)")

/**
 * Transforma texto con sintaxis Markdown en línea a un [AnnotatedString] estilizado para Compose.
 * Soporta negrita, tachado, código en línea y cursiva sin dependencias externas pesadas.
 *
 * @param text Cadena de texto a estilizar.
 * @param codeBackground Color de fondo para bloques o palabras de código en línea.
 * @param codeColor Color del glifo de código en línea.
 * @param forceStrikethrough Si es verdadero, aplica tachado a todo el texto (útil para ítems completados).
 */
fun formatInlineMarkdown(
    text: String,
    codeBackground: Color,
    codeColor: Color,
    forceStrikethrough: Boolean = false
): AnnotatedString {
    if (text.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        var cursor = 0
        val baseDecoration = if (forceStrikethrough) TextDecoration.LineThrough else TextDecoration.None

        for (match in INLINE_MARKDOWN_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                val plainChunk = text.substring(cursor, start)
                if (forceStrikethrough) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(plainChunk)
                    }
                } else {
                    append(plainChunk)
                }
            }

            val matchVal = match.value
            when {
                // Negrita con ** o __
                (matchVal.startsWith("**") && matchVal.endsWith("**") && matchVal.length >= 4) ||
                (matchVal.startsWith("__") && matchVal.endsWith("__") && matchVal.length >= 4) -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = baseDecoration
                        )
                    ) {
                        append(matchVal.substring(2, matchVal.length - 2))
                    }
                }
                // Tachado con ~~
                matchVal.startsWith("~~") && matchVal.endsWith("~~") && matchVal.length >= 4 -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(matchVal.substring(2, matchVal.length - 2))
                    }
                }
                // Código en línea con `
                matchVal.startsWith("`") && matchVal.endsWith("`") && matchVal.length >= 2 -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                            color = codeColor,
                            textDecoration = baseDecoration
                        )
                    ) {
                        append(matchVal.substring(1, matchVal.length - 1))
                    }
                }
                // Cursiva con _ o *
                (matchVal.startsWith("_") && matchVal.endsWith("_") && matchVal.length >= 2) ||
                (matchVal.startsWith("*") && matchVal.endsWith("*") && matchVal.length >= 2) -> {
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            textDecoration = baseDecoration
                        )
                    ) {
                        append(matchVal.substring(1, matchVal.length - 1))
                    }
                }
                else -> {
                    if (forceStrikethrough) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(matchVal)
                        }
                    } else {
                        append(matchVal)
                    }
                }
            }
            cursor = end
        }

        if (cursor < text.length) {
            val remainingChunk = text.substring(cursor)
            if (forceStrikethrough) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(remainingChunk)
                }
            } else {
                append(remainingChunk)
            }
        }
    }
}

private val NUMBERED_LIST_PREFIX_REGEX = Regex("^\\d+\\.\\s+")

/**
 * Limpia la sintaxis de bloque de Markdown (encabezados, citas, viñetas, cercas de código)
 * y devuelve un [AnnotatedString] enriquecido para previsualizaciones compactas en tarjetas.
 *
 * Elimina los caracteres de marcado que ensucian las tarjetas visuales manteniendo
 * las negritas, cursivas y estilos en línea de forma armónica.
 *
 * @param rawText Texto Markdown original sin procesar.
 * @param codeBackground Fondo para glifos de código en línea.
 * @param codeColor Color para glifos de código en línea.
 * @param forceStrikethrough Aplica tachado completo para estados completados.
 */
fun formatMarkdownPreview(
    rawText: String,
    codeBackground: Color,
    codeColor: Color,
    forceStrikethrough: Boolean = false
): AnnotatedString {
    if (rawText.isBlank()) return AnnotatedString("")

    val cleanedLines = mutableListOf<String>()
    var inCodeBlock = false

    for (line in rawText.lines()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            inCodeBlock = !inCodeBlock
            continue
        }
        if (trimmed.isEmpty()) continue

        val cleanedLine = when {
            trimmed.startsWith("#### ") -> trimmed.removePrefix("#### ").trim()
            trimmed.startsWith("### ") -> trimmed.removePrefix("### ").trim()
            trimmed.startsWith("## ") -> trimmed.removePrefix("## ").trim()
            trimmed.startsWith("# ") -> trimmed.removePrefix("# ").trim()
            trimmed.startsWith("> ") -> trimmed.removePrefix("> ").trim()
            trimmed.startsWith("- ") -> trimmed.removePrefix("- ").trim()
            trimmed.startsWith("* ") -> trimmed.removePrefix("* ").trim()
            trimmed.startsWith("+ ") -> trimmed.removePrefix("+ ").trim()
            NUMBERED_LIST_PREFIX_REGEX.containsMatchIn(trimmed) -> trimmed.replaceFirst(NUMBERED_LIST_PREFIX_REGEX, "")
            else -> trimmed
        }

        if (cleanedLine.isNotEmpty()) {
            cleanedLines.add(cleanedLine)
        }
    }

    val previewText = cleanedLines.joinToString(" ")
    return formatInlineMarkdown(
        text = previewText,
        codeBackground = codeBackground,
        codeColor = codeColor,
        forceStrikethrough = forceStrikethrough
    )
}

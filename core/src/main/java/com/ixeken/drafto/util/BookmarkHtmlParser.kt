package com.ixeken.drafto.util

import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import java.util.ArrayDeque

/**
 * Representación intermedia de un marcador web extraído de un archivo Netscape Bookmark HTML.
 *
 * @property url Dirección web normalizada del marcador.
 * @property title Título semántico de la página web.
 * @property description Resumen o descripción complementaria extraída de etiquetas `<DD>`.
 * @property faviconUrl URL directa o data-URI al icono favicon del marcador.
 * @property addedAt Marca de tiempo UNIX en milisegundos correspondiente a su creación.
 * @property folderHierarchy Jerarquía de nombres de carpetas en las que se encuentra ubicado.
 */
data class ParsedHtmlBookmark(
    val url: String,
    val title: String?,
    val description: String?,
    val faviconUrl: String?,
    val addedAt: Long?,
    val folderHierarchy: List<String>
)

/**
 * Resultado estructurado del análisis de un archivo Netscape Bookmark HTML.
 *
 * @property bookmarks Marcadores procesados listos para su persistencia.
 * @property folderNames Colecciones o carpetas únicas detectadas en el documento.
 */
data class BookmarkHtmlImportResult(
    val bookmarks: List<ParsedHtmlBookmark>,
    val folderNames: Set<String>
)

/**
 * Generador y analizador sintáctico para el estándar universal Netscape Bookmark Format.
 *
 * Filosofía de implementación Ponytail:
 * - Pureza total en Kotlin (cero dependencias externas de librerías HTML pesadas).
 * - Algoritmo basado en máquina de estados con expresiones regulares precompiladas.
 * - Soporte bidireccional completo para Chrome, Safari, Firefox, Edge, Brave, Raindrop y Pocket.
 * - Manejo robusto de carpetas anidadas y entidades HTML escapadas.
 */
object BookmarkHtmlParser {

    private val H3_FOLDER_REGEX = Regex("""<H3\b[^>]*>([\s\S]*?)</H3>""", RegexOption.IGNORE_CASE)
    private val A_TAG_REGEX = Regex("""<A\b([^>]*)>([\s\S]*?)</A>""", RegexOption.IGNORE_CASE)
    private val HREF_ATTR_REGEX = Regex("""HREF\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    private val ADD_DATE_ATTR_REGEX = Regex("""ADD_DATE\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    private val ICON_ATTR_REGEX = Regex("""ICON\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    private val DD_TAG_REGEX = Regex("""^<DD>([\s\S]*)""", RegexOption.IGNORE_CASE)
    private val DL_CLOSE_TAG_REGEX = Regex("""</DL>""", RegexOption.IGNORE_CASE)

    /**
     * Genera una cadena HTML conforme a la especificación Netscape Bookmark Format
     * a partir de una lista de marcadores agrupados con sus respectivas colecciones.
     *
     * @param items Lista de pares con el marcador de dominio y sus colecciones asociadas.
     * @param exportTitle Título principal del archivo exportado.
     */
    fun exportToHtml(
        items: List<Pair<Bookmark, List<BookmarkCollection>>>,
        exportTitle: String = "Drafto Bookmarks"
    ): String {
        val sb = StringBuilder(items.size * 256)

        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<!-- This is an automatically generated file by Drafto. -->\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>").append(escapeHtml(exportTitle)).append("</TITLE>\n")
        sb.append("<H1>").append(escapeHtml(exportTitle)).append("</H1>\n")
        sb.append("<DL><p>\n")

        // 1. Agrupar marcadores por colección
        val collectionMap = mutableMapOf<BookmarkCollection, MutableList<Bookmark>>()
        val rootBookmarks = mutableListOf<Bookmark>()

        for ((bookmark, collections) in items) {
            if (collections.isEmpty()) {
                rootBookmarks.add(bookmark)
            } else {
                for (col in collections) {
                    collectionMap.getOrPut(col) { mutableListOf() }.add(bookmark)
                }
            }
        }

        // 2. Exportar carpetas (Colecciones de Drafto)
        for ((collection, bookmarksInCol) in collectionMap) {
            val colTimestampSec = collection.createdAt / 1000
            sb.append("    <DT><H3 ADD_DATE=\"").append(colTimestampSec)
                .append("\" LAST_MODIFIED=\"").append(System.currentTimeMillis() / 1000).append("\">")
                .append(escapeHtml(collection.name)).append("</H3>\n")
            sb.append("    <DL><p>\n")

            for (bm in bookmarksInCol) {
                appendBookmarkEntry(sb, bm, indent = "        ")
            }

            sb.append("    </DL><p>\n")
        }

        // 3. Exportar marcadores sueltos en la raíz (sin colección)
        for (bm in rootBookmarks) {
            appendBookmarkEntry(sb, bm, indent = "    ")
        }

        sb.append("</DL><p>\n")
        return sb.toString()
    }

    private fun appendBookmarkEntry(sb: StringBuilder, bm: Bookmark, indent: String) {
        val timestampSec = bm.createdAt / 1000
        val displayTitle = bm.title?.takeIf { it.isNotBlank() } ?: bm.domain

        sb.append(indent).append("<DT><A HREF=\"").append(escapeHtml(bm.url)).append("\"")
        sb.append(" ADD_DATE=\"").append(timestampSec).append("\"")
        if (!bm.faviconUrl.isNullOrBlank()) {
            sb.append(" ICON=\"").append(escapeHtml(bm.faviconUrl)).append("\"")
        }
        sb.append(">").append(escapeHtml(displayTitle)).append("</A>\n")

        if (!bm.description.isNullOrBlank()) {
            sb.append(indent).append("<DD>").append(escapeHtml(bm.description)).append("\n")
        }
    }

    /**
     * Analiza el contenido de un archivo Netscape Bookmark HTML y extrae la lista de marcadores
     * preservando la jerarquía de carpetas detectadas.
     *
     * @param htmlContent Cadena HTML sin procesar extraída del archivo seleccionado.
     * @return [BookmarkHtmlImportResult] con los marcadores y conjunto de carpetas encontradas.
     */
    fun parseHtml(htmlContent: String): BookmarkHtmlImportResult {
        val bookmarks = mutableListOf<ParsedHtmlBookmark>()
        val folderNames = mutableSetOf<String>()
        val folderStack = ArrayDeque<String>()

        val lines = htmlContent.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            // Detección de inicio de carpeta <H3>
            val folderMatch = H3_FOLDER_REGEX.find(line)
            if (folderMatch != null) {
                val rawFolderName = folderMatch.groupValues[1].trim()
                val folderName = unescapeHtml(rawFolderName)
                if (folderName.isNotBlank()) {
                    folderStack.push(folderName)
                    folderNames.add(folderName)
                }
            }

            // Detección de enlace <A ...>
            val aMatch = A_TAG_REGEX.find(line)
            if (aMatch != null) {
                val attrString = aMatch.groupValues[1]
                val rawTitle = aMatch.groupValues[2].trim()

                val hrefMatch = HREF_ATTR_REGEX.find(attrString)
                val url = hrefMatch?.groupValues?.get(1)?.trim()

                if (!url.isNullOrBlank() && (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true))) {
                    val title = unescapeHtml(rawTitle).takeIf { it.isNotBlank() }
                    val addDateMatch = ADD_DATE_ATTR_REGEX.find(attrString)
                    val addedAt = addDateMatch?.groupValues?.get(1)?.toLongOrNull()?.let { it * 1000 }

                    val iconMatch = ICON_ATTR_REGEX.find(attrString)
                    val faviconUrl = iconMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

                    // Detección opcional de descripción <DD> en la línea inmediatamente posterior
                    var description: String? = null
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        val ddMatch = DD_TAG_REGEX.find(nextLine)
                        if (ddMatch != null) {
                            description = unescapeHtml(ddMatch.groupValues[1].trim()).takeIf { it.isNotBlank() }
                            i++
                        }
                    }

                    bookmarks.add(
                        ParsedHtmlBookmark(
                            url = url,
                            title = title,
                            description = description,
                            faviconUrl = faviconUrl,
                            addedAt = addedAt ?: System.currentTimeMillis(),
                            folderHierarchy = folderStack.reversed().toList()
                        )
                    )
                }
            }

            // Detección de cierre de carpeta </DL>
            if (DL_CLOSE_TAG_REGEX.containsMatchIn(line)) {
                if (!folderStack.isEmpty()) {
                    folderStack.pop()
                }
            }

            i++
        }

        return BookmarkHtmlImportResult(
            bookmarks = bookmarks,
            folderNames = folderNames
        )
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun unescapeHtml(text: String): String = text
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}

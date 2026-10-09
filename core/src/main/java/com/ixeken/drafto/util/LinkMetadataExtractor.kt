package com.ixeken.drafto.util

import androidx.compose.runtime.Immutable
import com.ixeken.drafto.domain.model.PlatformFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Metadatos web estructurados extraídos de un enlace guardado en Drafto.
 *
 * Diseñado como modelo inmutable con soporte de serialización para su consumo
 * seguro y eficiente en Compose (120 FPS sin recomposiciones accidentales) y Room.
 *
 * @property url URL original o canónica del enlace.
 * @property title Título extraído de OpenGraph, Twitter Cards o etiqueta `<title>`.
 * @property description Resumen o descripción del contenido.
 * @property imageUrl URL absoluta de la imagen de portada o previsualización.
 * @property domain Nombre de dominio limpio (ej. "youtube.com").
 * @property faviconUrl URL absoluta al favicon o icono táctil de la página.
 * @property platform Plataforma clasificada según [PlatformFilter].
 */
@Serializable
@Immutable
data class ExtractedMetadata(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val domain: String,
    val faviconUrl: String?,
    val platform: PlatformFilter
)

/**
 * Extractor ligero de metadatos web (OpenGraph, Twitter Cards y meta tags HTML estándar).
 *
 * Implementado bajo la filosofía Ponytail (sin dependencias pesadas de terceros como Jsoup o Ksoup):
 * - Utiliza [HttpURLConnection] nativo con timeouts estrictos de 6 segundos.
 * - Sigue redirecciones entre protocolos y URLs acortadas de forma segura.
 * - Limita la lectura del stream HTML a los primeros 256 KB o hasta el cierre del encabezado `</head>`,
 *   ahorrando ancho de banda y memoria RAM.
 * - Emplea expresiones regulares precompiladas para evitar recompilaciones y GC churn.
 * - Degrada elegantemente ante fallas de red, tiempos de espera o páginas sin metadatos.
 */
object LinkMetadataExtractor {

    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 6000
    private const val MAX_REDIRECTS = 4
    private const val MAX_CHARS_TO_READ = 256 * 1024 // 256 KB en caracteres

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Drafto/1.0"

    // Expresiones regulares precompiladas para parseo seguro sin recreación de objetos
    private val HTML_COMMENT_REGEX = Regex("""<!--[\s\S]*?-->""")
    private val META_TAG_REGEX = Regex("""<meta\s+([^>]+)>""", RegexOption.IGNORE_CASE)
    private val META_KEY_REGEX = Regex("""(?:name|property)\s*=\s*(?:["']([^"']*)["']|([^\s>]+))""", RegexOption.IGNORE_CASE)
    private val META_CONTENT_REGEX = Regex("""content\s*=\s*(?:["']([^"']*)["']|([^\s>]+))""", RegexOption.IGNORE_CASE)

    private val LINK_TAG_REGEX = Regex("""<link\s+([^>]+)>""", RegexOption.IGNORE_CASE)
    private val LINK_REL_REGEX = Regex("""rel\s*=\s*(?:["']([^"']*)["']|([^\s>]+))""", RegexOption.IGNORE_CASE)
    private val LINK_HREF_REGEX = Regex("""href\s*=\s*(?:["']([^"']*)["']|([^\s>]+))""", RegexOption.IGNORE_CASE)

    private val TITLE_TAG_REGEX = Regex("""<title\b[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)

    private val ENTITY_DECIMAL_REGEX = Regex("""&#(\d+);""")
    private val ENTITY_HEX_REGEX = Regex("""&#x([0-9a-fA-F]+);""")

    /**
     * Conjunto de dominios conocidos dedicados a artículos, publicaciones y blogs.
     */
    private val KNOWN_ARTICLE_DOMAINS = setOf(
        "medium.com",
        "substack.com",
        "dev.to",
        "hashnode.com",
        "theverge.com",
        "techcrunch.com",
        "wired.com",
        "arstechnica.com",
        "nytimes.com",
        "bbc.com",
        "cnn.com",
        "reuters.com",
        "bloomberg.com",
        "wsj.com",
        "forbes.com",
        "wikipedia.org",
        "wordpress.com",
        "blogger.com",
        "blogspot.com"
    )

    /**
     * Descarga y extrae metadatos de una URL en [Dispatchers.IO].
     *
     * Si [allowNetwork] es false, opera en modo silencioso y offline por motivos de privacidad,
     * retornando exclusivamente el dominio limpio y la plataforma calculados en memoria local
     * sin abrir conexiones HTTP ni consultar favicons o portadas en la red.
     *
     * Si la petición falla por falta de conectividad, tiempo de espera o bloqueo del servidor,
     * no propaga la excepción hacia la interfaz; en su lugar retorna un [ExtractedMetadata]
     * básico con la URL, el dominio limpio y un favicon de respaldo.
     */
    suspend fun extract(url: String, allowNetwork: Boolean = true): ExtractedMetadata = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        val defaultDomain = extractDomain(trimmedUrl)
        val defaultPlatform = detectPlatform(trimmedUrl)

        if (!allowNetwork || trimmedUrl.isBlank()) {
            return@withContext ExtractedMetadata(
                url = trimmedUrl,
                title = null,
                description = null,
                imageUrl = null,
                domain = defaultDomain,
                faviconUrl = null,
                platform = defaultPlatform
            )
        }

        val defaultFavicon = if (defaultDomain.isNotBlank()) makeAbsoluteUrl("/favicon.ico", trimmedUrl) else null

        var currentUrl = if (trimmedUrl.startsWith("http://", ignoreCase = true) ||
            trimmedUrl.startsWith("https://", ignoreCase = true)
        ) {
            trimmedUrl
        } else {
            "https://$trimmedUrl"
        }

        var redirects = 0
        var activeConnection: HttpURLConnection? = null

        try {
            while (redirects < MAX_REDIRECTS) {
                val connection = (URI(currentUrl).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9,es;q=0.8")
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val redirectLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (redirectLocation.isNullOrBlank()) break
                    currentUrl = makeAbsoluteUrl(redirectLocation, currentUrl)
                    redirects++
                } else {
                    activeConnection = connection
                    break
                }
            }

            val connection = activeConnection
                ?: return@withContext ExtractedMetadata(
                    url = trimmedUrl,
                    title = null,
                    description = null,
                    imageUrl = null,
                    domain = defaultDomain,
                    faviconUrl = defaultFavicon,
                    platform = defaultPlatform
                )

            val htmlContent = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val htmlBuilder = StringBuilder()
                val buffer = CharArray(4096)
                var totalRead = 0
                var charsRead: Int

                while (reader.read(buffer).also { charsRead = it } != -1) {
                    htmlBuilder.append(buffer, 0, charsRead)
                    totalRead += charsRead
                    if (totalRead >= MAX_CHARS_TO_READ || htmlBuilder.contains("</head>", ignoreCase = true)) {
                        break
                    }
                }
                htmlBuilder.toString()
            }

            parseHtml(htmlContent, currentUrl)
        } catch (e: Exception) {
            // Manejo silencioso y elegante para garantizar resiliencia en la UI
            ExtractedMetadata(
                url = trimmedUrl,
                title = null,
                description = null,
                imageUrl = null,
                domain = defaultDomain,
                faviconUrl = defaultFavicon,
                platform = defaultPlatform
            )
        } finally {
            activeConnection?.disconnect()
        }
    }

    /**
     * Parsea una cadena HTML y extrae metadatos OpenGraph, Twitter Cards o etiquetas estándar.
     *
     * Función pura, sincrónica y sin efectos secundarios, idónea para pruebas unitarias deterministas.
     */
    fun parseHtml(html: String, baseUrl: String): ExtractedMetadata {
        val domain = extractDomain(baseUrl)
        var platform = detectPlatform(baseUrl)
        val fallbackFavicon = if (domain.isNotBlank()) makeAbsoluteUrl("/favicon.ico", baseUrl) else null

        if (html.isBlank()) {
            return ExtractedMetadata(
                url = baseUrl,
                title = null,
                description = null,
                imageUrl = null,
                domain = domain,
                faviconUrl = fallbackFavicon,
                platform = platform
            )
        }

        // Eliminamos comentarios HTML para no parsear metatags comentados
        val cleanHtml = html.replace(HTML_COMMENT_REGEX, "")

        // 1. Parseo de etiquetas <meta>
        val metaMap = mutableMapOf<String, String>()
        for (match in META_TAG_REGEX.findAll(cleanHtml)) {
            val tagContent = match.groupValues[1]
            val keyMatch = META_KEY_REGEX.find(tagContent)
            val valueMatch = META_CONTENT_REGEX.find(tagContent)

            if (keyMatch != null && valueMatch != null) {
                val rawKey = keyMatch.groupValues[1].ifEmpty { keyMatch.groupValues[2] }
                val rawValue = valueMatch.groupValues[1].ifEmpty { valueMatch.groupValues[2] }
                val key = rawKey.lowercase().trim()
                if (key.isNotBlank() && rawValue.isNotBlank()) {
                    metaMap.putIfAbsent(key, rawValue)
                }
            }
        }

        // 2. Parseo de etiquetas <link> (favicons, touch icons, image_src)
        var iconHref: String? = null
        var appleTouchIconHref: String? = null
        var imageSrcHref: String? = null

        for (match in LINK_TAG_REGEX.findAll(cleanHtml)) {
            val tagContent = match.groupValues[1]
            val relMatch = LINK_REL_REGEX.find(tagContent)
            val hrefMatch = LINK_HREF_REGEX.find(tagContent)

            if (relMatch != null && hrefMatch != null) {
                val rawRel = relMatch.groupValues[1].ifEmpty { relMatch.groupValues[2] }.lowercase()
                val rawHref = hrefMatch.groupValues[1].ifEmpty { hrefMatch.groupValues[2] }.trim()

                if (rawHref.isNotBlank()) {
                    when {
                        rawRel.contains("apple-touch-icon") && appleTouchIconHref == null -> {
                            appleTouchIconHref = rawHref
                        }
                        (rawRel == "icon" || rawRel.contains("shortcut icon")) && iconHref == null -> {
                            iconHref = rawHref
                        }
                        rawRel == "image_src" && imageSrcHref == null -> {
                            imageSrcHref = rawHref
                        }
                    }
                }
            }
        }

        // 3. Resolución de Título (Prioridad: og:title -> twitter:title -> <title>)
        val rawTitle = metaMap["og:title"]
            ?: metaMap["twitter:title"]
            ?: TITLE_TAG_REGEX.find(cleanHtml)?.groupValues?.get(1)
        val title = rawTitle?.let { unescapeHtml(it).trim() }?.takeIf { it.isNotBlank() }

        // 4. Resolución de Descripción (Prioridad: og:description -> twitter:description -> description)
        val rawDesc = metaMap["og:description"]
            ?: metaMap["twitter:description"]
            ?: metaMap["description"]
        val description = rawDesc?.let { unescapeHtml(it).trim() }?.takeIf { it.isNotBlank() }

        // 5. Resolución de Imagen (Prioridad: og:image -> og:image:secure_url -> og:image:url -> twitter:image -> twitter:image:src -> thumbnail -> itemprop:image -> image_src)
        val rawImage = metaMap["og:image"]
            ?: metaMap["og:image:secure_url"]
            ?: metaMap["og:image:url"]
            ?: metaMap["twitter:image"]
            ?: metaMap["twitter:image:src"]
            ?: metaMap["thumbnail"]
            ?: metaMap["itemprop:image"]
            ?: metaMap["image"]
            ?: imageSrcHref
        val resolvedImageUrl = rawImage?.trim()?.takeIf { it.isNotBlank() }?.let { makeAbsoluteUrl(it, baseUrl) }
        val imageUrl = resolvedImageUrl?.takeIf { isValidImageUrl(it) }

        // 6. Resolución de Favicon
        val rawFavicon = appleTouchIconHref ?: iconHref ?: "/favicon.ico"
        val faviconUrl = makeAbsoluteUrl(rawFavicon, baseUrl).takeIf { it.isNotBlank() }

        // 7. Detección adicional de plataforma por tipo OpenGraph
        if (platform == PlatformFilter.GENERIC) {
            val ogType = metaMap["og:type"]?.lowercase()?.trim()
            if (ogType == "article") {
                platform = PlatformFilter.ARTICLE
            }
        }

        return ExtractedMetadata(
            url = baseUrl,
            title = title,
            description = description,
            imageUrl = imageUrl,
            domain = domain,
            faviconUrl = faviconUrl,
            platform = platform
        )
    }

    /**
     * Determina la categoría de plataforma correspondiente a partir del host o la ruta de la URL.
     */
    fun detectPlatform(url: String): PlatformFilter {
        val domain = extractDomain(url)
        val lowerUrl = url.lowercase()

        // 1. YouTube
        if (domain == "youtube.com" || domain.endsWith(".youtube.com") ||
            domain == "youtu.be" || domain.endsWith(".youtu.be")
        ) {
            return PlatformFilter.YOUTUBE
        }

        // 2. Instagram
        if (domain == "instagram.com" || domain.endsWith(".instagram.com") ||
            domain == "instagr.am" || domain.endsWith(".instagr.am")
        ) {
            return PlatformFilter.INSTAGRAM
        }

        // 3. GitHub
        if (domain == "github.com" || domain.endsWith(".github.com")) {
            return PlatformFilter.GITHUB
        }

        // 4. Twitter / X
        if (domain == "x.com" || domain.endsWith(".x.com") ||
            domain == "twitter.com" || domain.endsWith(".twitter.com")
        ) {
            return PlatformFilter.TWITTER_X
        }

        // 5. Dominios y heurísticas de Artículos / Noticias / Blogs
        val isKnownArticleDomain = KNOWN_ARTICLE_DOMAINS.any { domain == it || domain.endsWith(".$it") }
        val hasBlogSubdomain = domain.startsWith("blog.") || domain.contains(".blog.")
        val hasArticlePath = lowerUrl.contains("/blog/") ||
            lowerUrl.contains("/article/") ||
            lowerUrl.contains("/articles/") ||
            lowerUrl.contains("/post/") ||
            lowerUrl.contains("/posts/") ||
            lowerUrl.contains("/news/") ||
            lowerUrl.contains("/stories/") ||
            lowerUrl.contains("/story/") ||
            lowerUrl.contains("/p/")

        if (isKnownArticleDomain || hasBlogSubdomain || hasArticlePath) {
            return PlatformFilter.ARTICLE
        }

        return PlatformFilter.GENERIC
    }

    /**
     * Extrae el host o nombre de dominio limpio sin prefijos `www.` ni protocolos.
     */
    fun extractDomain(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""

        return try {
            val sanitized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val uri = URI(sanitized)
            val host = uri.host ?: ""
            cleanWwwPrefix(host)
        } catch (e: Exception) {
            val withoutScheme = trimmed.substringAfter("://")
                .substringBefore("/")
                .substringBefore("?")
                .substringBefore(":")
            cleanWwwPrefix(withoutScheme)
        }
    }

    /**
     * Transforma una ruta relativa, absoluta o con doble barra en una URL absoluta canónica.
     */
    fun makeAbsoluteUrl(urlOrPath: String, baseUrl: String): String {
        val trimmed = urlOrPath.trim()
        if (trimmed.isBlank()) return ""

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }

        val baseClean = baseUrl.trim()
        val sanitizedBase = if (baseClean.startsWith("http://", ignoreCase = true) ||
            baseClean.startsWith("https://", ignoreCase = true)
        ) {
            baseClean
        } else {
            "https://$baseClean"
        }

        return try {
            val baseUri = URI(sanitizedBase)
            if (trimmed.startsWith("//")) {
                val scheme = baseUri.scheme ?: "https"
                "$scheme:$trimmed"
            } else {
                baseUri.resolve(trimmed).toString()
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun cleanWwwPrefix(host: String): String {
        val lower = host.lowercase().trim()
        return if (lower.startsWith("www.")) {
            lower.substring(4)
        } else {
            lower
        }
    }

    private fun unescapeHtml(text: String): String {
        if (!text.contains('&')) return text

        var result = text
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&ndash;", "-")
            .replace("&mdash;", "-")

        if (result.contains("&#")) {
            result = ENTITY_DECIMAL_REGEX.replace(result) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null && code in 32..65535) code.toChar().toString() else match.value
            }
            result = ENTITY_HEX_REGEX.replace(result) { match ->
                val code = match.groupValues[1].toIntOrNull(16)
                if (code != null && code in 32..65535) code.toChar().toString() else match.value
            }
        }

        // Se reemplaza &amp; al final para evitar desescapar prematuramente entidades dobles (ej. &amp;lt;)
        return result.replace("&amp;", "&")
    }

    /**
     * Valida que un texto sea una URL web estructuralmente válida.
     *
     * Rechaza cadenas vacías, con espacios, saltos de línea o tabulaciones.
     * Exige prefijo http://, https:// o www. y al menos un punto en el host
     * para descartar texto libre que no represente un enlace real.
     */
    fun isValidUrl(text: String): Boolean {
        val t = text.trim()
        if (t.isBlank() || t.contains(" ") || t.contains("\n") || t.contains("\t")) return false
        val hasValidPrefix = t.startsWith("http://", ignoreCase = true) ||
            t.startsWith("https://", ignoreCase = true) ||
            t.startsWith("www.", ignoreCase = true)
        return hasValidPrefix && t.contains(".") && t.length >= 7
    }

    /**
     * Valida que una URL de imagen sea sintácticamente válida, pública y accesible,
     * descartando artefactos de desarrollo local (localhost, 127.0.0.1, 0.0.0.0).
     */
    fun isValidImageUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val uri = try { URI(trimmed) } catch (_: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false
        if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0" || host.endsWith(".local")) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }
}

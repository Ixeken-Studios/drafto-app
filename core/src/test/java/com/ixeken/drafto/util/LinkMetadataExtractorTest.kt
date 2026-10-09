package com.ixeken.drafto.util

import com.ixeken.drafto.domain.model.PlatformFilter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias para [LinkMetadataExtractor].
 *
 * Valida la extracción de metadatos OpenGraph, retrocompatibilidad con etiquetas HTML estándar,
 * normalización de rutas relativas, detección precisa de plataformas y resiliencia ante errores.
 */
class LinkMetadataExtractorTest {

    @Test
    fun parseHtml_extractsOpenGraphMetadataSuccessfully() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="Drafto - Minimalist Notes" />
                <meta property="og:description" content="Ultra-fast note taking app for Android." />
                <meta property="og:image" content="https://drafto.app/static/og-banner.jpg" />
                <link rel="icon" href="/favicon.ico" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = LinkMetadataExtractor.parseHtml(html, "https://drafto.app")

        assertEquals("Drafto - Minimalist Notes", metadata.title)
        assertEquals("Ultra-fast note taking app for Android.", metadata.description)
        assertEquals("https://drafto.app/static/og-banner.jpg", metadata.imageUrl)
        assertEquals("https://drafto.app/favicon.ico", metadata.faviconUrl)
        assertEquals("drafto.app", metadata.domain)
    }

    @Test
    fun parseHtml_fallsBackToStandardHtmlTagsWhenOpenGraphMissing() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Classic Website Title</title>
                <meta name="description" content="Standard meta description content without OG." />
                <link rel="shortcut icon" href="images/custom-icon.png" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = LinkMetadataExtractor.parseHtml(html, "https://example.com/subpage/")

        assertEquals("Classic Website Title", metadata.title)
        assertEquals("Standard meta description content without OG.", metadata.description)
        assertNull(metadata.imageUrl)
        assertEquals("https://example.com/subpage/images/custom-icon.png", metadata.faviconUrl)
        assertEquals("example.com", metadata.domain)
    }

    @Test
    fun parseHtml_handlesAppleTouchIconAndInvertedMetaAttributes() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta content="Inverted Content Title" property="og:title">
                <meta content="https://touch.example.com/inverted-image.jpg" property="og:image">
                <link rel="apple-touch-icon" href="/apple-touch-icon.png">
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = LinkMetadataExtractor.parseHtml(html, "https://touch.example.com")

        assertEquals("Inverted Content Title", metadata.title)
        assertEquals("https://touch.example.com/inverted-image.jpg", metadata.imageUrl)
        assertEquals("https://touch.example.com/apple-touch-icon.png", metadata.faviconUrl)
    }

    @Test
    fun parseHtml_unescapesHtmlEntitiesInTitleAndDescription() {
        val html = """
            <html>
            <head>
                <meta property="og:title" content="Drafto &amp; Co. &ndash; Fast &quot;Notes&quot; &#39;App&#39;" />
                <meta property="og:description" content="Save &lt;links&gt; &amp; notes seamlessly with &amp;lt;code&amp;gt;." />
            </head>
            </html>
        """.trimIndent()

        val metadata = LinkMetadataExtractor.parseHtml(html, "https://drafto.app")

        assertEquals("Drafto & Co. - Fast \"Notes\" 'App'", metadata.title)
        assertEquals("Save <links> & notes seamlessly with &lt;code&gt;.", metadata.description)
    }

    @Test
    fun makeAbsoluteUrl_normalizesRelativeAndProtocolRelativeUrls() {
        val base = "https://drafto.app/blog/tech-update"

        // Ruta absoluta en mismo host
        assertEquals(
            "https://drafto.app/assets/logo.png",
            LinkMetadataExtractor.makeAbsoluteUrl("/assets/logo.png", base)
        )

        // Ruta relativa
        assertEquals(
            "https://drafto.app/blog/cover.jpg",
            LinkMetadataExtractor.makeAbsoluteUrl("cover.jpg", base)
        )

        // URL con doble barra (protocol-relative)
        assertEquals(
            "https://cdn.drafto.app/banner.webp",
            LinkMetadataExtractor.makeAbsoluteUrl("//cdn.drafto.app/banner.webp", base)
        )

        // URL ya absoluta no se altera
        assertEquals(
            "https://images.unsplash.com/photo-12345",
            LinkMetadataExtractor.makeAbsoluteUrl("https://images.unsplash.com/photo-12345", base)
        )
    }

    @Test
    fun extractDomain_cleansWwwAndExtractsHostAccurately() {
        assertEquals("youtube.com", LinkMetadataExtractor.extractDomain("https://www.youtube.com/watch?v=123"))
        assertEquals("youtube.com", LinkMetadataExtractor.extractDomain("https://youtube.com/watch?v=123"))
        assertEquals("github.com", LinkMetadataExtractor.extractDomain("https://github.com/ixeken/drafto"))
        assertEquals("blog.drafto.app", LinkMetadataExtractor.extractDomain("https://blog.drafto.app/articles/intro"))
        assertEquals("x.com", LinkMetadataExtractor.extractDomain("http://x.com/elonmusk"))
        assertEquals("instagram.com", LinkMetadataExtractor.extractDomain("www.instagram.com/reel/xyz"))
        assertEquals("", LinkMetadataExtractor.extractDomain(""))
    }

    @Test
    fun detectPlatform_categorizesMajorPlatformsCorrectly() {
        // YouTube
        assertEquals(PlatformFilter.YOUTUBE, LinkMetadataExtractor.detectPlatform("https://www.youtube.com/watch?v=abc"))
        assertEquals(PlatformFilter.YOUTUBE, LinkMetadataExtractor.detectPlatform("https://youtu.be/abc"))
        assertEquals(PlatformFilter.YOUTUBE, LinkMetadataExtractor.detectPlatform("https://music.youtube.com/watch?v=abc"))

        // Instagram
        assertEquals(PlatformFilter.INSTAGRAM, LinkMetadataExtractor.detectPlatform("https://www.instagram.com/p/C999/"))
        assertEquals(PlatformFilter.INSTAGRAM, LinkMetadataExtractor.detectPlatform("https://instagram.com/reel/123"))

        // GitHub
        assertEquals(PlatformFilter.GITHUB, LinkMetadataExtractor.detectPlatform("https://github.com/ixeken/drafto"))
        assertEquals(PlatformFilter.GITHUB, LinkMetadataExtractor.detectPlatform("https://www.github.com/topics/android"))

        // Twitter / X
        assertEquals(PlatformFilter.TWITTER_X, LinkMetadataExtractor.detectPlatform("https://twitter.com/AndroidDev"))
        assertEquals(PlatformFilter.TWITTER_X, LinkMetadataExtractor.detectPlatform("https://x.com/AndroidDev/status/123"))
        assertEquals(PlatformFilter.TWITTER_X, LinkMetadataExtractor.detectPlatform("https://mobile.twitter.com/home"))

        // Article heuristics (Dominios conocidos y rutas de blog)
        assertEquals(PlatformFilter.ARTICLE, LinkMetadataExtractor.detectPlatform("https://medium.com/@user/clean-arch"))
        assertEquals(PlatformFilter.ARTICLE, LinkMetadataExtractor.detectPlatform("https://techblog.substack.com/p/new-version"))
        assertEquals(PlatformFilter.ARTICLE, LinkMetadataExtractor.detectPlatform("https://dev.to/author/my-awesome-post"))
        assertEquals(PlatformFilter.ARTICLE, LinkMetadataExtractor.detectPlatform("https://example.com/blog/2026-announcement"))
        assertEquals(PlatformFilter.ARTICLE, LinkMetadataExtractor.detectPlatform("https://company.org/news/press-release"))

        // Generic
        assertEquals(PlatformFilter.GENERIC, LinkMetadataExtractor.detectPlatform("https://example.com"))
        assertEquals(PlatformFilter.GENERIC, LinkMetadataExtractor.detectPlatform("https://tools.ietf.org/rfc/rfc2616.txt"))
    }

    @Test
    fun parseHtml_infersArticlePlatformFromOgType() {
        val html = """
            <html>
            <head>
                <meta property="og:type" content="article" />
                <meta property="og:title" content="In-depth Architecture Guide" />
            </head>
            </html>
        """.trimIndent()

        // Dominio no reconocido previamente en heurísticas estáticas
        val metadata = LinkMetadataExtractor.parseHtml(html, "https://somewebsite.org/read/123")
        assertEquals(PlatformFilter.ARTICLE, metadata.platform)
    }

    @Test
    fun parseHtml_handlesEmptyOrMalformedHtmlResiliently() {
        // HTML vacío
        val emptyMetadata = LinkMetadataExtractor.parseHtml("", "https://drafto.app")
        assertNull(emptyMetadata.title)
        assertNull(emptyMetadata.description)
        assertNull(emptyMetadata.imageUrl)
        assertEquals("drafto.app", emptyMetadata.domain)
        assertEquals("https://drafto.app/favicon.ico", emptyMetadata.faviconUrl)

        // HTML malformado / truncado
        val malformedHtml = """
            <html><head><meta property="og:title" content="Cut off title without closing
            <meta property="og:description"
        """.trimIndent()
        val malformedMetadata = LinkMetadataExtractor.parseHtml(malformedHtml, "https://drafto.app")
        assertNotNull(malformedMetadata)
        assertEquals("drafto.app", malformedMetadata.domain)
    }

    @Test
    fun extract_returnsGracefulDegradedResultOnNetworkFailure() = runBlocking {
        // Petición a un puerto local cerrado para simular error de red sin lanzar excepciones
        val degraded = LinkMetadataExtractor.extract("http://127.0.0.1:59999/unreachable-path")

        assertEquals("http://127.0.0.1:59999/unreachable-path", degraded.url)
        assertNull(degraded.title)
        assertNull(degraded.description)
        assertNull(degraded.imageUrl)
        assertEquals("127.0.0.1", degraded.domain)
        assertEquals("http://127.0.0.1:59999/favicon.ico", degraded.faviconUrl)
        assertEquals(PlatformFilter.GENERIC, degraded.platform)
    }

    @Test
    fun extract_whenNetworkDisabled_returnsOfflineMetadataWithoutNetwork() = runBlocking {
        val offlineMetadata = LinkMetadataExtractor.extract(
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            allowNetwork = false
        )

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", offlineMetadata.url)
        assertNull(offlineMetadata.title)
        assertNull(offlineMetadata.description)
        assertNull(offlineMetadata.imageUrl)
        assertNull(offlineMetadata.faviconUrl)
        assertEquals("youtube.com", offlineMetadata.domain)
        assertEquals(PlatformFilter.YOUTUBE, offlineMetadata.platform)
    }
}

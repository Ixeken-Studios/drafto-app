package com.ixeken.drafto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.util.LinkMetadataExtractor
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Actividad headless y traslucida para el guardado rapido de marcadores desde el menu compartir del sistema.
 *
 * No infla ninguna interfaz visual de usuario, intercepta intents con accion [Intent.ACTION_SEND] y tipo
 * MIME text/plain, extrae la URL compartida, la persiste de forma inmediata en [BookmarkRepository]
 * con metadatos preliminares y lanza la resolucion asincrona no bloqueante de metadatos completos
 * a traves de [LinkMetadataExtractor] sobre [Dispatchers.IO]. Notifica al usuario exclusivamente mediante
 * Toasts nativos sobre el contexto de aplicacion y finaliza su ciclo de vida de inmediato para no retener
 * recursos de ventana.
 */
@AndroidEntryPoint
class QuickSaveActivity : ComponentActivity() {

    @Inject
    lateinit var bookmarkRepository: BookmarkRepository

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val url = extractUrl(rawText)

        if (url.isNullOrBlank()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.toast_invalid_link),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        applicationScope.launch {
            try {
                val existing = bookmarkRepository.findBookmarkByUrl(url)
                if (existing != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.toast_bookmark_already_exists),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    return@launch
                }

                val isNetworkEnabled = settingsDataStore.fetchWebMetadataFlow.first()
                val domain = LinkMetadataExtractor.extractDomain(url)
                val platform = LinkMetadataExtractor.detectPlatform(url)
                val fallbackFavicon = if (isNetworkEnabled && domain.isNotBlank()) {
                    LinkMetadataExtractor.makeAbsoluteUrl("/favicon.ico", url)
                } else null

                val bookmark = Bookmark(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    title = domain,
                    description = null,
                    imageUrl = null,
                    domain = domain,
                    faviconUrl = fallbackFavicon,
                    createdAt = System.currentTimeMillis(),
                    isPinned = false,
                    platform = platform
                )

                bookmarkRepository.saveBookmark(bookmark)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.toast_bookmark_quick_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                val metadata = LinkMetadataExtractor.extract(url, allowNetwork = isNetworkEnabled)
                val enriched = bookmark.copy(
                    title = metadata.title ?: bookmark.title,
                    description = metadata.description,
                    imageUrl = metadata.imageUrl,
                    faviconUrl = metadata.faviconUrl ?: bookmark.faviconUrl,
                    platform = metadata.platform
                )
                bookmarkRepository.updateBookmark(enriched)
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = URL_REGEX.find(text)
        if (match != null) {
            return match.value.trimEnd('.', ',', ')', ']', '!', ';', '?', '>')
        }
        val trimmed = text.trim().trimEnd('.', ',', ')', ']', '!', ';', '?', '>')
        val isCandidate = trimmed.startsWith("www.", ignoreCase = true) ||
            (trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.contains("\n"))
        if (isCandidate) {
            val domain = LinkMetadataExtractor.extractDomain(trimmed)
            if (domain.contains(".")) {
                return "https://$trimmed"
            }
        }
        return null
    }

    companion object {
        private val URL_REGEX = Regex("""https?://[^\s<>"']+""")
    }
}

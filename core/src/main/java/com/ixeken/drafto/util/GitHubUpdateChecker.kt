package com.ixeken.drafto.util

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Resultado estructurado e inmutable de la verificación de versiones contra GitHub Releases.
 */
@Immutable
sealed interface UpdateCheckResult {
    /**
     * Existe una versión pública en GitHub superior a la instalada.
     */
    data class UpdateAvailable(
        val version: String,
        val changelog: String,
        val releaseUrl: String
    ) : UpdateCheckResult

    /**
     * La versión instalada coincide con el último release oficial publicado.
     */
    data class UpToDate(
        val version: String
    ) : UpdateCheckResult

    /**
     * La versión local es superior a la disponible en GitHub (desarrollo local o compilación futura).
     */
    data class TimeTraveler(
        val currentVersion: String,
        val latestVersion: String
    ) : UpdateCheckResult

    /**
     * El repositorio no existe, es privado o carece de versiones publicadas (HTTP 404).
     */
    data object RepoNotFoundOrPrivate : UpdateCheckResult

    /**
     * La comprobación falló por falta de red, tiempo de espera o respuesta de servidor.
     */
    data class Error(
        val message: String? = null
    ) : UpdateCheckResult
}

/**
 * Cliente ligero y autónomo para verificar actualizaciones vía GitHub Releases API.
 * Diseñado bajo la filosofía Ponytail:
 * - Emplea [HttpURLConnection] nativo sin bibliotecas externas de red.
 * - Timeout estricto de 6 segundos en conexión y lectura.
 * - Deserialización segura con KotlinX Serialization en [Dispatchers.IO].
 * - Comparación semántica de versiones sin dependencias de terceros.
 */
object GitHubUpdateChecker {

    private const val API_URL = "https://api.github.com/repos/Ixeken-Studios/drafto-app/releases/latest"
    private const val DEFAULT_RELEASE_URL = "https://github.com/Ixeken-Studios/drafto-app/releases/latest"
    private const val TIMEOUT_MS = 6000

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class GitHubReleaseDto(
        @SerialName("tag_name") val tagName: String = "",
        @SerialName("name") val name: String = "",
        @SerialName("body") val body: String = "",
        @SerialName("html_url") val htmlUrl: String = ""
    )

    /**
     * Consulta el último release en GitHub y lo compara con la versión actual de la aplicación.
     *
     * @param currentVersion Nombre de versión actual (ej. "1.0" o "1.0.0").
     * @return [UpdateCheckResult] con el diagnóstico correspondiente.
     */
    suspend fun checkUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val uri = URI(API_URL)
            connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Drafto-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.RepoNotFoundOrPrivate
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("HTTP $responseCode")
            }

            val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val releaseDto = json.decodeFromString<GitHubReleaseDto>(responseText)

            if (releaseDto.tagName.isBlank()) {
                return@withContext UpdateCheckResult.Error("No release tag found")
            }

            val remoteTag = releaseDto.tagName.trim()
            val comparison = compareVersions(currentVersion, remoteTag)
            val releaseUrl = if (releaseDto.htmlUrl.isNotBlank()) releaseDto.htmlUrl else DEFAULT_RELEASE_URL

            when {
                comparison < 0 -> UpdateCheckResult.UpdateAvailable(
                    version = remoteTag,
                    changelog = releaseDto.body.trim(),
                    releaseUrl = releaseUrl
                )
                comparison > 0 -> UpdateCheckResult.TimeTraveler(
                    currentVersion = currentVersion,
                    latestVersion = remoteTag
                )
                else -> UpdateCheckResult.UpToDate(
                    version = currentVersion
                )
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compara dos cadenas de versión semántica (ej. "1.0.0" vs "v1.1.0").
     * Retorna:
     * - Valor negativo si current < remote (hay actualización disponible).
     * - 0 si son equivalentes.
     * - Valor positivo si current > remote (versión futura / máquina del tiempo).
     */
    fun compareVersions(current: String, remote: String): Int {
        val currentParts = parseVersionParts(current)
        val remoteParts = parseVersionParts(remote)
        val maxLength = maxOf(currentParts.size, remoteParts.size)

        for (i in 0 until maxLength) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (c != r) {
                return c.compareTo(r)
            }
        }
        return 0
    }

    private fun parseVersionParts(version: String): List<Int> {
        val clean = version.trim().lowercase().removePrefix("v")
        val numericPart = clean.takeWhile { it.isDigit() || it == '.' }
        return numericPart.split(".").mapNotNull { it.toIntOrNull() }
    }
}

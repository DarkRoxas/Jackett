package fr.cinepass.ticket.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Recherche d'affiches animées via [GIPHY](https://developers.giphy.com).
 *
 * Aucune base de cinéma ne diffuse de « motion poster » par API : TMDB, OMDb et
 * consorts ne servent que des images fixes. GIPHY est la seule source
 * interrogeable qui renvoie des animations avec leurs dimensions, ce qui permet
 * d'en proposer une sélection comme pour les affiches classiques.
 *
 * Le contenu est communautaire : la qualité est très inégale et il ne s'agit pas
 * d'affiches officielles. D'où le classement (vertical d'abord, puis taille) et
 * l'affichage de la définition sous chaque vignette.
 */
class AnimatedPosterRepository(private val apiKeyProvider: suspend () -> String) {

    suspend fun isConfigured(): Boolean = apiKeyProvider().isNotBlank()

    suspend fun search(query: String, limit: Int = 30): List<AnimatedPoster> {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) error("Aucune clé GIPHY configurée.")
        if (query.isBlank()) return emptyList()

        val url = buildString {
            append("$API_BASE/gifs/search")
            append("?api_key=").append(apiKey)
            append("&q=").append(URLEncoder.encode(query, "UTF-8"))
            append("&limit=").append(limit)
            append("&rating=pg-13")
            append("&lang=fr")
        }

        return withContext(Dispatchers.IO) { parse(get(url)) }
    }

    private fun get(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code == 401 || code == 403) error("Clé GIPHY refusée ($code). Vérifiez-la dans les réglages.")
            if (code !in 200..299) error("GIPHY a répondu $code.")

            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(payload: JSONObject): List<AnimatedPoster> {
        val data = payload.optJSONArray("data") ?: return emptyList()

        val posters = (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val images = item.optJSONObject("images") ?: return@mapNotNull null
            val original = images.optJSONObject("original") ?: return@mapNotNull null

            val url = original.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val width = original.optString("width").toIntOrNull() ?: 0
            val height = original.optString("height").toIntOrNull() ?: 0
            if (width < MIN_SIZE || height < MIN_SIZE) return@mapNotNull null

            val preview = images.optJSONObject("fixed_width")?.optString("url")?.takeIf { it.isNotBlank() }

            AnimatedPoster(
                id = item.optString("id").takeIf { it.isNotBlank() } ?: url,
                title = item.optString("title").takeIf { it.isNotBlank() } ?: "Sans titre",
                url = url,
                previewUrl = preview ?: url,
                width = width,
                height = height,
            )
        }

        return posters.sortedWith(
            // Format vertical d'abord — c'est celui d'une affiche —, puis la
            // définition, une animation minuscule étant inutilisable.
            compareByDescending<AnimatedPoster> { it.isPortrait }
                .thenByDescending { it.width.toLong() * it.height },
        )
    }

    private companion object {
        const val API_BASE = "https://api.giphy.com/v1"

        /** En dessous, l'animation est trop petite pour tenir l'écran. */
        const val MIN_SIZE = 200
    }
}

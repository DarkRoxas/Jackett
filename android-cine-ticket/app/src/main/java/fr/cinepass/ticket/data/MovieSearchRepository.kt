package fr.cinepass.ticket.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Recherche de films dans la base [TMDB](https://www.themoviedb.org).
 *
 * La clé d'API est saisie dans les réglages de l'application (à défaut, elle
 * vient de `local.properties` via `BuildConfig`). Sans clé, la recherche est
 * simplement désactivée et la saisie manuelle reste disponible.
 */
class MovieSearchRepository(private val apiKeyProvider: suspend () -> String) {

    suspend fun isConfigured(): Boolean = apiKeyProvider().isNotBlank()

    suspend fun search(query: String, language: String = "fr-FR"): List<MovieSearchResult> {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) error("Aucune clé TMDB configurée.")
        if (query.isBlank()) return emptyList()

        val url = buildString {
            append("$API_BASE/search/movie")
            append("?api_key=").append(apiKey)
            append("&query=").append(URLEncoder.encode(query, "UTF-8"))
            append("&language=").append(language)
            append("&include_adult=false")
        }

        return withContext(Dispatchers.IO) { parse(get(url)) }
    }

    /**
     * Toutes les affiches d'un film, la plus pertinente en tête.
     *
     * `search/movie` ne renvoie qu'une affiche « primaire » qui n'est pas
     * forcément celle exploitée en salle. On interroge donc la galerie complète
     * et on classe : français d'abord, puis note et nombre de votes — ce qui
     * fait remonter l'affiche officielle plutôt qu'une variante ou un teaser.
     */
    suspend fun posters(movieId: Int): List<String> {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) return emptyList()

        val url = "$API_BASE/movie/$movieId/images" +
            "?api_key=$apiKey" +
            "&include_image_language=fr,null,en"

        return withContext(Dispatchers.IO) {
            runCatching { parsePosters(get(url)) }.getOrDefault(emptyList())
        }
    }

    /** Meilleure affiche disponible, ou [fallback] si la galerie est vide. */
    suspend fun bestPoster(movieId: Int, fallback: String?): String? =
        posters(movieId).firstOrNull() ?: fallback

    internal fun parsePosters(payload: JSONObject): List<String> {
        val posters = payload.optJSONArray("posters") ?: return emptyList()

        data class Candidate(
            val path: String,
            val languageRank: Int,
            val voteAverage: Double,
            val voteCount: Int,
        )

        val candidates = (0 until posters.length()).mapNotNull { index ->
            val item = posters.optJSONObject(index) ?: return@mapNotNull null
            val path = item.optString("file_path").takeIf { it.isNotBlank() } ?: return@mapNotNull null

            Candidate(
                path = path,
                languageRank = when (item.optString("iso_639_1").takeIf { it.isNotBlank() && it != "null" }) {
                    "fr" -> 3
                    null -> 2 // affiche sans texte : utilisable partout
                    "en" -> 1
                    else -> 0
                },
                voteAverage = item.optDouble("vote_average", 0.0),
                voteCount = item.optInt("vote_count"),
            )
        }

        return candidates
            .sortedWith(
                compareByDescending<Candidate> { it.languageRank }
                    .thenByDescending { it.voteAverage }
                    .thenByDescending { it.voteCount },
            )
            .map { "$IMAGE_BASE/$POSTER_SIZE${it.path}" }
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

            if (code == 401) error("Clé TMDB refusée (401). Vérifiez TMDB_API_KEY.")
            if (code !in 200..299) error("TMDB a répondu $code.")

            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(payload: JSONObject): List<MovieSearchResult> {
        val results = payload.optJSONArray("results") ?: return emptyList()

        return (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").ifBlank { item.optString("original_title") }
            if (title.isBlank()) return@mapNotNull null

            val posterPath = item.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }

            MovieSearchResult(
                id = item.optInt("id"),
                title = title,
                originalTitle = item.optString("original_title").takeIf { it.isNotBlank() && it != title },
                // release_date vaut "2014-11-05", ou "" pour un film sans date annoncée.
                releaseYear = item.optString("release_date").take(4).toIntOrNull(),
                posterUrl = posterPath?.let { "$IMAGE_BASE/$POSTER_SIZE$it" },
                thumbnailUrl = posterPath?.let { "$IMAGE_BASE/$THUMBNAIL_SIZE$it" },
                overview = item.optString("overview").takeIf { it.isNotBlank() },
            )
        }
    }

    private companion object {
        const val API_BASE = "https://api.themoviedb.org/3"
        const val IMAGE_BASE = "https://image.tmdb.org/t/p"
        const val POSTER_SIZE = "w780"
        const val THUMBNAIL_SIZE = "w185"
    }
}

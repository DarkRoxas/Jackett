package fr.cinepass.ticket.data

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieSearchRepositoryTest {

    private val repository = MovieSearchRepository { "cle-de-test" }

    private fun payload(vararg movies: String) =
        JSONObject("""{"page":1,"results":[${movies.joinToString(",")}]}""")

    private val dune2021 = """
        {
          "id": 438631,
          "title": "Dune",
          "original_title": "Dune",
          "release_date": "2021-09-15",
          "poster_path": "/affiche2021.jpg",
          "overview": "Paul Atréides rejoint Arrakis."
        }
    """.trimIndent()

    private val dune1984 = """
        {
          "id": 841,
          "title": "Dune",
          "original_title": "Dune",
          "release_date": "1984-12-14",
          "poster_path": "/affiche1984.jpg",
          "overview": "L'adaptation de David Lynch."
        }
    """.trimIndent()

    @Test
    fun `search is disabled without an api key`() = runBlocking {
        assertFalse(MovieSearchRepository { "" }.isConfigured())
        assertTrue(repository.isConfigured())
    }

    @Test
    fun `the year distinguishes two movies sharing a title`() {
        val results = repository.parse(payload(dune2021, dune1984))

        assertEquals(listOf("Dune (2021)", "Dune (1984)"), results.map { it.label })
    }

    @Test
    fun `poster urls are built for display and for the result list`() {
        val movie = repository.parse(payload(dune2021)).single()

        assertEquals("https://image.tmdb.org/t/p/w780/affiche2021.jpg", movie.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w185/affiche2021.jpg", movie.thumbnailUrl)
    }

    @Test
    fun `a movie without poster or release date stays usable`() {
        val bare = """{"id": 7, "title": "Court métrage", "release_date": "", "poster_path": null}"""
        val movie = repository.parse(payload(bare)).single()

        assertEquals("Court métrage", movie.label)
        assertNull(movie.releaseYear)
        assertNull(movie.posterUrl)
        assertNull(movie.thumbnailUrl)
    }

    @Test
    fun `the original title is kept only when it differs from the localised one`() {
        val localised = """
            {"id": 1, "title": "Le Parrain", "original_title": "The Godfather", "release_date": "1972-03-14"}
        """.trimIndent()

        assertEquals("The Godfather", repository.parse(payload(localised)).single().originalTitle)
        assertNull(repository.parse(payload(dune2021)).single().originalTitle)
    }

    @Test
    fun `entries without a usable title are dropped`() {
        val untitled = """{"id": 2, "title": "", "original_title": ""}"""

        assertEquals(1, repository.parse(payload(untitled, dune2021)).size)
    }

    // --- Galerie d'affiches ---

    private fun posters(vararg entries: String) =
        JSONObject("""{"id":1,"posters":[${entries.joinToString(",")}]}""")

    private fun poster(path: String, language: String?, vote: Double, count: Int) =
        """{"file_path":"$path","iso_639_1":${language?.let { "\"$it\"" } ?: "null"},""" +
            """"vote_average":$vote,"vote_count":$count}"""

    @Test
    fun `the french poster wins over an english one better rated`() {
        val ranked = repository.parsePosters(
            posters(
                poster("/anglaise.jpg", "en", vote = 8.0, count = 90),
                poster("/francaise.jpg", "fr", vote = 5.0, count = 3),
            ),
        )

        assertEquals("https://image.tmdb.org/t/p/w780/francaise.jpg", ranked.first())
    }

    @Test
    fun `among french posters the most voted comes first`() {
        val ranked = repository.parsePosters(
            posters(
                poster("/variante.jpg", "fr", vote = 5.1, count = 2),
                poster("/officielle.jpg", "fr", vote = 6.8, count = 12),
                poster("/teaser.jpg", "fr", vote = 5.1, count = 1),
            ),
        )

        assertEquals(
            listOf("/officielle.jpg", "/variante.jpg", "/teaser.jpg")
                .map { "https://image.tmdb.org/t/p/w780$it" },
            ranked,
        )
    }

    @Test
    fun `a textless poster is preferred to an english one`() {
        val ranked = repository.parsePosters(
            posters(
                poster("/anglaise.jpg", "en", vote = 9.0, count = 400),
                poster("/sans-texte.jpg", null, vote = 1.0, count = 1),
            ),
        )

        assertEquals("https://image.tmdb.org/t/p/w780/sans-texte.jpg", ranked.first())
    }

    @Test
    fun `a movie without gallery yields no poster`() {
        assertTrue(repository.parsePosters(JSONObject("""{"id":1}""")).isEmpty())
        assertTrue(repository.parsePosters(posters()).isEmpty())
    }

    @Test
    fun `an empty response yields no result`() {
        assertTrue(repository.parse(JSONObject("""{"page":1}""")).isEmpty())
        assertTrue(repository.parse(payload()).isEmpty())
    }
}

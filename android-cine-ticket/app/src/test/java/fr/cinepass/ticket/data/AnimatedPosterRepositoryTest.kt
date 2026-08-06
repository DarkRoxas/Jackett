package fr.cinepass.ticket.data

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedPosterRepositoryTest {

    private val repository = AnimatedPosterRepository { "cle-de-test" }

    private fun response(vararg entries: String) =
        JSONObject("""{"data":[${entries.joinToString(",")}]}""")

    private fun gif(
        id: String,
        width: Int,
        height: Int,
        title: String = "Motion poster",
    ) = """
        {
          "id": "$id",
          "title": "$title",
          "images": {
            "original": {"url":"https://media.giphy.com/$id.gif","width":"$width","height":"$height"},
            "fixed_width": {"url":"https://media.giphy.com/$id-200w.gif","width":"200","height":"300"}
          }
        }
    """.trimIndent()

    @Test
    fun `search is disabled without an api key`() = runBlocking {
        assertFalse(AnimatedPosterRepository { "" }.isConfigured())
        assertTrue(repository.isConfigured())
    }

    @Test
    fun `a portrait animation comes before a wider one`() {
        val results = repository.parse(
            response(
                gif("paysage", width = 1920, height = 1080),
                gif("affiche", width = 800, height = 1200),
            ),
        )

        assertEquals("affiche", results.first().id)
    }

    @Test
    fun `among portrait animations the largest comes first`() {
        val results = repository.parse(
            response(
                gif("petite", width = 400, height = 600),
                gif("grande", width = 1000, height = 1500),
            ),
        )

        assertEquals(listOf("grande", "petite"), results.map { it.id })
    }

    @Test
    fun `tiny animations are dropped`() {
        val results = repository.parse(
            response(
                gif("vignette", width = 120, height = 90),
                gif("utilisable", width = 800, height = 1200),
            ),
        )

        assertEquals(listOf("utilisable"), results.map { it.id })
    }

    @Test
    fun `the grid uses the lightweight rendition`() {
        val poster = repository.parse(response(gif("abc", width = 800, height = 1200))).single()

        assertEquals("https://media.giphy.com/abc-200w.gif", poster.previewUrl)
        assertEquals("https://media.giphy.com/abc.gif", poster.url)
        assertEquals("800 × 1200", poster.resolutionLabel)
        assertTrue(poster.isPortrait)
    }

    @Test
    fun `an entry without images is skipped`() {
        val results = repository.parse(
            response("""{"id":"vide","title":"Sans images"}""", gif("ok", 800, 1200)),
        )

        assertEquals(listOf("ok"), results.map { it.id })
    }

    @Test
    fun `an empty response yields no result`() {
        assertTrue(repository.parse(JSONObject("{}")).isEmpty())
        assertTrue(repository.parse(response()).isEmpty())
    }
}

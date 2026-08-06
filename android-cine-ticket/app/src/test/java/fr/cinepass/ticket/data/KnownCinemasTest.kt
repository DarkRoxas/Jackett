package fr.cinepass.ticket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownCinemasTest {

    @Test
    fun `the usual cinemas are offered even on a fresh install`() {
        val suggestions = KnownCinemas.suggestions(used = emptyList())

        assertEquals(KnownCinemas.defaults, suggestions)
        assertTrue(suggestions.any { it.contains("Montpon") })
        assertTrue(suggestions.any { it.contains("Sainte-Foy") })
    }

    @Test
    fun `a cinema already used is added to the list`() {
        val suggestions = KnownCinemas.suggestions(used = listOf("UGC Les Halles"))

        assertTrue(suggestions.containsAll(KnownCinemas.defaults))
        assertTrue(suggestions.contains("UGC Les Halles"))
    }

    @Test
    fun `a used cinema does not duplicate a default one`() {
        val suggestions = KnownCinemas.suggestions(used = listOf(KnownCinemas.defaults.first()))

        assertEquals(KnownCinemas.defaults.size, suggestions.size)
    }

    @Test
    fun `duplicates differing only by case or spacing are merged`() {
        val suggestions = KnownCinemas.suggestions(
            used = listOf("Le Rex", "le rex", "  Le Rex  "),
        )

        assertEquals(KnownCinemas.defaults.size + 1, suggestions.size)
    }
}

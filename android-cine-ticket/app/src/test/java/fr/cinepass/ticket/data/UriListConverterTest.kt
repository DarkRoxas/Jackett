package fr.cinepass.ticket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UriListConverterTest {

    private val converter = UriListConverter()

    @Test
    fun `the list survives a round trip through storage`() {
        val uris = listOf("file:///data/a.jpg", "file:///data/b.gif", "file:///data/c.webp")

        assertEquals(uris, converter.fromStorage(converter.toStorage(uris)))
    }

    @Test
    fun `order is preserved because the first poster is the cover`() {
        val uris = listOf("file:///cover.jpg", "file:///second.jpg")

        assertEquals("file:///cover.jpg", converter.fromStorage(converter.toStorage(uris)).first())
    }

    @Test
    fun `an empty list maps to an empty column`() {
        assertEquals("", converter.toStorage(emptyList()))
        assertTrue(converter.fromStorage("").isEmpty())
    }

    @Test
    fun `a single uri reads back as one poster`() {
        // C'est la forme produite par la migration depuis la colonne unique.
        assertEquals(listOf("file:///seule.jpg"), converter.fromStorage("file:///seule.jpg"))
    }

    @Test
    fun `blank lines are ignored`() {
        assertEquals(listOf("file:///a.jpg"), converter.fromStorage("\nfile:///a.jpg\n\n"))
    }
}

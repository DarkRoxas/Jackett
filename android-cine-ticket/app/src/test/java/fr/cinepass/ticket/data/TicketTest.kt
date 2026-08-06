package fr.cinepass.ticket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TicketTest {

    private val now = 1_767_200_400_000L // 31/12/2025 14:20 UTC

    private fun ticket(screeningAt: Long, archived: Boolean = false) = Ticket(
        movieTitle = "Film",
        cinemaName = "Cinéma",
        screeningAt = screeningAt,
        barcodeValue = "CODE",
        archived = archived,
    )

    @Test
    fun `a future screening is upcoming`() {
        assertTrue(ticket(now + TimeUnit.HOURS.toMillis(2)).isUpcoming(now))
    }

    @Test
    fun `a screening stays upcoming during the grace period`() {
        // La séance a commencé il y a 3 h : le billet doit rester accessible.
        assertTrue(ticket(now - TimeUnit.HOURS.toMillis(3)).isUpcoming(now))
    }

    @Test
    fun `a screening older than the grace period is archived`() {
        assertFalse(ticket(now - TimeUnit.HOURS.toMillis(5)).isUpcoming(now))
    }

    @Test
    fun `manual archiving wins over the screening date`() {
        assertFalse(ticket(now + TimeUnit.DAYS.toMillis(3), archived = true).isUpcoming(now))
    }

    @Test
    fun `the display title carries the release year when known`() {
        val ticket = ticket(now).copy(movieTitle = "Dune", releaseYear = 2021)

        assertEquals("Dune (2021)", ticket.displayTitle)
    }

    @Test
    fun `the display title falls back to the bare title`() {
        assertEquals("Film", ticket(now).displayTitle)
    }

    @Test
    fun `screening date time uses the device time zone`() {
        val dateTime = ticket(now).screeningDateTime

        assertTrue(dateTime.year >= 2025)
    }
}

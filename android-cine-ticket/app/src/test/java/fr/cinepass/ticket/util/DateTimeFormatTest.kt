package fr.cinepass.ticket.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DateTimeFormatTest {

    @Test
    fun `epoch conversion round trips through the device time zone`() {
        val dateTime = LocalDateTime.of(2026, 3, 14, 20, 30)

        assertEquals(dateTime, dateTime.toEpochMillis().toLocalDateTime())
    }

    @Test
    fun `date picker conversions keep the same calendar day`() {
        // Le DatePicker de Material 3 renvoie du minuit UTC : passer par le
        // fuseau local décalerait la date d'un jour à l'ouest de Greenwich.
        val date = LocalDate.of(2026, 3, 14)

        assertEquals(date, date.toUtcMillis().toUtcLocalDate())
    }

    @Test
    fun `time is formatted on 24 hours`() {
        val millis = LocalDateTime.of(2026, 3, 14, 9, 5).toEpochMillis()

        assertEquals("09:05", formatTime(millis))
    }

    @Test
    fun `short date is capitalised for the French locale`() {
        val formatted = formatShortDate(LocalDate.of(2026, 3, 14))

        assertEquals(formatted.first().uppercase(), formatted.first().toString())
        assertEquals("2026", formatted.takeLast(4))
    }

    @Test
    fun `date and time are joined for the ticket list`() {
        val millis = LocalDate.of(2026, 3, 14).atTime(LocalTime.of(20, 30)).toEpochMillis()

        assertEquals("${formatShortDate(millis)} · 20:30", formatDateTime(millis))
    }
}

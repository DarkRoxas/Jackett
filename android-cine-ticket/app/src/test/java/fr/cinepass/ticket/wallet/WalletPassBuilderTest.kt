package fr.cinepass.ticket.wallet

import fr.cinepass.ticket.data.Ticket
import fr.cinepass.ticket.data.TicketBarcodeFormat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class WalletPassBuilderTest {

    private val config = WalletConfig(
        issuerId = "3388000000012345678",
        classSuffix = "cinepass_event_class",
        issuerName = "CinePass",
        jwtEndpoint = "",
        serviceAccountEmail = "",
        serviceAccountPrivateKey = "",
    )

    private fun ticket(
        screeningAt: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
        format: TicketBarcodeFormat = TicketBarcodeFormat.QR_CODE,
    ) = Ticket(
        id = "3f1c6d0e-1234-4a5b-8c9d-0123456789ab",
        movieTitle = "Dune : Deuxième partie",
        cinemaName = "UGC Ciné Cité Les Halles",
        screeningAt = screeningAt,
        room = "12",
        seats = "H12, H13",
        bookingReference = "REF-42",
        barcodeValue = "TICKET-0001",
        barcodeFormat = format,
    )

    private fun firstObject(payload: JSONObject): JSONObject =
        payload.getJSONArray("eventTicketObjects").getJSONObject(0)

    private fun firstClass(payload: JSONObject): JSONObject =
        payload.getJSONArray("eventTicketClasses").getJSONObject(0)

    @Test
    fun `object id combines issuer id and ticket id without dashes`() {
        assertEquals(
            "3388000000012345678.3f1c6d0e12344a5b8c9d0123456789ab",
            WalletPassBuilder.objectIdFor(config, ticket()),
        )
    }

    @Test
    fun `payload carries the barcode as scanned`() {
        val barcode = firstObject(WalletPassBuilder.buildPayload(config, ticket())).getJSONObject("barcode")

        assertEquals("QR_CODE", barcode.getString("type"))
        assertEquals("TICKET-0001", barcode.getString("value"))
        assertEquals("REF-42", barcode.getString("alternateText"))
    }

    @Test
    fun `data matrix falls back to a barcode type Wallet accepts`() {
        val payload = WalletPassBuilder.buildPayload(config, ticket(format = TicketBarcodeFormat.DATA_MATRIX))

        assertEquals("QR_CODE", firstObject(payload).getJSONObject("barcode").getString("type"))
    }

    @Test
    fun `upcoming screening produces an active pass`() {
        val payload = WalletPassBuilder.buildPayload(config, ticket())

        assertEquals("ACTIVE", firstObject(payload).getString("state"))
    }

    @Test
    fun `past screening produces an expired pass`() {
        val payload = WalletPassBuilder.buildPayload(
            config,
            ticket(screeningAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)),
        )

        assertEquals("EXPIRED", firstObject(payload).getString("state"))
    }

    @Test
    fun `class is inlined so no console pre-creation is required`() {
        val eventClass = firstClass(WalletPassBuilder.buildPayload(config, ticket()))

        assertEquals("3388000000012345678.cinepass_event_class", eventClass.getString("id"))
        assertEquals("CinePass", eventClass.getString("issuerName"))
        assertEquals(
            "Dune : Deuxième partie",
            eventClass.getJSONObject("eventName").getJSONObject("defaultValue").getString("value"),
        )
        // Le début de séance doit être une date locale ISO, sans décalage.
        assertTrue(eventClass.getJSONObject("dateTime").getString("start").contains("T"))
    }

    @Test
    fun `seat and room are exposed to the pass`() {
        val seatInfo = firstObject(WalletPassBuilder.buildPayload(config, ticket())).getJSONObject("seatInfo")

        assertEquals("H12, H13", seatInfo.getJSONObject("seat").getJSONObject("defaultValue").getString("value"))
        assertEquals("12", seatInfo.getJSONObject("section").getJSONObject("defaultValue").getString("value"))
    }

    @Test
    fun `blank optional fields are omitted rather than sent empty`() {
        val bare = ticket().copy(room = null, seats = "", bookingReference = null, notes = null)
        val obj = firstObject(WalletPassBuilder.buildPayload(config, bare))

        assertFalse(obj.has("seatInfo"))
        assertFalse(obj.has("ticketNumber"))
        assertFalse(obj.getJSONObject("barcode").has("alternateText"))
    }
}

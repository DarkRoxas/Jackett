package fr.cinepass.ticket.wallet

import fr.cinepass.ticket.data.Ticket
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Construit la charge utile « Save to Google Wallet » d'un billet de cinéma
 * (ressources `EventTicketClass` / `EventTicketObject`).
 *
 * La classe est envoyée en ligne dans le JWT : le pass fonctionne donc avec un
 * simple identifiant émetteur, sans pré-création de classe dans la console.
 */
object WalletPassBuilder {

    private val isoLocal: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** Identifiant Wallet stable pour un billet : `<issuerId>.<uuid sans tiret>`. */
    fun objectIdFor(config: WalletConfig, ticket: Ticket): String =
        "${config.issuerId}.${ticket.id.replace("-", "")}"

    fun buildPayload(config: WalletConfig, ticket: Ticket): JSONObject {
        val start = Instant.ofEpochMilli(ticket.screeningAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(isoLocal)

        val eventTicketClass = JSONObject().apply {
            put("id", config.classId)
            put("issuerName", config.issuerName)
            put("reviewStatus", "UNDER_REVIEW")
            // displayTitle inclut l'année : deux homonymes restent distinguables
            // dans Wallet, où le titre est le seul repère du pass.
            put("eventName", localized(ticket.displayTitle))
            put("venue", JSONObject().apply {
                put("name", localized(ticket.cinemaName))
                put("address", localized(ticket.cinemaName))
            })
            put("dateTime", JSONObject().put("start", start))
            put("hexBackgroundColor", "#1b1033")
        }

        val eventTicketObject = JSONObject().apply {
            put("id", objectIdFor(config, ticket))
            put("classId", config.classId)
            put("state", if (ticket.isUpcoming()) "ACTIVE" else "EXPIRED")
            // Le champ barcode est optionnel côté Wallet : un billet sans code
            // reste un pass valide, il n'affiche simplement rien à scanner.
            if (ticket.hasBarcode) {
                put("barcode", JSONObject().apply {
                    put("type", ticket.barcodeFormat.walletType)
                    put("value", ticket.barcodeValue)
                    ticket.bookingReference?.takeIf { it.isNotBlank() }?.let {
                        put("alternateText", it)
                    }
                })
            }
            ticket.notes?.takeIf { it.isNotBlank() }?.let {
                put("ticketHolderName", it)
            }
            ticket.bookingReference?.takeIf { it.isNotBlank() }?.let {
                put("ticketNumber", it)
            }

            val seatInfo = JSONObject()
            ticket.seats?.takeIf { it.isNotBlank() }?.let { seatInfo.put("seat", localized(it)) }
            ticket.room?.takeIf { it.isNotBlank() }?.let { seatInfo.put("section", localized(it)) }
            if (seatInfo.length() > 0) put("seatInfo", seatInfo)

            // Doublon des infos clés en modules texte : elles restent visibles
            // même si l'utilisateur ouvre le pass hors ligne.
            put("textModulesData", JSONArray().apply {
                put(textModule("cinema", "Cinéma", ticket.cinemaName))
                ticket.room?.takeIf { it.isNotBlank() }?.let { put(textModule("room", "Salle", it)) }
                ticket.seats?.takeIf { it.isNotBlank() }?.let { put(textModule("seats", "Sièges", it)) }
            })
        }

        return JSONObject().apply {
            put("eventTicketClasses", JSONArray().put(eventTicketClass))
            put("eventTicketObjects", JSONArray().put(eventTicketObject))
        }
    }

    private fun localized(value: String, language: String = "fr"): JSONObject =
        JSONObject().put(
            "defaultValue",
            JSONObject().put("language", language).put("value", value),
        )

    private fun textModule(id: String, header: String, body: String): JSONObject =
        JSONObject().put("id", id).put("header", header).put("body", body)
}

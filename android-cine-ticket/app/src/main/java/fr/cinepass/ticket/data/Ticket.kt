package fr.cinepass.ticket.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Un billet de cinéma archivé localement.
 *
 * [barcodeValue] est la donnée réellement scannée au contrôle : c'est elle qui est
 * réaffichée dans l'app et poussée dans le pass Google Wallet.
 */
@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val movieTitle: String,
    /** Année de sortie : distingue deux films homonymes (remakes, rééditions). */
    val releaseYear: Int? = null,
    val cinemaName: String,
    /** Date et heure de la séance, en millisecondes epoch. */
    val screeningAt: Long,
    val room: String? = null,
    val seats: String? = null,
    val bookingReference: String? = null,
    val barcodeValue: String,
    val barcodeFormat: TicketBarcodeFormat = TicketBarcodeFormat.QR_CODE,
    /** URI locale (fichier copié dans le stockage interne) de l'affiche. */
    val posterUri: String? = null,
    val notes: String? = null,
    /** Identifiant de l'objet Wallet créé pour ce billet, si l'ajout a réussi. */
    val walletObjectId: String? = null,
    val addedToWalletAt: Long? = null,
    /** Archivage manuel : un billet passé est de toute façon considéré comme archivé. */
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val screeningDateTime: LocalDateTime
        get() = Instant.ofEpochMilli(screeningAt).atZone(ZoneId.systemDefault()).toLocalDateTime()

    /** Titre tel qu'affiché partout : « Dune (2021) ». */
    val displayTitle: String
        get() = releaseYear?.let { "$movieTitle ($it)" } ?: movieTitle

    /** Un billet reste « à venir » jusqu'à 4 h après le début de la séance. */
    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean =
        !archived && screeningAt + UPCOMING_GRACE_MILLIS > now

    companion object {
        const val UPCOMING_GRACE_MILLIS = 4 * 60 * 60 * 1000L
    }
}

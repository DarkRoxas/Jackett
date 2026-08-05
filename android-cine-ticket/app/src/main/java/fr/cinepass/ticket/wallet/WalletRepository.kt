package fr.cinepass.ticket.wallet

import android.content.Context
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PayClient
import fr.cinepass.ticket.data.Ticket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface WalletPreparation {
    data class Ready(val jwt: String, val objectId: String) : WalletPreparation
    data class Failure(val reason: String) : WalletPreparation
    data object NotConfigured : WalletPreparation
    data object Unavailable : WalletPreparation
}

class WalletRepository(
    context: Context,
    val config: WalletConfig,
) {
    private val payClient: PayClient = Pay.getClient(context.applicationContext)

    suspend fun isWalletAvailable(): Boolean = suspendCancellableCoroutine { cont ->
        payClient.getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES)
            .addOnSuccessListener { cont.resume(it == PayApiAvailabilityStatus.AVAILABLE) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** Produit le JWT à passer à `PayClient.savePassesJwt`. */
    suspend fun prepare(ticket: Ticket): WalletPreparation {
        if (!config.isConfigured) return WalletPreparation.NotConfigured

        val available = runCatching { isWalletAvailable() }.getOrDefault(false)
        if (!available) return WalletPreparation.Unavailable

        val objectId = WalletPassBuilder.objectIdFor(config, ticket)
        return runCatching {
            val jwt = if (config.canUseBackend) requestJwtFromBackend(ticket) else signLocally(ticket)
            WalletPreparation.Ready(jwt, objectId)
        }.getOrElse { error ->
            WalletPreparation.Failure(error.message ?: error::class.java.simpleName)
        }
    }

    private suspend fun signLocally(ticket: Ticket): String = withContext(Dispatchers.Default) {
        WalletJwt.sign(config, WalletPassBuilder.buildPayload(config, ticket))
    }

    /**
     * Demande le JWT signé au backend. Le corps envoyé décrit le billet ;
     * la réponse attendue est `{"jwt": "..."}` (ou le JWT en texte brut).
     */
    private suspend fun requestJwtFromBackend(ticket: Ticket): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("ticketId", ticket.id)
            put("movieTitle", ticket.movieTitle)
            put("cinemaName", ticket.cinemaName)
            put("screeningAt", ticket.screeningAt)
            put("room", ticket.room.orEmpty())
            put("seats", ticket.seats.orEmpty())
            put("bookingReference", ticket.bookingReference.orEmpty())
            put("barcodeValue", ticket.barcodeValue)
            put("barcodeFormat", ticket.barcodeFormat.walletType)
            put("objectId", WalletPassBuilder.objectIdFor(config, ticket))
        }.toString()

        val connection = (URL(config.jwtEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                error("Le backend Wallet a répondu $code : ${text.take(200)}")
            }
            val trimmed = text.trim()
            if (trimmed.startsWith("{")) JSONObject(trimmed).getString("jwt") else trimmed
        } finally {
            connection.disconnect()
        }
    }

    fun savePasses(jwt: String, activity: android.app.Activity) {
        payClient.savePassesJwt(jwt, activity, SAVE_PASS_REQUEST_CODE)
    }

    companion object {
        const val SAVE_PASS_REQUEST_CODE = 4711
    }
}

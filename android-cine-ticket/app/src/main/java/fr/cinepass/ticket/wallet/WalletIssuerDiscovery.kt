package fr.cinepass.ticket.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WalletIssuer(val id: String, val name: String)

/**
 * Retrouve les comptes émetteurs accessibles à un compte de service.
 *
 * Évite de demander l'identifiant émetteur à la main : il est déjà connu de
 * Google, et le fichier JSON du compte de service suffit à le lire.
 */
class WalletIssuerDiscovery {

    suspend fun listIssuers(key: ServiceAccountKey): List<WalletIssuer> = withContext(Dispatchers.IO) {
        val token = accessToken(key)
        val response = get("$WALLET_API/issuer", token)
        parseIssuers(response)
    }

    /**
     * Échange une assertion signée contre un jeton d'accès
     * ([flux JWT bearer](https://developers.google.com/identity/protocols/oauth2/service-account)).
     */
    private fun accessToken(key: ServiceAccountKey): String {
        val now = System.currentTimeMillis() / 1000
        val assertion = WalletJwt.signClaims(
            claims = JSONObject()
                .put("iss", key.clientEmail)
                .put("scope", SCOPE)
                .put("aud", TOKEN_ENDPOINT)
                .put("iat", now)
                .put("exp", now + 3600),
            privateKeyPem = key.privateKey,
        )

        val body = "grant_type=" + URLEncoder.encode(GRANT_TYPE, "UTF-8") +
            "&assertion=" + URLEncoder.encode(assertion, "UTF-8")

        val connection = (URL(TOKEN_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                error("Google a refusé la clé du compte de service (${describe(code, text)}).")
            }
            return JSONObject(text).getString("access_token")
        } finally {
            connection.disconnect()
        }
    }

    private fun get(url: String, token: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code == 403) {
                error(
                    "Ce compte de service n'a pas accès à l'API Wallet. Vérifiez qu'il est " +
                        "ajouté comme utilisateur dans la console Google Wallet.",
                )
            }
            if (code !in 200..299) error("L'API Wallet a répondu ${describe(code, text)}.")

            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseIssuers(payload: JSONObject): List<WalletIssuer> {
        val resources = payload.optJSONArray("resources") ?: return emptyList()

        return (0 until resources.length()).mapNotNull { index ->
            val item = resources.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("issuerId").takeIf { it.isNotBlank() } ?: return@mapNotNull null

            WalletIssuer(
                id = id,
                name = item.optString("name").takeIf { it.isNotBlank() } ?: "Émetteur $id",
            )
        }
    }

    private fun describe(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()

        return if (message.isNullOrBlank()) "$code" else "$code — $message"
    }

    private companion object {
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        const val SCOPE = "https://www.googleapis.com/auth/wallet_object.issuer"
        const val WALLET_API = "https://walletobjects.googleapis.com/walletobjects/v1"
    }
}

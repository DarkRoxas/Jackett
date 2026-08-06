package fr.cinepass.ticket.wallet

import org.json.JSONObject

/**
 * Contenu utile du fichier JSON téléchargé depuis Google Cloud pour un compte
 * de service. C'est le seul document que l'utilisateur ait à fournir : tout le
 * reste de la configuration Wallet s'en déduit.
 */
data class ServiceAccountKey(
    val clientEmail: String,
    val privateKey: String,
    val projectId: String?,
) {
    companion object {
        /** Renvoie null si le fichier n'est pas une clé de compte de service. */
        fun parse(json: String): ServiceAccountKey? = runCatching {
            val root = JSONObject(json)
            val clientEmail = root.optString("client_email")
            val privateKey = root.optString("private_key")

            if (clientEmail.isBlank() || privateKey.isBlank()) return null

            ServiceAccountKey(
                clientEmail = clientEmail,
                privateKey = privateKey,
                projectId = root.optString("project_id").takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }
}

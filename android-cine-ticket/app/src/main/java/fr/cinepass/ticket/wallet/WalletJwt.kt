package fr.cinepass.ticket.wallet

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Signature RS256 du JWT « savetowallet », sans dépendance externe.
 *
 * À n'utiliser qu'en développement : la clé privée du compte de service ne doit
 * pas se trouver dans l'APK d'une release (voir [WalletConfig]).
 */
object WalletJwt {

    private const val B64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    fun sign(config: WalletConfig, payload: JSONObject): String {
        require(config.canSignLocally) { "Signature locale impossible : configuration incomplète." }

        val header = JSONObject()
            .put("alg", "RS256")
            .put("typ", "JWT")

        val claims = JSONObject()
            .put("iss", config.serviceAccountEmail)
            .put("aud", "google")
            .put("typ", "savetowallet")
            .put("iat", System.currentTimeMillis() / 1000)
            .put("origins", JSONArray())
            .put("payload", payload)

        val signingInput = "${encode(header.toString())}.${encode(claims.toString())}"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(parsePrivateKey(config.serviceAccountPrivateKey))
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        return "$signingInput.${Base64.encodeToString(signature, B64_FLAGS)}"
    }

    private fun encode(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), B64_FLAGS)

    /** Accepte une clé PKCS#8 au format PEM (avec ou sans en-têtes) ou en base64 brut. */
    private fun parsePrivateKey(pem: String) = KeyFactory.getInstance("RSA").generatePrivate(
        PKCS8EncodedKeySpec(
            Base64.decode(
                pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")
                    .filterNot { it.isWhitespace() },
                Base64.DEFAULT,
            ),
        ),
    )
}

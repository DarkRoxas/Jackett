package fr.cinepass.ticket.wallet

import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * Signature RS256 du JWT « savetowallet », sans dépendance externe.
 *
 * On s'appuie sur [java.util.Base64] (disponible depuis l'API 26, soit le
 * `minSdk` du projet) plutôt que sur `android.util.Base64` : le code reste
 * testable sur la JVM.
 *
 * À n'utiliser qu'en développement : la clé privée du compte de service ne doit
 * pas se trouver dans l'APK d'une release (voir [WalletConfig]).
 */
object WalletJwt {

    private val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    fun sign(config: WalletConfig, payload: JSONObject): String =
        sign(
            issuer = config.serviceAccountEmail,
            privateKeyPem = config.serviceAccountPrivateKey,
            payload = payload,
        )

    fun sign(issuer: String, privateKeyPem: String, payload: JSONObject): String {
        require(issuer.isNotBlank() && privateKeyPem.isNotBlank()) {
            "Signature locale impossible : compte de service ou clé privée manquant."
        }

        val header = JSONObject()
            .put("alg", "RS256")
            .put("typ", "JWT")

        val claims = JSONObject()
            .put("iss", issuer)
            .put("aud", "google")
            .put("typ", "savetowallet")
            .put("iat", System.currentTimeMillis() / 1000)
            .put("origins", JSONArray())
            .put("payload", payload)

        val signingInput = "${encode(header.toString())}.${encode(claims.toString())}"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(parsePrivateKey(privateKeyPem))
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        return "$signingInput.${urlEncoder.encodeToString(signature)}"
    }

    private fun encode(value: String): String = urlEncoder.encodeToString(value.toByteArray(Charsets.UTF_8))

    /** Accepte une clé PKCS#8 au format PEM (avec ou sans en-têtes) ou en base64 brut. */
    internal fun parsePrivateKey(pem: String): PrivateKey {
        val body = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            // Une clé passée par local.properties arrive avec des "\n" littéraux.
            .replace("\\n", "")
            .filterNot { it.isWhitespace() }

        return KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)))
    }
}

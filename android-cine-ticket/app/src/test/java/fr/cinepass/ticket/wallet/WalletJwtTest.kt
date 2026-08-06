package fr.cinepass.ticket.wallet

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class WalletJwtTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    private val pem: String = buildString {
        appendLine("-----BEGIN PRIVATE KEY-----")
        appendLine(Base64.getEncoder().encodeToString(keyPair.private.encoded).chunked(64).joinToString("\n"))
        appendLine("-----END PRIVATE KEY-----")
    }

    private val payload = JSONObject().put("eventTicketObjects", org.json.JSONArray())

    private fun decode(segment: String): JSONObject =
        JSONObject(String(Base64.getUrlDecoder().decode(segment), Charsets.UTF_8))

    @Test
    fun `signed token has the three JWT segments`() {
        val jwt = WalletJwt.sign(issuer = "wallet@example.iam.gserviceaccount.com", privateKeyPem = pem, payload = payload)

        assertEquals(3, jwt.split(".").size)
        // Base64url : ni '+', ni '/', ni padding.
        assertTrue(jwt.none { it == '+' || it == '/' || it == '=' })
    }

    @Test
    fun `claims match what the Save to Wallet API expects`() {
        val jwt = WalletJwt.sign("wallet@example.iam.gserviceaccount.com", pem, payload)
        val (headerSegment, claimsSegment) = jwt.split(".").let { it[0] to it[1] }

        assertEquals("RS256", decode(headerSegment).getString("alg"))
        val claims = decode(claimsSegment)
        assertEquals("google", claims.getString("aud"))
        assertEquals("savetowallet", claims.getString("typ"))
        assertEquals("wallet@example.iam.gserviceaccount.com", claims.getString("iss"))
        assertTrue(claims.has("payload"))
    }

    @Test
    fun `signature verifies against the public key`() {
        val jwt = WalletJwt.sign("wallet@example.iam.gserviceaccount.com", pem, payload)
        val segments = jwt.split(".")
        val signingInput = "${segments[0]}.${segments[1]}"

        val verified = Signature.getInstance("SHA256withRSA").run {
            initVerify(keyPair.public)
            update(signingInput.toByteArray(Charsets.UTF_8))
            verify(Base64.getUrlDecoder().decode(segments[2]))
        }

        assertTrue(verified)
    }

    @Test
    fun `a key pasted with escaped newlines is still parsed`() {
        val escaped = pem.replace("\n", "\\n")

        assertArrayEquals(keyPair.private.encoded, WalletJwt.parsePrivateKey(escaped).encoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signing without a service account is rejected`() {
        WalletJwt.sign(issuer = "", privateKeyPem = pem, payload = payload)
    }
}

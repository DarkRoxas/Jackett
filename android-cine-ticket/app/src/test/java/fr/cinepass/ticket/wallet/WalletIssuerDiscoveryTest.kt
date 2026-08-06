package fr.cinepass.ticket.wallet

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletIssuerDiscoveryTest {

    private val discovery = WalletIssuerDiscovery()

    @Test
    fun `the issuer id is read from the wallet api response`() {
        val payload = JSONObject(
            """
            {"resources":[
              {"issuerId":"3388000000012345678","name":"Cinéma Le Lascaux"}
            ]}
            """.trimIndent(),
        )

        val issuers = discovery.parseIssuers(payload)

        assertEquals(1, issuers.size)
        assertEquals("3388000000012345678", issuers.single().id)
        assertEquals("Cinéma Le Lascaux", issuers.single().name)
    }

    @Test
    fun `several issuers are all offered`() {
        val payload = JSONObject(
            """
            {"resources":[
              {"issuerId":"1","name":"Premier"},
              {"issuerId":"2","name":"Second"}
            ]}
            """.trimIndent(),
        )

        assertEquals(listOf("Premier", "Second"), discovery.parseIssuers(payload).map { it.name })
    }

    @Test
    fun `an unnamed issuer falls back to a readable label`() {
        val payload = JSONObject("""{"resources":[{"issuerId":"3388000000012345678"}]}""")

        assertEquals("Émetteur 3388000000012345678", discovery.parseIssuers(payload).single().name)
    }

    @Test
    fun `an account without any issuer yields an empty list`() {
        assertTrue(discovery.parseIssuers(JSONObject("{}")).isEmpty())
        assertTrue(discovery.parseIssuers(JSONObject("""{"resources":[]}""")).isEmpty())
    }

    @Test
    fun `entries without an issuer id are skipped`() {
        val payload = JSONObject(
            """{"resources":[{"name":"Sans identifiant"},{"issuerId":"7","name":"Valide"}]}""",
        )

        assertEquals(listOf("7"), discovery.parseIssuers(payload).map { it.id })
    }
}

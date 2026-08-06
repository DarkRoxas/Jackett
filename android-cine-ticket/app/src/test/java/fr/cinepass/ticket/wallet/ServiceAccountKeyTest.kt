package fr.cinepass.ticket.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceAccountKeyTest {

    private val validJson = """
        {
          "type": "service_account",
          "project_id": "cinepass-demo",
          "private_key_id": "abc123",
          "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADAN\n-----END PRIVATE KEY-----\n",
          "client_email": "wallet@cinepass-demo.iam.gserviceaccount.com",
          "client_id": "1234567890"
        }
    """.trimIndent()

    @Test
    fun `the downloaded key file gives everything needed`() {
        val key = ServiceAccountKey.parse(validJson)!!

        assertEquals("wallet@cinepass-demo.iam.gserviceaccount.com", key.clientEmail)
        assertEquals("cinepass-demo", key.projectId)
        // Les retours à la ligne du PEM doivent survivre au décodage JSON.
        assertEquals(true, key.privateKey.contains("\n"))
        assertEquals(true, key.privateKey.startsWith("-----BEGIN PRIVATE KEY-----"))
    }

    @Test
    fun `a file missing the private key is rejected`() {
        val json = """{"type":"service_account","client_email":"a@b.iam.gserviceaccount.com"}"""

        assertNull(ServiceAccountKey.parse(json))
    }

    @Test
    fun `a file missing the account is rejected`() {
        val json = """{"private_key":"-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----"}"""

        assertNull(ServiceAccountKey.parse(json))
    }

    @Test
    fun `a file that is not json is rejected rather than crashing`() {
        assertNull(ServiceAccountKey.parse("ceci n'est pas du JSON"))
        assertNull(ServiceAccountKey.parse(""))
    }

    @Test
    fun `an unrelated json file is rejected`() {
        assertNull(ServiceAccountKey.parse("""{"hello":"world"}"""))
    }

    @Test
    fun `a key without project id stays usable`() {
        val json = """
            {
              "private_key": "-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----",
              "client_email": "wallet@demo.iam.gserviceaccount.com"
            }
        """.trimIndent()

        assertNull(ServiceAccountKey.parse(json)!!.projectId)
    }
}

package fr.cinepass.ticket.wallet

import fr.cinepass.ticket.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletConfigTest {

    private fun config(
        issuerId: String = "3388000000012345678",
        jwtEndpoint: String = "",
        serviceAccountEmail: String = "",
        serviceAccountPrivateKey: String = "",
    ) = WalletConfig(
        issuerId = issuerId,
        classSuffix = "ma_classe",
        issuerName = "Mon cinéma",
        jwtEndpoint = jwtEndpoint,
        serviceAccountEmail = serviceAccountEmail,
        serviceAccountPrivateKey = serviceAccountPrivateKey,
    )

    // Les champs renseignés dans l'app ne dépendent pas de BuildConfig : ce sont
    // les seules valeurs que ce test peut affirmer sans connaître local.properties.
    @Test
    fun `settings entered in the app are used as is`() {
        val settings = AppSettings(
            walletIssuerId = "3388000000012345678",
            walletIssuerName = "Mon cinéma",
            walletClassSuffix = "ma_classe",
            walletJwtEndpoint = "https://exemple.test/wallet/jwt",
            walletServiceAccountEmail = "wallet@exemple.iam.gserviceaccount.com",
            walletServiceAccountKey = "-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----",
        )

        val resolved = WalletConfig.from(settings)

        assertEquals("3388000000012345678", resolved.issuerId)
        assertEquals("Mon cinéma", resolved.issuerName)
        assertEquals("3388000000012345678.ma_classe", resolved.classId)
        assertEquals("https://exemple.test/wallet/jwt", resolved.jwtEndpoint)
        assertEquals("wallet@exemple.iam.gserviceaccount.com", resolved.serviceAccountEmail)
    }

    @Test
    fun `a backend endpoint is enough to save a pass`() {
        val backend = config(jwtEndpoint = "https://exemple.test/wallet/jwt")

        assertTrue(backend.canUseBackend)
        assertFalse(backend.canSignLocally)
        assertTrue(backend.isConfigured)
    }

    @Test
    fun `on-device signing needs both the account and its key`() {
        val emailOnly = config(serviceAccountEmail = "wallet@exemple.iam.gserviceaccount.com")
        assertFalse(emailOnly.canSignLocally)
        assertFalse(emailOnly.isConfigured)

        val complete = config(
            serviceAccountEmail = "wallet@exemple.iam.gserviceaccount.com",
            serviceAccountPrivateKey = "-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----",
        )
        assertTrue(complete.canSignLocally)
        assertTrue(complete.isConfigured)
    }

    @Test
    fun `nothing works without an issuer id`() {
        val orphan = config(
            issuerId = "",
            jwtEndpoint = "https://exemple.test/wallet/jwt",
            serviceAccountEmail = "wallet@exemple.iam.gserviceaccount.com",
            serviceAccountPrivateKey = "-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----",
        )

        assertFalse(orphan.canUseBackend)
        assertFalse(orphan.canSignLocally)
        assertFalse(orphan.isConfigured)
    }
}

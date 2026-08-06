package fr.cinepass.ticket.wallet

import fr.cinepass.ticket.BuildConfig
import fr.cinepass.ticket.data.AppSettings

/**
 * Paramètres de l'intégration Google Wallet.
 *
 * Deux façons d'obtenir le JWT « Save to Wallet » :
 *
 * 1. [jwtEndpoint] — **mode recommandé** : un backend détient la clé du compte de
 *    service et renvoie le JWT signé. Rien de secret ne se retrouve dans l'APK.
 * 2. [serviceAccountEmail] + [serviceAccountPrivateKey] — signature sur l'appareil,
 *    **réservée au dépannage** : une clé privée posée sur un téléphone est
 *    récupérable dès que l'appareil est compromis.
 *
 * Les valeurs viennent des réglages de l'application ; celles laissées vides
 * retombent sur `local.properties` via `BuildConfig`.
 */
data class WalletConfig(
    val issuerId: String,
    val classSuffix: String,
    val issuerName: String,
    val jwtEndpoint: String,
    val serviceAccountEmail: String,
    val serviceAccountPrivateKey: String,
) {
    val classId: String get() = "$issuerId.$classSuffix"

    val canSignLocally: Boolean
        get() = issuerId.isNotBlank() &&
            serviceAccountEmail.isNotBlank() &&
            serviceAccountPrivateKey.isNotBlank()

    val canUseBackend: Boolean
        get() = issuerId.isNotBlank() && jwtEndpoint.isNotBlank()

    val isConfigured: Boolean
        get() = canUseBackend || canSignLocally

    companion object {
        const val DEFAULT_CLASS_SUFFIX = "cinepass_event_class"
        const val DEFAULT_ISSUER_NAME = "CinePass"

        /** Réglages de l'app, complétés par les valeurs de compilation. */
        fun from(settings: AppSettings): WalletConfig = WalletConfig(
            issuerId = settings.walletIssuerId.ifBlank { BuildConfig.WALLET_ISSUER_ID },
            classSuffix = settings.walletClassSuffix
                .ifBlank { BuildConfig.WALLET_CLASS_SUFFIX }
                .ifBlank { DEFAULT_CLASS_SUFFIX },
            issuerName = settings.walletIssuerName
                .ifBlank { BuildConfig.WALLET_ISSUER_NAME }
                .ifBlank { DEFAULT_ISSUER_NAME },
            jwtEndpoint = settings.walletJwtEndpoint.ifBlank { BuildConfig.WALLET_JWT_ENDPOINT },
            serviceAccountEmail = settings.walletServiceAccountEmail
                .ifBlank { BuildConfig.WALLET_SA_EMAIL },
            serviceAccountPrivateKey = settings.walletServiceAccountKey
                .ifBlank { BuildConfig.WALLET_SA_PRIVATE_KEY },
        )
    }
}

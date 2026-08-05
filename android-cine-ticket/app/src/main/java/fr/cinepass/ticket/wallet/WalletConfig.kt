package fr.cinepass.ticket.wallet

import fr.cinepass.ticket.BuildConfig

/**
 * Paramètres de l'intégration Google Wallet.
 *
 * Deux façons d'obtenir le JWT « Save to Wallet » :
 *
 * 1. [jwtEndpoint] — **mode recommandé** : un backend détient la clé du compte de
 *    service et renvoie le JWT signé. Rien de secret ne se retrouve dans l'APK.
 * 2. [serviceAccountEmail] + [serviceAccountPrivateKey] — signature sur l'appareil,
 *    **uniquement pour le développement** (build debug). Une clé privée embarquée
 *    dans un APK est extractible : ne jamais publier une release ainsi configurée.
 *
 * Les valeurs proviennent de `local.properties` (non versionné) ou de variables
 * d'environnement, via `buildConfigField` — voir `app/build.gradle.kts`.
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
        get() = jwtEndpoint.isNotBlank()

    val isConfigured: Boolean
        get() = canUseBackend || canSignLocally

    companion object {
        fun fromBuildConfig(): WalletConfig = WalletConfig(
            issuerId = BuildConfig.WALLET_ISSUER_ID,
            classSuffix = BuildConfig.WALLET_CLASS_SUFFIX,
            issuerName = BuildConfig.WALLET_ISSUER_NAME,
            jwtEndpoint = BuildConfig.WALLET_JWT_ENDPOINT,
            serviceAccountEmail = BuildConfig.WALLET_SA_EMAIL,
            serviceAccountPrivateKey = BuildConfig.WALLET_SA_PRIVATE_KEY,
        )
    }
}

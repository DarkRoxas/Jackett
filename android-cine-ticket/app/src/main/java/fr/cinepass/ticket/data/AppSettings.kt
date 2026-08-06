package fr.cinepass.ticket.data

/**
 * Réglages saisis dans l'application et conservés sur l'appareil.
 *
 * Chaque champ laissé vide retombe sur la valeur de compilation
 * (`local.properties` → `BuildConfig`), ce qui permet aux deux modes de
 * configuration de coexister : réglages dans l'app pour l'utilisateur,
 * `local.properties` pour le développement.
 */
data class AppSettings(
    val tmdbApiKey: String = "",
    val walletIssuerId: String = "",
    val walletIssuerName: String = "",
    val walletClassSuffix: String = "",
    val walletJwtEndpoint: String = "",
    val walletServiceAccountEmail: String = "",
    val walletServiceAccountKey: String = "",
    /** Passe à true dès que l'écran de bienvenue a été validé ou ignoré. */
    val onboardingCompleted: Boolean = false,
)

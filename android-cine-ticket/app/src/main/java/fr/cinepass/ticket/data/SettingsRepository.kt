package fr.cinepass.ticket.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "cinepass_settings")

/**
 * Persistance des réglages via DataStore, dans le stockage privé de l'app.
 *
 * La clé de compte de service Wallet y est stockée en clair : sur un appareil
 * non rooté elle reste inaccessible aux autres applications, mais c'est une
 * raison de plus de préférer le mode « backend » en usage réel.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            tmdbApiKey = prefs[TMDB_API_KEY].orEmpty(),
            giphyApiKey = prefs[GIPHY_API_KEY].orEmpty(),
            walletIssuerId = prefs[WALLET_ISSUER_ID].orEmpty(),
            walletIssuerName = prefs[WALLET_ISSUER_NAME].orEmpty(),
            walletClassSuffix = prefs[WALLET_CLASS_SUFFIX].orEmpty(),
            walletJwtEndpoint = prefs[WALLET_JWT_ENDPOINT].orEmpty(),
            walletServiceAccountEmail = prefs[WALLET_SA_EMAIL].orEmpty(),
            walletServiceAccountKey = prefs[WALLET_SA_KEY].orEmpty(),
            onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[TMDB_API_KEY] = settings.tmdbApiKey.trim()
            prefs[GIPHY_API_KEY] = settings.giphyApiKey.trim()
            prefs[WALLET_ISSUER_ID] = settings.walletIssuerId.trim()
            prefs[WALLET_ISSUER_NAME] = settings.walletIssuerName.trim()
            prefs[WALLET_CLASS_SUFFIX] = settings.walletClassSuffix.trim()
            prefs[WALLET_JWT_ENDPOINT] = settings.walletJwtEndpoint.trim()
            prefs[WALLET_SA_EMAIL] = settings.walletServiceAccountEmail.trim()
            prefs[WALLET_SA_KEY] = settings.walletServiceAccountKey.trim()
            prefs[ONBOARDING_COMPLETED] = settings.onboardingCompleted
        }
    }

    suspend fun completeOnboarding() {
        context.settingsDataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    private companion object {
        val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val GIPHY_API_KEY = stringPreferencesKey("giphy_api_key")
        val WALLET_ISSUER_ID = stringPreferencesKey("wallet_issuer_id")
        val WALLET_ISSUER_NAME = stringPreferencesKey("wallet_issuer_name")
        val WALLET_CLASS_SUFFIX = stringPreferencesKey("wallet_class_suffix")
        val WALLET_JWT_ENDPOINT = stringPreferencesKey("wallet_jwt_endpoint")
        val WALLET_SA_EMAIL = stringPreferencesKey("wallet_sa_email")
        val WALLET_SA_KEY = stringPreferencesKey("wallet_sa_key")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}

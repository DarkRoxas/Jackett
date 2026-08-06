package fr.cinepass.ticket.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.cinepass.ticket.data.AppSettings
import fr.cinepass.ticket.data.SettingsRepository
import fr.cinepass.ticket.wallet.ServiceAccountKey
import fr.cinepass.ticket.wallet.WalletIssuer
import fr.cinepass.ticket.wallet.WalletIssuerDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val loading: Boolean = true,
    val saved: Boolean = false,
    /** Import du fichier JSON en cours (lecture + interrogation de Google). */
    val importing: Boolean = false,
    val importError: String? = null,
    val importSuccess: String? = null,
    /** Renseigné quand le compte de service donne accès à plusieurs émetteurs. */
    val issuerChoices: List<WalletIssuer> = emptyList(),
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val issuerDiscovery: WalletIssuerDiscovery,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = SettingsUiState(settings = repository.current(), loading = false)
        }
    }

    fun onFieldChange(transform: (AppSettings) -> AppSettings) {
        _state.update { it.copy(settings = transform(it.settings), saved = false) }
    }

    /**
     * Chemin simplifié : le fichier JSON du compte de service suffit. On en tire
     * le compte et sa clé, puis on demande à Google les émetteurs auxquels ce
     * compte a accès — l'identifiant émetteur n'a donc pas à être saisi.
     */
    fun importServiceAccount(json: String) {
        val key = ServiceAccountKey.parse(json)
        if (key == null) {
            _state.update {
                it.copy(
                    importError = "Ce fichier n'est pas une clé de compte de service Google " +
                        "(client_email et private_key attendus).",
                    importSuccess = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                settings = it.settings.copy(
                    walletServiceAccountEmail = key.clientEmail,
                    walletServiceAccountKey = key.privateKey,
                ),
                importing = true,
                importError = null,
                importSuccess = null,
                issuerChoices = emptyList(),
            )
        }

        viewModelScope.launch {
            val outcome = runCatching { issuerDiscovery.listIssuers(key) }

            outcome.fold(
                onSuccess = { issuers ->
                    when {
                        issuers.isEmpty() -> _state.update {
                            it.copy(
                                importing = false,
                                importError = "Compte de service reconnu, mais aucun compte " +
                                    "émetteur ne lui est rattaché. Créez-en un dans la console " +
                                    "Google Wallet, puis recommencez.",
                            )
                        }

                        issuers.size == 1 -> applyIssuer(issuers.single(), persist = true)

                        // Plusieurs émetteurs : on laisse l'utilisateur trancher.
                        else -> _state.update {
                            it.copy(importing = false, issuerChoices = issuers)
                        }
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            importing = false,
                            importError = error.message ?: "Vérification impossible.",
                        )
                    }
                },
            )
        }
    }

    fun onIssuerChosen(issuer: WalletIssuer) = applyIssuer(issuer, persist = true)

    private fun applyIssuer(issuer: WalletIssuer, persist: Boolean) {
        _state.update {
            it.copy(
                settings = it.settings.copy(
                    walletIssuerId = issuer.id,
                    walletIssuerName = it.settings.walletIssuerName.ifBlank { issuer.name },
                ),
                importing = false,
                issuerChoices = emptyList(),
                importSuccess = "Compte Wallet relié : ${issuer.name}",
            )
        }
        if (persist) save()
    }

    /** [markOnboardingDone] est vrai depuis l'écran de bienvenue uniquement. */
    fun save(markOnboardingDone: Boolean = false, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val settings = _state.value.settings
            repository.save(
                if (markOnboardingDone) settings.copy(onboardingCompleted = true) else settings,
            )
            _state.update { it.copy(saved = true) }
            onSaved()
        }
    }

    fun skipOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.completeOnboarding()
            onDone()
        }
    }

    fun clearWallet() {
        _state.update {
            it.copy(
                settings = it.settings.copy(
                    walletIssuerId = "",
                    walletIssuerName = "",
                    walletServiceAccountEmail = "",
                    walletServiceAccountKey = "",
                ),
                importSuccess = null,
                importError = null,
                issuerChoices = emptyList(),
            )
        }
        save()
    }
}

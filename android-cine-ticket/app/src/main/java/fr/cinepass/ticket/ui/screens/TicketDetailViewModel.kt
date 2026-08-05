package fr.cinepass.ticket.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.cinepass.ticket.data.Ticket
import fr.cinepass.ticket.data.TicketRepository
import fr.cinepass.ticket.wallet.WalletPreparation
import fr.cinepass.ticket.wallet.WalletRepository
import fr.cinepass.ticket.wallet.WalletResultBus
import fr.cinepass.ticket.wallet.WalletSaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Message éphémère à afficher dans une snackbar. */
data class UiMessage(val text: String, val id: Long = System.nanoTime())

/** Demande d'ouverture du flux Google Wallet, consommée par l'écran. */
data class WalletLaunchRequest(val jwt: String, val objectId: String, val id: Long = System.nanoTime())

class TicketDetailViewModel(
    private val ticketId: String,
    private val repository: TicketRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    val ticket: StateFlow<Ticket?> = repository.observeById(ticketId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _brightnessBoost = MutableStateFlow(true)
    val brightnessBoost: StateFlow<Boolean> = _brightnessBoost.asStateFlow()

    private val _walletBusy = MutableStateFlow(false)
    val walletBusy: StateFlow<Boolean> = _walletBusy.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val _walletLaunch = MutableStateFlow<WalletLaunchRequest?>(null)
    val walletLaunch: StateFlow<WalletLaunchRequest?> = _walletLaunch.asStateFlow()

    /** Objet Wallet dont on attend la confirmation d'enregistrement. */
    private var pendingObjectId: String? = null

    init {
        viewModelScope.launch {
            WalletResultBus.results.collect { result ->
                _walletBusy.value = false
                when (result) {
                    WalletSaveResult.Saved -> {
                        pendingObjectId?.let { repository.markSavedToWallet(ticketId, it) }
                        pendingObjectId = null
                        _message.value = UiMessage("Billet ajouté à Google Wallet.")
                    }

                    WalletSaveResult.Cancelled -> {
                        pendingObjectId = null
                        _message.value = UiMessage("Ajout à Google Wallet annulé.")
                    }

                    is WalletSaveResult.Error -> {
                        pendingObjectId = null
                        _message.value = UiMessage(
                            "Échec de l'ajout à Google Wallet : ${result.message ?: "erreur inconnue"}",
                        )
                    }
                }
            }
        }
    }

    fun setBrightnessBoost(enabled: Boolean) {
        _brightnessBoost.value = enabled
    }

    fun addToWallet() {
        val current = ticket.value ?: return
        if (_walletBusy.value) return

        _walletBusy.value = true
        viewModelScope.launch {
            when (val preparation = walletRepository.prepare(current)) {
                is WalletPreparation.Ready -> {
                    pendingObjectId = preparation.objectId
                    _walletLaunch.value = WalletLaunchRequest(preparation.jwt, preparation.objectId)
                }

                WalletPreparation.NotConfigured -> {
                    _walletBusy.value = false
                    _message.value = UiMessage(
                        "Google Wallet n'est pas configuré : renseignez WALLET_ISSUER_ID et la " +
                            "source du JWT dans local.properties.",
                    )
                }

                WalletPreparation.Unavailable -> {
                    _walletBusy.value = false
                    _message.value = UiMessage("Google Wallet n'est pas disponible sur cet appareil.")
                }

                is WalletPreparation.Failure -> {
                    _walletBusy.value = false
                    _message.value = UiMessage("Préparation du pass impossible : ${preparation.reason}")
                }
            }
        }
    }

    fun onWalletLaunchConsumed() {
        _walletLaunch.value = null
    }

    fun onMessageShown(id: Long) {
        _message.update { current -> if (current?.id == id) null else current }
    }

    fun toggleArchived() {
        val current = ticket.value ?: return
        viewModelScope.launch { repository.setArchived(current.id, !current.archived) }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = ticket.value ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDeleted()
        }
    }
}

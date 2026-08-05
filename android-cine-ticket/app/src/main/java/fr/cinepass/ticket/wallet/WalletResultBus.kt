package fr.cinepass.ticket.wallet

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface WalletSaveResult {
    data object Saved : WalletSaveResult
    data object Cancelled : WalletSaveResult
    data class Error(val message: String?) : WalletSaveResult
}

/**
 * `PayClient.savePassesJwt` répond via `onActivityResult` : ce bus relaie le
 * résultat depuis l'activité vers le ViewModel de l'écran de détail.
 */
object WalletResultBus {
    private val _results = MutableSharedFlow<WalletSaveResult>(extraBufferCapacity = 4)
    val results: SharedFlow<WalletSaveResult> = _results

    fun publish(result: WalletSaveResult) {
        _results.tryEmit(result)
    }
}

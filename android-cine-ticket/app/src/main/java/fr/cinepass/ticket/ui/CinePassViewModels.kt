package fr.cinepass.ticket.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.cinepass.ticket.AppContainer
import fr.cinepass.ticket.CinePassApplication
import fr.cinepass.ticket.ui.screens.SettingsViewModel
import fr.cinepass.ticket.ui.screens.TicketDetailViewModel
import fr.cinepass.ticket.ui.screens.TicketEditViewModel
import fr.cinepass.ticket.ui.screens.TicketListViewModel

private val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CinePassApplication)
        .container

object CinePassViewModelFactories {

    val list = viewModelFactory {
        initializer { TicketListViewModel(this.container.ticketRepository) }
    }

    val settings = viewModelFactory {
        initializer {
            SettingsViewModel(
                repository = this.container.settingsRepository,
                issuerDiscovery = this.container.walletIssuerDiscovery,
            )
        }
    }

    fun detail(ticketId: String) = viewModelFactory {
        initializer {
            TicketDetailViewModel(
                ticketId = ticketId,
                repository = this.container.ticketRepository,
                walletRepository = this.container.walletRepository,
            )
        }
    }

    fun edit(ticketId: String?) = viewModelFactory {
        initializer {
            TicketEditViewModel(
                ticketId = ticketId,
                repository = this.container.ticketRepository,
                movieSearchRepository = this.container.movieSearchRepository,
                animatedPosterRepository = this.container.animatedPosterRepository,
            )
        }
    }
}

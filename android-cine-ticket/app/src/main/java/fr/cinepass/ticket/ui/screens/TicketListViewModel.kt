package fr.cinepass.ticket.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.cinepass.ticket.data.Ticket
import fr.cinepass.ticket.data.TicketRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TicketListUiState(
    val upcoming: List<Ticket> = emptyList(),
    val archived: List<Ticket> = emptyList(),
)

class TicketListViewModel(repository: TicketRepository) : ViewModel() {

    val state: StateFlow<TicketListUiState> = repository.observeAll()
        .map { tickets ->
            val now = System.currentTimeMillis()
            TicketListUiState(
                // Les séances à venir sont listées de la plus proche à la plus lointaine…
                upcoming = tickets.filter { it.isUpcoming(now) }.sortedBy { it.screeningAt },
                // …et les archives de la plus récente à la plus ancienne.
                archived = tickets.filterNot { it.isUpcoming(now) }.sortedByDescending { it.screeningAt },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TicketListUiState(),
        )
}

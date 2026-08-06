package fr.cinepass.ticket.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.cinepass.ticket.data.MovieSearchRepository
import fr.cinepass.ticket.data.MovieSearchResult
import fr.cinepass.ticket.data.Ticket
import fr.cinepass.ticket.data.TicketBarcodeFormat
import fr.cinepass.ticket.data.TicketRepository
import fr.cinepass.ticket.util.toEpochMillis
import fr.cinepass.ticket.util.toLocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class TicketFormState(
    val movieTitle: String = "",
    val releaseYear: String = "",
    val cinemaName: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.of(20, 0),
    val room: String = "",
    val seats: String = "",
    val bookingReference: String = "",
    val barcodeValue: String = "",
    val barcodeFormat: TicketBarcodeFormat = TicketBarcodeFormat.QR_CODE,
    val posterUri: String? = null,
    val notes: String = "",
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val posterDownloading: Boolean = false,
    val movieError: String? = null,
    val cinemaError: String? = null,
    val barcodeError: String? = null,
)

data class MovieSearchState(
    val visible: Boolean = false,
    val query: String = "",
    val searching: Boolean = false,
    val results: List<MovieSearchResult> = emptyList(),
    val error: String? = null,
    val available: Boolean = false,
)

class TicketEditViewModel(
    private val ticketId: String?,
    private val repository: TicketRepository,
    private val movieSearchRepository: MovieSearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TicketFormState(isNew = ticketId == null))
    val state: StateFlow<TicketFormState> = _state.asStateFlow()

    private val _search = MutableStateFlow(MovieSearchState(available = movieSearchRepository.isConfigured))
    val search: StateFlow<MovieSearchState> = _search.asStateFlow()

    /** Billet existant en cours d'édition, conservé pour préserver les champs non exposés. */
    private var original: Ticket? = null

    /** Recherche en cours : annulée à chaque frappe pour ne garder que la dernière. */
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = ticketId?.let { repository.findById(it) }
            original = existing
            _state.value = if (existing == null) {
                TicketFormState(loading = false, isNew = true)
            } else {
                val dateTime: LocalDateTime = existing.screeningAt.toLocalDateTime()
                TicketFormState(
                    movieTitle = existing.movieTitle,
                    releaseYear = existing.releaseYear?.toString().orEmpty(),
                    cinemaName = existing.cinemaName,
                    date = dateTime.toLocalDate(),
                    time = dateTime.toLocalTime(),
                    room = existing.room.orEmpty(),
                    seats = existing.seats.orEmpty(),
                    bookingReference = existing.bookingReference.orEmpty(),
                    barcodeValue = existing.barcodeValue,
                    barcodeFormat = existing.barcodeFormat,
                    posterUri = existing.posterUri,
                    notes = existing.notes.orEmpty(),
                    loading = false,
                    isNew = false,
                )
            }
        }
    }

    fun onMovieTitleChange(value: String) = _state.update { it.copy(movieTitle = value, movieError = null) }
    fun onCinemaChange(value: String) = _state.update { it.copy(cinemaName = value, cinemaError = null) }
    fun onRoomChange(value: String) = _state.update { it.copy(room = value) }
    fun onSeatsChange(value: String) = _state.update { it.copy(seats = value) }
    fun onReferenceChange(value: String) = _state.update { it.copy(bookingReference = value) }
    fun onBarcodeChange(value: String) = _state.update { it.copy(barcodeValue = value, barcodeError = null) }
    fun onFormatChange(value: TicketBarcodeFormat) = _state.update { it.copy(barcodeFormat = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }
    fun onDateChange(value: LocalDate) = _state.update { it.copy(date = value) }
    fun onTimeChange(value: LocalTime) = _state.update { it.copy(time = value) }

    fun onReleaseYearChange(value: String) {
        // 4 chiffres au plus : le champ n'accepte qu'une année.
        val digits = value.filter(Char::isDigit).take(4)
        _state.update { it.copy(releaseYear = digits) }
    }

    // --- Recherche TMDB ---

    fun openMovieSearch() {
        _search.update {
            it.copy(
                visible = true,
                query = _state.value.movieTitle,
                available = movieSearchRepository.isConfigured,
                error = null,
            )
        }
        if (_search.value.query.isNotBlank()) onSearchQueryChange(_search.value.query)
    }

    fun closeMovieSearch() {
        searchJob?.cancel()
        _search.update { it.copy(visible = false, searching = false) }
    }

    fun onSearchQueryChange(query: String) {
        _search.update { it.copy(query = query, error = null) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _search.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        if (!movieSearchRepository.isConfigured) return

        searchJob = viewModelScope.launch {
            // Anti-rebond : on laisse la frappe se terminer avant d'appeler TMDB.
            delay(350)
            _search.update { it.copy(searching = true) }
            val outcome = runCatching { movieSearchRepository.search(query) }
            _search.update { current ->
                outcome.fold(
                    onSuccess = { current.copy(searching = false, results = it, error = null) },
                    onFailure = {
                        current.copy(
                            searching = false,
                            results = emptyList(),
                            error = it.message ?: "Recherche impossible.",
                        )
                    },
                )
            }
        }
    }

    fun onMovieSelected(movie: MovieSearchResult) {
        searchJob?.cancel()
        _search.update { it.copy(visible = false, searching = false) }
        _state.update {
            it.copy(
                movieTitle = movie.title,
                releaseYear = movie.releaseYear?.toString().orEmpty(),
                movieError = null,
                posterDownloading = movie.posterUrl != null,
            )
        }

        val posterUrl = movie.posterUrl ?: return
        viewModelScope.launch {
            val imported = repository.importPosterFromUrl(posterUrl)
            _state.update {
                if (imported == null) it.copy(posterDownloading = false)
                else it.copy(posterUri = imported, posterDownloading = false)
            }
        }
    }

    // --- Affiche ---

    fun onPosterPicked(uri: Uri) {
        viewModelScope.launch {
            val imported = repository.importPoster(uri) ?: return@launch
            _state.update { it.copy(posterUri = imported) }
        }
    }

    fun onPosterCleared() {
        _state.update { it.copy(posterUri = null) }
    }

    fun save(onSaved: (String) -> Unit) {
        val form = _state.value
        val movieError = "Le titre du film est obligatoire.".takeIf { form.movieTitle.isBlank() }
        val cinemaError = "Le nom du cinéma est obligatoire.".takeIf { form.cinemaName.isBlank() }
        val barcodeError = "Le contenu du code-barres est obligatoire.".takeIf { form.barcodeValue.isBlank() }

        if (movieError != null || cinemaError != null || barcodeError != null) {
            _state.update {
                it.copy(movieError = movieError, cinemaError = cinemaError, barcodeError = barcodeError)
            }
            return
        }

        val base = original
        val ticket = (base ?: Ticket(movieTitle = "", cinemaName = "", screeningAt = 0, barcodeValue = "")).copy(
            movieTitle = form.movieTitle.trim(),
            releaseYear = form.releaseYear.toIntOrNull(),
            cinemaName = form.cinemaName.trim(),
            screeningAt = LocalDateTime.of(form.date, form.time).toEpochMillis(),
            room = form.room.trim().ifBlank { null },
            seats = form.seats.trim().ifBlank { null },
            bookingReference = form.bookingReference.trim().ifBlank { null },
            barcodeValue = form.barcodeValue.trim(),
            barcodeFormat = form.barcodeFormat,
            posterUri = form.posterUri,
            notes = form.notes.trim().ifBlank { null },
        )

        viewModelScope.launch {
            // L'affiche remplacée n'est plus référencée : on libère le fichier.
            val previousPoster = base?.posterUri
            if (previousPoster != null && previousPoster != ticket.posterUri) {
                repository.deletePoster(previousPoster)
            }
            repository.save(ticket)
            onSaved(ticket.id)
        }
    }
}

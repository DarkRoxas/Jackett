package fr.cinepass.ticket.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.cinepass.ticket.data.AppSettings
import fr.cinepass.ticket.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val loading: Boolean = true,
    val saved: Boolean = false,
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

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
}

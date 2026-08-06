package fr.cinepass.ticket.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.cinepass.ticket.data.AppSettings
import fr.cinepass.ticket.data.SettingsRepository
import fr.cinepass.ticket.ui.screens.OnboardingScreen
import fr.cinepass.ticket.wallet.WalletRepository

/**
 * Aiguillage de premier niveau : tant que l'écran de bienvenue n'a pas été
 * validé (ou ignoré), il passe avant le reste de l'application.
 */
@Composable
fun CinePassApp(
    settingsRepository: SettingsRepository,
    walletRepository: WalletRepository,
) {
    val settings: AppSettings? by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = null,
    )

    when (val current = settings) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        // onDone n'a rien à faire : le flux des réglages émet la nouvelle
        // valeur et cet aiguillage bascule tout seul.
        else -> if (!current.onboardingCompleted) {
            OnboardingScreen(onDone = {})
        } else {
            CinePassNavHost(walletRepository = walletRepository)
        }
    }
}

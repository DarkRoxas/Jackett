package fr.cinepass.ticket.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.cinepass.ticket.ui.CinePassViewModelFactories

/**
 * Premier lancement : on explique les deux réglages facultatifs et on propose
 * de les saisir tout de suite. Tout est utilisable sans, d'où le « Plus tard ».
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = CinePassViewModelFactories.settings),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.LocalActivity,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Bienvenue dans CinéPass",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Vos billets sont enregistrés sur cet appareil et s'affichent en " +
                    "luminosité maximale au moment du contrôle. Deux réglages facultatifs " +
                    "ajoutent la recherche de films et l'export vers Google Wallet — vous " +
                    "pourrez les renseigner plus tard depuis les réglages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        SettingsForm(settings = state.settings, onChange = viewModel::onFieldChange)

        Button(
            onClick = { viewModel.save(markOnboardingDone = true, onSaved = onDone) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Enregistrer et commencer")
        }

        TextButton(
            onClick = { viewModel.skipOnboarding(onDone) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Plus tard")
        }

        Spacer(Modifier.height(16.dp))
    }
}

package fr.cinepass.ticket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.cinepass.ticket.R
import fr.cinepass.ticket.data.Ticket
import fr.cinepass.ticket.ui.CinePassViewModelFactories
import fr.cinepass.ticket.ui.components.BarcodeView
import fr.cinepass.ticket.ui.components.MaxBrightnessEffect
import fr.cinepass.ticket.ui.components.findActivity
import fr.cinepass.ticket.util.formatFullDate
import fr.cinepass.ticket.util.formatTime
import fr.cinepass.ticket.wallet.WalletRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    walletRepository: WalletRepository,
    viewModel: TicketDetailViewModel = viewModel(
        factory = CinePassViewModelFactories.detail(ticketId),
        key = "detail-$ticketId",
    ),
) {
    val ticket by viewModel.ticket.collectAsStateWithLifecycle()
    val brightnessBoost by viewModel.brightnessBoost.collectAsStateWithLifecycle()
    val walletBusy by viewModel.walletBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val walletLaunch by viewModel.walletLaunch.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }

    // Luminosité maximale tant que le billet (et donc son code) est affiché.
    MaxBrightnessEffect(enabled = brightnessBoost)

    LaunchedEffect(walletLaunch?.id) {
        val request = walletLaunch ?: return@LaunchedEffect
        val activity = context.findActivity()
        if (activity != null) {
            walletRepository.savePasses(request.jwt, activity)
        }
        viewModel.onWalletLaunchConsumed()
    }

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.text)
        viewModel.onMessageShown(current.id)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(ticket?.movieTitle.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    val current = ticket
                    if (current != null) {
                        IconButton(onClick = { onEdit(current.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_ticket))
                        }
                        IconButton(onClick = viewModel::toggleArchived) {
                            Icon(
                                imageVector = if (current.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = stringResource(
                                    if (current.archived) R.string.action_unarchive else R.string.action_archive,
                                ),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = ticket
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(0.dp))

            current.posterUri?.let { poster ->
                AsyncImage(
                    model = poster,
                    contentDescription = "Affiche du film",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            ScreeningSummary(current)

            BarcodeCard(current)

            BrightnessToggle(
                enabled = brightnessBoost,
                onToggle = viewModel::setBrightnessBoost,
            )

            WalletButton(
                busy = walletBusy,
                alreadySaved = current.addedToWalletAt != null,
                onClick = viewModel::addToWallet,
            )

            current.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Notes", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ce billet ?") },
            text = { Text("Le billet et son affiche seront définitivement supprimés de l'appareil.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ScreeningSummary(ticket: Ticket) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ticket.movieTitle, style = MaterialTheme.typography.headlineSmall)
            Text(ticket.cinemaName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${formatFullDate(ticket.screeningAt)} à ${formatTime(ticket.screeningAt)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ticket.room?.takeIf { it.isNotBlank() }?.let { InfoBlock("Salle", it) }
                ticket.seats?.takeIf { it.isNotBlank() }?.let { InfoBlock("Sièges", it) }
            }
            ticket.bookingReference?.takeIf { it.isNotBlank() }?.let {
                InfoBlock("Référence", it)
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BarcodeCard(ticket: Ticket) {
    // Carte volontairement blanche en clair comme en sombre : les scanners
    // s'appuient sur le contraste noir sur blanc.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BarcodeView(content = ticket.barcodeValue, format = ticket.barcodeFormat)
            Spacer(Modifier.height(12.dp))
            Text(
                text = ticket.barcodeValue,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
            )
            Text(
                text = ticket.barcodeFormat.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888888),
            )
        }
    }
}

@Composable
private fun BrightnessToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.brightness_boost), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.brightness_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun WalletButton(busy: Boolean, alreadySaved: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(Icons.Default.Wallet, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                if (alreadySaved) "Mettre à jour dans Google Wallet"
                else stringResource(R.string.action_add_to_wallet),
            )
        }
    }
}

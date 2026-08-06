package fr.cinepass.ticket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import fr.cinepass.ticket.util.formatShortDate
import fr.cinepass.ticket.util.formatTime
import fr.cinepass.ticket.wallet.WalletRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenViewer: (String, ViewerMode) -> Unit,
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

        // Ordre repris de Google Wallet : les informations, puis un code-barres
        // compact, puis l'affiche qui occupe tout le reste de l'écran.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PassCard(
                ticket = current,
                onBarcodeClick = { onOpenViewer(current.id, ViewerMode.BARCODE) },
            )

            // Un appui sur une affiche l'ouvre seule, en luminosité maximale ;
            // les suivantes se feuillettent du doigt.
            if (current.posterUris.isNotEmpty()) {
                PosterPager(
                    posterUris = current.posterUris,
                    onOpen = { onOpenViewer(current.id, ViewerMode.POSTER) },
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrightnessToggle(enabled = brightnessBoost, onToggle = viewModel::setBrightnessBoost)

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

                Spacer(Modifier.height(16.dp))
            }
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

/**
 * Les affiches du billet, feuilletables. Chacune s'affiche entière, à la
 * largeur de l'écran ; un appui ouvre la vue plein écran.
 */
@Composable
private fun PosterPager(posterUris: List<String>, onOpen: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { posterUris.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState) { page ->
            AsyncImage(
                model = posterUris[page],
                contentDescription = stringResource(R.string.poster_fullscreen),
                // FillWidth : la hauteur suit le ratio réel de l'affiche, donc
                // plus aucun rognage.
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
            )
        }

        if (posterUris.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(posterUris.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Le « pass » proprement dit : informations de séance et code-barres compact,
 * sur un fond clair constant — c'est aussi ce que voit un contrôleur.
 */
@Composable
private fun PassCard(ticket: Ticket, onBarcodeClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PASS_BACKGROUND),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = ticket.cinemaName,
                style = MaterialTheme.typography.bodyLarge,
                color = PASS_SECONDARY,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = ticket.displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = PASS_PRIMARY,
            )

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth()) {
                PassField("Date", formatShortDate(ticket.screeningAt), Modifier.weight(1f))
                PassField("Heure", formatTime(ticket.screeningAt), Modifier.weight(1f), TextAlign.End)
            }

            val room = ticket.room?.takeIf { it.isNotBlank() }
            val seats = ticket.seats?.takeIf { it.isNotBlank() }
            if (room != null || seats != null) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    room?.let { PassField("Salle", it, Modifier.weight(1f)) }
                    seats?.let { PassField("Sièges", it, Modifier.weight(1f), TextAlign.End) }
                }
            }

            ticket.bookingReference?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(12.dp))
                PassField("Référence", it, Modifier.fillMaxWidth())
            }

            // Sans code à présenter, on n'affiche ni séparateur ni bloc vide.
            if (ticket.hasBarcode) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = PASS_SECONDARY.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                CompactBarcode(ticket = ticket, onClick = onBarcodeClick)
            }
        }
    }
}

@Composable
private fun PassField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PASS_SECONDARY,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = PASS_PRIMARY,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Code-barres réduit, comme sur un pass Wallet : il reste lisible de près, et
 * un appui ouvre la vue plein écran pour le passage sous un scanner.
 */
@Composable
private fun CompactBarcode(ticket: Ticket, onClick: () -> Unit) {
    val width = if (ticket.barcodeFormat.isTwoDimensional) 180.dp else 280.dp

    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(10.dp),
        ) {
            BarcodeView(
                content = ticket.barcodeValue,
                format = ticket.barcodeFormat,
                modifier = Modifier.width(width),
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tap_to_enlarge),
            style = MaterialTheme.typography.labelSmall,
            color = PASS_SECONDARY,
        )
        Text(
            text = ticket.barcodeValue,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = PASS_SECONDARY,
            textAlign = TextAlign.Center,
        )
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

// Le pass garde les mêmes couleurs en thème clair et sombre : c'est un document
// qu'on présente, pas une surface de l'application.
private val PASS_BACKGROUND = Color(0xFFEAF1F1)
private val PASS_PRIMARY = Color(0xFF16191C)
private val PASS_SECONDARY = Color(0xFF5A6165)

package fr.cinepass.ticket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.cinepass.ticket.R
import fr.cinepass.ticket.ui.CinePassViewModelFactories
import fr.cinepass.ticket.ui.components.BarcodeView
import fr.cinepass.ticket.ui.components.MaxBrightnessEffect
import fr.cinepass.ticket.ui.components.findActivity

enum class ViewerMode { POSTER, BARCODE }

/**
 * Vue immersive : barres système masquées, luminosité au maximum, et rien
 * d'autre à l'écran que l'affiche (fond noir) ou le code-barres (fond blanc,
 * pour le contraste attendu par les scanners). Un appui revient au billet.
 */
@Composable
fun FullScreenViewerScreen(
    ticketId: String,
    mode: ViewerMode,
    onClose: () -> Unit,
    viewModel: TicketDetailViewModel = viewModel(
        factory = CinePassViewModelFactories.detail(ticketId),
        key = "detail-$ticketId",
    ),
) {
    val ticket by viewModel.ticket.collectAsStateWithLifecycle()

    MaxBrightnessEffect(enabled = true)
    ImmersiveModeEffect()

    val background = if (mode == ViewerMode.POSTER) Color.Black else Color.White
    val foreground = if (mode == ViewerMode.POSTER) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .clickable(
                // Toute la surface ferme la vue : pas d'effet d'appui parasite.
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val current = ticket
        if (current != null) when (mode) {
            ViewerMode.POSTER -> {
                val posters = current.posterUris
                if (posters.isEmpty()) {
                    Text(
                        text = "Aucune affiche pour ce billet.",
                        color = foreground,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { posters.size })

                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        AsyncImage(
                            model = posters[page],
                            contentDescription = stringResource(R.string.poster_fullscreen),
                            // Fit : l'affiche entière reste visible, sans rognage.
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (posters.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${posters.size}",
                            color = foreground.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                        )
                    }
                }
            }

            ViewerMode.BARCODE -> Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BarcodeView(
                    content = current.barcodeValue,
                    format = current.barcodeFormat,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = current.barcodeValue,
                    color = foreground,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = stringResource(R.string.tap_to_close),
            color = foreground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

/** Masque les barres système le temps de la vue, puis les restaure. */
@Composable
private fun ImmersiveModeEffect() {
    val view = LocalView.current
    val activity = view.context.findActivity()

    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }

        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

package fr.cinepass.ticket

import android.app.Application
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import fr.cinepass.ticket.data.AnimatedPosterRepository
import fr.cinepass.ticket.data.CinePassDatabase
import fr.cinepass.ticket.data.MovieSearchRepository
import fr.cinepass.ticket.data.SettingsRepository
import fr.cinepass.ticket.data.TicketRepository
import fr.cinepass.ticket.wallet.WalletConfig
import fr.cinepass.ticket.wallet.WalletIssuerDiscovery
import fr.cinepass.ticket.wallet.WalletRepository

/** Conteneur d'injection minimaliste : pas besoin d'un framework DI ici. */
class AppContainer(context: Context) {
    private val database = CinePassDatabase.get(context)

    val settingsRepository = SettingsRepository(context)
    val ticketRepository = TicketRepository(context, database.ticketDao())

    // Les deux intégrations relisent les réglages à chaque appel : une clé
    // saisie dans l'app est active sans redémarrage.
    val walletRepository = WalletRepository(context) {
        WalletConfig.from(settingsRepository.current())
    }
    val movieSearchRepository = MovieSearchRepository {
        settingsRepository.current().tmdbApiKey.ifBlank { BuildConfig.TMDB_API_KEY }
    }
    val animatedPosterRepository = AnimatedPosterRepository {
        settingsRepository.current().giphyApiKey.ifBlank { BuildConfig.GIPHY_API_KEY }
    }
    val walletIssuerDiscovery = WalletIssuerDiscovery()
}

class CinePassApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Chargeur d'images commun à l'app, doté des décodeurs animés : une affiche
     * GIF ou WebP animée choisie dans la galerie s'anime au lieu de rester figée
     * sur sa première image.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CinePassApplication).container

package fr.cinepass.ticket

import android.app.Application
import android.content.Context
import fr.cinepass.ticket.data.CinePassDatabase
import fr.cinepass.ticket.data.MovieSearchRepository
import fr.cinepass.ticket.data.SettingsRepository
import fr.cinepass.ticket.data.TicketRepository
import fr.cinepass.ticket.wallet.WalletConfig
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
}

class CinePassApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CinePassApplication).container

package fr.cinepass.ticket.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class TicketRepository(
    private val context: Context,
    private val dao: TicketDao,
) {

    fun observeAll(): Flow<List<Ticket>> = dao.observeAll()

    fun observeById(id: String): Flow<Ticket?> = dao.observeById(id)

    fun observeCinemaNames(): Flow<List<String>> = dao.observeCinemaNames()

    suspend fun findById(id: String): Ticket? = dao.findById(id)

    suspend fun save(ticket: Ticket) = dao.upsert(ticket)

    suspend fun setArchived(id: String, archived: Boolean) = dao.setArchived(id, archived)

    suspend fun markSavedToWallet(id: String, objectId: String) =
        dao.markSavedToWallet(id, objectId, System.currentTimeMillis())

    suspend fun delete(ticket: Ticket) {
        ticket.posterUris.forEach { deletePoster(it) }
        dao.delete(ticket)
    }

    /**
     * Copie l'affiche choisie dans le stockage interne de l'app : l'URI du sélecteur
     * de médias n'est lisible que le temps de la session.
     */
    suspend fun importPoster(source: Uri): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, POSTER_DIR).apply { mkdirs() }
        // L'extension suit le type réel : une affiche animée reste un .gif.
        val extension = when (context.contentResolver.getType(source)) {
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/png" -> "png"
            else -> "jpg"
        }
        val target = File(dir, "${UUID.randomUUID()}.$extension")
        runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    /** Télécharge l'affiche TMDB pour que le billet reste consultable hors ligne. */
    suspend fun importPosterFromUrl(url: String): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, POSTER_DIR).apply { mkdirs() }
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null

                // Une affiche animée doit rester un .gif : on suit le type annoncé.
                val extension = when (connection.contentType?.substringBefore(';')?.trim()) {
                    "image/gif" -> "gif"
                    "image/webp" -> "webp"
                    "image/png" -> "png"
                    else -> "jpg"
                }
                val target = File(dir, "${UUID.randomUUID()}.$extension")
                connection.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                Uri.fromFile(target).toString()
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    suspend fun deletePoster(uri: String) = withContext(Dispatchers.IO) {
        runCatching {
            val file = Uri.parse(uri).path?.let(::File) ?: return@runCatching
            if (file.parentFile?.name == POSTER_DIR) file.delete()
        }
        Unit
    }

    private companion object {
        const val POSTER_DIR = "posters"
    }
}

package fr.cinepass.ticket.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TicketRepository(
    private val context: Context,
    private val dao: TicketDao,
) {

    fun observeAll(): Flow<List<Ticket>> = dao.observeAll()

    fun observeById(id: String): Flow<Ticket?> = dao.observeById(id)

    suspend fun findById(id: String): Ticket? = dao.findById(id)

    suspend fun save(ticket: Ticket) = dao.upsert(ticket)

    suspend fun setArchived(id: String, archived: Boolean) = dao.setArchived(id, archived)

    suspend fun markSavedToWallet(id: String, objectId: String) =
        dao.markSavedToWallet(id, objectId, System.currentTimeMillis())

    suspend fun delete(ticket: Ticket) {
        ticket.posterUri?.let { deletePoster(it) }
        dao.delete(ticket)
    }

    /**
     * Copie l'affiche choisie dans le stockage interne de l'app : l'URI du sélecteur
     * de médias n'est lisible que le temps de la session.
     */
    suspend fun importPoster(source: Uri): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, POSTER_DIR).apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            Uri.fromFile(target).toString()
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

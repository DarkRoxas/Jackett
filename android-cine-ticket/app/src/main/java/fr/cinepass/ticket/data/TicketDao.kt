package fr.cinepass.ticket.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Query("SELECT * FROM tickets ORDER BY screeningAt DESC")
    fun observeAll(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    fun observeById(id: String): Flow<Ticket?>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun findById(id: String): Ticket?

    @Upsert
    suspend fun upsert(ticket: Ticket)

    @Delete
    suspend fun delete(ticket: Ticket)

    @Query("UPDATE tickets SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE tickets SET walletObjectId = :objectId, addedToWalletAt = :savedAt WHERE id = :id")
    suspend fun markSavedToWallet(id: String, objectId: String, savedAt: Long)
}

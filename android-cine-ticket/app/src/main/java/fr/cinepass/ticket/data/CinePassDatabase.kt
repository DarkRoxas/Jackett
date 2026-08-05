package fr.cinepass.ticket.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class BarcodeFormatConverter {
    @TypeConverter
    fun toName(format: TicketBarcodeFormat): String = format.name

    @TypeConverter
    fun fromName(name: String): TicketBarcodeFormat = TicketBarcodeFormat.fromName(name)
}

@Database(entities = [Ticket::class], version = 1, exportSchema = true)
@TypeConverters(BarcodeFormatConverter::class)
abstract class CinePassDatabase : RoomDatabase() {

    abstract fun ticketDao(): TicketDao

    companion object {
        @Volatile
        private var instance: CinePassDatabase? = null

        fun get(context: Context): CinePassDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CinePassDatabase::class.java,
                "cinepass.db",
            ).build().also { instance = it }
        }
    }
}

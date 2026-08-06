package fr.cinepass.ticket.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class BarcodeFormatConverter {
    @TypeConverter
    fun toName(format: TicketBarcodeFormat): String = format.name

    @TypeConverter
    fun fromName(name: String): TicketBarcodeFormat = TicketBarcodeFormat.fromName(name)
}

/** Ajout de l'année de sortie : les billets déjà enregistrés sont conservés. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tickets ADD COLUMN releaseYear INTEGER")
    }
}

/** Mémorise le film TMDB choisi, pour pouvoir rechoisir une affiche plus tard. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tickets ADD COLUMN tmdbId INTEGER")
    }
}

@Database(entities = [Ticket::class], version = 3, exportSchema = true)
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
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}

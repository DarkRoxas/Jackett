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

/**
 * Les URI d'affiches sont stockées une par ligne : aucune ne contient de retour
 * chariot, et la migration depuis la colonne unique se réduit à une copie.
 */
class UriListConverter {
    @TypeConverter
    fun toStorage(uris: List<String>): String = uris.joinToString(SEPARATOR)

    @TypeConverter
    fun fromStorage(value: String): List<String> =
        value.split(SEPARATOR).filter { it.isNotBlank() }

    private companion object {
        const val SEPARATOR = "\n"
    }
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

/**
 * Une affiche unique devient une liste d'affiches. SQLite ne sachant pas
 * supprimer une colonne, la table est recréée puis recopiée ; l'affiche
 * existante devient le premier élément de la liste.
 */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tickets_new (
                id TEXT NOT NULL,
                movieTitle TEXT NOT NULL,
                releaseYear INTEGER,
                cinemaName TEXT NOT NULL,
                screeningAt INTEGER NOT NULL,
                room TEXT,
                seats TEXT,
                bookingReference TEXT,
                barcodeValue TEXT NOT NULL,
                barcodeFormat TEXT NOT NULL,
                posterUris TEXT NOT NULL,
                tmdbId INTEGER,
                notes TEXT,
                walletObjectId TEXT,
                addedToWalletAt INTEGER,
                archived INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO tickets_new
            SELECT id, movieTitle, releaseYear, cinemaName, screeningAt, room, seats,
                   bookingReference, barcodeValue, barcodeFormat, COALESCE(posterUri, ''),
                   tmdbId, notes, walletObjectId, addedToWalletAt, archived, createdAt
            FROM tickets
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE tickets")
        db.execSQL("ALTER TABLE tickets_new RENAME TO tickets")
    }
}

@Database(entities = [Ticket::class], version = 4, exportSchema = true)
@TypeConverters(BarcodeFormatConverter::class, UriListConverter::class)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}

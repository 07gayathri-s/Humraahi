package com.humraahi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, TripEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HumraahiDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var instance: HumraahiDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_trips (
                        cachedForUserId TEXT NOT NULL,
                        id TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT NOT NULL,
                        createdBy TEXT NOT NULL,
                        PRIMARY KEY(cachedForUserId, id)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): HumraahiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HumraahiDatabase::class.java,
                    "humraahi.db"
                )
                    .addMigrations(migration1To2)
                    .build()
                    .also { instance = it }
            }
    }
}

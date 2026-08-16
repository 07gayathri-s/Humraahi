package com.humraahi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HumraahiDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var instance: HumraahiDatabase? = null

        fun getInstance(context: Context): HumraahiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HumraahiDatabase::class.java,
                    "humraahi.db"
                ).build().also { instance = it }
            }
    }
}

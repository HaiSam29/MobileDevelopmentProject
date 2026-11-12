package com.example.mobiledevelopmentproject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

private const val DATABASE_NAME = "restje.db"

// Verhoog version als je een tabel of kolom wijzigt.
@Database(
    entities = [CachedReservation::class, FavoriteStore::class, CachedAddress::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedReservationDao(): CachedReservationDao
    abstract fun favoriteStoreDao(): FavoriteStoreDao
    abstract fun cachedAddressDao(): CachedAddressDao

    companion object {
        // Eén database voor de hele app, een tweede openen is traag en kan botsen.
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}

package com.example.mobiledevelopmentproject.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CachedAddressDao {

    // null als dit adres nog niet in de cache staat.
    @Query("SELECT * FROM cached_addresses WHERE `key` = :key")
    suspend fun get(key: String): CachedAddress?

    @Upsert
    suspend fun upsert(address: CachedAddress)
}

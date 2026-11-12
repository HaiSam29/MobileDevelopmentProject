package com.example.mobiledevelopmentproject.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStoreDao {

    @Query("SELECT * FROM favorite_stores WHERE userId = :userId ORDER BY name")
    fun getFavorites(userId: String): Flow<List<FavoriteStore>>

    // Voor het hartje op het detailscherm.
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stores WHERE storeId = :storeId AND userId = :userId)")
    fun isFavorite(storeId: String, userId: String): Flow<Boolean>

    @Upsert
    suspend fun add(favorite: FavoriteStore)

    @Query("DELETE FROM favorite_stores WHERE storeId = :storeId AND userId = :userId")
    suspend fun remove(storeId: String, userId: String)
}

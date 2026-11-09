package com.example.mobiledevelopmentproject.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY name")
    fun getFavorites(userId: String): Flow<List<FavoriteEntity>>

    // Voor het hartje op het detailscherm.
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE storeId = :storeId AND userId = :userId)")
    fun isFavorite(storeId: String, userId: String): Flow<Boolean>

    @Upsert
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE storeId = :storeId AND userId = :userId")
    suspend fun remove(storeId: String, userId: String)
}

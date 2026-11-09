package com.example.mobiledevelopmentproject.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    // Nieuwste eerst. De Flow geeft een nieuwe lijst telkens de tabel verandert.
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getOrders(userId: String): Flow<List<OrderEntity>>

    // Upsert: toevoegen als het id nog niet bestaat, anders bijwerken.
    @Upsert
    suspend fun upsert(order: OrderEntity)

    @Upsert
    suspend fun upsertAll(orders: List<OrderEntity>)
}

package com.example.mobiledevelopmentproject.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedReservationDao {

    // Nieuwste eerst. De Flow geeft een nieuwe lijst telkens de tabel verandert.
    @Query("SELECT * FROM cached_reservations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReservations(userId: String): Flow<List<CachedReservation>>

    // Upsert: toevoegen als het id nog niet bestaat, anders bijwerken.
    @Upsert
    suspend fun upsert(reservation: CachedReservation)

    @Upsert
    suspend fun upsertAll(reservations: List<CachedReservation>)
}

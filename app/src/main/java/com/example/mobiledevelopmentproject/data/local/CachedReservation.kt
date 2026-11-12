package com.example.mobiledevelopmentproject.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mobiledevelopmentproject.data.model.OrderStatus

// Een reservatie zoals ze op de telefoon bewaard wordt, zodat Bestellingen offline werkt.
// Winkelnaam en pakkettitel staan er als kopie in, want offline kan je die niet opvragen.
// Tijden zijn milliseconden sinds 1970.
@Entity(tableName = "cached_reservations")
data class CachedReservation(
    // Zelfde id als de reservatie in Firestore.
    @PrimaryKey val id: String,
    val userId: String,
    val bagId: String,
    val bagTitle: String,
    val storeName: String,
    val quantity: Int,
    val pickupCode: String,
    val pickupStart: Long,
    val pickupEnd: Long,
    val status: OrderStatus,
    val createdAt: Long
)

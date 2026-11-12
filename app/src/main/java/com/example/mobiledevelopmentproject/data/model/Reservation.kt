package com.example.mobiledevelopmentproject.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// Een document in de collectie "reservations".
// bagTitle, storeName en de ophaaltijden zijn extra velden: een kopie van het moment van reserveren,
// zodat Bestellingen ze offline uit Room kan tonen.
data class Reservation(
    @DocumentId var id: String = "",
    var bagId: String = "",
    var storeId: String = "",
    var customerId: String = "",
    var quantity: Int = 0,
    var pickupCode: String = "",
    // "reserved", "collected" of "cancelled", zie OrderStatus.
    var status: String = OrderStatus.RESERVED.value,
    // Milliseconden sinds 1970.
    var createdAt: Long = 0,
    var bagTitle: String = "",
    var storeName: String = "",
    var pickupStart: Timestamp = Timestamp(0, 0),
    var pickupEnd: Timestamp = Timestamp(0, 0)
)

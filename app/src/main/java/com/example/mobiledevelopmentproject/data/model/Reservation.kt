package com.example.mobiledevelopmentproject.data.model

import com.google.firebase.firestore.DocumentId

// Een document in de collectie "reservations".
// Titel, winkelnaam en ophaaltijden zijn een kopie van het moment van reserveren,
// zodat Bestellingen ze offline uit Room kan tonen.
data class Reservation(
    @DocumentId var id: String = "",
    var packageId: String = "",
    var storeId: String = "",
    var customerId: String = "",
    var packageTitle: String = "",
    var storeName: String = "",
    var quantity: Int = 0,
    var pickupCode: String = "",
    var pickupFrom: Long = 0,
    var pickupUntil: Long = 0,
    var status: OrderStatus = OrderStatus.RESERVED,
    var createdAt: Long = 0
)

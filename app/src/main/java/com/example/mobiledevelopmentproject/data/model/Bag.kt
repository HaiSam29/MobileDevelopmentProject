package com.example.mobiledevelopmentproject.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

// Een document in de collectie "bags".
// storeName en location zijn een kopie van de winkel, zo hoef je die niet apart op te vragen.
data class Bag(
    @DocumentId var id: String = "",
    var storeId: String = "",
    var storeName: String = "",
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var originalValue: Double = 0.0,
    var quantityTotal: Int = 0,
    // Daalt bij elke reservatie, de klant ziet dit als "Nog 3 over".
    var quantityLeft: Int = 0,
    var pickupStart: Timestamp = Timestamp(0, 0),
    var pickupEnd: Timestamp = Timestamp(0, 0),
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    // false als de handelaar het pakket offline haalt (H7).
    var active: Boolean = true,
    var photoBase64: String = "",
    // Extra veld, milliseconden sinds 1970.
    var createdAt: Long = 0
)

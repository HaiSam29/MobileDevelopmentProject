package com.example.mobiledevelopmentproject.data.model

import com.google.firebase.firestore.DocumentId

// Een document in de collectie "packages". Tijden zijn milliseconden sinds 1970.
data class FoodPackage(
    @DocumentId var id: String = "",
    var storeId: String = "",
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var originalValue: Double = 0.0,
    var quantity: Int = 0,
    // Daalt bij elke reservatie, de klant ziet dit als "Nog 3 over".
    var quantityAvailable: Int = 0,
    var pickupFrom: Long = 0,
    var pickupUntil: Long = 0,
    var photoBase64: String = "",
    var createdAt: Long = 0
)

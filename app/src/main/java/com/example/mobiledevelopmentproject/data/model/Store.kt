package com.example.mobiledevelopmentproject.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

// Een document in de collectie "stores".
// Firestore heeft een lege constructor en var-velden nodig om het document in te vullen,
// daarom heeft elk veld een standaardwaarde.
data class Store(
    @DocumentId var id: String = "",
    var ownerId: String = "",
    var name: String = "",
    var category: String = "",
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    // Straat + huisnummer.
    var address: String = "",
    // Extra veld, de handelaar kan het aanpassen (H2).
    var postalCode: String = "",
    var city: String = ""
)

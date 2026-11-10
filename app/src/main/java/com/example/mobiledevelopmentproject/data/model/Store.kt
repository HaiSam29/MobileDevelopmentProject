package com.example.mobiledevelopmentproject.data.model

import com.google.firebase.firestore.DocumentId

// Een document in de collectie "stores".
// Firestore heeft een lege constructor en var-velden nodig om het document in te vullen,
// daarom heeft elk veld een standaardwaarde.
data class  Store(
    @DocumentId var id: String = "",
    var ownerId: String = "",
    var name: String = "",
    var category: String = "",
    var street: String = "",
    var postalCode: String = "",
    var city: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)

package com.example.mobiledevelopmentproject.data.model

// Adres voor "Mijn winkel" (H2). De velden passen op address, postalCode en city van Store.
data class GeocodedAddress(
    // Straat + huisnummer, leeg als Nominatim geen straat kent.
    val street: String,
    val postcode: String?,
    val city: String
)

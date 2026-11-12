package com.example.mobiledevelopmentproject.data.remote

import com.google.gson.annotations.SerializedName

// Het JSON-antwoord van /reverse, enkel de velden die we gebruiken.
// Alles is nullable: Gson vult velden die ontbreken met null, ook als het type dat niet toelaat.
// Met ? moet de compiler ons dwingen om dat geval af te handelen.
data class NominatimResponse(
    @SerializedName("display_name") val displayName: String?,
    val address: NominatimAddress?,
    // Nominatim geeft HTTP 200 met enkel dit veld als er niets gevonden is, bv. midden in zee.
    val error: String?
)

data class NominatimAddress(
    val road: String?,
    @SerializedName("house_number") val houseNumber: String?,
    val postcode: String?,
    // Afhankelijk van de grootte van de plaats is er city, town of village.
    val city: String?,
    val town: String?,
    val village: String?
)

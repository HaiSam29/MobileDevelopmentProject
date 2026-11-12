package com.example.mobiledevelopmentproject.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Een resultaat van Nominatim, zodat we dezelfde plek niet opnieuw opvragen.
// key is de zoom plus lat en lon afgerond op 4 decimalen (ongeveer 10 m), bv. "z18:51.2194,4.4025".
// Door de zoom in de sleutel overschrijven zoom 18 en zoom 10 elkaar niet.
@Entity(tableName = "cached_addresses")
data class CachedAddress(
    @PrimaryKey val key: String,
    // Zoom 18: straat + huisnummer, bv. "Meir 12". Zoom 10: de gemeente.
    val displayName: String,
    // Enkel bij zoom 18, en niet elke plek heeft een postcode.
    val postcode: String?,
    val city: String,
    // Milliseconden sinds 1970.
    val fetchedAt: Long
)

package com.example.mobiledevelopmentproject.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Een resultaat van Nominatim, zodat we dezelfde plek niet opnieuw opvragen.
// key is lat en lon afgerond op 4 decimalen (ongeveer 10 m), bv. "51.2194,4.4025".
@Entity(tableName = "cached_addresses")
data class CachedAddress(
    @PrimaryKey val key: String,
    val displayName: String,
    val city: String,
    // Milliseconden sinds 1970.
    val fetchedAt: Long
)

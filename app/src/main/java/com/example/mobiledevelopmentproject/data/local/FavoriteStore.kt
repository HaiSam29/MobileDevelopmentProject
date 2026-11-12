package com.example.mobiledevelopmentproject.data.local

import androidx.room.Entity

// Een favoriete winkel. De sleutel is winkel + gebruiker samen,
// zodat twee gebruikers op dezelfde telefoon elk hun eigen favorieten hebben.
@Entity(tableName = "favorite_stores", primaryKeys = ["storeId", "userId"])
data class FavoriteStore(
    val storeId: String,
    val userId: String,
    val name: String,
    val category: String,
    val address: String,
    // Milliseconden sinds 1970.
    val addedAt: Long
)

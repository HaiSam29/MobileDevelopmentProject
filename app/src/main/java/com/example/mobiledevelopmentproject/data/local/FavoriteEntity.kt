package com.example.mobiledevelopmentproject.data.local

import androidx.room.Entity

// Een favoriete winkel. De sleutel is winkel + gebruiker samen,
// zodat twee gebruikers op dezelfde telefoon elk hun eigen favorieten hebben.
@Entity(tableName = "favorites", primaryKeys = ["storeId", "userId"])
data class FavoriteEntity(
    val storeId: String,
    val userId: String,
    val name: String,
    val category: String,
    val street: String,
    val city: String
)

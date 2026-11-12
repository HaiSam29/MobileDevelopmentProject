package com.example.mobiledevelopmentproject.data.model

// value is de tekst die in Firestore in het veld "status" komt.
// Room bewaart de enum zelf als zijn naam, bv. "RESERVED".
enum class OrderStatus(val value: String) {
    RESERVED("reserved"),
    COLLECTED("collected"),
    CANCELLED("cancelled");

    companion object {
        // Onbekende tekst wordt RESERVED, zodat de app niet crasht.
        fun fromValue(value: String): OrderStatus {
            return entries.firstOrNull { it.value == value } ?: RESERVED
        }
    }
}

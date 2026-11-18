package com.example.mobiledevelopmentproject.data.model

// value is de tekst die in Firestore in het veld "role" komt.
enum class UserRole(val value: String) {
    CUSTOMER("customer"),
    MERCHANT("merchant");

    companion object {
        // Zoekt de rol bij een waarde uit Firestore, of null als die onbekend is.
        fun fromValue(value: String?): UserRole? {
            return entries.find { it.value == value }
        }
    }
}

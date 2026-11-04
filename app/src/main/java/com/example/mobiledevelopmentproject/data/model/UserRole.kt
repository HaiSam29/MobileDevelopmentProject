package com.example.mobiledevelopmentproject.data.model

// value is de tekst die in Firestore in het veld "role" komt.
enum class UserRole(val value: String) {
    CUSTOMER("customer"),
    MERCHANT("merchant")
}

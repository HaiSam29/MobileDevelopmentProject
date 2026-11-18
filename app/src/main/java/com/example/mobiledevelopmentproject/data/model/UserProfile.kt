package com.example.mobiledevelopmentproject.data.model

// Wat de app na het inloggen over de gebruiker nodig heeft, uit het document users/{uid}.
data class UserProfile(
    val name: String,
    val role: UserRole
)

package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.model.UserRole
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val USERS_COLLECTION = "users"
private const val SAVE_TIMEOUT_MS = 10_000L

// Enige plek voor Firebase-code rond inloggen en registreren.
// Bij een fout gooit elke functie een FirebaseException, de ViewModel vangt die op.
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // De uid van de ingelogde gebruiker, of null als niemand ingelogd is.
    fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(name: String, email: String, password: String, role: UserRole) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw FirebaseException("No user after registration")

        try {
            saveProfile(user.uid, name, email, role)
        } catch (e: FirebaseException) {
            // Zonder profiel heeft het account geen rol en blijft het e-mailadres bezet.
            removeAccount(user)
            throw e
        }
    }

    private suspend fun saveProfile(uid: String, name: String, email: String, role: UserRole) {
        val profile = mapOf(
            "name" to name,
            "email" to email,
            "role" to role.value
        )

        // Zonder internet blijft set().await() wachten, daarom de timeout.
        // Het blok eindigt op true omdat await() hier altijd null teruggeeft,
        // en null betekent bij withTimeoutOrNull dat de tijd op is.
        val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
            firestore.collection(USERS_COLLECTION).document(uid).set(profile).await()
            true
        }

        if (saved == null) {
            throw FirebaseNetworkException("Saving the profile timed out")
        }
    }

    private suspend fun removeAccount(user: FirebaseUser) {
        try {
            user.delete().await()
        } catch (e: FirebaseException) {
            // Verwijderen lukt niet, bijvoorbeeld zonder internet. Dan toch uitloggen.
            auth.signOut()
        }
    }
}

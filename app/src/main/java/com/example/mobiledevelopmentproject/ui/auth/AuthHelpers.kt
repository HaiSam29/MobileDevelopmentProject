package com.example.mobiledevelopmentproject.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import com.example.mobiledevelopmentproject.R
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

// Geeft de foutmelding voor een e-mailadres terug, of null als het in orde is.
@StringRes
fun validateEmail(email: String): Int? = when {
    email.isBlank() -> R.string.error_email_required
    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.error_email_invalid
    else -> null
}

// Vertaalt een Firebase-fout naar een melding uit strings.xml.
@StringRes
fun FirebaseException.toErrorMessage(): Int = when (this) {
    is FirebaseNetworkException -> R.string.error_no_internet
    is FirebaseTooManyRequestsException -> R.string.error_too_many_attempts
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> R.string.error_wrong_credentials
    else -> R.string.error_general
}

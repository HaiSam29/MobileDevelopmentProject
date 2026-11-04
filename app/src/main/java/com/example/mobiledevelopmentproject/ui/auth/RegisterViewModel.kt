package com.example.mobiledevelopmentproject.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.model.UserRole
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 6

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, generalError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(role = role) }
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val name = state.name.trim()
        val email = state.email.trim()
        val nameError = if (name.isEmpty()) R.string.error_name_required else null
        val emailError = validateEmail(email)
        val passwordError = validatePassword(state.password)

        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            try {
                repository.register(name, email, state.password, state.role)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: FirebaseException) {
                _uiState.update { it.withError(e).copy(isLoading = false) }
            }
        }
    }

    @StringRes
    private fun validatePassword(password: String): Int? = when {
        password.isEmpty() -> R.string.error_password_required
        password.length < MIN_PASSWORD_LENGTH -> R.string.error_password_too_short
        else -> null
    }

    // Fouten over een veld komen bij dat veld te staan, de rest boven de knop.
    // WeakPassword staat voor InvalidCredentials omdat het daar een subklasse van is.
    private fun RegisterUiState.withError(e: FirebaseException): RegisterUiState = when (e) {
        is FirebaseAuthUserCollisionException -> copy(emailError = R.string.error_email_in_use)
        is FirebaseAuthWeakPasswordException -> copy(passwordError = R.string.error_password_too_short)
        is FirebaseAuthInvalidCredentialsException -> copy(emailError = R.string.error_email_invalid)
        else -> copy(generalError = e.toErrorMessage())
    }
}

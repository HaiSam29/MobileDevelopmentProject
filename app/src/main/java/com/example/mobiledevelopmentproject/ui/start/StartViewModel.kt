package com.example.mobiledevelopmentproject.ui.start

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.model.UserRole
import com.example.mobiledevelopmentproject.ui.auth.toErrorMessage
import com.google.firebase.FirebaseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Waar de app na het startscherm naartoe moet.
sealed interface StartDestination {
    data object Login : StartDestination
    data class CustomerHome(val name: String) : StartDestination
    data class MerchantHome(val name: String) : StartDestination
}

data class StartUiState(
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null,
    // Niet null zodra bekend is welk scherm past, het scherm navigeert dan.
    val destination: StartDestination? = null
)

class StartViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        loadStart()
    }

    // Kiest het startscherm. Ook gebruikt door de knop "Opnieuw proberen".
    fun loadStart() {
        if (repository.currentUserId() == null) {
            _uiState.update { it.copy(isLoading = false, destination = StartDestination.Login) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val profile = repository.getUserRole()
                val destination = when (profile.role) {
                    UserRole.CUSTOMER -> StartDestination.CustomerHome(profile.name)
                    UserRole.MERCHANT -> StartDestination.MerchantHome(profile.name)
                }
                _uiState.update { it.copy(isLoading = false, destination = destination) }
            } catch (e: FirebaseException) {
                // Geen crash: het scherm toont de fout met een knop om opnieuw te proberen.
                _uiState.update { it.copy(isLoading = false, error = e.toErrorMessage()) }
            }
        }
    }

    // Uitweg als de rol blijft mislukken, bv. bij een account zonder profiel.
    fun logout() {
        repository.logout()
        _uiState.update { it.copy(error = null, destination = StartDestination.Login) }
    }
}

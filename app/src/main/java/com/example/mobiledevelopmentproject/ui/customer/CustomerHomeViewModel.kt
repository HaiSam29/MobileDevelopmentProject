package com.example.mobiledevelopmentproject.ui.customer

import androidx.lifecycle.ViewModel
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.location.ChosenLocationRepository

// ViewModel van het startscherm van de klant. Voorlopig enkel uitloggen,
// de lijst met pakketten komt later.
class CustomerHomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() {
        repository.logout()
        // Een volgende gebruiker op dit toestel mag de locatie van de vorige niet zien.
        ChosenLocationRepository.clear()
    }
}

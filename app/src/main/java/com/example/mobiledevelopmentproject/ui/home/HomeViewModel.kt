package com.example.mobiledevelopmentproject.ui.home

import androidx.lifecycle.ViewModel
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.location.ChosenLocationRepository

// ViewModel van het startscherm. Voorlopig enkel uitloggen.
class HomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() {
        repository.logout()
        // Een volgende gebruiker op dit toestel mag de locatie van de vorige niet zien.
        ChosenLocationRepository.clear()
    }
}

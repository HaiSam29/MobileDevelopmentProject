package com.example.mobiledevelopmentproject.ui.merchant

import androidx.lifecycle.ViewModel
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.location.ChosenLocationRepository

// ViewModel van het startscherm van de handelaar. Voorlopig enkel uitloggen,
// de eigen pakketten komen later.
class MerchantHomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() {
        repository.logout()
        // Een volgende gebruiker op dit toestel mag de locatie van de vorige niet zien.
        ChosenLocationRepository.clear()
    }
}

package com.example.mobiledevelopmentproject.ui.customer

import androidx.lifecycle.ViewModel
import com.example.mobiledevelopmentproject.data.AuthRepository

// ViewModel van het startscherm van de klant. Voorlopig enkel uitloggen,
// de lijst met pakketten komt later.
class CustomerHomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() {
        repository.logout()
    }
}

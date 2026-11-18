package com.example.mobiledevelopmentproject.ui.home

import androidx.lifecycle.ViewModel
import com.example.mobiledevelopmentproject.data.AuthRepository

// ViewModel van het startscherm. Voorlopig enkel uitloggen.
class HomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() {
        repository.logout()
    }
}

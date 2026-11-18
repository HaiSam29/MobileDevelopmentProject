package com.example.mobiledevelopmentproject.ui.merchant

import androidx.lifecycle.ViewModel
import com.example.mobiledevelopmentproject.data.AuthRepository

// ViewModel van het startscherm van de handelaar. Voorlopig enkel uitloggen,
// de eigen pakketten komen later.
class MerchantHomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() {
        repository.logout()
    }
}

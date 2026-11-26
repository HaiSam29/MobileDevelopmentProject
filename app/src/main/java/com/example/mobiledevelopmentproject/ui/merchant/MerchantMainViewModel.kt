package com.example.mobiledevelopmentproject.ui.merchant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.StoreRepository
import com.google.firebase.FirebaseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// startTab is null zolang we nog niet weten of de handelaar een winkel heeft.
data class MerchantMainUiState(
    val startTab: MerchantTab? = null
)

// Kiest op welke tab de handelaar start: zonder winkel eerst "Mijn winkel",
// anders "Mijn pakketten".
class MerchantMainViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val storeRepository: StoreRepository = StoreRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantMainUiState())
    val uiState: StateFlow<MerchantMainUiState> = _uiState.asStateFlow()

    // init loopt één keer: na een rotatie bestaat deze ViewModel nog,
    // dus de winkel wordt niet opnieuw opgevraagd.
    init {
        viewModelScope.launch {
            _uiState.value = MerchantMainUiState(startTab = findStartTab())
        }
    }

    private suspend fun findStartTab(): MerchantTab {
        val uid = authRepository.currentUserId() ?: return MerchantTab.MY_STORE
        return try {
            // Het document-id van een winkel is de uid van de eigenaar.
            val store = storeRepository.getStore(uid)
            if (store == null) MerchantTab.MY_STORE else MerchantTab.MY_BAGS
        } catch (e: FirebaseException) {
            // Geen crash, bv. zonder internet. "Mijn winkel" toont zelf een fout met
            // "Opnieuw proberen", dus daar zit de handelaar het best.
            MerchantTab.MY_STORE
        }
    }
}

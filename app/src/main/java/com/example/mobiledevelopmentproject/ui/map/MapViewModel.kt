package com.example.mobiledevelopmentproject.ui.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Start op het centrum van Antwerpen. Zoom 14 toont ongeveer de hele binnenstad.
private const val ANTWERP_LATITUDE = 51.2194
private const val ANTWERP_LONGITUDE = 4.4025
private const val DEFAULT_ZOOM = 14.0

data class MapUiState(
    val latitude: Double = ANTWERP_LATITUDE,
    val longitude: Double = ANTWERP_LONGITUDE,
    val zoom: Double = DEFAULT_ZOOM
)

// De ViewModel overleeft een rotatie, de MapView niet.
// Daarom staat de kaartpositie hier en niet in de MapView.
class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onPositionChange(latitude: Double, longitude: Double, zoom: Double) {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude, zoom = zoom) }
    }
}

package com.example.mobiledevelopmentproject.ui.map

// Een punt op de kaart. Eigen klasse zodat de UiState en de ViewModel
// geen GeoPoint van osmdroid of van Firestore nodig hebben (die heten allebei GeoPoint).
data class MapPoint(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        // Startpunt als er nog geen pin is: het centrum van Antwerpen.
        val ANTWERP = MapPoint(51.2194, 4.4025)
    }
}

package com.example.mobiledevelopmentproject.data.location

import com.example.mobiledevelopmentproject.data.model.MapPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Waar de locatie van de klant vandaan komt.
enum class LocationSource {
    // Eigen locatie, toestemming "Precies".
    PRECISE,
    // Eigen locatie, toestemming "Bij benadering": afstanden kunnen een paar km afwijken.
    APPROXIMATE,
    // De klant heeft zelf een pin op de kaart gezet.
    PIN
}

// De locatie waarmee de app rekent: voor "Pakketten in de buurt van {gemeente}"
// en om pakketten op afstand te sorteren.
data class ChosenLocation(
    val point: MapPoint,
    val source: LocationSource
)

// De centrale plek voor de gekozen locatie.
// Een object bestaat maar één keer en leeft zo lang als de app: de locatie overleeft dus
// een rotatie en is ook te lezen vanuit andere schermen (Kaart, Ontdekken).
// Elke ViewModel die de locatie nodig heeft, leest dezelfde StateFlow.
object ChosenLocationRepository {

    // null zolang de klant nog geen locatie gedeeld of gekozen heeft.
    private val _location = MutableStateFlow<ChosenLocation?>(null)
    val location: StateFlow<ChosenLocation?> = _location.asStateFlow()

    fun set(location: ChosenLocation) {
        _location.value = location
    }

    // Bij uitloggen, zodat een volgende gebruiker niet de locatie van de vorige ziet.
    fun clear() {
        _location.value = null
    }
}

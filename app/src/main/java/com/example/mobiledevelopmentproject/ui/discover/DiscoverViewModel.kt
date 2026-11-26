package com.example.mobiledevelopmentproject.ui.discover

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.GeocodingException
import com.example.mobiledevelopmentproject.data.GeocodingRepository
import com.example.mobiledevelopmentproject.data.local.AppDatabase
import com.example.mobiledevelopmentproject.data.location.ChosenLocationRepository
import com.example.mobiledevelopmentproject.data.location.LocationSource
import com.example.mobiledevelopmentproject.data.model.MapPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// Pas als de locatie minstens zo ver verschuift, zoeken we de gemeente opnieuw op.
// Zo geven GPS-schommelingen of opnieuw je locatie ophalen op dezelfde plek geen nieuwe aanvraag.
private const val MIN_DISTANCE_METERS = 300.0

data class DiscoverUiState(
    // false zolang de klant nog geen locatie gedeeld of gekozen heeft.
    val hasLocation: Boolean = false,
    // true bij toestemming "Bij benadering": het scherm toont dan dat afstanden kunnen afwijken.
    val isApproximate: Boolean = false,
    // true terwijl de gemeente opgezocht wordt.
    val isLoadingCity: Boolean = false,
    // null als de gemeente (nog) niet gekend is: het scherm toont dan "Pakketten in de buurt".
    val city: String? = null,
    // Waarom het opzoeken mislukte, of null.
    @StringRes val cityError: Int? = null,
    // Enkel bij geen internet of een serverfout: dan kan opnieuw proberen wel lukken.
    // Bij "niets gevonden" zou het dezelfde vraag nog eens naar Nominatim sturen.
    val canRetryCity: Boolean = false
)

// ViewModel van de kop van Ontdekken: "Pakketten in de buurt van {gemeente}".
// De locatie zelf komt uit ChosenLocationRepository (gezet door MapViewModel),
// deze ViewModel zoekt enkel de gemeente op die bij die locatie hoort.
class DiscoverViewModel(
    private val geocodingRepository: GeocodingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    // Het lopende opzoeken. Bij een nieuwe locatie stoppen we het,
    // zodat een oud resultaat de nieuwe gemeente niet overschrijft.
    private var lookupJob: Job? = null

    // Het punt waarvoor we de gemeente laatst opzochten, voor "Opnieuw proberen".
    private var lastPoint: MapPoint? = null

    init {
        // 1. Of er een locatie is en of ze bij benadering is. Dit volgt elke wijziging,
        //    zodat de melding verdwijnt zodra de klant "Precies" toestaat, ook op dezelfde plek.
        viewModelScope.launch {
            ChosenLocationRepository.location.collect { location ->
                _uiState.update {
                    it.copy(
                        hasLocation = location != null,
                        isApproximate = location?.source == LocationSource.APPROXIMATE
                    )
                }
            }
        }

        // 2. De gemeente. distinctUntilChanged laat een punt enkel door als het
        //    echt verschilt van het vorige doorgelaten punt (zie isSamePlace).
        viewModelScope.launch {
            ChosenLocationRepository.location
                .map { it?.point }
                .distinctUntilChanged(::isSamePlace)
                .collect { point -> onPointChanged(point) }
        }
    }

    // Knop "Opnieuw proberen" na een fout.
    fun onRetryCity() {
        lastPoint?.let { lookupCity(it) }
    }

    private fun onPointChanged(point: MapPoint?) {
        lastPoint = point
        if (point == null) {
            // Geen locatie meer (bv. uitgelogd): alles leegmaken.
            lookupJob?.cancel()
            _uiState.update { it.copy(isLoadingCity = false, city = null, cityError = null, canRetryCity = false) }
        } else {
            lookupCity(point)
        }
    }

    private fun lookupCity(point: MapPoint) {
        lookupJob?.cancel()
        _uiState.update { it.copy(isLoadingCity = true, cityError = null, canRetryCity = false) }

        lookupJob = viewModelScope.launch {
            try {
                // Eerst in de cache (Room), anders bij Nominatim (max 1 aanvraag per seconde).
                val city = geocodingRepository.getCity(point.latitude, point.longitude)
                _uiState.update { it.copy(isLoadingCity = false, city = city) }
            } catch (e: GeocodingException) {
                // Geen gemeente: het scherm toont "Pakketten in de buurt" met de melding.
                _uiState.update {
                    it.copy(
                        isLoadingCity = false,
                        city = null,
                        cityError = e.toErrorMessage(),
                        canRetryCity = e.reason != GeocodingException.Reason.NOT_FOUND
                    )
                }
            }
        }
    }

    companion object {
        // GeocodingRepository heeft de DAO uit Room nodig, en die een Context.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val dao = AppDatabase.getInstance(application).cachedAddressDao()
                DiscoverViewModel(GeocodingRepository(dao))
            }
        }
    }
}

// Twee punten tellen als dezelfde plek als ze minder dan MIN_DISTANCE_METERS uit elkaar liggen.
// Van of naar null (geen locatie) is altijd een wijziging.
private fun isSamePlace(old: MapPoint?, new: MapPoint?): Boolean {
    if (old == null || new == null) return old == new
    val distance = GeoPoint(old.latitude, old.longitude)
        .distanceToAsDouble(GeoPoint(new.latitude, new.longitude))
    return distance < MIN_DISTANCE_METERS
}

@StringRes
private fun GeocodingException.toErrorMessage(): Int = when (reason) {
    GeocodingException.Reason.NO_INTERNET -> R.string.error_no_internet
    GeocodingException.Reason.NOT_FOUND -> R.string.error_city_not_found
    GeocodingException.Reason.SERVER_ERROR -> R.string.error_city_lookup_failed
}

package com.example.mobiledevelopmentproject.ui.map

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.location.ChosenLocation
import com.example.mobiledevelopmentproject.data.location.ChosenLocationRepository
import com.example.mobiledevelopmentproject.data.location.LocationProvider
import com.example.mobiledevelopmentproject.data.location.LocationResult
import com.example.mobiledevelopmentproject.data.location.LocationSource
import com.example.mobiledevelopmentproject.data.model.MapPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Zoom 14 toont ongeveer de hele binnenstad van Antwerpen.
private const val DEFAULT_ZOOM = 14.0

// Zoom 16 toont een paar straten rond de eigen locatie.
private const val MY_LOCATION_ZOOM = 16.0

// Wat het kaartscherm toont.
enum class LocationMode {
    // Uitleg waarom we de locatie vragen, met "Locatie toestaan" en "Zelf kiezen op de kaart".
    ASK,
    // De locatie wordt opgehaald.
    LOADING,
    // De kaart met de eigen locatie als blauwe stip.
    OWN_LOCATION,
    // Toestemming is er, maar er is geen locatie (locatie staat uit of nog niet gevonden).
    UNAVAILABLE,
    // De klant zet zelf een pin op de kaart.
    PICK_PIN
}

data class MapUiState(
    // Waar de kaart naar kijkt, zodat dat een rotatie overleeft.
    val mapCenter: MapPoint = MapPoint.ANTWERP,
    val mapZoom: Double = DEFAULT_ZOOM,
    val mode: LocationMode = LocationMode.ASK,
    // De eigen locatie, null zolang die er niet is.
    val myLocation: MapPoint? = null,
    // true bij toestemming "Bij benadering".
    val isApproximate: Boolean = false,
    // De pin die de klant zelf zet.
    val pin: MapPoint? = null,
    // true als de toestemming geweigerd is: het scherm legt dan uit waarom de klant een pin zet.
    val permissionDenied: Boolean = false,
    // Waarom er geen locatie is, bij mode UNAVAILABLE.
    @StringRes val locationError: Int? = null,
    // Eenmalige opdracht: centreer de kaart op dit punt. Het scherm roept daarna onCentered() op.
    // Nodig omdat OsmMap mapCenter enkel gebruikt als startpositie.
    val centerOn: MapPoint? = null,
    // true na "Gebruik deze locatie", het scherm toont dan één keer een melding.
    val isPinSaved: Boolean = false
)

// De ViewModel overleeft een rotatie, de MapView niet.
// Daarom staan de kaartpositie, de locatie en de pin hier en niet in de MapView.
class MapViewModel(
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Het lopende ophalen van de locatie. Kiest de klant intussen "Zelf kiezen op de kaart",
    // dan stoppen we het, anders springt het scherm later toch naar de eigen locatie.
    private var locationJob: Job? = null

    // init loopt één keer: na een rotatie bestaat deze ViewModel nog.
    // We vragen hier nooit toestemming: dat gebeurt pas na een tik op "Locatie toestaan".
    init {
        val chosen = ChosenLocationRepository.location.value
        when {
            // Al een locatie gekozen (bv. eerder op dit scherm): die meteen tonen.
            chosen != null -> showChosenLocation(chosen)
            // Toestemming al eerder gegeven: geen uitleg nodig, meteen ophalen.
            locationProvider.hasAnyLocation() -> fetchMyLocation()
            // Anders blijft mode ASK: het scherm toont de uitleg.
        }
    }

    // Het resultaat van de vraag om toestemming. Het scherm vraagt FINE en COARSE samen.
    // fine = "Precies", enkel coarse = "Bij benadering", geen van beide = geweigerd.
    fun onPermissionResult(fine: Boolean, coarse: Boolean) {
        if (fine || coarse) {
            fetchMyLocation()
        } else {
            showPinPicker(permissionDenied = true)
        }
    }

    // Knop "Opnieuw proberen" als er geen locatie was.
    fun onRetryLocation() {
        fetchMyLocation()
    }

    // Knop "Zelf kiezen op de kaart".
    fun onChooseOnMap() {
        showPinPicker(permissionDenied = false)
    }

    // Opgeroepen na een tik op de kaart of als de pin losgelaten wordt.
    fun onPinPlaced(point: MapPoint) {
        _uiState.update { it.copy(pin = point) }
    }

    // Knop "Gebruik deze locatie": de pin wordt de gekozen locatie voor de hele app.
    fun onUsePin() {
        val pin = _uiState.value.pin ?: return
        ChosenLocationRepository.set(ChosenLocation(pin, LocationSource.PIN))
        _uiState.update { it.copy(isPinSaved = true) }
    }

    // Het scherm heeft de melding "locatie opgeslagen" getoond.
    fun onPinSavedMessageShown() {
        _uiState.update { it.copy(isPinSaved = false) }
    }

    // Het scherm heeft de kaart gecentreerd.
    fun onCentered() {
        _uiState.update { it.copy(centerOn = null) }
    }

    // De kaart meldt waar ze naar kijkt wanneer het scherm pauzeert (bv. bij een rotatie).
    fun onMapPositionChange(latitude: Double, longitude: Double, zoom: Double) {
        _uiState.update { it.copy(mapCenter = MapPoint(latitude, longitude), mapZoom = zoom) }
    }

    private fun fetchMyLocation() {
        _uiState.update { it.copy(mode = LocationMode.LOADING, locationError = null, permissionDenied = false) }

        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            when (val result = locationProvider.getCurrentLocation()) {
                is LocationResult.Found -> {
                    val source = if (result.approximate) LocationSource.APPROXIMATE else LocationSource.PRECISE
                    val chosen = ChosenLocation(result.point, source)
                    ChosenLocationRepository.set(chosen)
                    showChosenLocation(chosen)
                }
                LocationResult.LocationOff -> showUnavailable(R.string.error_location_off)
                LocationResult.NotFound -> showUnavailable(R.string.error_location_not_found)
                // Toestemming intussen ingetrokken: de klant kan nog altijd een pin zetten.
                LocationResult.NoPermission -> showPinPicker(permissionDenied = true)
            }
        }
    }

    // Toont een locatie uit ChosenLocationRepository: een eigen locatie als blauwe stip,
    // een eerder gekozen pin als pin die de klant nog kan verplaatsen.
    private fun showChosenLocation(chosen: ChosenLocation) {
        _uiState.update {
            when (chosen.source) {
                LocationSource.PRECISE, LocationSource.APPROXIMATE -> it.copy(
                    mode = LocationMode.OWN_LOCATION,
                    myLocation = chosen.point,
                    isApproximate = chosen.source == LocationSource.APPROXIMATE,
                    mapCenter = chosen.point,
                    mapZoom = MY_LOCATION_ZOOM,
                    centerOn = chosen.point
                )
                LocationSource.PIN -> it.copy(
                    mode = LocationMode.PICK_PIN,
                    pin = chosen.point,
                    mapCenter = chosen.point,
                    mapZoom = MY_LOCATION_ZOOM,
                    centerOn = chosen.point
                )
            }
        }
    }

    private fun showUnavailable(@StringRes error: Int) {
        _uiState.update { it.copy(mode = LocationMode.UNAVAILABLE, locationError = error) }
    }

    private fun showPinPicker(permissionDenied: Boolean) {
        locationJob?.cancel()
        _uiState.update {
            it.copy(mode = LocationMode.PICK_PIN, permissionDenied = permissionDenied, locationError = null)
        }
    }

    companion object {
        // LocationProvider heeft een Context nodig. We geven de Application mee: die leeft
        // even lang als de app. Een Activity zou na een rotatie al weg zijn, terwijl deze
        // ViewModel dan nog bestaat.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                MapViewModel(LocationProvider(application))
            }
        }
    }
}

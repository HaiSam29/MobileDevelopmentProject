package com.example.mobiledevelopmentproject.ui.store

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.GeocodingException
import com.example.mobiledevelopmentproject.data.GeocodingRepository
import com.example.mobiledevelopmentproject.data.StoreRepository
import com.example.mobiledevelopmentproject.data.local.AppDatabase
import com.example.mobiledevelopmentproject.data.model.Store
import com.example.mobiledevelopmentproject.data.model.StoreCategory
import com.example.mobiledevelopmentproject.ui.auth.toErrorMessage
import com.example.mobiledevelopmentproject.ui.map.MapPoint
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Zoom 17 toont een paar straten, genoeg om de juiste gevel te kiezen.
private const val STORE_ZOOM = 17.0

data class MyStoreUiState(
    // Formulier
    val name: String = "",
    val category: StoreCategory? = null,
    val street: String = "",
    val postcode: String = "",
    val city: String = "",
    // Kaart: de pin en waar de kaart naar kijkt, zodat beide een rotatie overleven.
    val pin: MapPoint? = null,
    val mapCenter: MapPoint = MapPoint.ANTWERP,
    val mapZoom: Double = STORE_ZOOM,
    // Laden, adres opzoeken en opslaan
    val isLoadingStore: Boolean = true,
    val isLookingUpAddress: Boolean = false,
    val isSaving: Boolean = false,
    // true als het adres net automatisch ingevuld is, het scherm toont dan een hint.
    val addressAutoFilled: Boolean = false,
    // Foutmeldingen bij een veld
    @StringRes val nameError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val locationError: Int? = null,
    @StringRes val streetError: Int? = null,
    @StringRes val postcodeError: Int? = null,
    @StringRes val cityError: Int? = null,
    // Algemene foutmeldingen
    @StringRes val loadError: Int? = null,
    @StringRes val addressLookupError: Int? = null,
    @StringRes val saveError: Int? = null,
    // true na een geslaagde opslag, het scherm toont dan één keer een melding.
    val isSaved: Boolean = false
)

class MyStoreViewModel(
    private val geocodingRepository: GeocodingRepository,
    private val storeRepository: StoreRepository = StoreRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyStoreUiState())
    val uiState: StateFlow<MyStoreUiState> = _uiState.asStateFlow()

    // De lopende adresopzoeking. Bij een nieuwe pin stoppen we de vorige,
    // anders kan een oud antwoord later binnenkomen en het nieuwe adres overschrijven.
    private var lookupJob: Job? = null

    // init loopt één keer: na een rotatie bestaat deze ViewModel nog,
    // dus de winkel wordt niet opnieuw geladen en de ingevulde velden blijven staan.
    init {
        loadStore()
    }

    // Laadt de winkel van de handelaar. Heeft hij er nog geen, dan blijft het formulier leeg.
    // Ook gebruikt door de knop "Opnieuw proberen" als laden mislukte.
    fun loadStore() {
        val uid = authRepository.currentUserId()
        if (uid == null) {
            _uiState.update { it.copy(isLoadingStore = false, loadError = R.string.error_general) }
            return
        }

        _uiState.update { it.copy(isLoadingStore = true, loadError = null) }

        viewModelScope.launch {
            try {
                // Het document-id van een winkel is de uid van de eigenaar.
                val store = storeRepository.getStore(uid)
                _uiState.update { state ->
                    if (store == null) state.copy(isLoadingStore = false) else state.withStore(store)
                }
            } catch (e: FirebaseException) {
                _uiState.update { it.copy(isLoadingStore = false, loadError = e.toErrorMessage()) }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, saveError = null) }
    }

    fun onCategoryChange(category: StoreCategory) {
        _uiState.update { it.copy(category = category, categoryError = null, saveError = null) }
    }

    fun onStreetChange(street: String) {
        _uiState.update { it.copy(street = street, streetError = null, saveError = null) }
    }

    fun onPostcodeChange(postcode: String) {
        _uiState.update { it.copy(postcode = postcode, postcodeError = null, saveError = null) }
    }

    fun onCityChange(city: String) {
        _uiState.update { it.copy(city = city, cityError = null, saveError = null) }
    }

    // Opgeroepen na een tik op de kaart of als de pin losgelaten wordt, nooit tijdens het slepen.
    // Zet de pin en vraagt het adres op. GeocodingRepository kijkt eerst in de cache
    // en houdt zich aan max 1 aanvraag per seconde.
    fun onPinPlaced(point: MapPoint) {
        _uiState.update {
            it.copy(
                pin = point,
                locationError = null,
                saveError = null,
                isLookingUpAddress = true,
                addressAutoFilled = false,
                addressLookupError = null
            )
        }

        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            try {
                val address = geocodingRepository.getStreetAddress(point.latitude, point.longitude)
                // Een nieuwe plek is een nieuw adres: alle drie de velden worden overschreven.
                // De handelaar kan ze daarna nog aanpassen.
                _uiState.update {
                    it.copy(
                        street = address.street,
                        postcode = address.postcode.orEmpty(),
                        city = address.city,
                        streetError = null,
                        postcodeError = null,
                        cityError = null,
                        isLookingUpAddress = false,
                        addressAutoFilled = true
                    )
                }
            } catch (e: GeocodingException) {
                // De pin blijft staan, de handelaar kan het adres zelf intypen.
                _uiState.update {
                    it.copy(isLookingUpAddress = false, addressLookupError = e.toErrorMessage())
                }
            }
        }
    }

    // De kaart meldt waar ze naar kijkt wanneer het scherm pauzeert (bv. bij een rotatie).
    fun onMapPositionChange(latitude: Double, longitude: Double, zoom: Double) {
        _uiState.update { it.copy(mapCenter = MapPoint(latitude, longitude), mapZoom = zoom) }
    }

    // Het scherm heeft de melding "opgeslagen" getoond.
    fun onSavedMessageShown() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun save() {
        val state = _uiState.value
        // Tijdens het opzoeken nog niet opslaan, anders bewaren we het oude adres bij de nieuwe pin.
        if (state.isLoadingStore || state.isLookingUpAddress || state.isSaving) return

        val validated = state.validate()
        if (validated.hasFieldErrors()) {
            _uiState.value = validated
            return
        }

        val uid = authRepository.currentUserId()
        if (uid == null) {
            _uiState.update { it.copy(saveError = R.string.error_general) }
            return
        }

        // validate() heeft gecontroleerd dat category en pin niet null zijn.
        val pin = requireNotNull(state.pin)
        val store = Store(
            ownerId = uid,
            name = state.name.trim(),
            category = requireNotNull(state.category).value,
            location = GeoPoint(pin.latitude, pin.longitude),
            address = state.street.trim(),
            postalCode = state.postcode.trim(),
            city = state.city.trim()
        )

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                // Maakt de winkel aan, of overschrijft de bestaande (zelfde document-id).
                storeRepository.saveStore(store)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: FirebaseException) {
                _uiState.update { it.copy(isSaving = false, saveError = e.toErrorMessage()) }
            }
        }
    }

    companion object {
        // GeocodingRepository heeft de DAO uit Room nodig, en daarvoor een Context.
        // Een factory geeft de ViewModel die mee, zonder dat de ViewModel zelf een Context bijhoudt.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val dao = AppDatabase.getInstance(application).cachedAddressDao()
                MyStoreViewModel(GeocodingRepository(dao))
            }
        }
    }
}

// Kiest de melding bij een fout tijdens het opzoeken van een adres.
@StringRes
private fun GeocodingException.toErrorMessage(): Int = when (reason) {
    GeocodingException.Reason.NO_INTERNET -> R.string.error_no_internet
    GeocodingException.Reason.NOT_FOUND -> R.string.error_address_not_found
    GeocodingException.Reason.SERVER_ERROR -> R.string.error_address_lookup_failed
}

// Vult het formulier met een winkel uit Firestore en zet de kaart op de pin.
private fun MyStoreUiState.withStore(store: Store): MyStoreUiState {
    val pin = MapPoint(store.location.latitude, store.location.longitude)
    return copy(
        name = store.name,
        category = StoreCategory.fromValue(store.category),
        street = store.address,
        postcode = store.postalCode,
        city = store.city,
        pin = pin,
        mapCenter = pin,
        isLoadingStore = false
    )
}

// Zet een foutmelding bij elk verplicht veld dat leeg is.
private fun MyStoreUiState.validate(): MyStoreUiState = copy(
    nameError = if (name.isBlank()) R.string.error_store_name_required else null,
    categoryError = if (category == null) R.string.error_category_required else null,
    locationError = if (pin == null) R.string.error_location_required else null,
    streetError = if (street.isBlank()) R.string.error_street_required else null,
    postcodeError = if (postcode.isBlank()) R.string.error_postcode_required else null,
    cityError = if (city.isBlank()) R.string.error_city_required else null
)

private fun MyStoreUiState.hasFieldErrors(): Boolean {
    return listOf(nameError, categoryError, locationError, streetError, postcodeError, cityError)
        .any { it != null }
}

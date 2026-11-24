package com.example.mobiledevelopmentproject.ui.bag

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.AuthRepository
import com.example.mobiledevelopmentproject.data.BagRepository
import com.example.mobiledevelopmentproject.data.PhotoEncoder
import com.example.mobiledevelopmentproject.data.StoreRepository
import com.example.mobiledevelopmentproject.data.model.Bag
import com.example.mobiledevelopmentproject.data.model.Store
import com.example.mobiledevelopmentproject.ui.auth.toErrorMessage
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CreateBagUiState(
    // Formulier. Alles als tekst, pas bij opslaan zetten we het om naar getallen.
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val originalValue: String = "",
    val quantity: String = "",
    // Ophaalvenster in minuten na middernacht (17:30 = 1050), null = nog niet gekozen.
    val pickupStartMinutes: Int? = null,
    val pickupEndMinutes: Int? = null,
    // De foto als Base64-tekst, "" = nog geen foto.
    // Tekst en geen Bitmap: zo is het meteen klaar voor Firestore, en het blijft
    // samen met de rest van de state in de ViewModel staan bij een rotatie.
    val photoBase64: String = "",
    // true terwijl een foto verkleind en omgezet wordt.
    val isProcessingPhoto: Boolean = false,
    // De winkel van de handelaar, nodig om het pakket aan te koppelen.
    val store: Store? = null,
    // Laden en opslaan
    val isLoadingStore: Boolean = true,
    // true als de handelaar nog geen winkel heeft, dan kan hij nog geen pakket plaatsen.
    val hasNoStore: Boolean = false,
    val isSaving: Boolean = false,
    // true na een geslaagde opslag, het scherm gaat dan terug.
    val isSaved: Boolean = false,
    // Foutmeldingen bij een veld
    @StringRes val titleError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val priceError: Int? = null,
    @StringRes val originalValueError: Int? = null,
    @StringRes val quantityError: Int? = null,
    @StringRes val pickupError: Int? = null,
    @StringRes val photoError: Int? = null,
    // Algemene foutmeldingen
    @StringRes val loadError: Int? = null,
    @StringRes val saveError: Int? = null
)

// contentResolver is nodig om een foto uit de galerij te lezen, zie Factory onderaan.
class CreateBagViewModel(
    private val contentResolver: ContentResolver,
    private val bagRepository: BagRepository = BagRepository(),
    private val storeRepository: StoreRepository = StoreRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBagUiState())
    val uiState: StateFlow<CreateBagUiState> = _uiState.asStateFlow()

    // init loopt één keer: na een rotatie bestaat deze ViewModel nog,
    // dus de winkel wordt niet opnieuw geladen en de ingevulde velden blijven staan.
    init {
        loadStore()
    }

    // Laadt de winkel van de handelaar. Ook gebruikt door de knop "Opnieuw proberen".
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
                _uiState.update {
                    it.copy(isLoadingStore = false, store = store, hasNoStore = store == null)
                }
            } catch (e: FirebaseException) {
                _uiState.update { it.copy(isLoadingStore = false, loadError = e.toErrorMessage()) }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, titleError = null, saveError = null) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, descriptionError = null, saveError = null) }
    }

    // Prijs en originele waarde wissen allebei de fouten:
    // "de prijs moet lager zijn" hangt van beide velden af.
    fun onPriceChange(price: String) {
        _uiState.update {
            it.copy(price = price, priceError = null, originalValueError = null, saveError = null)
        }
    }

    fun onOriginalValueChange(originalValue: String) {
        _uiState.update {
            it.copy(originalValue = originalValue, priceError = null, originalValueError = null, saveError = null)
        }
    }

    fun onQuantityChange(quantity: String) {
        _uiState.update { it.copy(quantity = quantity, quantityError = null, saveError = null) }
    }

    fun onPickupStartChange(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(pickupStartMinutes = hour * 60 + minute, pickupError = null, saveError = null)
        }
    }

    fun onPickupEndChange(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(pickupEndMinutes = hour * 60 + minute, pickupError = null, saveError = null)
        }
    }

    // Foto van de camera-app. TakePicturePreview geeft meteen een (kleine) Bitmap.
    fun onPhotoTaken(bitmap: Bitmap) {
        processPhoto { PhotoEncoder.encodeBitmap(bitmap) }
    }

    // Foto uit de galerij. We krijgen enkel een Uri (een adres van de foto), geen Bitmap.
    fun onPhotoPicked(uri: Uri) {
        processPhoto { PhotoEncoder.encodeUri(contentResolver, uri) }
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(photoError = R.string.error_no_camera) }
    }

    // Zet een foto om naar Base64-tekst. encode geeft null als het niet lukt.
    private fun processPhoto(encode: () -> String?) {
        // Een nieuwe foto wist de oude foutmeldingen.
        _uiState.update { it.copy(isProcessingPhoto = true, photoError = null, saveError = null) }

        viewModelScope.launch {
            // Verkleinen en comprimeren is zwaar rekenwerk. Op de main thread
            // zou het scherm haperen, daarom op Dispatchers.Default.
            val base64 = withContext(Dispatchers.Default) { encode() }
            _uiState.update {
                if (base64 == null) {
                    // De vorige foto (als die er was) blijft staan.
                    it.copy(isProcessingPhoto = false, photoError = R.string.error_photo_failed)
                } else {
                    it.copy(isProcessingPhoto = false, photoBase64 = base64)
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        // Tijdens het omzetten van een foto nog niet opslaan, anders bewaren we de oude foto.
        if (state.isLoadingStore || state.isSaving || state.isProcessingPhoto) return

        // Het huidige uur, om te controleren dat het einduur nog niet voorbij is.
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val validated = state.validate(nowMinutes)
        if (validated.hasFieldErrors()) {
            _uiState.value = validated
            return
        }

        // Zonder winkel toont het scherm het formulier niet, dit is enkel een vangnet.
        val store = state.store
        if (store == null) {
            _uiState.update { it.copy(saveError = R.string.error_general) }
            return
        }

        // validate() heeft gecontroleerd dat deze waarden geldig zijn.
        val quantity = requireNotNull(state.quantity.trim().toIntOrNull())
        val bag = Bag(
            storeId = store.id,
            // Kopie van de winkel, zo moet de klant de winkel niet apart opvragen.
            storeName = store.name,
            location = store.location,
            title = state.title.trim(),
            description = state.description.trim(),
            price = requireNotNull(parseAmount(state.price)),
            originalValue = requireNotNull(parseAmount(state.originalValue)),
            quantityTotal = quantity,
            quantityLeft = quantity,
            pickupStart = todayAt(requireNotNull(state.pickupStartMinutes)),
            pickupEnd = todayAt(requireNotNull(state.pickupEndMinutes)),
            active = true,
            photoBase64 = state.photoBase64,
            createdAt = System.currentTimeMillis()
        )

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                bagRepository.addBag(bag)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: FirebaseException) {
                _uiState.update { it.copy(isSaving = false, saveError = e.toErrorMessage()) }
            }
        }
    }

    companion object {
        // Een ViewModel mag geen Activity of Context bijhouden: na een rotatie wordt de
        // Activity vernietigd, maar de ViewModel leeft verder en zou de oude Activity
        // in het geheugen vasthouden (geheugenlek). De Application leeft zo lang als de app,
        // dus haar ContentResolver mag de ViewModel wel bijhouden.
        // De factory haalt de Application op en geeft de ContentResolver mee.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                CreateBagViewModel(application.contentResolver)
            }
        }
    }
}

// Leest een bedrag. Aanvaardt zowel "4,50" (Belgische schrijfwijze) als "4.50".
// Geeft null als het geen getal is.
private fun parseAmount(text: String): Double? {
    return text.trim().replace(',', '.').toDoubleOrNull()
}

// Maakt een Timestamp van vandaag op het gekozen uur (minuten na middernacht).
private fun todayAt(minutesAfterMidnight: Int): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60)
    calendar.set(Calendar.MINUTE, minutesAfterMidnight % 60)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return Timestamp(calendar.time)
}

// Zet een foutmelding bij elk veld dat niet in orde is.
// nowMinutes is het huidige uur in minuten na middernacht.
private fun CreateBagUiState.validate(nowMinutes: Int): CreateBagUiState {
    val priceValue = parseAmount(price)
    val originalValueNumber = parseAmount(originalValue)
    val quantityNumber = quantity.trim().toIntOrNull()
    val start = pickupStartMinutes
    val end = pickupEndMinutes

    val originalValueValid = originalValueNumber != null && originalValueNumber > 0
    // Enkel vergelijken als de originele waarde zelf geldig is.
    val priceNotLower = originalValueNumber != null && originalValueNumber > 0 &&
        priceValue != null && priceValue >= originalValueNumber

    return copy(
        titleError = if (title.isBlank()) R.string.error_bag_title_required else null,
        descriptionError = if (description.isBlank()) R.string.error_bag_description_required else null,
        priceError = when {
            priceValue == null || priceValue <= 0 -> R.string.error_price_invalid
            priceNotLower -> R.string.error_price_not_lower
            else -> null
        },
        originalValueError = if (originalValueValid) null else R.string.error_original_value_invalid,
        quantityError = if (quantityNumber == null || quantityNumber < 1) R.string.error_quantity_invalid else null,
        pickupError = when {
            start == null || end == null -> R.string.error_pickup_required
            end <= start -> R.string.error_pickup_end_before_start
            // Het venster is vandaag: een einduur dat al voorbij is heeft geen zin.
            end <= nowMinutes -> R.string.error_pickup_end_passed
            else -> null
        },
        // De foto is verplicht: de klant ziet ze in de lijst en op het detailscherm.
        photoError = if (photoBase64.isEmpty()) R.string.error_photo_required else null
    )
}

private fun CreateBagUiState.hasFieldErrors(): Boolean {
    return listOf(
        titleError, descriptionError, priceError, originalValueError, quantityError, pickupError, photoError
    ).any { it != null }
}

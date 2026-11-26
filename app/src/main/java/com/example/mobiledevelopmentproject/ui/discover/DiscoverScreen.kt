package com.example.mobiledevelopmentproject.ui.discover

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.ui.customer.CustomerHomeScreen
import com.example.mobiledevelopmentproject.ui.customer.CustomerHomeViewModel
import com.example.mobiledevelopmentproject.ui.map.LOCATION_PERMISSIONS
import com.example.mobiledevelopmentproject.ui.map.LoadingLocationCard
import com.example.mobiledevelopmentproject.ui.map.LocationMode
import com.example.mobiledevelopmentproject.ui.map.LocationRationale
import com.example.mobiledevelopmentproject.ui.map.LocationUnavailableCard
import com.example.mobiledevelopmentproject.ui.map.MapUiState
import com.example.mobiledevelopmentproject.ui.map.MapViewModel
import com.example.mobiledevelopmentproject.ui.map.PickLocationContent

// De tab Ontdekken.
// - Kop bovenaan (DiscoverHeader): "Pakketten in de buurt van {gemeente}", Wijzig en uitloggen.
// - Met een locatie: daaronder het deel van Mohamed (CustomerHomeScreen), later de lijst met pakketten.
// - Zonder locatie: de locatie-flow van de Kaart (uitleg, laden, geen locatie).
// - Pin-kaart (na "Wijzig" of "Zelf kiezen op de kaart"): met of zonder locatie.
//
// mapViewModel is dezelfde ViewModel als die van de Kaart-tab (gemaakt in CustomerMainScreen).
// Zo tonen Ontdekken en Kaart altijd dezelfde locatie, en is de locatie-logica er maar één keer.
@Composable
fun DiscoverScreen(
    name: String,
    mapViewModel: MapViewModel,
    onLoggedOut: () -> Unit,
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory),
    // Dezelfde ViewModel als in CustomerHomeScreen (zelfde tab, dus zelfde instantie).
    homeViewModel: CustomerHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()

    // Toestemming vragen, net zoals op de Kaart: enkel na een tik van de klant.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        mapViewModel.onPermissionResult(
            fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        )
    }
    val requestLocation = { permissionLauncher.launch(LOCATION_PERMISSIONS) }

    // Melding na "Gebruik deze locatie", zoals op de Kaart.
    val snackbarHostState = remember { SnackbarHostState() }
    val pinSavedMessage = stringResource(R.string.location_pin_saved)
    LaunchedEffect(mapState.isPinSaved) {
        if (mapState.isPinSaved) {
            snackbarHostState.showSnackbar(pinSavedMessage)
            mapViewModel.onPinSavedMessageShown()
        }
    }

    // De pin-kaart is open: na "Wijzig", "Zelf kiezen op de kaart" of een weigering.
    val isPicking = mapState.mode == LocationMode.PICK_PIN

    // De terugknop van het toestel annuleert de pin-kaart, net als de pijl terug.
    BackHandler(enabled = isPicking) {
        mapViewModel.onPickPinBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        // consumeWindowInsets: CustomerHomeScreen heeft een eigen Scaffold. Zonder dit
        // telt die de ruimte voor de statusbalk nog eens bij, met een gat onder de kop.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // Zelfde volgorde als de uitlogknop van Mohamed: eerst uitloggen, dan navigeren.
            val logout = {
                homeViewModel.logout()
                onLoggedOut()
            }

            // De kop als aparte rij: met een locatie, en op de pin-kaart (daar neemt het kaartje
            // "Kies je locatie" bovenaan de hele breedte in). In de andere gevallen zonder locatie
            // staat enkel het uitlogicoon rechtsboven over de inhoud: een rij zou de uitleg
            // op een liggend scherm te weinig plaats geven.
            val showHeaderRow = uiState.hasLocation || isPicking
            if (showHeaderRow) {
                DiscoverHeader(
                    state = uiState,
                    // Tijdens het kiezen heeft "Wijzig" geen zin.
                    showChangeButton = !isPicking,
                    // "Wijzig" opent de pin-kaart hier in Ontdekken, de pin start op de huidige locatie.
                    onChangeLocationClick = mapViewModel::onChooseOnMap,
                    onRetryClick = viewModel::onRetryCity,
                    onLogoutClick = logout
                )
            }

            val contentModifier = Modifier
                .weight(1f)
                .fillMaxWidth()

            if (isPicking) {
                // Dezelfde pin-kaart als op de Kaart. "Gebruik deze locatie" bewaart de pin,
                // de pijl terug annuleert zonder iets te wijzigen.
                PickLocationContent(
                    pin = mapState.pin,
                    center = mapState.mapCenter,
                    zoom = mapState.mapZoom,
                    permissionDenied = mapState.permissionDenied,
                    onPinPlaced = mapViewModel::onPinPlaced,
                    onPositionChange = mapViewModel::onMapPositionChange,
                    onBackClick = mapViewModel::onPickPinBack,
                    onUsePinClick = mapViewModel::onUsePin,
                    modifier = contentModifier
                )
            } else if (uiState.hasLocation) {
                Column(modifier = contentModifier) {
                    // De locatie wordt opnieuw opgehaald, bv. na "Precieze locatie toestaan".
                    if (mapState.mode == LocationMode.LOADING) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (uiState.isApproximate) {
                        ApproximateLocationBanner(
                            onAllowPreciseClick = requestLocation,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // Hier komt de lijst van Mohamed. Zijn scherm (met uitlogknop) blijft ongewijzigd.
                    Box(modifier = Modifier.weight(1f)) {
                        CustomerHomeScreen(name = name, onLoggedOut = onLoggedOut, viewModel = homeViewModel)
                    }
                }
            } else {
                Box(modifier = contentModifier) {
                    NoLocationContent(
                        mapState = mapState,
                        mapViewModel = mapViewModel,
                        onAllowClick = requestLocation,
                        modifier = Modifier.fillMaxSize()
                    )
                    LogoutIconButton(
                        onClick = logout,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// Zolang er geen locatie is: dezelfde stappen als op de Kaart, met dezelfde composables.
// (De pin-kaart staat hierboven in DiscoverScreen, want die kan ook mét locatie open zijn.)
@Composable
private fun NoLocationContent(
    mapState: MapUiState,
    mapViewModel: MapViewModel,
    onAllowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = mapState.mode
    val locationError = mapState.locationError

    when {
        mode == LocationMode.LOADING -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            LoadingLocationCard()
        }
        mode == LocationMode.UNAVAILABLE && locationError != null -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            LocationUnavailableCard(
                messageRes = locationError,
                onRetryClick = mapViewModel::onRetryLocation,
                onChooseOnMapClick = mapViewModel::onChooseOnMap
            )
        }
        // ASK: de uitleg met "Locatie toestaan" en "Zelf kiezen op de kaart".
        else -> LocationRationale(
            onAllowClick = onAllowClick,
            onChooseOnMapClick = mapViewModel::onChooseOnMap,
            modifier = modifier
        )
    }
}

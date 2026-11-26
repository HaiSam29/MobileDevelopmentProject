package com.example.mobiledevelopmentproject.ui.map

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.model.MapPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

// FINE en COARSE altijd samen vragen: dan kiest de gebruiker zelf tussen precies en benadering.
// Ook gebruikt in Ontdekken.
val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Activity Result API: Android toont het dialoogvenster en geeft per permissie terug
    // of ze toegestaan is. Bij "Bij benadering" is enkel COARSE true.
    // De launcher wordt na een rotatie opnieuw geregistreerd, het antwoord komt dus altijd aan.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onPermissionResult(
            fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        )
    }
    // Enkel na een tik van de gebruiker. Is de toestemming er al,
    // dan toont Android geen venster en komt het antwoord meteen terug.
    val requestLocation = { permissionLauncher.launch(LOCATION_PERMISSIONS) }

    val snackbarHostState = remember { SnackbarHostState() }
    val pinSavedMessage = stringResource(R.string.location_pin_saved)

    // Na "Gebruik deze locatie" één keer een melding tonen.
    // onPinSavedMessageShown pas daarna, anders stopt de LaunchedEffect de melding meteen.
    LaunchedEffect(uiState.isPinSaved) {
        if (uiState.isPinSaved) {
            snackbarHostState.showSnackbar(pinSavedMessage)
            viewModel.onPinSavedMessageShown()
        }
    }

    // De terugknop van het toestel annuleert de pin-kaart, net als de pijl terug.
    BackHandler(enabled = uiState.mode == LocationMode.PICK_PIN) {
        viewModel.onPickPinBack()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        if (uiState.mode == LocationMode.ASK) {
            LocationRationale(
                onAllowClick = requestLocation,
                onChooseOnMapClick = viewModel::onChooseOnMap,
                modifier = contentModifier
            )
        } else if (uiState.mode == LocationMode.PICK_PIN) {
            PickLocationContent(
                pin = uiState.pin,
                center = uiState.mapCenter,
                zoom = uiState.mapZoom,
                permissionDenied = uiState.permissionDenied,
                onPinPlaced = viewModel::onPinPlaced,
                onPositionChange = viewModel::onMapPositionChange,
                onBackClick = viewModel::onPickPinBack,
                onUsePinClick = viewModel::onUsePin,
                modifier = contentModifier
            )
        } else {
            // Eén OsmMap voor laden, gekozen locatie en geen locatie: zo blijft het dezelfde
            // MapView als de mode wisselt, en springt de kaart niet terug naar het startpunt.
            Box(modifier = contentModifier) {
                OsmMap(
                    latitude = uiState.mapCenter.latitude,
                    longitude = uiState.mapCenter.longitude,
                    zoom = uiState.mapZoom,
                    modifier = Modifier.fillMaxSize(),
                    onPositionChange = viewModel::onMapPositionChange,
                    update = { mapView ->
                        // Hooguit één van beide is niet null: de eigen locatie of de gekozen pin.
                        showMyLocation(mapView, uiState.myLocation)
                        showChosenPin(mapView, uiState.chosenPin)
                        uiState.centerOn?.let { point ->
                            centerMap(mapView, point, uiState.mapZoom)
                            viewModel.onCentered()
                        }
                    }
                )

                // Eigen val, want uiState is een gedelegeerde property ("by") en kan niet smart-cast worden.
                val locationError = uiState.locationError

                if (uiState.mode == LocationMode.LOADING) {
                    LoadingLocationCard(modifier = Modifier.align(Alignment.TopCenter))
                } else if (uiState.mode == LocationMode.UNAVAILABLE && locationError != null) {
                    LocationUnavailableCard(
                        messageRes = locationError,
                        onRetryClick = viewModel::onRetryLocation,
                        onChooseOnMapClick = viewModel::onChooseOnMap,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Zelf een plek kiezen, ook met toestemming. Opent de pin-kaart.
                // Niet bij UNAVAILABLE: daar staat dezelfde knop al in het kaartje.
                if (uiState.mode != LocationMode.UNAVAILABLE) {
                    ExtendedFloatingActionButton(
                        onClick = viewModel::onChooseOnMap,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 40.dp)
                    ) {
                        Text(text = stringResource(R.string.button_choose_on_map), fontWeight = FontWeight.SemiBold)
                    }
                }

                // Terug naar de eigen locatie (ook na een gekozen pin) en de kaart erop centreren.
                // Extra ruimte onderaan: de bronvermelding van OpenStreetMap staat rechtsonder
                // en moet zichtbaar blijven.
                FloatingActionButton(
                    onClick = requestLocation,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_pin),
                        contentDescription = stringResource(R.string.action_my_location)
                    )
                }
            }
        }
    }
}

// Verplaatst de kaart naar point. Vóór de eerste layout kent de MapView zijn grootte nog niet,
// dan zetten we het midden meteen in plaats van te animeren.
private fun centerMap(mapView: MapView, point: MapPoint, zoom: Double) {
    val geoPoint = GeoPoint(point.latitude, point.longitude)
    if (mapView.isLaidOut) {
        mapView.controller.animateTo(geoPoint, zoom, null)
    } else {
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(geoPoint)
    }
}

// Kaartje bovenaan zolang de locatie opgehaald wordt. Ook gebruikt in Ontdekken.
@Composable
fun LoadingLocationCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(text = stringResource(R.string.location_loading), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Kaartje bovenaan als er geen locatie is (locatie staat uit, of nog niet gevonden). Ook gebruikt in Ontdekken.
// De klant kan opnieuw proberen of zelf een plek kiezen: hij zit nooit vast.
@Composable
fun LocationUnavailableCard(
    @StringRes messageRes: Int,
    onRetryClick: () -> Unit,
    onChooseOnMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onRetryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = stringResource(R.string.button_retry), fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onChooseOnMapClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.button_choose_on_map),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

package com.example.mobiledevelopmentproject.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.model.MapPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

// FINE en COARSE altijd samen vragen: dan kiest de gebruiker zelf tussen precies en benadering.
private val LOCATION_PERMISSIONS = arrayOf(
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

    Scaffold { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        if (uiState.mode == LocationMode.ASK) {
            LocationRationale(
                onAllowClick = requestLocation,
                onChooseOnMapClick = viewModel::onChooseOnMap,
                modifier = contentModifier
            )
        } else {
            // Eén OsmMap voor laden, eigen locatie en geen locatie: zo blijft het dezelfde
            // MapView als de mode wisselt, en springt de kaart niet terug naar het startpunt.
            Box(modifier = contentModifier) {
                OsmMap(
                    latitude = uiState.mapCenter.latitude,
                    longitude = uiState.mapCenter.longitude,
                    zoom = uiState.mapZoom,
                    modifier = Modifier.fillMaxSize(),
                    onPositionChange = viewModel::onMapPositionChange,
                    update = { mapView ->
                        showMyLocation(mapView, uiState.myLocation)
                        uiState.centerOn?.let { point ->
                            centerMap(mapView, point, uiState.mapZoom)
                            viewModel.onCentered()
                        }
                    }
                )

                if (uiState.mode == LocationMode.LOADING) {
                    LoadingLocationCard(modifier = Modifier.align(Alignment.TopCenter))
                }

                // Opnieuw je locatie ophalen en de kaart erop centreren, bv. na verschuiven.
                FloatingActionButton(
                    onClick = requestLocation,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
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

// Kaartje bovenaan zolang de locatie opgehaald wordt.
@Composable
private fun LoadingLocationCard(modifier: Modifier = Modifier) {
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

package com.example.mobiledevelopmentproject.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay

// Herbruikbare kaart. latitude, longitude en zoom bepalen enkel de startpositie:
// een andere waarde doorgeven verplaatst een kaart die al bestaat niet.
// onPositionChange krijgt de huidige positie wanneer het scherm gepauzeerd wordt.
@Composable
fun OsmMap(
    latitude: Double,
    longitude: Double,
    zoom: Double,
    modifier: Modifier = Modifier,
    onPositionChange: (latitude: Double, longitude: Double, zoom: Double) -> Unit = { _, _, _ -> }
) {
    val mapView = rememberMapView(latitude, longitude, zoom, onPositionChange)

    // clipToBounds: osmdroid tekent anders tegels buiten zijn eigen vlak.
    AndroidView(
        factory = { mapView },
        modifier = modifier.clipToBounds()
    )
}

// Maakt een MapView die de levenscyclus van het scherm volgt:
// onResume en onPause samen met het scherm, onDetach wanneer het scherm verdwijnt.
@Composable
private fun rememberMapView(
    latitude: Double,
    longitude: Double,
    zoom: Double,
    onPositionChange: (latitude: Double, longitude: Double, zoom: Double) -> Unit
): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // De observer blijft bestaan over recomposities heen en moet toch de laatste callback gebruiken.
    val currentOnPositionChange by rememberUpdatedState(onPositionChange)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(latitude, longitude))
            // De gebruiksvoorwaarden van OpenStreetMap vereisen een zichtbare bronvermelding.
            overlays.add(CopyrightOverlay(context))
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    val center = mapView.mapCenter
                    currentOnPositionChange(center.latitude, center.longitude, mapView.zoomLevelDouble)
                    mapView.onPause()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}

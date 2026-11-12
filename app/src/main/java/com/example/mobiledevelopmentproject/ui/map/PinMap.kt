package com.example.mobiledevelopmentproject.ui.map

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

// Kaart waarop de gebruiker één pin zet met een tik, of de pin versleept
// (lang drukken op de pin en dan slepen).
// onPinPlaced wordt enkel opgeroepen na een tik of als de pin losgelaten wordt,
// nooit tijdens het slepen of het verschuiven van de kaart. Zo vraagt de ViewModel
// niet bij elke beweging een adres op.
// De pin zelf staat in de ViewModel: deze composable tekent enkel wat in pin staat.
@Composable
fun PinMap(
    pin: MapPoint?,
    center: MapPoint,
    zoom: Double,
    onPinPlaced: (MapPoint) -> Unit,
    modifier: Modifier = Modifier,
    onPositionChange: (latitude: Double, longitude: Double, zoom: Double) -> Unit = { _, _, _ -> }
) {
    // De overlays blijven bestaan over recomposities heen en moeten toch de laatste callback gebruiken.
    val currentOnPinPlaced by rememberUpdatedState(onPinPlaced)

    OsmMap(
        latitude = center.latitude,
        longitude = center.longitude,
        zoom = zoom,
        modifier = modifier,
        onPositionChange = onPositionChange,
        update = { mapView ->
            // De eerste keer (en na een rotatie, want dan is er een nieuwe MapView)
            // is er nog geen tik-overlay.
            if (mapView.overlays.none { it is MapEventsOverlay }) {
                addTapListener(mapView) { currentOnPinPlaced(it) }
                keepTouchesInMap(mapView)
            }
            showPin(mapView, pin) { currentOnPinPlaced(it) }
        }
    )
}

// Een tik op de kaart zet de pin op die plek.
private fun addTapListener(mapView: MapView, onTap: (MapPoint) -> Unit) {
    val receiver = object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
            onTap(MapPoint(point.latitude, point.longitude))
            return true
        }

        // false: lang drukken laten we over aan de pin, om die te verslepen.
        override fun longPressHelper(point: GeoPoint): Boolean = false
    }
    // Index 0 = onderste overlay. De pin ligt erboven en krijgt een tik dus eerst.
    mapView.overlays.add(0, MapEventsOverlay(receiver))
}

// De kaart staat in een scrollbaar formulier. Zonder dit pikt het formulier
// het slepen af en scrollt het scherm in plaats van de kaart.
@SuppressLint("ClickableViewAccessibility")
private fun keepTouchesInMap(mapView: MapView) {
    mapView.setOnTouchListener { view, _ ->
        view.parent.requestDisallowInterceptTouchEvent(true)
        // false: de MapView verwerkt de aanraking daarna zelf nog.
        false
    }
}

// Zet de pin op de juiste plek, maakt hem aan als hij er nog niet is,
// of haalt hem weg als pin null is.
private fun showPin(mapView: MapView, pin: MapPoint?, onDragEnd: (MapPoint) -> Unit) {
    var marker = mapView.overlays.filterIsInstance<Marker>().firstOrNull()

    if (pin == null) {
        marker?.let { mapView.overlays.remove(it) }
    } else {
        if (marker == null) {
            marker = createMarker(mapView, onDragEnd)
            mapView.overlays.add(marker)
        }
        marker.position = GeoPoint(pin.latitude, pin.longitude)
    }
    // Opnieuw tekenen, anders zie je de nieuwe plek pas bij de volgende beweging.
    mapView.invalidate()
}

private fun createMarker(mapView: MapView, onDragEnd: (MapPoint) -> Unit): Marker {
    return Marker(mapView).apply {
        // De punt onderaan de pin staat op de gekozen plek.
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        isDraggable = true
        // Geen tekstballon bij een tik op de pin.
        infoWindow = null
        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}

            override fun onMarkerDrag(marker: Marker) {}

            override fun onMarkerDragEnd(marker: Marker) {
                onDragEnd(MapPoint(marker.position.latitude, marker.position.longitude))
            }
        })
    }
}

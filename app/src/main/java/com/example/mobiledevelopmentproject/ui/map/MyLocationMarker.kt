package com.example.mobiledevelopmentproject.ui.map

import androidx.core.content.ContextCompat
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.model.MapPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Herkent de marker van de eigen locatie tussen de andere markers (bv. de pin).
private const val MY_LOCATION_MARKER_ID = "my_location"

// Toont de eigen locatie als blauwe stip. Bedoeld voor de update-callback van OsmMap:
// zet de stip op de juiste plek, maakt hem aan als hij er nog niet is,
// of haalt hem weg als location null is.
fun showMyLocation(mapView: MapView, location: MapPoint?) {
    var marker = mapView.overlays.filterIsInstance<Marker>().firstOrNull { it.id == MY_LOCATION_MARKER_ID }

    if (location == null) {
        marker?.let { mapView.overlays.remove(it) }
    } else {
        if (marker == null) {
            marker = createMyLocationMarker(mapView)
            mapView.overlays.add(marker)
        }
        marker.position = GeoPoint(location.latitude, location.longitude)
    }
    // Opnieuw tekenen, anders zie je de nieuwe plek pas bij de volgende beweging.
    mapView.invalidate()
}

private fun createMyLocationMarker(mapView: MapView): Marker {
    return Marker(mapView).apply {
        id = MY_LOCATION_MARKER_ID
        icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_my_location_dot)
        // Het midden van de stip staat op de locatie (bij de pin is dat de punt onderaan).
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        // Geen tekstballon bij een tik op de stip.
        infoWindow = null
        // true: de tik is afgehandeld. Anders verschuift osmdroid de kaart naar de stip.
        setOnMarkerClickListener { _, _ -> true }
    }
}

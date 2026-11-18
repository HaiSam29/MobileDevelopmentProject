package com.example.mobiledevelopmentproject.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.mobiledevelopmentproject.data.model.MapPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// Zo lang wachten we op een nieuwe locatie, daarna proberen we de laatst gekende.
private const val TIMEOUT_MILLIS = 10_000L

// Een laatst gekende locatie die ouder is, gebruiken we niet: de klant kan intussen elders zijn.
private const val MAX_LAST_KNOWN_AGE_MILLIS = 30 * 60 * 1000L

// Het resultaat van getCurrentLocation. Met een sealed interface moet de ViewModel
// in een when elk geval afhandelen, ook de gevallen zonder locatie.
sealed interface LocationResult {
    // approximate = true bij toestemming "Bij benadering".
    data class Found(val point: MapPoint, val approximate: Boolean) : LocationResult

    // Locatie staat uit in de instellingen van het toestel.
    data object LocationOff : LocationResult

    // Locatie staat aan, maar er kwam binnen de tijd geen locatie binnen (bv. binnen, geen GPS-fix).
    data object NotFound : LocationResult

    // Geen toestemming (meer), bv. ingetrokken in de instellingen terwijl de app open stond.
    data object NoPermission : LocationResult
}

// Haalt één keer de huidige locatie op met de LocationManager van Android.
// Krijgt de application context: die leeft even lang als de app,
// zodat deze klasse geen Activity vasthoudt die na een rotatie al weg is.
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    // true als de gebruiker "Precies" koos.
    fun hasPreciseLocation(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

    // true bij "Precies" of "Bij benadering". Bij precies krijg je altijd ook COARSE.
    fun hasAnyLocation(): Boolean = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    // MissingPermission: we controleren de toestemming bovenaan zelf,
    // en vangen een SecurityException op als ze intussen toch weg is.
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult {
        val precise = hasPreciseLocation()
        if (!precise && !hasAnyLocation()) return LocationResult.NoPermission

        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return LocationResult.LocationOff
        val providers = usableProviders(precise)
        if (providers.isEmpty()) return LocationResult.LocationOff

        return try {
            // Eerst een nieuwe locatie vragen aan de beste provider. Komt er niets binnen
            // de tijd, dan de recentste laatst gekende locatie van alle bruikbare providers.
            val location = withTimeoutOrNull(TIMEOUT_MILLIS) { requestLocation(providers.first()) }
                ?: lastKnownLocation(providers)

            if (location == null) {
                LocationResult.NotFound
            } else {
                LocationResult.Found(MapPoint(location.latitude, location.longitude), approximate = !precise)
            }
        } catch (e: SecurityException) {
            LocationResult.NoPermission
        }
    }

    // De providers die aanstaan en die we met deze toestemming mogen gebruiken, de beste eerst.
    // GPS is het nauwkeurigst. Vóór Android 12 vraagt GPS altijd FINE.
    // Vanaf Android 12 mag het ook met enkel COARSE, dan maakt Android de locatie zelf minder precies.
    private fun usableProviders(precise: Boolean): List<String> {
        val candidates = if (precise || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        return candidates.filter { locationManager.isProviderEnabled(it) }
    }

    // LocationManagerCompat.getCurrentLocation werkt met een callback.
    // suspendCancellableCoroutine maakt er een suspend-functie van: de coroutine wacht
    // tot de callback komt. Wordt de coroutine gestopt (bv. door de timeout),
    // dan stopt de CancellationSignal ook de aanvraag bij Android.
    @SuppressLint("MissingPermission")
    private suspend fun requestLocation(provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(appContext)
            ) { location ->
                // location kan null zijn als Android geen locatie vond.
                continuation.resume(location)
            }
        }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(providers: List<String>): Location? {
        val now = System.currentTimeMillis()
        return providers
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .filter { now - it.time <= MAX_LAST_KNOWN_AGE_MILLIS }
            .maxByOrNull { it.time }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }
}

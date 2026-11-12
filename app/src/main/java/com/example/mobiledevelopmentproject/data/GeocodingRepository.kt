package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.local.CachedAddress
import com.example.mobiledevelopmentproject.data.local.CachedAddressDao
import com.example.mobiledevelopmentproject.data.model.GeocodedAddress
import com.example.mobiledevelopmentproject.data.remote.NominatimApi
import com.example.mobiledevelopmentproject.data.remote.NominatimClient
import com.example.mobiledevelopmentproject.data.remote.NominatimResponse
import com.example.mobiledevelopmentproject.data.remote.RateLimiter
import com.example.mobiledevelopmentproject.data.remote.ZOOM_CITY
import com.example.mobiledevelopmentproject.data.remote.ZOOM_STREET
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

// Zet coördinaten om in een adres (reverse geocoding).
// Eerst kijken we in Room, pas als het adres daar niet staat vragen we het aan Nominatim.
// Bij een fout gooit elke functie een GeocodingException, de ViewModel vangt die op.
// api en rateLimiter zijn parameters zodat een test er een nep-versie in kan steken.
class GeocodingRepository(
    private val dao: CachedAddressDao,
    private val api: NominatimApi = NominatimClient.api,
    private val rateLimiter: RateLimiter = NominatimClient.rateLimiter
) {

    // Voor "Mijn winkel": straat + huisnummer, postcode en gemeente.
    suspend fun getStreetAddress(lat: Double, lon: Double): GeocodedAddress {
        val cached = lookup(lat, lon, ZOOM_STREET)
        return GeocodedAddress(
            street = cached.displayName,
            postcode = cached.postcode,
            city = cached.city
        )
    }

    // Voor "Pakketten in de buurt van {gemeente}": enkel de gemeente.
    suspend fun getCity(lat: Double, lon: Double): String {
        return lookup(lat, lon, ZOOM_CITY).city
    }

    private suspend fun lookup(lat: Double, lon: Double, zoom: Int): CachedAddress {
        val key = cacheKey(lat, lon, zoom)

        // Staat het al in de cache, dan hoeven we niet te wachten op de limiter.
        dao.get(key)?.let { return it }

        return rateLimiter.run {
            // Nog eens kijken: misschien vroeg een andere aanvraag dezelfde plek op terwijl we wachtten.
            dao.get(key) ?: fetchAndCache(key, lat, lon, zoom)
        }
    }

    private suspend fun fetchAndCache(key: String, lat: Double, lon: Double, zoom: Int): CachedAddress {
        val response = try {
            api.reverse(lat = lat, lon = lon, zoom = zoom)
        } catch (e: IOException) {
            // Geen internet of timeout.
            throw GeocodingException(GeocodingException.Reason.NO_INTERNET, e)
        } catch (e: HttpException) {
            // Nominatim antwoordde met een foutcode, bv. 429 (te veel aanvragen) of 500.
            throw GeocodingException(GeocodingException.Reason.SERVER_ERROR, e)
        } catch (e: JsonParseException) {
            // Het antwoord was geen geldige JSON.
            throw GeocodingException(GeocodingException.Reason.SERVER_ERROR, e)
        }

        val address = toCachedAddress(key, response, zoom, System.currentTimeMillis())
            ?: throw GeocodingException(GeocodingException.Reason.NOT_FOUND)

        // Enkel geslaagde resultaten bewaren, zodat een volgende poging na een fout opnieuw kan.
        dao.upsert(address)
        return address
    }
}

// Locale.ROOT zorgt voor een punt als decimaalteken. Met een Nederlandse taal
// op de telefoon zou %.4f anders "51,2194" geven.
internal fun cacheKey(lat: Double, lon: Double, zoom: Int): String {
    return String.format(Locale.ROOT, "z%d:%.4f,%.4f", zoom, lat, lon)
}

// Zet het antwoord van Nominatim om naar een rij voor de cache.
// Geeft null als er geen adres of gemeente gevonden is.
internal fun toCachedAddress(
    key: String,
    response: NominatimResponse,
    zoom: Int,
    fetchedAt: Long
): CachedAddress? {
    if (response.error != null) return null
    val address = response.address ?: return null

    // Afhankelijk van de grootte van de plaats geeft Nominatim city, town of village.
    val city = address.city ?: address.town ?: address.village ?: return null

    return if (zoom == ZOOM_STREET) {
        // "Meir 12", of enkel "Meir" als er geen huisnummer is.
        val street = listOfNotNull(address.road, address.houseNumber).joinToString(" ")
        CachedAddress(key, displayName = street, postcode = address.postcode, city = city, fetchedAt = fetchedAt)
    } else {
        CachedAddress(key, displayName = city, postcode = null, city = city, fetchedAt = fetchedAt)
    }
}

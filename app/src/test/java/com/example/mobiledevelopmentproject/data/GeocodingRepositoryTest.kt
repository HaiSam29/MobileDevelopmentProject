package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.local.CachedAddress
import com.example.mobiledevelopmentproject.data.local.CachedAddressDao
import com.example.mobiledevelopmentproject.data.remote.NominatimAddress
import com.example.mobiledevelopmentproject.data.remote.NominatimApi
import com.example.mobiledevelopmentproject.data.remote.NominatimResponse
import com.example.mobiledevelopmentproject.data.remote.RateLimiter
import com.example.mobiledevelopmentproject.data.remote.ZOOM_CITY
import com.example.mobiledevelopmentproject.data.remote.ZOOM_STREET
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Locale

// Nep-DAO: een gewone Map in plaats van een echte database, zo kan de test zonder emulator.
private class FakeCachedAddressDao : CachedAddressDao {
    val rows = mutableMapOf<String, CachedAddress>()

    override suspend fun get(key: String): CachedAddress? = rows[key]

    override suspend fun upsert(address: CachedAddress) {
        rows[address.key] = address
    }
}

// Nep-API: telt de aanvragen en geeft terug (of gooit) wat de test kiest.
private class FakeNominatimApi : NominatimApi {
    var calls = 0
    var delayMs = 0L
    var answer: () -> NominatimResponse = { antwerpResponse() }

    override suspend fun reverse(
        lat: Double,
        lon: Double,
        zoom: Int,
        format: String,
        addressDetails: Int,
        language: String
    ): NominatimResponse {
        calls++
        delay(delayMs)
        return answer()
    }
}

private fun antwerpResponse() = NominatimResponse(
    displayName = "12, Meir, Antwerpen, 2000, België",
    address = NominatimAddress(
        road = "Meir",
        houseNumber = "12",
        postcode = "2000",
        city = "Antwerpen",
        town = null,
        village = null
    ),
    error = null
)

private fun addressWith(city: String? = null, town: String? = null, village: String? = null) =
    NominatimAddress(road = "Kerkstraat", houseNumber = null, postcode = "9999", city = city, town = town, village = village)

@OptIn(ExperimentalCoroutinesApi::class)
class GeocodingRepositoryTest {

    private val dao = FakeCachedAddressDao()
    private val api = FakeNominatimApi()

    private fun TestScope.newRepository() =
        GeocodingRepository(dao, api, RateLimiter(intervalMs = 1_000, now = { currentTime }))

    // Hulpfunctie: voert block uit en geeft de reden van de GeocodingException terug.
    private suspend fun failureReason(block: suspend () -> Unit): GeocodingException.Reason {
        try {
            block()
        } catch (e: GeocodingException) {
            return e.reason
        }
        fail("Er werd een GeocodingException verwacht")
        throw IllegalStateException()
    }

    // --- Sleutel van de cache ---

    @Test
    fun cacheKeyRoundsToFourDecimals() {
        assertEquals("z18:51.2194,4.4025", cacheKey(51.219448, 4.402464, ZOOM_STREET))
        assertEquals("z10:-33.8688,151.2093", cacheKey(-33.86882, 151.20929, ZOOM_CITY))
    }

    @Test
    fun cacheKeyUsesDotWithDutchLanguage() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("nl-BE"))
        try {
            assertEquals("z18:51.2194,4.4025", cacheKey(51.2194, 4.4025, ZOOM_STREET))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun cacheKeyIsDifferentForStreetAndCity() {
        assertTrue(cacheKey(51.2194, 4.4025, ZOOM_STREET) != cacheKey(51.2194, 4.4025, ZOOM_CITY))
    }

    // --- Antwoord omzetten ---

    @Test
    fun streetAddressHasStreetNumberPostcodeAndCity() {
        val row = toCachedAddress("k", antwerpResponse(), ZOOM_STREET, fetchedAt = 0)!!

        assertEquals("Meir 12", row.displayName)
        assertEquals("2000", row.postcode)
        assertEquals("Antwerpen", row.city)
    }

    @Test
    fun streetWithoutHouseNumberIsOnlyTheStreet() {
        val response = NominatimResponse(null, addressWith(city = "Gent"), null)

        assertEquals("Kerkstraat", toCachedAddress("k", response, ZOOM_STREET, 0)!!.displayName)
    }

    @Test
    fun cityLookupHasNoPostcode() {
        val row = toCachedAddress("k", antwerpResponse(), ZOOM_CITY, fetchedAt = 0)!!

        assertEquals("Antwerpen", row.displayName)
        assertNull(row.postcode)
    }

    @Test
    fun cityFallsBackToTownAndVillage() {
        val town = NominatimResponse(null, addressWith(town = "Mol"), null)
        val village = NominatimResponse(null, addressWith(village = "Zoersel"), null)

        assertEquals("Mol", toCachedAddress("k", town, ZOOM_CITY, 0)!!.city)
        assertEquals("Zoersel", toCachedAddress("k", village, ZOOM_CITY, 0)!!.city)
    }

    @Test
    fun noCityOrErrorGivesNull() {
        val noCity = NominatimResponse(null, addressWith(), null)
        val error = NominatimResponse(null, null, "Unable to geocode")

        assertNull(toCachedAddress("k", noCity, ZOOM_CITY, 0))
        assertNull(toCachedAddress("k", error, ZOOM_CITY, 0))
    }

    // --- Repository met cache ---

    @Test
    fun firstLookupAsksNominatimAndSavesResult() = runTest {
        val address = newRepository().getStreetAddress(51.2194, 4.4025)

        assertEquals("Meir 12", address.street)
        assertEquals("2000", address.postcode)
        assertEquals("Antwerpen", address.city)
        assertEquals(1, api.calls)
        assertEquals(setOf("z18:51.2194,4.4025"), dao.rows.keys)
    }

    @Test
    fun secondLookupNearbyUsesCache() = runTest {
        val repository = newRepository()
        repository.getStreetAddress(51.2194, 4.4025)

        // Minder dan 10 m verder: zelfde afgeronde sleutel.
        repository.getStreetAddress(51.21941, 4.40249)

        assertEquals(1, api.calls)
    }

    @Test
    fun streetAndCityAreCachedSeparately() = runTest {
        val repository = newRepository()
        repository.getStreetAddress(51.2194, 4.4025)
        val city = repository.getCity(51.2194, 4.4025)

        assertEquals("Antwerpen", city)
        assertEquals(2, api.calls)
        assertEquals(2, dao.rows.size)
    }

    @Test
    fun sameLookupAtTheSameTimeAsksNominatimOnce() = runTest {
        val repository = newRepository()
        api.delayMs = 500

        launch { repository.getCity(51.2194, 4.4025) }
        launch { repository.getCity(51.2194, 4.4025) }
        advanceUntilIdle()

        assertEquals(1, api.calls)
    }

    @Test
    fun noInternetGivesNoInternetAndCachesNothing() = runTest {
        val repository = newRepository()
        api.answer = { throw IOException("geen verbinding") }

        val reason = failureReason { repository.getCity(51.2194, 4.4025) }

        assertEquals(GeocodingException.Reason.NO_INTERNET, reason)
        assertTrue(dao.rows.isEmpty())

        // Internet is terug: de volgende poging lukt wel.
        api.answer = { antwerpResponse() }
        assertEquals("Antwerpen", repository.getCity(51.2194, 4.4025))
    }

    @Test
    fun httpErrorGivesServerError() = runTest {
        val repository = newRepository()
        api.answer = { throw HttpException(Response.error<Any>(429, "".toResponseBody())) }

        val reason = failureReason { repository.getCity(51.2194, 4.4025) }

        assertEquals(GeocodingException.Reason.SERVER_ERROR, reason)
    }

    @Test
    fun nothingFoundGivesNotFound() = runTest {
        val repository = newRepository()
        api.answer = { NominatimResponse(null, null, "Unable to geocode") }

        val reason = failureReason { repository.getStreetAddress(0.0, 0.0) }

        assertEquals(GeocodingException.Reason.NOT_FOUND, reason)
        assertTrue(dao.rows.isEmpty())
    }
}

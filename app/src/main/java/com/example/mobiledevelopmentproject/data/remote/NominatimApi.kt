package com.example.mobiledevelopmentproject.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// zoom 18 geeft een gebouw (straat + huisnummer), zoom 10 een gemeente.
const val ZOOM_STREET = 18
const val ZOOM_CITY = 10

// Retrofit maakt zelf de code voor deze interface.
// Elke @Query wordt een parameter in de URL, bv. reverse?lat=51.2194&lon=4.4025&zoom=18&...
interface NominatimApi {

    // suspend: Retrofit voert de aanvraag uit op een achtergrondthread.
    // Bij geen internet gooit dit een IOException, bij een HTTP-fout een HttpException.
    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        // Namen in het Nederlands, bv. "Brussel" in plaats van "Bruxelles".
        @Query("accept-language") language: String = "nl"
    ): NominatimResponse
}

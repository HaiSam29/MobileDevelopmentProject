package com.example.mobiledevelopmentproject.data.remote

import com.example.mobiledevelopmentproject.data.USER_AGENT
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Moet eindigen op "/", anders weigert Retrofit de URL.
private const val BASE_URL = "https://nominatim.openstreetmap.org/"
private const val TIMEOUT_SECONDS = 10L

// object: één client voor de hele app. Een OkHttpClient aanmaken is duur,
// en zo delen alle aanvragen dezelfde verbindingen.
object NominatimClient {

    private val httpClient = OkHttpClient.Builder()
        // Een interceptor ziet elke aanvraag voordat ze vertrekt.
        // Hier zetten we onze eigen User-Agent, verplicht volgens de regels van Nominatim.
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        // Zonder timeout kan een aanvraag bij slecht internet heel lang blijven hangen.
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // by lazy: Retrofit wordt pas gebouwd bij het eerste gebruik.
    val api: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}

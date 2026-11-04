package com.example.mobiledevelopmentproject.ui.map

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

// OpenStreetMap blokkeert apps die zich niet duidelijk identificeren.
private const val USER_AGENT = "Restje/1.0 (studentenproject AP Hogeschool)"

// Moet uitgevoerd zijn voordat de eerste MapView aangemaakt wordt.
fun configureOsmdroid(context: Context) {
    val appContext = context.applicationContext

    Configuration.getInstance().apply {
        // Eerst load: die zet standaardwaarden, de regels eronder overschrijven ze.
        load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        // Voor de OpenStreetMap-tegels negeert osmdroid userAgentValue en stuurt het
        // "packagenaam/versie". Extra headers worden daarna gezet en overschrijven die waarde.
        userAgentValue = USER_AGENT
        additionalHttpRequestProperties[userAgentHttpHeader] = USER_AGENT
        // Mappen van de app zelf: geen opslagpermissies nodig.
        osmdroidBasePath = File(appContext.filesDir, "osmdroid")
        osmdroidTileCache = File(appContext.cacheDir, "osmdroid/tiles")
    }
}

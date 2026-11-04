package com.example.mobiledevelopmentproject.ui.map

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

// Moet uitgevoerd zijn voordat de eerste MapView aangemaakt wordt.
fun configureOsmdroid(context: Context) {
    val appContext = context.applicationContext

    Configuration.getInstance().apply {
        // Eerst load: die zet standaardwaarden, de regels eronder overschrijven ze.
        load(appContext, appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        // De tegelservers van OpenStreetMap vereisen een User-Agent die de app identificeert.
        userAgentValue = appContext.packageName
        // Mappen van de app zelf: geen opslagpermissies nodig.
        osmdroidBasePath = File(appContext.filesDir, "osmdroid")
        osmdroidTileCache = File(appContext.cacheDir, "osmdroid/tiles")
    }
}

package com.example.mobiledevelopmentproject.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Kijkt of het toestel nu een internetverbinding heeft.
// Nodig vóór een schrijfactie naar Firestore: zonder internet zet Firestore die in een
// wachtrij en voert ze later toch uit, ook als de app al "geen internet" gemeld heeft.
class NetworkChecker(context: Context) {

    // applicationContext, zodat we geen Activity vasthouden.
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

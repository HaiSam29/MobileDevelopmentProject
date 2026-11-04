package com.example.mobiledevelopmentproject

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mobiledevelopmentproject.ui.auth.LoginScreen
import com.example.mobiledevelopmentproject.ui.theme.MobileDevelopmentProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // De app is altijd licht, dus de systeembalken moeten altijd donkere iconen tonen,
        // ook als de telefoon in donkere modus staat.
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            MobileDevelopmentProjectTheme {
                // Tijdelijk: in stap 6 komt hier de AppNavHost.
                LoginScreen(
                    onRegisterClick = {},
                    onLoginSuccess = { Log.d("Login", "Ingelogd") }
                )
            }
        }
    }
}

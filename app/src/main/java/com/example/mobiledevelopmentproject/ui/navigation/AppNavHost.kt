package com.example.mobiledevelopmentproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobiledevelopmentproject.ui.auth.LoginScreen
import com.example.mobiledevelopmentproject.ui.auth.RegisterScreen
import com.example.mobiledevelopmentproject.ui.home.HomePlaceholderScreen
import com.example.mobiledevelopmentproject.ui.map.MapScreen
import com.example.mobiledevelopmentproject.ui.store.MyStoreScreen

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MAP = "map"
    const val MY_STORE = "my_store"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = { navController.navigateToHome() }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackClick = { navController.navigateUp() },
                onLoginClick = { navController.navigateUp() },
                onRegisterSuccess = { navController.navigateToHome() }
            )
        }
        composable(Routes.HOME) {
            HomePlaceholderScreen(
                onOpenMapClick = { navController.navigate(Routes.MAP) },
                onOpenMyStoreClick = { navController.navigate(Routes.MY_STORE) }
            )
        }
        composable(Routes.MAP) {
            MapScreen()
        }
        composable(Routes.MY_STORE) {
            MyStoreScreen()
        }
    }
}

// popUpTo haalt inloggen en registreren van de backstack,
// zodat de Terug-knop na het inloggen niet terug naar het formulier gaat.
private fun NavController.navigateToHome() {
    navigate(Routes.HOME) {
        popUpTo(Routes.LOGIN) { inclusive = true }
    }
}

package com.example.mobiledevelopmentproject.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mobiledevelopmentproject.ui.auth.LoginScreen
import com.example.mobiledevelopmentproject.ui.auth.RegisterScreen
import com.example.mobiledevelopmentproject.ui.customer.CustomerHomeScreen
import com.example.mobiledevelopmentproject.ui.map.MapScreen
import com.example.mobiledevelopmentproject.ui.merchant.MerchantHomeScreen
import com.example.mobiledevelopmentproject.ui.start.StartScreen
import com.example.mobiledevelopmentproject.ui.store.MyStoreScreen

private object Routes {
    const val START = "start"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CUSTOMER_HOME = "customer_home"
    const val MERCHANT_HOME = "merchant_home"
    const val MAP = "map"
    const val MY_STORE = "my_store"

    // De naam gaat mee als optioneel argument, zo werkt het ook met een lege naam.
    const val ARG_NAME = "name"
    const val CUSTOMER_HOME_WITH_NAME = "$CUSTOMER_HOME?$ARG_NAME={$ARG_NAME}"
    const val MERCHANT_HOME_WITH_NAME = "$MERCHANT_HOME?$ARG_NAME={$ARG_NAME}"
}

private val nameArgument = listOf(
    navArgument(Routes.ARG_NAME) {
        type = NavType.StringType
        defaultValue = ""
    }
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.START) {
        // Het startscherm kiest: inloggen, of het startscherm van de juiste rol.
        composable(Routes.START) {
            StartScreen(
                onNotLoggedIn = { navController.navigateToLogin() },
                onCustomer = { name -> navController.navigateFromStart(Routes.CUSTOMER_HOME, name) },
                onMerchant = { name -> navController.navigateFromStart(Routes.MERCHANT_HOME, name) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = { navController.navigateToStart() }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackClick = { navController.navigateUp() },
                onLoginClick = { navController.navigateUp() },
                onRegisterSuccess = { navController.navigateToStart() }
            )
        }
        composable(Routes.CUSTOMER_HOME_WITH_NAME, arguments = nameArgument) { entry ->
            CustomerHomeScreen(
                name = entry.arguments?.getString(Routes.ARG_NAME).orEmpty(),
                onMapClick = { navController.navigate(Routes.MAP) },
                onLoggedOut = { navController.navigateToLogin() }
            )
        }
        composable(Routes.MERCHANT_HOME_WITH_NAME, arguments = nameArgument) { entry ->
            MerchantHomeScreen(
                name = entry.arguments?.getString(Routes.ARG_NAME).orEmpty(),
                onMyStoreClick = { navController.navigate(Routes.MY_STORE) },
                // Het scherm "Pakket aanmaken" komt later.
                onCreateBagClick = {},
                onLoggedOut = { navController.navigateToLogin() }
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

// Na inloggen of registreren terug naar het startscherm, dat de rol leest.
// popUpTo haalt inloggen en registreren van de backstack,
// zodat de Terug-knop niet terug naar het formulier gaat.
private fun NavController.navigateToStart() {
    navigate(Routes.START) {
        popUpTo(Routes.LOGIN) { inclusive = true }
    }
}

// Van het startscherm naar het scherm van de rol. Het startscherm zelf gaat van de backstack,
// anders zou Terug opnieuw de laadindicator tonen.
// Uri.encode zorgt dat spaties en tekens als "&" in de naam de route niet breken.
private fun NavController.navigateFromStart(route: String, name: String) {
    navigate("$route?${Routes.ARG_NAME}=${Uri.encode(name)}") {
        popUpTo(Routes.START) { inclusive = true }
    }
}

// popUpTo de hele graaf maakt de backstack leeg,
// zodat de Terug-knop na het uitloggen niet terug in de app gaat.
private fun NavController.navigateToLogin() {
    navigate(Routes.LOGIN) {
        popUpTo(graph.id) { inclusive = true }
    }
}

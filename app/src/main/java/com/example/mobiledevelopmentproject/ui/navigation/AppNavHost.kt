package com.example.mobiledevelopmentproject.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mobiledevelopmentproject.ui.auth.LoginScreen
import com.example.mobiledevelopmentproject.ui.auth.RegisterScreen
import com.example.mobiledevelopmentproject.ui.bag.CreateBagScreen
import com.example.mobiledevelopmentproject.ui.customer.CustomerMainScreen
import com.example.mobiledevelopmentproject.ui.merchant.MerchantMainScreen
import com.example.mobiledevelopmentproject.ui.merchant.MerchantTab
import com.example.mobiledevelopmentproject.ui.start.StartScreen

private object Routes {
    const val START = "start"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CUSTOMER_HOME = "customer_home"
    const val MERCHANT_HOME = "merchant_home"
    const val CREATE_BAG = "create_bag"

    // De naam gaat mee als optioneel argument, zo werkt het ook met een lege naam.
    const val ARG_NAME = "name"
    const val CUSTOMER_HOME_WITH_NAME = "$CUSTOMER_HOME?$ARG_NAME={$ARG_NAME}"
    const val MERCHANT_HOME_WITH_NAME = "$MERCHANT_HOME?$ARG_NAME={$ARG_NAME}"

    // Sleutel in de savedStateHandle van het handelaarsscherm: welke tab moet open.
    const val KEY_OPEN_TAB = "open_tab"
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
            // Het hoofdscherm van de klant, met de navigatiebalk en de vier tabs.
            CustomerMainScreen(
                name = entry.arguments?.getString(Routes.ARG_NAME).orEmpty(),
                onLoggedOut = { navController.navigateToLogin() }
            )
        }
        // Het hoofdscherm van de handelaar, met de navigatiebalk en de twee tabs.
        // De naam in de route wordt hier niet meer gebruikt, maar blijft voor StartScreen.
        composable(Routes.MERCHANT_HOME_WITH_NAME, arguments = nameArgument) { entry ->
            // Een andere bestemming kan hier een tab klaarzetten, zie CREATE_BAG hieronder.
            val openTabName by entry.savedStateHandle
                .getStateFlow<String?>(Routes.KEY_OPEN_TAB, null)
                .collectAsStateWithLifecycle()
            MerchantMainScreen(
                openTab = openTabName?.let { MerchantTab.valueOf(it) },
                onOpenTabHandled = { entry.savedStateHandle[Routes.KEY_OPEN_TAB] = null },
                onCreateBagClick = { navController.navigate(Routes.CREATE_BAG) },
                onLoggedOut = { navController.navigateToLogin() }
            )
        }
        // Pakket aanmaken staat buiten de tabs, dus zonder navigatiebalk.
        // Terug en "Pakket plaatsen" gaan terug naar het handelaarsscherm (tab Mijn pakketten).
        composable(Routes.CREATE_BAG) {
            CreateBagScreen(
                onBackClick = { navController.navigateUp() },
                onBagCreated = { navController.navigateUp() },
                // Zonder winkel: terug naar het handelaarsscherm en daar de tab "Mijn winkel" openen.
                // We zetten de tab klaar in de savedStateHandle van het vorige scherm.
                onMyStoreClick = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(Routes.KEY_OPEN_TAB, MerchantTab.MY_STORE.name)
                    navController.navigateUp()
                }
            )
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

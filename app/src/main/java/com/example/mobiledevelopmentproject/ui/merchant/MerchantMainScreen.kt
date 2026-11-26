package com.example.mobiledevelopmentproject.ui.merchant

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.ui.store.MyStoreScreen

// De twee tabs van de handelaar, in de volgorde van de navigatiebalk.
enum class MerchantTab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    MY_STORE("my_store", R.string.tab_my_store, R.drawable.ic_store),
    MY_BAGS("my_bags", R.string.tab_my_bags, R.drawable.ic_bag)
}

// Het hoofdscherm van de handelaar: de navigatiebalk onderaan met daarboven de gekozen tab.
// Zelfde opbouw als CustomerMainScreen: een eigen NavHost voor de tabs, zodat enkel
// de handelaar deze balk krijgt.
// openTab: een tab die van buitenaf gevraagd wordt (bv. vanuit "Pakket aanmaken" zonder winkel).
// onOpenTabHandled laat weten dat die vraag verwerkt is, zodat ze maar één keer gebeurt.
@Composable
fun MerchantMainScreen(
    openTab: MerchantTab?,
    onOpenTabHandled: () -> Unit,
    onCreateBagClick: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: MerchantMainViewModel = viewModel(),
    homeViewModel: MerchantHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // rememberNavController bewaart de back-stack in de saved state:
    // de gekozen tab overleeft dus een rotatie.
    val tabNavController = rememberNavController()
    val startTab = uiState.startTab

    // Nog niet bekend of er een winkel is: dan weten we ook de starttab niet.
    if (startTab == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        bottomBar = { MerchantNavigationBar(tabNavController) },
        // Zie CustomerMainScreen: de schermen in de tabs doen de statusbalk zelf.
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = startTab.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(MerchantTab.MY_STORE.route) {
                MyStoreScreen(
                    // Eerst uitloggen (wist ook de gekozen locatie), daarna naar inloggen.
                    onLogoutClick = {
                        homeViewModel.logout()
                        onLoggedOut()
                    }
                )
            }
            composable(MerchantTab.MY_BAGS.route) {
                MyBagsScreen(onCreateBagClick = onCreateBagClick)
            }
        }

        // Een gevraagde tab openen, daarna de vraag wissen. Dit staat na de NavHost,
        // zodat de tabs al bestaan op het moment dat we navigeren.
        LaunchedEffect(openTab) {
            if (openTab != null) {
                tabNavController.navigateToTab(openTab)
                onOpenTabHandled()
            }
        }
    }
}

@Composable
private fun MerchantNavigationBar(navController: NavController) {
    // De balk leest de gekozen tab uit de navController, zo klopt hij ook na Terug of rotatie.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        MerchantTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(tab) },
                // contentDescription null: het label eronder zegt al wat de tab is.
                icon = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
                label = { Text(text = stringResource(tab.label)) }
            )
        }
    }
}

// Zelfde als bij de klant: geen stapel opbouwen, Terug gaat naar de starttab,
// en de staat van elke tab wordt bewaard en teruggezet.
private fun NavController.navigateToTab(tab: MerchantTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

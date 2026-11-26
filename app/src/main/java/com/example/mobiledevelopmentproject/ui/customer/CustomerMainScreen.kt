package com.example.mobiledevelopmentproject.ui.customer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.ui.favorites.FavoritesScreen
import com.example.mobiledevelopmentproject.ui.map.MapScreen
import com.example.mobiledevelopmentproject.ui.orders.OrdersScreen

// De vier tabs van de klant. Elke tab heeft een eigen route, label en icoon.
// De volgorde hier is ook de volgorde in de navigatiebalk.
enum class CustomerTab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    DISCOVER("discover", R.string.tab_discover, R.drawable.ic_tab_discover),
    MAP("map", R.string.tab_map, R.drawable.ic_tab_map),
    ORDERS("orders", R.string.tab_orders, R.drawable.ic_tab_orders),
    FAVORITES("favorites", R.string.tab_favorites, R.drawable.ic_tab_favorites)
}

// Het hoofdscherm van de klant: de navigatiebalk onderaan met daarboven de gekozen tab.
// De tabs hebben een eigen NavHost met een eigen navController. Zo krijgt enkel de klant
// de navigatiebalk, en blijft de NavHost van de app (inloggen, rollen) ongewijzigd.
@Composable
fun CustomerMainScreen(
    name: String,
    onLoggedOut: () -> Unit
) {
    // rememberNavController bewaart de back-stack in de saved state:
    // de gekozen tab overleeft dus een rotatie.
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = { CustomerNavigationBar(tabNavController) },
        // Geen ruimte voor de statusbalk hier: de schermen in de tabs doen dat zelf,
        // in hun eigen achtergrondkleur. innerPadding is dan enkel de hoogte van de balk.
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = CustomerTab.DISCOVER.route,
            // consumeWindowInsets: de schermen in de tabs hebben een eigen Scaffold. Zonder dit
            // tellen die de ruimte voor de systeembalk onderaan nog eens bij, met een gat als gevolg.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // Ontdekken is voorlopig het startscherm van Mohamed, met de uitlogknop.
            composable(CustomerTab.DISCOVER.route) {
                CustomerHomeScreen(name = name, onLoggedOut = onLoggedOut)
            }
            composable(CustomerTab.MAP.route) {
                MapScreen()
            }
            composable(CustomerTab.ORDERS.route) {
                OrdersScreen()
            }
            composable(CustomerTab.FAVORITES.route) {
                FavoritesScreen()
            }
        }
    }
}

@Composable
private fun CustomerNavigationBar(navController: NavController) {
    // De balk leest de huidige route uit de navController, zo is er maar één bron
    // voor "welke tab is gekozen" en klopt die ook na Terug of een rotatie.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        CustomerTab.entries.forEach { tab ->
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

// Wisselen van tab zonder een stapel op te bouwen:
// - popUpTo(start): alles boven Ontdekken gaat van de back-stack. Terug vanuit een tab gaat
//   dus naar Ontdekken, en Terug vanuit Ontdekken sluit de app.
// - saveState / restoreState: de staat van een tab (bv. de kaartpositie) wordt bewaard
//   en teruggezet als je later naar die tab terugkeert.
// - launchSingleTop: nog eens op de huidige tab tikken maakt geen tweede kopie.
private fun NavController.navigateToTab(tab: CustomerTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

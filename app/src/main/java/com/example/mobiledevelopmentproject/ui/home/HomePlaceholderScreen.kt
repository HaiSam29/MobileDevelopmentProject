package com.example.mobiledevelopmentproject.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mobiledevelopmentproject.R

// Tijdelijk scherm na inloggen. Wordt vervangen door het startscherm per rol.
// De kaartknop verdwijnt zodra de navigatiebalk onderaan er is.
// De knop "Mijn winkel" verdwijnt zodra het startscherm van de handelaar er is.
@Composable
fun HomePlaceholderScreen(
    onOpenMapClick: () -> Unit,
    onOpenMyStoreClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_placeholder),
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onOpenMapClick) {
                Text(text = stringResource(R.string.button_open_map))
            }
            Button(onClick = onOpenMyStoreClick) {
                Text(text = stringResource(R.string.button_open_my_store))
            }
        }
    }
}

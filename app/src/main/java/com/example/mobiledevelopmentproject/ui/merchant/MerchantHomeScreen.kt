package com.example.mobiledevelopmentproject.ui.merchant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R

// Startscherm van de handelaar. De naam komt mee via de navigatie vanuit het StartScreen.
@Composable
fun MerchantHomeScreen(
    name: String,
    onMyStoreClick: () -> Unit,
    onCreateBagClick: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: MerchantHomeViewModel = viewModel()
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_greeting, name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onMyStoreClick) {
                Text(text = stringResource(R.string.button_my_store))
            }
            Button(onClick = onCreateBagClick) {
                Text(text = stringResource(R.string.button_create_bag))
            }
            // Eerst uitloggen, daarna pas naar het inlogscherm.
            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLoggedOut()
                }
            ) {
                Text(text = stringResource(R.string.button_logout))
            }
        }
    }
}

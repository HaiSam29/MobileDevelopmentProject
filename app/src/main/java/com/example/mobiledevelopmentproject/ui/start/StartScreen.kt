package com.example.mobiledevelopmentproject.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.ui.auth.ErrorMessage

// Eerste scherm van de app. Toont een laadindicator tot bekend is welk startscherm past.
@Composable
fun StartScreen(
    onNotLoggedIn: () -> Unit,
    onCustomer: (name: String) -> Unit,
    onMerchant: (name: String) -> Unit,
    viewModel: StartViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigeren is iets van het scherm, de ViewModel zegt enkel waarheen.
    LaunchedEffect(uiState.destination) {
        when (val destination = uiState.destination) {
            StartDestination.Login -> onNotLoggedIn()
            is StartDestination.CustomerHome -> onCustomer(destination.name)
            is StartDestination.MerchantHome -> onMerchant(destination.name)
            null -> Unit
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        // Eigen val, want uiState is een gedelegeerde property ("by") en kan niet smart-cast worden.
        val error = uiState.error

        if (error != null) {
            Column(
                modifier = modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ErrorMessage(messageRes = error)
                Button(onClick = viewModel::loadStart) {
                    Text(text = stringResource(R.string.button_retry))
                }
                TextButton(onClick = viewModel::logout) {
                    Text(text = stringResource(R.string.button_logout))
                }
            }
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

package com.example.mobiledevelopmentproject.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        OsmMap(
            latitude = uiState.mapCenter.latitude,
            longitude = uiState.mapCenter.longitude,
            zoom = uiState.mapZoom,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onPositionChange = viewModel::onMapPositionChange
        )
    }
}

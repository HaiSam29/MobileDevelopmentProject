package com.example.mobiledevelopmentproject.ui.discover

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.ui.theme.MobileDevelopmentProjectTheme
import com.example.mobiledevelopmentproject.ui.theme.WarningBackground
import com.example.mobiledevelopmentproject.ui.theme.WarningText

// De kop bovenaan Ontdekken (scherm 03 in SCHERMEN.md):
// "Pakketten in de buurt van" klein, de gemeente groot eronder, rechts "Wijzig" en uitloggen.
// Stateless: alles komt uit DiscoverUiState, de knoppen melden enkel een klik.
// Zonder locatie (op de pin-kaart) staat enkel het uitlogicoon er, zodat de klant altijd kan uitloggen.
@Composable
fun DiscoverHeader(
    state: DiscoverUiState,
    showChangeButton: Boolean,
    onChangeLocationClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // weight(1f): de titel neemt de rest van de breedte, een lange gemeente
                // duwt de knoppen niet van het scherm.
                Column(modifier = Modifier.weight(1f)) {
                    if (state.hasLocation) {
                        HeaderTitle(state = state, onRetryClick = onRetryClick)
                    }
                }
                if (state.hasLocation && showChangeButton) {
                    TextButton(onClick = onChangeLocationClick) {
                        Text(
                            text = stringResource(R.string.discover_change_location),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                LogoutIconButton(onClick = onLogoutClick)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// Uitlogicoon zoals in het ontwerp. Apart, zodat Ontdekken het ook zonder kop kan tonen.
@Composable
fun LogoutIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_logout),
            contentDescription = stringResource(R.string.button_logout)
        )
    }
}

// De titel in drie situaties: gemeente opzoeken, gemeente gekend, of geen gemeente (fout).
@Composable
private fun HeaderTitle(state: DiscoverUiState, onRetryClick: () -> Unit) {
    val city = state.city
    when {
        state.isLoadingCity -> {
            SmallLabel(R.string.discover_nearby_label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.discover_city_loading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        city != null -> {
            SmallLabel(R.string.discover_nearby_label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_location_pin),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = city,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        else -> {
            // Fallback zonder gemeente, met de reden en (als het zin heeft) een knop om het opnieuw te proberen.
            Text(
                text = stringResource(R.string.discover_nearby_fallback),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            state.cityError?.let { error ->
                Text(
                    text = stringResource(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (state.canRetryCity) {
                    TextButton(
                        onClick = onRetryClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = stringResource(R.string.button_retry), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallLabel(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Melding bij toestemming "Bij benadering" (scherm 14 in SCHERMEN.md).
// Met de knop kan de klant alsnog een precieze locatie toestaan.
@Composable
fun ApproximateLocationBanner(
    onAllowPreciseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WarningBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = WarningText
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.discover_approximate_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarningText
                )
            }
            OutlinedButton(
                onClick = onAllowPreciseClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, WarningText)
            ) {
                Text(
                    text = stringResource(R.string.button_allow_precise_location),
                    color = WarningText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverHeaderPreview() {
    MobileDevelopmentProjectTheme {
        Column {
            DiscoverHeader(
                state = DiscoverUiState(hasLocation = true, city = "Mechelen"),
                showChangeButton = true,
                onChangeLocationClick = {},
                onRetryClick = {},
                onLogoutClick = {}
            )
            ApproximateLocationBanner(onAllowPreciseClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

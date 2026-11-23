package com.example.mobiledevelopmentproject.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.model.MapPoint
import com.example.mobiledevelopmentproject.ui.theme.WarningText

// "Kies je locatie" (scherm 12 in SCHERMEN.md): de klant zet zelf een pin op de kaart.
// Hergebruikt PinMap van "Mijn winkel": tik om de pin te zetten, lang drukken om te verslepen.
// Stateless: de pin en de kaartpositie staan in MapViewModel.
@Composable
fun PickLocationContent(
    pin: MapPoint?,
    center: MapPoint,
    zoom: Double,
    permissionDenied: Boolean,
    onPinPlaced: (MapPoint) -> Unit,
    onPositionChange: (latitude: Double, longitude: Double, zoom: Double) -> Unit,
    onBackClick: () -> Unit,
    onUsePinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // De balk staat ónder de kaart en niet erover: zo blijft de verplichte bronvermelding
    // van OpenStreetMap (rechtsonder op de kaart) altijd zichtbaar.
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PinMap(
                pin = pin,
                center = center,
                zoom = zoom,
                onPinPlaced = onPinPlaced,
                onPositionChange = onPositionChange,
                modifier = Modifier.fillMaxSize()
            )
            PickLocationHeader(
                permissionDenied = permissionDenied,
                onBackClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        UsePinBar(enabled = pin != null, onClick = onUsePinClick)
    }
}

// Kaartje bovenaan met pijl terug, titel en uitleg.
// Na een weigering staat er één korte regel bij, zodat er (ook liggend) genoeg kaart zichtbaar blijft.
@Composable
private fun PickLocationHeader(
    permissionDenied: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.pick_location_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.map_pin_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Waarom de klant nu zelf een plek kiest.
                if (permissionDenied) {
                    Text(
                        text = stringResource(R.string.location_permission_denied),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarningText
                    )
                }
            }
        }
    }
}

// Witte balk onder de kaart met de knop. Uitgeschakeld zolang er nog geen pin staat.
@Composable
private fun UsePinBar(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = stringResource(R.string.button_use_location), fontWeight = FontWeight.SemiBold)
        }
    }
}

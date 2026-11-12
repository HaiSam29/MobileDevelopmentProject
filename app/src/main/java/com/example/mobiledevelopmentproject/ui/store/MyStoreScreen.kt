package com.example.mobiledevelopmentproject.ui.store

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.model.StoreCategory
import com.example.mobiledevelopmentproject.ui.auth.ErrorMessage
import com.example.mobiledevelopmentproject.ui.auth.LabeledTextField
import com.example.mobiledevelopmentproject.ui.auth.LoadingButton
import com.example.mobiledevelopmentproject.ui.map.PinMap

private val MAP_HEIGHT = 220.dp

// Koppelt de ViewModel aan het scherm.
@Composable
fun MyStoreScreen(
    viewModel: MyStoreViewModel = viewModel(factory = MyStoreViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.store_saved)

    // Na een geslaagde opslag één keer een melding tonen.
    // onSavedMessageShown pas daarna, anders stopt de LaunchedEffect de melding meteen.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar(savedMessage)
            viewModel.onSavedMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { MyStoreTopBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        // Eigen val, want uiState is een gedelegeerde property ("by") en kan niet smart-cast worden.
        val loadError = uiState.loadError

        when {
            uiState.isLoadingStore -> LoadingContent(contentModifier)
            loadError != null -> LoadErrorContent(
                messageRes = loadError,
                onRetryClick = viewModel::loadStore,
                modifier = contentModifier
            )
            else -> MyStoreForm(
                uiState = uiState,
                viewModel = viewModel,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun MyStoreTopBar() {
    Column(modifier = Modifier.statusBarsPadding()) {
        Text(
            text = stringResource(R.string.my_store_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        HorizontalDivider()
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadErrorContent(@StringRes messageRes: Int, onRetryClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ErrorMessage(messageRes = messageRes)
        Button(onClick = onRetryClick) {
            Text(text = stringResource(R.string.button_retry))
        }
    }
}

@Composable
private fun MyStoreForm(
    uiState: MyStoreUiState,
    viewModel: MyStoreViewModel,
    modifier: Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        LabeledTextField(
            label = stringResource(R.string.label_store_name),
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            placeholder = stringResource(R.string.hint_store_name),
            errorRes = uiState.nameError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        CategoryDropdown(
            selected = uiState.category,
            onSelect = viewModel::onCategoryChange,
            errorRes = uiState.categoryError
        )
        Spacer(modifier = Modifier.height(16.dp))

        FieldLabel(text = stringResource(R.string.label_location))
        Spacer(modifier = Modifier.height(8.dp))
        LocationMap(uiState = uiState, viewModel = viewModel)
        if (uiState.locationError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ErrorMessage(messageRes = uiState.locationError)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AddressStatus(uiState = uiState)
        Spacer(modifier = Modifier.height(16.dp))

        LabeledTextField(
            label = stringResource(R.string.label_street),
            value = uiState.street,
            onValueChange = viewModel::onStreetChange,
            placeholder = stringResource(R.string.hint_street),
            errorRes = uiState.streetError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Postcode en gemeente naast elkaar, zoals in het ontwerp.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                LabeledTextField(
                    label = stringResource(R.string.label_postcode),
                    value = uiState.postcode,
                    onValueChange = viewModel::onPostcodeChange,
                    placeholder = stringResource(R.string.hint_postcode),
                    errorRes = uiState.postcodeError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LabeledTextField(
                    label = stringResource(R.string.label_city),
                    value = uiState.city,
                    onValueChange = viewModel::onCityChange,
                    placeholder = stringResource(R.string.hint_city),
                    errorRes = uiState.cityError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.saveError != null) {
            ErrorMessage(messageRes = uiState.saveError)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Ook tijdens het opzoeken van het adres bezig: dan wacht de ViewModel met opslaan.
        LoadingButton(
            text = stringResource(R.string.button_save),
            isLoading = uiState.isSaving || uiState.isLookingUpAddress,
            onClick = {
                focusManager.clearFocus()
                viewModel.save()
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: StoreCategory?,
    onSelect: (StoreCategory) -> Unit,
    @StringRes errorRes: Int?
) {
    // Of de lijst open staat is enkel iets van het scherm, daarom niet in de ViewModel.
    // rememberSaveable houdt het bij over een rotatie.
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.label_category))
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected?.let { stringResource(it.label) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(text = stringResource(R.string.hint_category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                isError = errorRes != null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                StoreCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(category.label)) },
                        onClick = {
                            onSelect(category)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (errorRes != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ErrorMessage(messageRes = errorRes)
        }
    }
}

// De kaart met de pin en onderaan een korte uitleg.
@Composable
private fun LocationMap(uiState: MyStoreUiState, viewModel: MyStoreViewModel) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (uiState.locationError != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
    ) {
        PinMap(
            pin = uiState.pin,
            center = uiState.mapCenter,
            zoom = uiState.mapZoom,
            onPinPlaced = viewModel::onPinPlaced,
            onPositionChange = viewModel::onMapPositionChange,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = stringResource(R.string.map_pin_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// Onder de kaart: bezig met opzoeken, een fout, of "adres automatisch ingevuld".
@Composable
private fun AddressStatus(uiState: MyStoreUiState) {
    when {
        uiState.isLookingUpAddress -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.address_looking_up),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        uiState.addressLookupError != null -> ErrorMessage(messageRes = uiState.addressLookupError)
        uiState.addressAutoFilled -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.address_auto_filled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

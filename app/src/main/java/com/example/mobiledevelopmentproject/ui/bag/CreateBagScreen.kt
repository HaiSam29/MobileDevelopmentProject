package com.example.mobiledevelopmentproject.ui.bag

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.PhotoEncoder
import com.example.mobiledevelopmentproject.ui.auth.ErrorMessage
import com.example.mobiledevelopmentproject.ui.auth.LabeledTextField
import com.example.mobiledevelopmentproject.ui.auth.LoadingButton
import java.util.Locale

// Standaarduur in de klok als er nog niets gekozen is: 17:00.
private const val DEFAULT_PICKUP_MINUTES = 17 * 60

// Koppelt de ViewModel aan het scherm.
@Composable
fun CreateBagScreen(
    onBackClick: () -> Unit,
    onBagCreated: () -> Unit,
    onMyStoreClick: () -> Unit,
    // De Factory geeft de ViewModel een ContentResolver mee, zie CreateBagViewModel.
    viewModel: CreateBagViewModel = viewModel(factory = CreateBagViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.bag_saved)

    // Na een geslaagde opslag één keer een melding tonen en teruggaan.
    // Een Toast, want een Snackbar zou verdwijnen samen met dit scherm.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            onBagCreated()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { CreateBagTopBar(onBackClick = onBackClick) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        // Eigen val, want uiState is een gedelegeerde property ("by") en kan niet smart-cast worden.
        val loadError = uiState.loadError

        when {
            uiState.isLoadingStore -> LoadingContent(contentModifier)
            loadError != null -> MessageWithButton(
                message = { ErrorMessage(messageRes = loadError) },
                buttonText = stringResource(R.string.button_retry),
                onButtonClick = viewModel::loadStore,
                modifier = contentModifier
            )
            uiState.hasNoStore -> MessageWithButton(
                message = {
                    Text(
                        text = stringResource(R.string.create_bag_no_store),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                },
                buttonText = stringResource(R.string.button_my_store),
                onButtonClick = onMyStoreClick,
                modifier = contentModifier
            )
            else -> CreateBagForm(
                uiState = uiState,
                viewModel = viewModel,
                modifier = contentModifier
            )
        }
    }
}

// Terugknop en titel, met een lijn eronder zoals bij "Mijn winkel".
@Composable
private fun CreateBagTopBar(onBackClick: () -> Unit) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.create_bag_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// Een melding in het midden met een knop eronder.
// Gebruikt bij een laadfout en als de handelaar nog geen winkel heeft.
@Composable
private fun MessageWithButton(
    message: @Composable () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        message()
        Button(onClick = onButtonClick) {
            Text(text = buttonText)
        }
    }
}

@Composable
private fun CreateBagForm(
    uiState: CreateBagUiState,
    viewModel: CreateBagViewModel,
    modifier: Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        FieldLabel(text = stringResource(R.string.label_photo))
        Spacer(modifier = Modifier.height(8.dp))
        PhotoPicker(uiState = uiState, viewModel = viewModel)
        Spacer(modifier = Modifier.height(16.dp))

        LabeledTextField(
            label = stringResource(R.string.label_bag_title),
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            placeholder = stringResource(R.string.hint_bag_title),
            errorRes = uiState.titleError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        DescriptionField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            errorRes = uiState.descriptionError
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Prijs en originele waarde naast elkaar, zoals in het ontwerp.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                LabeledTextField(
                    label = stringResource(R.string.label_price),
                    value = uiState.price,
                    onValueChange = viewModel::onPriceChange,
                    placeholder = stringResource(R.string.hint_price),
                    errorRes = uiState.priceError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LabeledTextField(
                    label = stringResource(R.string.label_original_value),
                    value = uiState.originalValue,
                    onValueChange = viewModel::onOriginalValueChange,
                    placeholder = stringResource(R.string.hint_original_value),
                    errorRes = uiState.originalValueError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        LabeledTextField(
            label = stringResource(R.string.label_quantity),
            value = uiState.quantity,
            onValueChange = viewModel::onQuantityChange,
            placeholder = stringResource(R.string.hint_quantity),
            errorRes = uiState.quantityError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        FieldLabel(text = stringResource(R.string.label_pickup_window))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField(
                label = stringResource(R.string.label_pickup_start),
                minutes = uiState.pickupStartMinutes,
                isError = uiState.pickupError != null,
                onTimeSelected = viewModel::onPickupStartChange,
                modifier = Modifier.weight(1f)
            )
            TimeField(
                label = stringResource(R.string.label_pickup_end),
                minutes = uiState.pickupEndMinutes,
                isError = uiState.pickupError != null,
                onTimeSelected = viewModel::onPickupEndChange,
                modifier = Modifier.weight(1f)
            )
        }
        if (uiState.pickupError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ErrorMessage(messageRes = uiState.pickupError)
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.saveError != null) {
            ErrorMessage(messageRes = uiState.saveError)
            Spacer(modifier = Modifier.height(12.dp))
        }

        LoadingButton(
            text = stringResource(R.string.button_place_bag),
            // Ook laden tonen terwijl de foto nog omgezet wordt, dan kan je nog niet opslaan.
            isLoading = uiState.isSaving || uiState.isProcessingPhoto,
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

// Vak met stippellijn voor de foto, met de knoppen "Camera" en "Galerij".
@Composable
private fun PhotoPicker(uiState: CreateBagUiState, viewModel: CreateBagViewModel) {
    // Camera: de camera-app van het toestel neemt de foto en geeft een kleine Bitmap terug.
    // Hiervoor is GEEN CAMERA-toestemming nodig, want niet onze app maar de camera-app
    // gebruikt de camera. Zet daarom ook geen CAMERA-permissie in de manifest:
    // dan zou Android ze wél verplichten, ook voor TakePicturePreview.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // null als de handelaar annuleert, dan doen we niets.
        if (bitmap != null) viewModel.onPhotoTaken(bitmap)
    }

    // Galerij: de fotokiezer van het systeem. Ook geen toestemming nodig,
    // we krijgen enkel toegang tot de ene foto die de handelaar kiest.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // null als de handelaar annuleert.
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    val onCameraClick = {
        try {
            // TakePicturePreview heeft geen invoer nodig, daarom null.
            cameraLauncher.launch(null)
        } catch (e: ActivityNotFoundException) {
            // Geen camera-app op het toestel (kan op sommige emulators).
            viewModel.onCameraUnavailable()
        }
    }
    val onGalleryClick = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Base64 -> Bitmap enkel opnieuw doen als de foto verandert, niet bij elke hertekening.
    val photo = remember(uiState.photoBase64) {
        PhotoEncoder.decode(uiState.photoBase64)?.asImageBitmap()
    }

    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (uiState.photoError != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            // Stippellijn: Compose heeft geen kant-en-klare stippelrand, dus we tekenen ze zelf.
            // Na background(), zo ligt de lijn erboven. De foto ligt dan weer over de lijn.
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val half = strokeWidth / 2
                drawRoundRect(
                    color = borderColor,
                    // Een halve lijndikte naar binnen, anders valt de helft buiten het vak.
                    topLeft = Offset(half, half),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(16.dp.toPx() - half),
                    style = Stroke(
                        width = strokeWidth,
                        // 10 dp lijn, 6 dp opening, steeds opnieuw.
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
    ) {
        when {
            uiState.isProcessingPhoto -> CircularProgressIndicator()
            photo != null -> Image(
                bitmap = photo,
                contentDescription = stringResource(R.string.content_description_bag_photo),
                // Crop vult het hele vak, wat er buiten valt wordt afgesneden.
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Nog geen foto: tekst en knoppen in het vak, zoals in het ontwerp.
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.hint_add_photo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                PhotoButtons(enabled = true, onCameraClick = onCameraClick, onGalleryClick = onGalleryClick)
            }
        }
    }

    // Met een foto (of tijdens het laden) staan de knoppen onder het vak, zo bedekken ze
    // de foto niet. Dezelfde knoppen vervangen dan de foto.
    if (photo != null || uiState.isProcessingPhoto) {
        Spacer(modifier = Modifier.height(12.dp))
        PhotoButtons(
            enabled = !uiState.isProcessingPhoto,
            onCameraClick = onCameraClick,
            onGalleryClick = onGalleryClick
        )
    }

    if (uiState.photoError != null) {
        Spacer(modifier = Modifier.height(6.dp))
        ErrorMessage(messageRes = uiState.photoError)
    }
}

// De knoppen "Camera" en "Galerij" naast elkaar, gecentreerd.
@Composable
private fun PhotoButtons(enabled: Boolean, onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        PhotoButton(
            text = stringResource(R.string.button_camera),
            iconRes = R.drawable.ic_camera,
            enabled = enabled,
            onClick = onCameraClick
        )
        PhotoButton(
            text = stringResource(R.string.button_gallery),
            iconRes = R.drawable.ic_gallery,
            enabled = enabled,
            onClick = onGalleryClick
        )
    }
}

@Composable
private fun PhotoButton(text: String, @DrawableRes iconRes: Int, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp)) {
        // contentDescription null: de tekst ernaast zegt al wat de knop doet.
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

// Zoals LabeledTextField, maar met meerdere regels voor een langere tekst.
@Composable
private fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes errorRes: Int?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.label_bag_description))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = stringResource(R.string.hint_bag_description)) },
            isError = errorRes != null,
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        if (errorRes != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ErrorMessage(messageRes = errorRes)
        }
    }
}

// Een vak met het gekozen uur. Een tik opent een klok om het uur te kiezen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    minutes: Int?,
    isError: Boolean,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Of de klok open staat is enkel iets van het scherm, daarom niet in de ViewModel.
    // rememberSaveable houdt het bij over een rotatie.
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .clickable(role = Role.Button) { showPicker = true }
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = minutes?.let { formatTime(it) } ?: stringResource(R.string.hint_pick_time),
                style = MaterialTheme.typography.bodyLarge,
                color = if (minutes == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = stringResource(R.string.action_pick_time)
            )
        }
    }

    if (showPicker) {
        // Start op het gekozen uur, of op 17:00 als er nog niets gekozen is.
        val startMinutes = minutes ?: DEFAULT_PICKUP_MINUTES
        // rememberTimePickerState onthoudt het uur in de klok zelf ook over een rotatie.
        val pickerState = rememberTimePickerState(
            initialHour = startMinutes / 60,
            initialMinute = startMinutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(pickerState.hour, pickerState.minute)
                        showPicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = stringResource(R.string.button_cancel))
                }
            },
            text = { TimePicker(state = pickerState) }
        )
    }
}

// 1050 minuten -> "17:30" (24-uursnotatie).
private fun formatTime(minutes: Int): String {
    return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
}

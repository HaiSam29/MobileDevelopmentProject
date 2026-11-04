package com.example.mobiledevelopmentproject.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledevelopmentproject.R
import com.example.mobiledevelopmentproject.data.model.UserRole
import com.example.mobiledevelopmentproject.ui.theme.MobileDevelopmentProjectTheme

// Koppelt de ViewModel aan het scherm.
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    RegisterContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRoleChange = viewModel::onRoleChange,
        onRegisterClick = viewModel::register,
        onBackClick = onBackClick,
        onLoginClick = onLoginClick
    )
}

// Tekent enkel wat in de UiState staat, zonder ViewModel. Daardoor werkt de Preview.
@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            // Zelfde opbouw als het inlogscherm: het formulier scrollt, de link blijft onderaan.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                LabeledTextField(
                    label = stringResource(R.string.label_name),
                    value = uiState.name,
                    onValueChange = onNameChange,
                    placeholder = stringResource(R.string.hint_name),
                    errorRes = uiState.nameError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabeledTextField(
                    label = stringResource(R.string.label_email),
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.hint_email),
                    errorRes = uiState.emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabeledTextField(
                    label = stringResource(R.string.label_password),
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(R.string.hint_new_password),
                    errorRes = uiState.passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.label_role),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                RolePicker(selectedRole = uiState.role, onRoleChange = onRoleChange)
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.generalError != null) {
                    ErrorMessage(messageRes = uiState.generalError)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LoadingButton(
                    text = stringResource(R.string.button_register),
                    isLoading = uiState.isLoading,
                    onClick = {
                        focusManager.clearFocus()
                        onRegisterClick()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            FooterLink(
                text = stringResource(R.string.register_has_account),
                linkText = stringResource(R.string.link_login),
                onClick = onLoginClick
            )
        }
    }
}

@Composable
private fun RolePicker(selectedRole: UserRole, onRoleChange: (UserRole) -> Unit) {
    // IntrinsicSize.Min maakt beide kaarten even hoog, ook als een tekst langer is.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoleCard(
            title = stringResource(R.string.role_customer),
            description = stringResource(R.string.role_customer_description),
            iconRes = R.drawable.ic_bag,
            selected = selectedRole == UserRole.CUSTOMER,
            onClick = { onRoleChange(UserRole.CUSTOMER) },
            modifier = Modifier.weight(1f)
        )
        RoleCard(
            title = stringResource(R.string.role_merchant),
            description = stringResource(R.string.role_merchant_description),
            iconRes = R.drawable.ic_store,
            selected = selectedRole == UserRole.MERCHANT,
            onClick = { onRoleChange(UserRole.MERCHANT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outline,
                shape = shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            // onClick = null: de hele kaart is klikbaar, niet enkel het rondje.
            RadioButton(selected = selected, onClick = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterContentPreview() {
    MobileDevelopmentProjectTheme {
        RegisterContent(
            uiState = RegisterUiState(),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onRoleChange = {},
            onRegisterClick = {},
            onBackClick = {},
            onLoginClick = {}
        )
    }
}

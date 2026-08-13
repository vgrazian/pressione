package com.pressione.iperteso.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Navigate on successful login
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showRecovery) {
                // ── Login flow (matching Vue app) ──
                Icon(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(80.dp),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.login_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Username with placeholder like Vue app
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        localError = null
                        viewModel.clearError()
                    },
                    label = { Text(stringResource(R.string.common_username)) },
                    placeholder = { Text(stringResource(R.string.login_username_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password with placeholder like Vue app
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                        viewModel.clearError()
                    },
                    label = { Text(stringResource(R.string.common_password)) },
                    placeholder = { Text(stringResource(R.string.login_password_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible)
                                    stringResource(R.string.login_hide_password)
                                else stringResource(R.string.login_show_password)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (username.isBlank() || password.isBlank()) {
                                localError = context.getString(R.string.login_fill_fields)
                            } else {
                                localError = null
                                viewModel.login(username, password)
                            }
                        }
                    ),
                    enabled = !uiState.isLoading
                )

                // Error message
                val error = localError ?: uiState.error
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login button with "Accesso in corso..." text like Vue app
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            localError = context.getString(R.string.login_fill_fields)
                        } else {
                            localError = null
                            viewModel.login(username, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.login_in_progress))
                    } else {
                        Text(stringResource(R.string.login_button))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Forgot password link
                TextButton(onClick = { showRecovery = true }) {
                    Text(stringResource(R.string.login_forgot), color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // ── Recovery flow (matching Vue app) ──
                Text(
                    text = stringResource(R.string.login_recovery_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                var forgotEmail by remember { mutableStateOf("") }
                var forgotMessage by remember { mutableStateOf<String?>(null) }

                OutlinedTextField(
                    value = forgotEmail,
                    onValueChange = { forgotEmail = it },
                    label = { Text(stringResource(R.string.common_email)) },
                    placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (forgotMessage != null) {
                    Text(
                        text = forgotMessage!!,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { /* TODO: wire requestPasswordReset */ },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = forgotEmail.isNotBlank()
                ) {
                    Text(stringResource(R.string.login_send_request))
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { showRecovery = false }) {
                    Text(stringResource(R.string.login_back_to_login), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

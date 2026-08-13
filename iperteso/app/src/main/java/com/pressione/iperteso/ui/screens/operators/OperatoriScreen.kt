package com.pressione.iperteso.ui.screens.operators

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.User
import com.pressione.iperteso.ui.components.AppBottomNav
import com.pressione.iperteso.ui.components.AppTab
import com.pressione.iperteso.ui.components.SkeletonLoader
import com.pressione.iperteso.ui.theme.ErrorRed
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatoriScreen(
    session: AuthSession,
    onNavigateBack: () -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    viewModel: OperatorsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewUserForm by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // user to confirm
    var pendingActionType by remember { mutableStateOf<String?>(null) } // "deactivate" | "activate" | "delete"

    LaunchedEffect(session) { viewModel.initialize(session.username) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.operators_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showNewUserForm = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.operators_new_user))
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNav(current = AppTab.OPERATORS, isAdmin = true, onNavigate = onNavigateTab)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error / success banners
            uiState.errorMessage?.let { msg ->
                Banner(msg, isError = true, onDismiss = { viewModel.clearMessages() }, onRetry = { viewModel.loadUsers() })
            }
            uiState.successMessage?.let { msg ->
                Banner(msg, isError = false, onDismiss = { viewModel.clearMessages() })
            }

            if (showNewUserForm) {
                NewUserForm(
                    onCreate = { username, email, password, role, active ->
                        viewModel.createUser(username, email, password, role, active)
                        showNewUserForm = false
                    },
                    onCancel = { showNewUserForm = false }
                )
            }

            if (uiState.isLoading) {
                SkeletonLoader(modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.users, key = { it.username }) { user ->
                        UserRow(
                            user = user,
                            isSelf = user.username == session.username,
                            onToggleRole = { viewModel.toggleRole(user) },
                            onToggleActive = {
                                pendingAction = user.username
                                pendingActionType = if (user.active) "deactivate" else "activate"
                            },
                            onDelete = {
                                pendingAction = user.username
                                pendingActionType = "delete"
                            },
                            onResetPassword = { viewModel.openReset(user) }
                        )
                    }
                }
            }
        }
    }

    // Confirm dialogs
    val targetUser = uiState.users.find { it.username == pendingAction }
    if (pendingActionType != null && targetUser != null) {
        val isDelete = pendingActionType == "delete"
        val isDeactivate = pendingActionType == "deactivate"
        AlertDialog(
            onDismissRequest = { pendingAction = null; pendingActionType = null },
            title = {
                Text(
                    stringResource(
                        if (isDelete) R.string.operators_delete_title
                        else if (isDeactivate) R.string.operators_deactivate_title
                        else R.string.operators_activate_title
                    )
                )
            },
            text = {
                Text(
                    if (isDelete) stringResource(R.string.operators_delete_confirm, targetUser.username)
                    else if (isDeactivate) stringResource(R.string.operators_deactivate_confirm, targetUser.username)
                    else stringResource(R.string.operators_activate_confirm, targetUser.username)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isDelete) viewModel.deleteUser(targetUser)
                    else viewModel.toggleActive(targetUser)
                    pendingAction = null; pendingActionType = null
                }) {
                    Text(
                        stringResource(if (isDelete) R.string.common_delete else R.string.common_confirm),
                        color = ErrorRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null; pendingActionType = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Reset password dialog
    uiState.resetUser?.let { user ->
        ResetPasswordDialog(
            user = user,
            onConfirm = { password -> viewModel.resetPassword(user, password) },
            onDismiss = { viewModel.closeReset() }
        )
    }
}

@Composable
private fun Banner(msg: String, isError: Boolean, onDismiss: () -> Unit, onRetry: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                msg,
                modifier = Modifier.weight(1f),
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (isError && onRetry != null) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) }
        }
    }
}

@Composable
private fun NewUserForm(
    onCreate: (String, String, String, String, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val errFillAll = stringResource(R.string.operators_error_fill_all)
    val errPwShort = stringResource(R.string.operators_error_pw_short)
    val errEmail = stringResource(R.string.operators_error_email)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.operators_new_user), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(username, { username = it }, label = { Text(stringResource(R.string.operators_username)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.operators_email)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.operators_password)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                Text(stringResource(R.string.operators_role_admin))
                Spacer(Modifier.width(16.dp))
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Text(stringResource(R.string.operators_active))
            }
            error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    when {
                        username.isBlank() || email.isBlank() || password.isBlank() -> error = errFillAll
                        password.length < 8 -> error = errPwShort
                        !email.contains("@") -> error = errEmail
                        else -> onCreate(username.trim(), email.trim(), password, if (isAdmin) "admin" else "user", isActive)
                    }
                }) { Text(stringResource(R.string.operators_create)) }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: User,
    isSelf: Boolean,
    onToggleRole: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.active) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = if (user.active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            if (user.role == "admin") stringResource(R.string.operators_role_admin)
                            else stringResource(R.string.operators_role_user),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.role == "admin") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        if (!user.active) {
                            Text(stringResource(R.string.operators_inactive), style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.operators_role_admin), style = MaterialTheme.typography.labelSmall)
                    Checkbox(
                        checked = user.role == "admin",
                        onCheckedChange = { if (!isSelf) onToggleRole() },
                        enabled = !isSelf
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.operators_active), style = MaterialTheme.typography.labelSmall)
                    Checkbox(
                        checked = user.active,
                        onCheckedChange = { if (!isSelf) onToggleActive() },
                        enabled = !isSelf
                    )
                }
                if (user.active) {
                    IconButton(onClick = onResetPassword) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.operators_reset_pw))
                    }
                }
                if (!isSelf) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetPasswordDialog(
    user: User,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val errPwShort = stringResource(R.string.operators_error_pw_short)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.operators_reset_pw_for, user.username)) },
        text = {
            Column {
                OutlinedTextField(
                    password, { password = it },
                    label = { Text(stringResource(R.string.operators_new_pw)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.length < 8) error = errPwShort
                else { onConfirm(password); onDismiss() }
            }) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

package com.pressione.iperteso.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.services.CsvExporter
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.services.ReminderScheduler
import com.pressione.iperteso.ui.theme.ErrorRed
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    session: AuthSession,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    medicationViewModel: MedicationViewModel = koinViewModel()
) {
    var darkMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var remindersEnabled by remember { mutableStateOf(false) }
    val medState by medicationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentLanguage = LocaleManager.getLanguage(context)
    val activity = context as? android.app.Activity

    // POST_NOTIFICATIONS permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderScheduler.schedule(context, 8, 0)
            remindersEnabled = true
        }
    }

    fun toggleReminders(enabled: Boolean) {
        if (enabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderScheduler.schedule(context, 8, 0)
                remindersEnabled = true
            }
        } else {
            ReminderScheduler.cancel(context)
            remindersEnabled = false
        }
    }

    LaunchedEffect(session) { medicationViewModel.initialize(session.username) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_account))
            ListItem(
                headlineContent = { Text(session.username) },
                supportingContent = { Text(session.email) },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.settings_profile)) }
            )
            HorizontalDivider()

            // ── Admin Section (RBAC: only for admin role) ──
            if (session.role == "admin") {
                SectionHeader(stringResource(R.string.settings_admin))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_manage_users)) },
                    supportingContent = { Text(stringResource(R.string.settings_manage_users_sub)) },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.settings_manage_users)) }
                )
                HorizontalDivider()
            }

            SectionHeader(stringResource(R.string.settings_appearance))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dark_mode)) },
                supportingContent = { Text(stringResource(if (darkMode) R.string.settings_on else R.string.settings_off)) },
                leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = stringResource(R.string.settings_dark_mode)) },
                trailingContent = { Switch(checked = darkMode, onCheckedChange = { darkMode = it }) }
            )
            HorizontalDivider()

            // ── Language (i18n) ──
            SectionHeader(stringResource(R.string.settings_language))
            ListItem(
                headlineContent = { Text("Italiano") },
                trailingContent = {
                    RadioButton(
                        selected = currentLanguage == LocaleManager.LANG_ITALIAN,
                        onClick = { activity?.let { LocaleManager.setLanguage(it, LocaleManager.LANG_ITALIAN) } }
                    )
                },
                modifier = Modifier.clickable {
                    activity?.let { LocaleManager.setLanguage(it, LocaleManager.LANG_ITALIAN) }
                }
            )
            ListItem(
                headlineContent = { Text("English") },
                trailingContent = {
                    RadioButton(
                        selected = currentLanguage == LocaleManager.LANG_ENGLISH,
                        onClick = { activity?.let { LocaleManager.setLanguage(it, LocaleManager.LANG_ENGLISH) } }
                    )
                },
                modifier = Modifier.clickable {
                    activity?.let { LocaleManager.setLanguage(it, LocaleManager.LANG_ENGLISH) }
                }
            )
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_reminders))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_daily_reminder)) },
                supportingContent = {
                    Text(stringResource(if (remindersEnabled) R.string.settings_reminder_at else R.string.settings_off))
                },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.settings_reminders)) },
                trailingContent = {
                    Switch(checked = remindersEnabled, onCheckedChange = { toggleReminders(it) })
                }
            )
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_medications))
            if (!medState.isLoading) {
                for (med in medState.medications) {
                    MedicationItem(
                        medication = med,
                        onStop = { medicationViewModel.stopMedication(med) },
                        onDelete = { medicationViewModel.deleteMedication(med.id) }
                    )
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_add_medication)) },
                leadingContent = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_add_medication)) },
                modifier = Modifier.clickable { medicationViewModel.showAddDialog() }
            )
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_data))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export_csv)) },
                supportingContent = { Text(stringResource(R.string.settings_export_csv_sub)) },
                leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.settings_export_csv)) },
                modifier = Modifier.clickable {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val db = com.pressione.iperteso.IperTesoApplication.instance.database
                            var readingList = emptyList<com.pressione.iperteso.domain.model.Reading>()
                            db.readingDao().getReadingsByUser(session.username).collect { entities ->
                                readingList = entities.map {
                                    com.pressione.iperteso.domain.model.Reading(
                                        id = it.id, username = it.username, systolic = it.systolic,
                                        diastolic = it.diastolic, heartRate = it.heartRate,
                                        timestamp = java.time.Instant.ofEpochMilli(it.timestamp),
                                        notes = it.notes
                                    )
                                }
                                return@collect
                            }
                            kotlinx.coroutines.delay(50)
                            val file = CsvExporter.export(context, session.username, readingList)
                            withContext(Dispatchers.Main) {
                                CsvExporter.shareCsv(context, file)
                            }
                        }
                    }
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_delete_all)) },
                supportingContent = { Text(stringResource(R.string.settings_delete_all_sub)) },
                leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.settings_delete_all)) },
                modifier = Modifier.clickable { showDeleteDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text(stringResource(R.string.settings_about_text)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.settings_about)) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_logout), color = ErrorRed) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.settings_logout), tint = ErrorRed) },
                modifier = Modifier.clickable { showLogoutDialog = true }
            )
        }

        if (medState.showAddDialog) {
            MedicationDialog(
                editing = medState.editingMedication,
                onDismiss = { medicationViewModel.dismissDialog() },
                onSave = { name, dosage, freq, notes, start, end ->
                    medicationViewModel.saveMedication(name, dosage, freq, notes, start, end)
                }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.settings_logout)) },
                text = { Text(stringResource(R.string.settings_logout_confirm)) },
                confirmButton = {
                    TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                        Text(stringResource(R.string.settings_logout), color = ErrorRed)
                    }
                },
                dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.settings_delete_all)) },
                text = { Text(stringResource(R.string.settings_delete_all_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        medicationViewModel.deleteAllData()
                    }) {
                        Text(stringResource(R.string.settings_delete_all_button), color = ErrorRed)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
            )
        }
    }
}

@Composable
private fun MedicationItem(
    medication: Medication,
    onStop: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Medication, contentDescription = null,
                tint = if (medication.isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Row {
                    if (medication.dosage.isNotBlank()) {
                        Text(medication.dosage, style = MaterialTheme.typography.bodySmall)
                        Text(" \u00B7 ", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "${dateFormat.format(medication.startDate.atZone(ZoneId.systemDefault()))} \u2014 " +
                        if (medication.isActive) stringResource(R.string.settings_medication_in_progress)
                        else dateFormat.format(medication.endDate!!.atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (medication.notes.isNotBlank()) {
                    Text(medication.notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (medication.isActive) {
                TextButton(onClick = onStop) { Text(stringResource(R.string.settings_medication_stop), style = MaterialTheme.typography.labelSmall) }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_medication_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationDialog(
    editing: Medication?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Instant, Instant?) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var dosage by remember { mutableStateOf(editing?.dosage ?: "") }
    var frequency by remember { mutableStateOf(editing?.frequency ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var startDate by remember { mutableStateOf(editing?.startDate ?: Instant.now()) }
    var endDate by remember { mutableStateOf(editing?.endDate) }
    var stillTaking by remember { mutableStateOf(editing?.isActive ?: true) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) stringResource(R.string.settings_med_edit) else stringResource(R.string.settings_med_new)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_med_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(value = dosage, onValueChange = { dosage = it },
                        label = { Text(stringResource(R.string.settings_med_dosage)) }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = frequency, onValueChange = { frequency = it },
                        label = { Text(stringResource(R.string.settings_med_frequency)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.settings_med_notes)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showStartPicker = true }) {
                    Text(stringResource(R.string.settings_med_start, dateFormat.format(startDate.atZone(ZoneId.systemDefault()))))
                }
                if (!stillTaking) {
                    TextButton(onClick = { showEndPicker = true }) {
                        Text(stringResource(R.string.settings_med_end, endDate?.let { dateFormat.format(it.atZone(ZoneId.systemDefault())) } ?: stringResource(R.string.settings_med_end_not_set)))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = stillTaking, onCheckedChange = { stillTaking = it; if (it) endDate = null })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_med_still_taking), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, dosage, frequency, notes, startDate, endDate) },
                enabled = name.isNotBlank()) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )

    if (showStartPicker) {
        val startPickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis)
                    }
                    showStartPicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }
    if (showEndPicker) {
        val endPickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.toEpochMilli() ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis)
                    }
                    showEndPicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

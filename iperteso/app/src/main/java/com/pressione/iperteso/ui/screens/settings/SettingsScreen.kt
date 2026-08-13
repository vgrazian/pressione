package com.pressione.iperteso.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.activity.result.contract.ActivityResultContracts
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.domain.model.TimeBand
import com.pressione.iperteso.services.BackupService
import com.pressione.iperteso.services.CsvExporter
import com.pressione.iperteso.services.CsvImporter
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.services.ReminderScheduler
import com.pressione.iperteso.services.ThemeManager
import com.pressione.iperteso.services.TimeBandsStore
import com.pressione.iperteso.ui.components.AppBottomNav
import com.pressione.iperteso.ui.components.AppTab
import com.pressione.iperteso.ui.components.CollapsibleSection
import com.pressione.iperteso.ui.components.TimeBandSlider
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
    onNavigateTab: (AppTab) -> Unit,
    onLogout: () -> Unit,
    medicationViewModel: MedicationViewModel = koinViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var remindersEnabled by remember { mutableStateOf(false) }
    val medState by medicationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by ThemeManager.mode.collectAsState()
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

    // ── Profilo esteso ──
    var showProfileDialog by remember { mutableStateOf(false) }
    var profileBirthDate by remember { mutableStateOf("") }
    var profileGender by remember { mutableStateOf("") }
    var profileFirstName by remember { mutableStateOf("") }
    var profileLastName by remember { mutableStateOf("") }
    var profileFiscalCode by remember { mutableStateOf("") }
    var profilePhone by remember { mutableStateOf("") }
    var profileStreet by remember { mutableStateOf("") }
    var profileStreetNumber by remember { mutableStateOf("") }
    var profileCity by remember { mutableStateOf("") }
    var profilePostalCode by remember { mutableStateOf("") }
    var profileMessage by remember { mutableStateOf("") }

    // ── Password / Email ──
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var emailMessage by remember { mutableStateOf("") }

    // ── Dati (import/backup/restore) ──
    var dataMessage by remember { mutableStateOf("") }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var showImportModeDialog by remember { mutableStateOf(false) }

    // ── Fasce orarie ──
    var bands by remember { mutableStateOf(TimeBand.defaults()) }
    var savedBands by remember { mutableStateOf<List<TimeBand>?>(null) }
    var bandsMessage by remember { mutableStateOf("") }

    val db = remember { com.pressione.iperteso.IperTesoApplication.instance.database }

    LaunchedEffect(session.username) {
        // Load profile + time bands
        val user = db.userDao().getUser(session.username)
        if (user != null) {
            profileBirthDate = user.birthDate ?: ""
            profileGender = user.gender ?: ""
            profileFirstName = user.firstName ?: ""
            profileLastName = user.lastName ?: ""
            profileFiscalCode = user.fiscalCode ?: ""
            profilePhone = user.phone ?: ""
            profileStreet = user.street ?: ""
            profileStreetNumber = user.streetNumber ?: ""
            profileCity = user.city ?: ""
            profilePostalCode = user.postalCode ?: ""
        }
        bands = TimeBandsStore.load(session.username, db.settingsDao())
        savedBands = bands.toList()
    }

    fun saveProfile() {
        scope.launch {
            val authRepo = org.koin.java.KoinJavaComponent.get<com.pressione.iperteso.data.repository.AuthRepository>(
                com.pressione.iperteso.data.repository.AuthRepository::class.java
            )
            val result = authRepo.updateProfile(
                username = session.username,
                birthDate = profileBirthDate.ifBlank { null },
                gender = profileGender.ifBlank { null },
                profileCompleted = true,
                firstName = profileFirstName.ifBlank { null },
                lastName = profileLastName.ifBlank { null },
                fiscalCode = profileFiscalCode.ifBlank { null },
                phone = profilePhone.ifBlank { null },
                street = profileStreet.ifBlank { null },
                streetNumber = profileStreetNumber.ifBlank { null },
                city = profileCity.ifBlank { null },
                postalCode = profilePostalCode.ifBlank { null }
            )
            profileMessage = if (result.isSuccess) "Profilo salvato!" else (result.exceptionOrNull()?.message ?: "Errore")
            showProfileDialog = false
        }
    }

    fun changePassword() {
        scope.launch {
            val authRepo = org.koin.java.KoinJavaComponent.get<com.pressione.iperteso.data.repository.AuthRepository>(
                com.pressione.iperteso.data.repository.AuthRepository::class.java
            )
            val result = authRepo.changePassword(session.username, currentPassword, newPassword)
            passwordMessage = if (result.isSuccess) "Password aggiornata!" else (result.exceptionOrNull()?.message ?: "Errore")
            if (result.isSuccess) {
                currentPassword = ""
                newPassword = ""
                showPasswordDialog = false
            }
        }
    }

    fun changeEmail() {
        scope.launch {
            val authRepo = org.koin.java.KoinJavaComponent.get<com.pressione.iperteso.data.repository.AuthRepository>(
                com.pressione.iperteso.data.repository.AuthRepository::class.java
            )
            val result = authRepo.changeEmail(session.username, newEmail)
            emailMessage = if (result.isSuccess) "Email aggiornata!" else (result.exceptionOrNull()?.message ?: "Errore")
            if (result.isSuccess) {
                newEmail = ""
                showEmailDialog = false
            }
        }
    }

    fun runImport(mode: CsvImporter.Mode) {
        val content = pendingImportContent ?: return
        scope.launch {
            val repo = com.pressione.iperteso.data.repository.ReadingRepository(
                com.pressione.iperteso.data.remote.api.ReadingsApi(), db.readingDao()
            )
            val result = withContext(Dispatchers.IO) {
                CsvImporter.import(session.username, content, repo, mode)
            }
            dataMessage = "Importate ${result.imported}" +
                (if (result.skipped > 0) ", ${result.skipped} saltate" else "") +
                (if (result.overwritten > 0) ", ${result.overwritten} sovrascritte" else "") +
                (if (result.errors.isNotEmpty()) " (${result.errors.size} errori)" else "")
            pendingImportContent = null
            showImportModeDialog = false
        }
    }

    fun runBackup() {
        scope.launch {
            val json = withContext(Dispatchers.IO) { BackupService.export(session.username, db) }
            dataMessage = "Backup creato"
            withContext(Dispatchers.Main) {
                val file = java.io.File(context.cacheDir, "iperteso_backup_${session.username}.json")
                file.writeText(json)
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                ).let { uri ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Backup"))
                }
            }
        }
    }

    fun runRestore(json: String) {
        scope.launch {
            val readingRepo = com.pressione.iperteso.data.repository.ReadingRepository(
                com.pressione.iperteso.data.remote.api.ReadingsApi(), db.readingDao()
            )
            val medRepo = com.pressione.iperteso.data.repository.MedicationRepository(
                com.pressione.iperteso.data.remote.api.MedicationApi(), db.medicationDao()
            )
            val count = withContext(Dispatchers.IO) {
                BackupService.restore(session.username, json, readingRepo, medRepo, db)
            }
            dataMessage = "Ripristinate $count voci"
        }
    }

    fun saveBands() {
        scope.launch {
            TimeBandsStore.save(session.username, bands, db.settingsDao())
            savedBands = bands.toList()
            bandsMessage = "Fasce orarie salvate!"
        }
    }

    fun resetBands() {
        bands = TimeBand.defaults()
        bandsMessage = "Fasce ripristinate (salva per confermare)"
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                pendingImportContent = content
                showImportModeDialog = true
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                runRestore(json)
            }
        }
    }

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
        },
        bottomBar = {
            AppBottomNav(
                current = AppTab.SETTINGS,
                isAdmin = session.role == "admin",
                onNavigate = onNavigateTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            // ── Account (always visible) ──
            SectionHeader(stringResource(R.string.settings_account))
            ListItem(
                headlineContent = { Text(session.username) },
                supportingContent = { Text(session.email) },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.settings_profile)) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_change_password)) },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.settings_change_password)) },
                modifier = Modifier.clickable { showPasswordDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_change_email)) },
                leadingContent = { Icon(Icons.Default.Email, contentDescription = stringResource(R.string.settings_change_email)) },
                modifier = Modifier.clickable { showEmailDialog = true }
            )
            HorizontalDivider()

            // ── Language (always visible) ──
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

            // ── Profilo ──
            CollapsibleSection(
                title = "👤 " + stringResource(R.string.settings_profile),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_edit_profile)) },
                    supportingContent = { Text(stringResource(R.string.settings_edit_profile_sub)) },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.settings_edit_profile)) },
                    modifier = Modifier.clickable { showProfileDialog = true }
                )
            }

            // ── Farmaci ──
            CollapsibleSection(
                title = "💊 " + stringResource(R.string.settings_medications),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
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
            }

            // ── Promemoria ──
            CollapsibleSection(
                title = "🔔 " + stringResource(R.string.settings_reminders),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
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
            }

            // ── Fasce Orarie ──
            CollapsibleSection(
                title = "⏰ " + stringResource(R.string.settings_time_bands),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.settings_time_bands_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                TimeBandSlider(bands = bands, onBandsChange = { bands = it })
                Spacer(modifier = Modifier.height(8.dp))
                val bandsDirty = savedBands != null && savedBands != bands
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { saveBands() }, enabled = bandsDirty) {
                        Text(stringResource(R.string.settings_time_bands_save))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { resetBands() }) {
                        Text(stringResource(R.string.settings_time_bands_reset))
                    }
                }
                if (bandsMessage.isNotBlank()) {
                    Text(bandsMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Aspetto ──
            CollapsibleSection(
                title = "🎨 " + stringResource(R.string.settings_appearance),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeManager.Mode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeManager.Mode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            ThemeManager.Mode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeManager.Mode.DARK -> stringResource(R.string.settings_theme_dark)
                        }
                        androidx.compose.material3.FilterChip(
                            selected = themeMode == mode,
                            onClick = { ThemeManager.setMode(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // ── Importa / Esporta ──
            CollapsibleSection(
                title = "📥 " + stringResource(R.string.settings_data),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
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
                    headlineContent = { Text(stringResource(R.string.settings_import_csv)) },
                    supportingContent = { Text(stringResource(R.string.settings_import_csv_sub)) },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.settings_import_csv)) },
                    modifier = Modifier.clickable { importCsvLauncher.launch(arrayOf("text/*", "text/csv", "application/json")) }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backup)) },
                    supportingContent = { Text(stringResource(R.string.settings_backup_sub)) },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.settings_backup)) },
                    modifier = Modifier.clickable { runBackup() }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_restore)) },
                    supportingContent = { Text(stringResource(R.string.settings_restore_sub)) },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.settings_restore)) },
                    modifier = Modifier.clickable { restoreLauncher.launch(arrayOf("application/json", "text/*")) }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_delete_all), color = ErrorRed) },
                    supportingContent = { Text(stringResource(R.string.settings_delete_all_sub)) },
                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.settings_delete_all), tint = ErrorRed) },
                    modifier = Modifier.clickable { showDeleteDialog = true }
                )
                if (dataMessage.isNotBlank()) {
                    Text(
                        dataMessage,
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Informazioni ──
            CollapsibleSection(
                title = "ℹ️ " + stringResource(R.string.settings_about),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.settings_about_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Logout ──
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

        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                title = { Text(stringResource(R.string.settings_edit_profile)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(profileFirstName, { profileFirstName = it }, label = { Text(stringResource(R.string.settings_profile_first_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileLastName, { profileLastName = it }, label = { Text(stringResource(R.string.settings_profile_last_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileBirthDate, { profileBirthDate = it }, label = { Text(stringResource(R.string.settings_profile_birth_date)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileGender, { profileGender = it }, label = { Text(stringResource(R.string.settings_profile_gender)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileFiscalCode, { profileFiscalCode = it }, label = { Text(stringResource(R.string.settings_profile_fiscal_code)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profilePhone, { profilePhone = it }, label = { Text(stringResource(R.string.settings_profile_phone)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileStreet, { profileStreet = it }, label = { Text(stringResource(R.string.settings_profile_street)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileStreetNumber, { profileStreetNumber = it }, label = { Text(stringResource(R.string.settings_profile_street_number)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profileCity, { profileCity = it }, label = { Text(stringResource(R.string.settings_profile_city)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(profilePostalCode, { profilePostalCode = it }, label = { Text(stringResource(R.string.settings_profile_postal_code)) }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { TextButton(onClick = { saveProfile() }) { Text(stringResource(R.string.common_save)) } },
                dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
            )
        }

        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text(stringResource(R.string.settings_change_password)) },
                text = {
                    Column {
                        OutlinedTextField(
                            currentPassword, { currentPassword = it },
                            label = { Text(stringResource(R.string.settings_current_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            newPassword, { newPassword = it },
                            label = { Text(stringResource(R.string.settings_new_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        if (passwordMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(passwordMessage, color = ErrorRed)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { changePassword() }) { Text(stringResource(R.string.common_save)) } },
                dismissButton = { TextButton(onClick = { showPasswordDialog = false; passwordMessage = "" }) { Text(stringResource(R.string.common_cancel)) } }
            )
        }

        if (showEmailDialog) {
            AlertDialog(
                onDismissRequest = { showEmailDialog = false },
                title = { Text(stringResource(R.string.settings_change_email)) },
                text = {
                    Column {
                        OutlinedTextField(
                            newEmail, { newEmail = it },
                            label = { Text(stringResource(R.string.settings_new_email)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (emailMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(emailMessage, color = ErrorRed)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { changeEmail() }) { Text(stringResource(R.string.common_save)) } },
                dismissButton = { TextButton(onClick = { showEmailDialog = false; emailMessage = "" }) { Text(stringResource(R.string.common_cancel)) } }
            )
        }

        if (showImportModeDialog) {
            AlertDialog(
                onDismissRequest = { showImportModeDialog = false; pendingImportContent = null },
                title = { Text(stringResource(R.string.settings_import_csv)) },
                text = {
                    Column {
                        Text(stringResource(R.string.settings_import_mode_question))
                        TextButton(onClick = { runImport(CsvImporter.Mode.ADD) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_import_mode_add)) }
                        TextButton(onClick = { runImport(CsvImporter.Mode.SKIP) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_import_mode_skip)) }
                        TextButton(onClick = { runImport(CsvImporter.Mode.OVERWRITE) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_import_mode_overwrite)) }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showImportModeDialog = false; pendingImportContent = null }) { Text(stringResource(R.string.common_cancel)) } }
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

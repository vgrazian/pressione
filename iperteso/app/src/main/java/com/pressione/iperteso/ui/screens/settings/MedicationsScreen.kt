package com.pressione.iperteso.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.ui.components.AppBottomNav
import com.pressione.iperteso.ui.components.AppTab
import com.pressione.iperteso.ui.components.SkeletonLoader
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Dedicated Medications screen — first-class destination for medication tracking.
 * Port of the web app's "Farmaci" section, promoted to its own tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    session: AuthSession,
    onNavigateBack: () -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    viewModel: MedicationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(session) { viewModel.initialize(session.username) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_medications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_add_medication))
            }
        },
        bottomBar = {
            AppBottomNav(current = AppTab.FARMACI, onNavigate = onNavigateTab)
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> SkeletonLoader(modifier = Modifier.padding(paddingValues))
            uiState.medications.isEmpty() -> EmptyMedications(
                modifier = Modifier.padding(paddingValues),
                onAdd = { viewModel.showAddDialog() }
            )
            else -> {
                val active = uiState.medications.filter { it.isActive }
                val historical = uiState.medications.filter { !it.isActive }
                val showHeaders = active.isNotEmpty() && historical.isNotEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showHeaders) {
                        item {
                            Text(
                                stringResource(R.string.medications_active),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(active, key = { it.id }) { med ->
                        MedicationItem(
                            medication = med,
                            onStop = { viewModel.stopMedication(med) },
                            onDelete = { viewModel.deleteMedication(med.id) },
                            onEdit = { viewModel.showEditDialog(med) }
                        )
                    }
                    if (historical.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.medications_history),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        items(historical, key = { it.id }) { med ->
                            MedicationItem(
                                medication = med,
                                onStop = {},
                                onDelete = { viewModel.deleteMedication(med.id) },
                                onEdit = { viewModel.showEditDialog(med) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        MedicationDialog(
            editing = uiState.editingMedication,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { name, activeIngredient, dosage, freq, notes, start, end ->
                viewModel.saveMedication(name, activeIngredient, dosage, freq, notes, start, end)
            }
        )
    }
}

@Composable
private fun EmptyMedications(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💊", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.medications_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_add_medication))
            }
        }
    }
}

@Composable
private fun MedicationItem(
    medication: Medication,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Medication, contentDescription = null,
                tint = if (medication.isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (medication.activeIngredient.isNotBlank()) {
                    Text(
                        medication.activeIngredient,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                val meta = listOf(
                    medication.dosage.takeIf { it.isNotBlank() },
                    medication.frequency.takeIf { it.isNotBlank() }
                ).filterNotNull()
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${dateFormat.format(medication.startDate.atZone(ZoneId.systemDefault()))} — " +
                    if (medication.isActive) stringResource(R.string.settings_medication_in_progress)
                    else dateFormat.format(medication.endDate!!.atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (medication.notes.isNotBlank()) {
                    Text(
                        medication.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            if (medication.isActive) {
                TextButton(onClick = onStop) {
                    Text(stringResource(R.string.settings_medication_stop), style = MaterialTheme.typography.labelSmall)
                }
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
    onSave: (String, String, String, String, String, Instant, Instant?) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var activeIngredient by remember { mutableStateOf(editing?.activeIngredient ?: "") }
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
                OutlinedTextField(value = activeIngredient, onValueChange = { activeIngredient = it },
                    label = { Text(stringResource(R.string.settings_med_active_ingredient)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
            TextButton(onClick = { onSave(name, activeIngredient, dosage, frequency, notes, startDate, endDate) },
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

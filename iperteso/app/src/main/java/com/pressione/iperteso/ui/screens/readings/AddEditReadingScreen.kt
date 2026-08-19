package com.pressione.iperteso.ui.screens.readings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.ui.components.CategoryBadge
import com.pressione.iperteso.ui.theme.ErrorRed
import com.pressione.iperteso.ui.theme.MedicalGreen
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReadingScreen(
    session: AuthSession,
    editingReading: Reading? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddEditReadingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (editingReading != null) {
            viewModel.initializeForEdit(session.username, editingReading)
        } else {
            viewModel.initializeForNew(session.username)
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onNavigateBack()
        }
    }

    val sysFocus = remember { FocusRequester() }
    val diaFocus = remember { FocusRequester() }
    val hrFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (editingReading == null) {
            // Auto-focus the first field for new readings (saves a tap)
            sysFocus.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) stringResource(R.string.add_edit_title_edit)
                        else stringResource(R.string.add_edit_title_new)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Systolic & Diastolic ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = uiState.systolic,
                    onValueChange = { viewModel.updateSystolic(it) },
                    label = { Text(stringResource(R.string.add_edit_systolic)) },
                    isError = uiState.systolicError != null,
                    supportingText = uiState.systolicError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { diaFocus.requestFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(sysFocus)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = uiState.diastolic,
                    onValueChange = { viewModel.updateDiastolic(it) },
                    label = { Text(stringResource(R.string.add_edit_diastolic)) },
                    isError = uiState.diastolicError != null,
                    supportingText = uiState.diastolicError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { hrFocus.requestFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(diaFocus)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Heart Rate ────────────────────────────────
            OutlinedTextField(
                value = uiState.heartRate,
                onValueChange = { viewModel.updateHeartRate(it) },
                label = { Text(stringResource(R.string.add_edit_heart_rate)) },
                isError = uiState.heartRateError != null,
                supportingText = uiState.heartRateError?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(hrFocus)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Live Classification ──────────────────────
            if (uiState.category != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.add_edit_classification),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CategoryBadge(category = uiState.category!!)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Date ──────────────────────────────────────
            var showDatePicker by remember { mutableStateOf(false) }
            val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

            OutlinedTextField(
                value = uiState.date.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.add_edit_date)) },
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = true
            )

            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.add_edit_change_date))
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val newDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                viewModel.updateDate(newDate)
                            }
                            showDatePicker = false
                        }) { Text(stringResource(R.string.common_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Time ──────────────────────────────────────
            var showTimePicker by remember { mutableStateOf(false) }
            val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

            OutlinedTextField(
                value = uiState.time.format(timeFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.add_edit_time)) },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.add_edit_change_time))
            }

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = uiState.time.hour,
                    initialMinute = uiState.time.minute
                )
                DatePickerDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateTime(
                                LocalTime.of(timePickerState.hour, timePickerState.minute)
                            )
                            showTimePicker = false
                        }) { Text(stringResource(R.string.common_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                ) {
                    TimePicker(state = timePickerState)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Notes ─────────────────────────────────────
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text(stringResource(R.string.add_edit_notes)) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Duplicate Error ───────────────────────────
            if (uiState.duplicateError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(uiState.duplicateError!!),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Save Button ───────────────────────────────
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.add_edit_save_description))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isEditing) stringResource(R.string.common_update) else stringResource(R.string.common_save))
            }

            // ── Save Error ───────────────────────────────
            if (uiState.saveError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(uiState.saveError!!),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

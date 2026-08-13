package com.pressione.iperteso.ui.screens.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.ui.components.CategoryBadge
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedReportScreen(
    token: String,
    onNavigateBack: () -> Unit,
    viewModel: SharedReportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(token) { viewModel.loadReport(token) }

    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            !uiState.isPinVerified -> {
                PinGate(
                    uiState = uiState,
                    pin = pin,
                    onPinChange = { pin = it },
                    onVerify = { viewModel.verifyPin(pin) },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                ReportContent(
                    readings = uiState.readings,
                    username = uiState.username,
                    dateFormat = dateFormat,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun errorMessage(error: SharedReportError): String = when (error) {
    SharedReportError.NOT_FOUND -> stringResource(R.string.report_error_not_found)
    SharedReportError.EXPIRED -> stringResource(R.string.report_error_expired)
    SharedReportError.PIN_WRONG -> stringResource(R.string.report_error_pin)
    SharedReportError.GENERIC -> stringResource(R.string.report_error_generic)
}

@Composable
private fun PinGate(
    uiState: SharedReportUiState,
    pin: String,
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.report_pin_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.report_pin_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { onPinChange(it.filter { c -> c.isDigit() }.take(4)) },
            label = { Text(stringResource(R.string.report_pin_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage(uiState.error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onVerify,
            enabled = pin.length == 4,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.report_unlock))
        }
    }
}

@Composable
private fun ReportContent(
    readings: List<Reading>,
    username: String,
    dateFormat: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val sys = readings.map { it.systolic }
    val dia = readings.map { it.diastolic }
    val hr = readings.map { it.heartRate }
    val avgSys = if (sys.isNotEmpty()) sys.average().toFloat() else 0f
    val avgDia = if (dia.isNotEmpty()) dia.average().toFloat() else 0f
    val avgHr = if (hr.isNotEmpty()) hr.average().toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.report_patient) + " $username", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // KPI row
        Row(modifier = Modifier.fillMaxWidth()) {
            StatChip(stringResource(R.string.report_avg_sys), "%.0f".format(avgSys), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            StatChip(stringResource(R.string.report_avg_dia), "%.0f".format(avgDia), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            StatChip(stringResource(R.string.report_avg_bpm), "%.0f".format(avgHr), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatChip(stringResource(R.string.report_total), "${readings.size}", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            val hypertensive = readings.count { it.systolic > 140 || it.diastolic > 90 }
            val load = if (readings.isNotEmpty()) hypertensive.toFloat() / readings.size * 100 else 0f
            StatChip(stringResource(R.string.report_load), "%.0f%%".format(load), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.report_recent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        readings.take(20).forEach { r ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dateFormat.format(r.timestamp), style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${r.systolic}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(" / ", style = MaterialTheme.typography.titleMedium)
                            Text("${r.diastolic}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${r.heartRate} BPM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    CategoryBadge(category = r.category)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

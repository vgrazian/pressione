package com.pressione.iperteso.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.data.repository.toDomainUser
import com.pressione.iperteso.data.remote.api.ReadingReportJson
import com.pressione.iperteso.data.remote.api.SharedReportApi
import com.pressione.iperteso.data.remote.api.SharedReportRequest
import com.pressione.iperteso.data.remote.api.SharedReportResponse
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.domain.model.TimeBand
import com.pressione.iperteso.domain.statistics.Statistics
import com.pressione.iperteso.domain.statistics.StatisticsCalculator
import com.pressione.iperteso.services.LocaleManager
import com.pressione.iperteso.services.PdfReportGenerator
import com.pressione.iperteso.services.TimeBandsStore
import com.pressione.iperteso.ui.components.AppBottomNav
import com.pressione.iperteso.ui.components.AppTab
import com.pressione.iperteso.ui.components.SkeletonLoader
import com.pressione.iperteso.ui.screens.settings.MedicationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalysisScreen(
    session: AuthSession,
    onNavigateBack: () -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    viewModel: AnalysisViewModel = koinViewModel(),
    medicationViewModel: MedicationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medState by medicationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showShareLinkDialog by remember { mutableStateOf(false) }
    var shareLink by remember { mutableStateOf<String?>(null) }
    var sharePin by remember { mutableStateOf<String?>(null) }
    var protectPin by remember { mutableStateOf(false) }
    var anonymizeLink by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf("") }
    var activeLinks by remember { mutableStateOf<List<SharedReportResponse>>(emptyList()) }
    var timeBands by remember { mutableStateOf(TimeBand.defaults()) }

    fun copyToClipboard(text: String) {
        val clip = android.content.ClipData.newPlainText("link", text)
        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
            .setPrimaryClip(clip)
    }

    suspend fun refreshActiveLinks() {
        activeLinks = runCatching { SharedReportApi().getSharedReports(session.username) }
            .getOrDefault(emptyList())
            .filter { r ->
                !r.revoked && runCatching {
                    java.time.Instant.parse(r.expiresAt).isAfter(java.time.Instant.now())
                }.getOrDefault(false)
            }
    }

    suspend fun generatePdfFile(): java.io.File? = runCatching {
        val user = com.pressione.iperteso.IperTesoApplication.instance.database
            .userDao().getUser(session.username)?.toDomainUser()
        PdfReportGenerator.generate(
            context, session.username, uiState.readings, medState.medications, user = user
        )
    }.getOrNull()

    suspend fun exportPdf() {
        withContext(Dispatchers.IO) {
            val file = generatePdfFile()
            withContext(Dispatchers.Main) {
                if (file != null) PdfReportGenerator.sharePdf(context, file)
            }
        }
    }

    suspend fun sharePdfEmail() {
        withContext(Dispatchers.IO) {
            val file = generatePdfFile()
            val s = StatisticsCalculator.computeStatistics(uiState.readings)
            val subject = "Report Pressione - ${session.username}"
            val body = "Report Pressione - ${session.username}\n\n" +
                "Media: ${"%.0f".format(s.avgSystolic)}/${"%.0f".format(s.avgDiastolic)} mmHg\n" +
                "BPM medio: ${"%.0f".format(s.avgHeartRate)}\n" +
                "Misurazioni: ${s.readingsCount}\n\nGenerato automaticamente"
            withContext(Dispatchers.Main) {
                if (file != null) PdfReportGenerator.sharePdfViaEmail(context, file, subject, body)
            }
        }
    }

    suspend fun sharePdfWhatsApp() {
        withContext(Dispatchers.IO) {
            val file = generatePdfFile()
            withContext(Dispatchers.Main) {
                if (file != null) PdfReportGenerator.sharePdfViaWhatsApp(context, file, "Report Pressione - ${session.username}")
            }
        }
    }

    suspend fun generateShareLink() {
        try {
            val token = java.util.UUID.randomUUID().toString().replace("-", "")
            val pinHash: String? = if (protectPin) {
                val pin = (1000..9999).random().toString()
                sharePin = pin
                com.pressione.iperteso.util.PasswordHasher.hash(pin)
            } else {
                sharePin = null
                null
            }
            val api = SharedReportApi()
            val expiresAt = java.time.Instant.now().plusSeconds(48 * 3600).toString()
            val profile = runCatching {
                com.pressione.iperteso.IperTesoApplication.instance.database
                    .userDao().getUser(session.username)
            }.getOrNull()
            val firstName = profile?.firstName?.trim().orEmpty()
            val lastName = profile?.lastName?.trim().orEmpty()
            val displayName = when {
                firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
                firstName.isNotBlank() -> firstName
                lastName.isNotBlank() -> lastName
                else -> session.username
            }
            val reportData = ReadingReportJson.readingsToJson(
                uiState.readings,
                anonymize = anonymizeLink,
                displayName = displayName,
                birthDate = profile?.birthDate,
                gender = profile?.gender
            )
            api.createSharedReport(
                SharedReportRequest(
                    username = session.username,
                    token = token,
                    reportData = reportData,
                    pinHash = pinHash,
                    expiresAt = expiresAt
                )
            )
            shareLink = "https://vgrazian.github.io/pressione/#/share/$token"
            // Auto-copy to clipboard (link + PIN if present)
            val copyText = if (sharePin != null) "$shareLink\nPIN: $sharePin" else shareLink!!
            copyToClipboard(copyText)
            shareMessage = context.getString(R.string.analysis_link_copied)
            showShareLinkDialog = true
            refreshActiveLinks()
        } catch (e: Exception) {
            shareMessage = "Errore: ${e.message}"
        }
    }

    LaunchedEffect(session) { viewModel.initialize(session.username) }

    LaunchedEffect(session.username) { medicationViewModel.initialize(session.username) }

    LaunchedEffect(session.username) {
        val db = com.pressione.iperteso.IperTesoApplication.instance.database
        timeBands = TimeBandsStore.load(session.username, db.settingsDao())
        refreshActiveLinks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analysis_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNav(
                current = AppTab.ANALYSIS,
                onNavigate = onNavigateTab
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            SkeletonLoader(modifier = Modifier.padding(paddingValues))
        } else if (uiState.readings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.analysis_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.analysis_no_data_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ── Report actions (export / share) ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.analysis_report),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { scope.launch { exportPdf() } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.analysis_share_pdf), maxLines = 1)
                            }
                            Button(
                                onClick = { scope.launch { generateShareLink() } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.analysis_link), maxLines = 1)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { scope.launch { sharePdfEmail() } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.analysis_share_email))
                            }
                            OutlinedButton(
                                onClick = { scope.launch { sharePdfWhatsApp() } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.analysis_share_whatsapp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = protectPin, onCheckedChange = { protectPin = it })
                                Text(
                                    stringResource(R.string.analysis_share_pin_opt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = anonymizeLink, onCheckedChange = { anonymizeLink = it })
                                Text(
                                    stringResource(R.string.analysis_share_anonymize),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (activeLinks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.analysis_link_active),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            activeLinks.forEach { link ->
                                val copiedMsg = stringResource(R.string.analysis_link_copied)
                                val revokedMsg = stringResource(R.string.analysis_link_revoked)
                                ActiveLinkRow(
                                    link = link,
                                    onCopy = {
                                        copyToClipboard("https://vgrazian.github.io/pressione/#/share/${link.token}")
                                        shareMessage = copiedMsg
                                    },
                                    onRevoke = {
                                        scope.launch {
                                            runCatching { SharedReportApi().revokeSharedReport(session.username, link.token) }
                                            refreshActiveLinks()
                                            shareMessage = revokedMsg
                                        }
                                    }
                                )
                            }
                        }
                        if (shareMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                shareMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ── Period Selector ──────────────────────────
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = uiState.periodDays == null,
                        onClick = { viewModel.setPeriod(null) },
                        label = {
                            Text(
                                text = stringResource(R.string.readings_filter_all),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    listOf(
                        7 to stringResource(R.string.readings_period_7),
                        30 to stringResource(R.string.readings_period_30),
                        90 to stringResource(R.string.readings_period_90),
                        180 to stringResource(R.string.readings_period_180)
                    ).forEach { (days, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = uiState.periodDays == days,
                            onClick = { viewModel.setPeriod(days) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                // ── Tab Row ──────────────────────────────────
                val tabs = listOf(
                    stringResource(R.string.analysis_tab_trend),
                    stringResource(R.string.analysis_tab_variations),
                    stringResource(R.string.analysis_tab_distribution),
                    stringResource(R.string.analysis_tab_compare),
                    stringResource(R.string.analysis_history)
                )
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.setTab(index) },
                            text = { Text(title, maxLines = 1) }
                        )
                    }
                }

                // ── Tab Content ──────────────────────────────
                val filteredReadings = viewModel.getFilteredReadings()
                val stats = StatisticsCalculator.computeStatistics(filteredReadings)

                when (uiState.selectedTab) {
                    0 -> TrendTab(readings = filteredReadings, stats = stats, medications = medState.medications)
                    1 -> VariationsTab(readings = filteredReadings, stats = stats)
                    2 -> DistributionTab(readings = filteredReadings, bands = timeBands)
                    3 -> ComparisonTab(
                        readings7 = viewModel.getFilteredReadingsForDays(7),
                        readings30 = viewModel.getFilteredReadingsForDays(30),
                        bands = timeBands
                    )
                    else -> HistoryTab(readings = filteredReadings, bands = timeBands)
                }
            }
        }
    }

    // Share link dialog
    if (showShareLinkDialog && shareLink != null) {
        val copiedMsg = stringResource(R.string.analysis_link_copied)
        AlertDialog(
            onDismissRequest = { showShareLinkDialog = false },
            title = { Text(stringResource(R.string.analysis_share_created)) },
            text = {
                Column {
                    Text(stringResource(R.string.analysis_share_hint))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = shareLink!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (sharePin != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "PIN: $sharePin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareLinkDialog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val clip = android.content.ClipData.newPlainText("link", shareLink!!)
                    (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(clip)
                    shareMessage = copiedMsg
                }) {
                    Text(stringResource(R.string.analysis_link_copy))
                }
            }
        )
    }
}

@Composable
private fun TrendTab(readings: List<Reading>, stats: Statistics, medications: List<Medication> = emptyList()) {
    val derivatives = remember(readings) { StatisticsCalculator.computeDerivatives(readings) }
    val surge = remember(readings) { StatisticsCalculator.computeMorningSurge(readings) }
    val medDateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val relevantMeds = remember(readings, medications) {
        val minT = readings.minOfOrNull { it.timestamp.toEpochMilli() } ?: return@remember emptyList()
        val maxT = readings.maxOfOrNull { it.timestamp.toEpochMilli() } ?: return@remember emptyList()
        medications.filter { med ->
            med.startDate.toEpochMilli() in minT..maxT ||
                (med.endDate?.toEpochMilli()?.let { it in minT..maxT } ?: false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Stats KPI grid (SYS/DIA, BPM, count, load) — mirrors web stats-grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                stringResource(R.string.analysis_sysdia_media),
                "%.0f/%.0f".format(stats.avgSystolic, stats.avgDiastolic),
                Modifier.weight(1f)
            )
            StatCard(
                stringResource(R.string.analysis_bpm_medio),
                "%.0f".format(stats.avgHeartRate),
                Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                stringResource(R.string.analysis_count),
                "${stats.readingsCount}",
                Modifier.weight(1f)
            )
            StatCard(
                stringResource(R.string.analysis_load),
                "%.0f%%".format(stats.hypertensiveLoad),
                Modifier.weight(1f),
                valueColor = if (stats.hypertensiveLoad > 30f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        // Rapid-change alert
        if (derivatives.alarmSegments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DerivativeAlertBox(count = derivatives.alarmSegments.size)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trend chart (Canvas-based, no external library)
        if (readings.size >= 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.analysis_trend), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    BpTrendChart(readings = readings, medications = relevantMeds)
                    if (relevantMeds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.nav_medications),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        relevantMeds.forEachIndexed { index, med ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    buildString {
                                        append(med.name)
                                        append("   + ")
                                        append(medDateFormat.format(med.startDate.atZone(ZoneId.systemDefault())))
                                        med.endDate?.let {
                                            append("   - ")
                                            append(medDateFormat.format(it.atZone(ZoneId.systemDefault())))
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.analysis_measurements_hint, readings.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Morning Surge & Hypertensive Load
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.analysis_picco), style = MaterialTheme.typography.labelSmall)
                    Text(
                        "%+.0f mmHg".format(stats.morningSurge),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (surge.alert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.analysis_load), style = MaterialTheme.typography.labelSmall)
                    Text(
                        "%.0f%%".format(stats.hypertensiveLoad),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VariationsTab(readings: List<Reading>, stats: Statistics) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.analysis_variations), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.analysis_variations_sub),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        if (readings.size >= 2) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.analysis_derivative), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    DerivativesBarChart(readings = readings, valueOf = { it.systolic })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.analysis_derivative_dia), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    DerivativesBarChart(readings = readings, valueOf = { it.diastolic })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("%.0f".format(stats.avgSystolic), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.analysis_avg_sys), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${stats.maxSystolic}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.analysis_max_sys), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("%.0f".format(stats.hrv), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Text(stringResource(R.string.analysis_hrv), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributionTab(readings: List<Reading>, bands: List<TimeBand>) {
    // Compute distribution
    val distribution = readings.groupBy { it.category }
        .mapValues { it.value.size }
    val surge = remember(readings) { StatisticsCalculator.computeMorningSurge(readings, bands) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.analysis_distribution),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))

        val isEnglish = LocaleManager.getLanguage(LocalContext.current) == LocaleManager.LANG_ENGLISH

        Category.entries.forEach { category ->
            val count = distribution[category] ?: 0
            if (count > 0 || category == Category.OPTIMAL) {
                val percentage = if (readings.isNotEmpty())
                    (count.toFloat() / readings.size * 100) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEnglish) category.labelEn else category.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "$count",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "(%.0f%%)".format(percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Doughnut chart for ESC/ESH distribution
        if (readings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.analysis_distribution), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryDoughnutChart(distribution = distribution)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Time-of-day cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.analysis_tod_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                bands.chunked(2).forEach { rowBands ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowBands.forEach { band ->
                            TodCard(readings, band, Modifier.weight(1f))
                        }
                        if (rowBands.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (surge.delta != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.analysis_picco_info, "%+.0f".format(surge.delta)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (surge.alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonTab(readings7: List<Reading>, readings30: List<Reading>, bands: List<TimeBand>) {
    val stats7 = StatisticsCalculator.computeStatistics(readings7)
    val stats30 = StatisticsCalculator.computeStatistics(readings30)
    val deriv7 = StatisticsCalculator.computeDerivatives(readings7)
    val deriv30 = StatisticsCalculator.computeDerivatives(readings30)
    val surge7 = StatisticsCalculator.computeMorningSurge(readings7, bands)
    val surge30 = StatisticsCalculator.computeMorningSurge(readings30, bands)

    fun surgeText(s: com.pressione.iperteso.domain.statistics.MorningSurgeResult): String =
        if (s.delta == null) "N/D" else "%+.0f mmHg".format(s.delta)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.analysis_compare_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.analysis_compare_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("", modifier = Modifier.weight(1.2f))
                    Text(
                        stringResource(R.string.analysis_period_7),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        stringResource(R.string.analysis_period_30),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                CompareMetricRow(
                    stringResource(R.string.analysis_count),
                    "${stats7.readingsCount}", "${stats30.readingsCount}"
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_sysdia_media),
                    "%.0f/%.0f".format(stats7.avgSystolic, stats7.avgDiastolic),
                    "%.0f/%.0f".format(stats30.avgSystolic, stats30.avgDiastolic)
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_bpm_medio),
                    "%.0f".format(stats7.avgHeartRate),
                    "%.0f".format(stats30.avgHeartRate)
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_compare_var_up),
                    if (deriv7.maxPositiveRate > 0) "+%.0f mmHg/h".format(deriv7.maxPositiveRate) else "0",
                    if (deriv30.maxPositiveRate > 0) "+%.0f mmHg/h".format(deriv30.maxPositiveRate) else "0",
                    color = MaterialTheme.colorScheme.error
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_compare_var_down),
                    if (deriv7.maxNegativeRate < 0) "%.0f mmHg/h".format(deriv7.maxNegativeRate) else "0",
                    if (deriv30.maxNegativeRate < 0) "%.0f mmHg/h".format(deriv30.maxNegativeRate) else "0",
                    color = Color(0xFFEF6C00)
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_compare_dpdt),
                    "${deriv7.alarmSegments.size}", "${deriv30.alarmSegments.size}",
                    color = if (deriv7.alarmSegments.isNotEmpty() || deriv30.alarmSegments.isNotEmpty()) MaterialTheme.colorScheme.error else null
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_compare_col_load),
                    "${stats7.hypertensiveLoad.toInt()}%", "${stats30.hypertensiveLoad.toInt()}%",
                    color = if (stats7.hypertensiveLoad > 30f || stats30.hypertensiveLoad > 30f) MaterialTheme.colorScheme.error else null
                )
                CompareMetricRow(
                    stringResource(R.string.analysis_compare_picco),
                    surgeText(surge7), surgeText(surge30),
                    color = if (surge7.alert || surge30.alert) MaterialTheme.colorScheme.error else null
                )
            }
        }
    }
}

@Composable
private fun CompareMetricRow(label: String, value7: String, value30: String, color: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value7,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            value30,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DerivativeAlertBox(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚠", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.analysis_alert_derivative, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TodCard(readings: List<Reading>, band: TimeBand, modifier: Modifier = Modifier) {
    val bandReadings = remember(readings, band) {
        readings.filter { band.contains(it.timestamp.atZone(ZoneId.systemDefault()).hour) }
    }
    val avgSys = if (bandReadings.isNotEmpty()) Math.round(bandReadings.map { it.systolic }.average()).toInt() else null
    val avgDia = if (bandReadings.isNotEmpty()) Math.round(bandReadings.map { it.diastolic }.average()).toInt() else null
    val avgHr = if (bandReadings.isNotEmpty()) Math.round(bandReadings.map { it.heartRate }.average()).toInt() else null

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(bandEmoji(band.key), style = MaterialTheme.typography.titleMedium)
            Text(band.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            if (avgSys != null && avgDia != null) {
                Text("$avgSys/$avgDia mmHg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
            }
            if (avgHr != null) {
                Text("$avgHr BPM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                stringResource(R.string.analysis_tod_readings, bandReadings.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryTab(readings: List<Reading>, bands: List<TimeBand>) {
    var grouped by remember { mutableStateOf(false) }
    val isEnglish = LocaleManager.getLanguage(LocalContext.current) == LocaleManager.LANG_ENGLISH
    val sorted = remember(readings) { readings.sortedByDescending { it.timestamp.toEpochMilli() } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.FilterChip(
                selected = !grouped,
                onClick = { grouped = false },
                label = { Text(stringResource(R.string.analysis_history_list)) }
            )
            androidx.compose.material3.FilterChip(
                selected = grouped,
                onClick = { grouped = true },
                label = { Text(stringResource(R.string.analysis_history_grouped)) }
            )
        }

        if (!grouped) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(sorted.take(30)) { r -> ReadingRow(r, isEnglish) }
                if (sorted.size > 30) {
                    item {
                        Text(
                            stringResource(R.string.analysis_history_more, sorted.size - 30),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            val groupedDays = remember(sorted, bands) {
                sorted.groupBy { r ->
                    r.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }.map { (day, dayReadings) ->
                    day to bands.associateWith { band ->
                        dayReadings.filter { band.contains(it.timestamp.atZone(ZoneId.systemDefault()).hour) }
                    }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                groupedDays.take(14).forEach { (day, bandMap) ->
                    item {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    bands.forEach { band ->
                        val rs = bandMap[band].orEmpty()
                        if (rs.isNotEmpty()) {
                            item {
                                Text(
                                    "${bandEmoji(band.key)} ${band.label} (${rs.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(rs) { r -> ReadingRow(r, isEnglish) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(r: Reading, isEnglish: Boolean) {
    val time = r.timestamp.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.4f))
        Text(
            "${r.systolic}/${r.diastolic}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                r.systolic >= 180 || r.diastolic >= 110 -> MaterialTheme.colorScheme.error
                r.systolic >= 140 || r.diastolic >= 90 -> MaterialTheme.colorScheme.error
                r.systolic >= 130 || r.diastolic >= 85 -> Color(0xFFEF6C00)
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        Text("${r.heartRate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.6f))
        Text(
            if (isEnglish) r.category.labelEn else r.category.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ActiveLinkRow(link: SharedReportResponse, onCopy: () -> Unit, onRevoke: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                link.token.take(12) + "…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(
                    R.string.analysis_link_expires,
                    runCatching {
                        java.time.Instant.parse(link.expiresAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    }.getOrDefault(link.expiresAt.take(16).replace("T", " "))
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.analysis_link_copy))
        }
        TextButton(onClick = onRevoke) {
            Text(stringResource(R.string.analysis_link_revoke), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun bandEmoji(key: String): String = when (key) {
    "MORNING" -> "🌅"
    "AFTERNOON" -> "☀️"
    "EVENING" -> "🌇"
    "NIGHT" -> "🌙"
    else -> "⏰"
}

// ── Statistics moved to domain/statistics/StatisticsCalculator.kt ──

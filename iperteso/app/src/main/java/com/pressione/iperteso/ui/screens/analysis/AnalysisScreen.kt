package com.pressione.iperteso.ui.screens.analysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Category
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    session: AuthSession,
    onNavigateBack: () -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    viewModel: AnalysisViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showShareLinkDialog by remember { mutableStateOf(false) }
    var shareLink by remember { mutableStateOf<String?>(null) }
    var sharePin by remember { mutableStateOf<String?>(null) }
    var timeBands by remember { mutableStateOf(TimeBand.defaults()) }

    LaunchedEffect(session) { viewModel.initialize(session.username) }

    LaunchedEffect(session.username) {
        val db = com.pressione.iperteso.IperTesoApplication.instance.database
        timeBands = TimeBandsStore.load(session.username, db.settingsDao())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analysis_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.readings.isNotEmpty()) {
                        // Share via temporary link
                        IconButton(onClick = {
                            scope.launch {
                                val token = java.util.UUID.randomUUID().toString().replace("-", "")
                                val pin = (1000..9999).random().toString()
                                val api = com.pressione.iperteso.data.remote.api.SharedReportApi()
                                val expiresAt = java.time.Instant.now().plusSeconds(48 * 3600).toString()
                                val reportData = com.pressione.iperteso.data.remote.api.ReadingReportJson
                                    .readingsToJson(uiState.readings)
                                api.createSharedReport(
                                    com.pressione.iperteso.data.remote.api.SharedReportRequest(
                                        username = session.username,
                                        token = token,
                                        reportData = reportData,
                                        pin = pin,
                                        expiresAt = expiresAt
                                    )
                                )
                                shareLink = "https://vgrazian.github.io/pressione/#/share/$token"
                                sharePin = pin
                                showShareLinkDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Link, contentDescription = stringResource(R.string.analysis_link))
                        }
                        // Share as PDF
                        IconButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val file = PdfReportGenerator.generate(
                                        context, session.username, uiState.readings, emptyList()
                                    )
                                    withContext(Dispatchers.Main) {
                                        PdfReportGenerator.sharePdf(context, file)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.analysis_share_pdf))
                        }
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
                    Text("📊", style = MaterialTheme.typography.displayLarge)
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
                // ── Period Selector ──────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    listOf(
                        7 to stringResource(R.string.analysis_period_7),
                        30 to stringResource(R.string.analysis_period_30),
                        90 to stringResource(R.string.analysis_period_90)
                    ).forEach { (days, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = uiState.periodDays == days,
                            onClick = { viewModel.setPeriod(days) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                // ── Tab Row ──────────────────────────────────
                val tabs = listOf(
                    stringResource(R.string.analysis_tab_trend),
                    stringResource(R.string.analysis_tab_variations),
                    stringResource(R.string.analysis_tab_distribution),
                    stringResource(R.string.analysis_tab_compare)
                )
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.setTab(index) },
                            text = { Text(title) }
                        )
                    }
                }

                // ── Tab Content ──────────────────────────────
                val filteredReadings = viewModel.getFilteredReadings()
                val stats = StatisticsCalculator.computeStatistics(filteredReadings)

                when (uiState.selectedTab) {
                    0 -> TrendTab(readings = filteredReadings, stats = stats)
                    1 -> VariationsTab(readings = filteredReadings, stats = stats)
                    2 -> DistributionTab(readings = filteredReadings, bands = timeBands)
                    3 -> ComparisonTab(
                        readings7 = viewModel.getFilteredReadingsForDays(7),
                        readings30 = viewModel.getFilteredReadingsForDays(30)
                    )
                }
            }
        }
    }

    // Share link dialog
    if (showShareLinkDialog && shareLink != null) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.analysis_share_app_link) + " iperteso://share/${shareLink!!.substringAfterLast('/')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showShareLinkDialog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }
}

@Composable
private fun TrendTab(readings: List<Reading>, stats: Statistics) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats KPI Row
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(stringResource(R.string.analysis_avg_sys), "%.0f".format(stats.avgSystolic), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            StatCard(stringResource(R.string.analysis_avg_dia), "%.0f".format(stats.avgDiastolic), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            StatCard(stringResource(R.string.analysis_avg_bpm), "%.0f".format(stats.avgHeartRate), Modifier.weight(1f))
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
                    BpTrendChart(readings = readings)
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.analysis_morning_surge), style = MaterialTheme.typography.labelSmall)
                    Text(
                        "%.0f mmHg".format(stats.morningSurge),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                    DerivativesBarChart(readings = readings)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.analysis_by_time), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                // Show time-of-day averages using configurable time bands
                val readingsByTod = readings.groupBy { reading ->
                    val h = reading.timestamp.atZone(ZoneId.systemDefault()).hour
                    bands.firstOrNull { it.contains(h) }?.label
                        ?: stringResource(R.string.analysis_night)
                }
                readingsByTod.forEach { (label, list) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${list.size} " + stringResource(R.string.analysis_readings_suffix), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.analysis_sys_label) + " %.0f".format(list.map { it.systolic }.average()),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonTab(readings7: List<Reading>, readings30: List<Reading>) {
    val stats7 = StatisticsCalculator.computeStatistics(readings7)
    val stats30 = StatisticsCalculator.computeStatistics(readings30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
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
                // Header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.analysis_compare_col_period), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.analysis_compare_col_readings), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(stringResource(R.string.analysis_compare_col_sys), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(stringResource(R.string.analysis_compare_col_dia), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(stringResource(R.string.analysis_compare_col_bpm), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Spacer(modifier = Modifier.height(8.dp))
                CompareRow(stringResource(R.string.analysis_period_7), stats7)
                CompareRow(stringResource(R.string.analysis_period_30), stats30)
            }
        }

        // Hypertensive load comparison
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.analysis_period_7), style = MaterialTheme.typography.labelSmall)
                    Text("${stats7.readingsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.analysis_compare_col_load) + " %.0f%%".format(stats7.hypertensiveLoad),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.analysis_period_30), style = MaterialTheme.typography.labelSmall)
                    Text("${stats30.readingsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.analysis_compare_col_load) + " %.0f%%".format(stats30.hypertensiveLoad),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareRow(period: String, stats: Statistics) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(period, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${stats.readingsCount}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("%.0f".format(stats.avgSystolic), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("%.0f".format(stats.avgDiastolic), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("%.0f".format(stats.avgHeartRate), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Statistics moved to domain/statistics/StatisticsCalculator.kt ──

package com.pressione.iperteso.ui.screens.readings

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.ui.components.AppBottomNav
import com.pressione.iperteso.ui.components.AppTab
import com.pressione.iperteso.ui.components.CategoryBadge
import com.pressione.iperteso.ui.components.ReadingCard
import com.pressione.iperteso.ui.components.SkeletonLoader
import com.pressione.iperteso.ui.theme.ErrorRed
import org.koin.androidx.compose.koinViewModel

// Severity-grouped filter constants (reduces 8 chips to 4)
private val normotensionCategories = setOf(Category.OPTIMAL, Category.NORMAL, Category.HIGH_NORMAL)
private val hypertensionCategories = setOf(Category.GRADE_1, Category.GRADE_2, Category.GRADE_3)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReadingListScreen(
    session: AuthSession,
    onNavigateBack: () -> Unit,
    onEditReading: (String) -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    viewModel: ReadingListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(session) {
        viewModel.initialize(session.username)
    }

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var readingToDelete by remember { mutableStateOf<Reading?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                viewModel.setSearchQuery(it)
                            },
                            onSearch = {},
                            active = true,
                            onActiveChange = {
                                if (!it) {
                                    searchActive = false
                                    searchQuery = ""
                                    viewModel.setSearchQuery("")
                                }
                            },
                            placeholder = { Text(stringResource(R.string.readings_search_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.readings_search))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.setSearchQuery("")
                                    }) {
                                        Icon(Icons.Default.SearchOff, contentDescription = stringResource(R.string.readings_clear))
                                    }
                                }
                            }
                        ) {}
                    } else {
                        Text(stringResource(R.string.readings_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.readings_search))
                        }
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNav(
                current = AppTab.LIST,
                onNavigate = onNavigateTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Category Filter Chips (grouped by severity) ──
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text(stringResource(R.string.readings_filter_all)) }
                )
                // Grouped: only 3 severity bands instead of 7 individual categories
                FilterChip(
                    selected = uiState.selectedCategory in normotensionCategories,
                    onClick = {
                        viewModel.setCategoryFilter(
                            if (uiState.selectedCategory in normotensionCategories) null
                            else Category.OPTIMAL
                        )
                    },
                    label = { Text(stringResource(R.string.readings_filter_normal)) }
                )
                FilterChip(
                    selected = uiState.selectedCategory in hypertensionCategories,
                    onClick = {
                        viewModel.setCategoryFilter(
                            if (uiState.selectedCategory in hypertensionCategories) null
                            else Category.GRADE_1
                        )
                    },
                    label = { Text(stringResource(R.string.readings_filter_hypertension)) }
                )
                FilterChip(
                    selected = uiState.selectedCategory == Category.CRISIS,
                    onClick = {
                        viewModel.setCategoryFilter(
                            if (uiState.selectedCategory == Category.CRISIS) null
                            else Category.CRISIS
                        )
                    },
                    label = { Text(stringResource(R.string.readings_filter_crisis)) }
                )
            }

            // ── Reading List ─────────────────────────────
            if (uiState.isLoading) {
                SkeletonLoader()
            } else if (uiState.readings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.readings_none_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        uiState.readings,
                        key = { it.id }
                    ) { reading ->
                        SwipeableReadingItem(
                            reading = reading,
                            onEdit = { onEditReading(reading.id) },
                            onDelete = { readingToDelete = reading },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }

        // ── Delete Confirmation Dialog ──────────────────
        if (readingToDelete != null) {
            AlertDialog(
                onDismissRequest = { readingToDelete = null },
                title = { Text(stringResource(R.string.readings_delete_title)) },
                text = {
                    Text(stringResource(R.string.readings_delete_message) + "\n" +
                         "${readingToDelete!!.systolic}/${readingToDelete!!.diastolic} " +
                         "— ${readingToDelete!!.heartRate} BPM")
                },
                confirmButton = {
                    TextButton(onClick = {
                        readingToDelete?.let { viewModel.deleteReading(it.id) }
                        readingToDelete = null
                    }) {
                        Text(stringResource(R.string.common_delete), color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { readingToDelete = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableReadingItem(
    reading: Reading,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't dismiss, we show dialog instead
            } else true
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> ErrorRed.copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
                label = "swipeBg"
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = ErrorRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.common_delete), color = ErrorRed, fontWeight = FontWeight.Medium)
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        ReadingCard(
            reading = reading,
            onClick = onEdit
        )
    }
}

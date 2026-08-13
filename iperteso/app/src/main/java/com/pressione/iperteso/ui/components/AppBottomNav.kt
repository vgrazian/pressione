package com.pressione.iperteso.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.R

/**
 * Main tabs (Home, Lista, Analisi, Farmaci, Altro).
 * Admin "Gestione utenti" lives inside Impostazioni, not the nav bar.
 */
enum class AppTab { HOME, LIST, ANALYSIS, FARMACI, SETTINGS }

data class NavItem(val tab: AppTab, val icon: ImageVector, val label: String)

@Composable
fun AppBottomNav(
    current: AppTab,
    onNavigate: (AppTab) -> Unit
) {
    val items = listOf(
        NavItem(AppTab.HOME, Icons.Filled.Home, stringResource(R.string.nav_home)),
        NavItem(AppTab.LIST, Icons.AutoMirrored.Filled.List, stringResource(R.string.nav_list)),
        NavItem(AppTab.ANALYSIS, Icons.Filled.ShowChart, stringResource(R.string.nav_analysis)),
        NavItem(AppTab.FARMACI, Icons.Filled.Medication, stringResource(R.string.nav_medications)),
        NavItem(AppTab.SETTINGS, Icons.Filled.Settings, stringResource(R.string.nav_settings))
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = current == item.tab,
                onClick = { onNavigate(item.tab) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

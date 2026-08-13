package com.pressione.iperteso.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pressione.iperteso.ui.screens.analysis.AnalysisScreen
import com.pressione.iperteso.ui.screens.auth.AuthViewModel
import com.pressione.iperteso.ui.screens.auth.LoginScreen
import com.pressione.iperteso.ui.screens.home.HomeScreen
import com.pressione.iperteso.ui.screens.readings.AddEditReadingScreen
import com.pressione.iperteso.ui.screens.readings.ReadingListScreen
import com.pressione.iperteso.ui.screens.report.SharedReportScreen
import com.pressione.iperteso.ui.screens.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

/**
 * Navigation routes matching the web app's vue-router.
 * Auth guard: all routes after LOGIN check session != null.
 * RBAC guard: admin-only features check session.isAdmin.
 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ADD_READING = "add_reading"
    const val EDIT_READING = "edit_reading/{id}"
    const val READING_LIST = "reading_list"
    const val ANALYSIS = "analysis"
    const val SETTINGS = "settings"
    const val SHARED_REPORT = "shared/{token}"
}

@Composable
fun NavGraph(sharedToken: String? = null) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Deep link: open shared report directly if token provided
    LaunchedEffect(sharedToken) {
        if (sharedToken != null) {
            navController.navigate("shared/$sharedToken") {
                launchSingleTop = true
            }
        }
    }

    // Navigate to home on login success
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { /* handled by LaunchedEffect */ },
                viewModel = authViewModel
            )
        }

        composable(Routes.HOME) {
            val session = authState.session
            if (session != null) {
                HomeScreen(
                    session = session,
                    onNavigateToAdd = { navController.navigate(Routes.ADD_READING) },
                    onNavigateToList = { navController.navigate(Routes.READING_LIST) },
                    onNavigateToAnalysis = { navController.navigate(Routes.ANALYSIS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.ADD_READING) {
            val session = authState.session
            if (session != null) {
                AddEditReadingScreen(
                    session = session,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Routes.EDIT_READING,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val session = authState.session
            if (session != null) {
                AddEditReadingScreen(
                    session = session,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.READING_LIST) {
            val session = authState.session
            if (session != null) {
                ReadingListScreen(
                    session = session,
                    onNavigateBack = { navController.popBackStack() },
                    onEditReading = { id -> navController.navigate("edit_reading/$id") }
                )
            }
        }

        composable(Routes.ANALYSIS) {
            val session = authState.session
            if (session != null) {
                AnalysisScreen(
                    session = session,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.SETTINGS) {
            val session = authState.session
            if (session != null) {
                SettingsScreen(
                    session = session,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = Routes.SHARED_REPORT,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token").orEmpty()
            SharedReportScreen(
                token = token,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

package com.pressione.iperteso.di

import com.pressione.iperteso.IperTesoApplication
import com.pressione.iperteso.data.SessionManager
import com.pressione.iperteso.data.remote.api.AuthApi
import com.pressione.iperteso.data.remote.api.MedicationApi
import com.pressione.iperteso.data.remote.api.ReadingsApi
import com.pressione.iperteso.data.remote.api.SharedReportApi
import com.pressione.iperteso.data.repository.AuthRepository
import com.pressione.iperteso.data.repository.MedicationRepository
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.ui.screens.analysis.AnalysisViewModel
import com.pressione.iperteso.ui.screens.auth.AuthViewModel
import com.pressione.iperteso.ui.screens.home.HomeViewModel
import com.pressione.iperteso.ui.screens.operators.OperatorsViewModel
import com.pressione.iperteso.ui.screens.readings.AddEditReadingViewModel
import com.pressione.iperteso.ui.screens.readings.ReadingListViewModel
import com.pressione.iperteso.ui.screens.settings.MedicationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── API ─────────────────────────────────────────────
    single { AuthApi() }
    single { ReadingsApi() }
    single { MedicationApi() }
    single { SharedReportApi() }

    // ── Database ────────────────────────────────────────
    single { IperTesoApplication.instance.database }
    single { get<com.pressione.iperteso.data.local.AppDatabase>().readingDao() }
    single { get<com.pressione.iperteso.data.local.AppDatabase>().userDao() }
    single { get<com.pressione.iperteso.data.local.AppDatabase>().settingsDao() }

    single { get<com.pressione.iperteso.data.local.AppDatabase>().medicationDao() }

    // ── Repositories ────────────────────────────────────
    single { AuthRepository(get(), get(), get()) }
    single { ReadingRepository(get(), get()) }
    single { MedicationRepository(get(), get()) }

    // ── Session ────────────────────────────────────────
    single { SessionManager(androidContext()) }

    // ── ViewModels ──────────────────────────────────────
    viewModel { AuthViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { ReadingListViewModel(get()) }
    viewModel { AddEditReadingViewModel(get()) }
    viewModel { AnalysisViewModel(get()) }
    viewModel { MedicationViewModel(get()) }
    viewModel { OperatorsViewModel(get()) }
}

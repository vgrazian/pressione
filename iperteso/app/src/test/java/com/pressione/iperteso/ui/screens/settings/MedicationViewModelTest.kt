package com.pressione.iperteso.ui.screens.settings

import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.repository.MedicationRepository
import com.pressione.iperteso.domain.model.Medication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationViewModelTest {

    private lateinit var medicationRepository: MedicationRepository
    private lateinit var settingsDao: SettingsDao
    private lateinit var viewModel: MedicationViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val start = Instant.parse("2026-08-01T08:00:00Z")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        medicationRepository = mockk()
        settingsDao = mockk()
        every { medicationRepository.getMedications(any()) } returns flowOf(emptyList())
        coEvery { medicationRepository.refreshFromServer(any()) } returns Unit
        coEvery { medicationRepository.upsertMedication(any()) } returns Result.success(
            Medication(id = "x", username = "x", name = "x", startDate = Instant.now())
        )
        coEvery { medicationRepository.deleteMedication(any()) } returns Unit
        coEvery { settingsDao.getSetting(any(), any()) } returns null
        coEvery { settingsDao.setSetting(any()) } returns Unit
        viewModel = MedicationViewModel(medicationRepository, settingsDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveMedication adds a new medication and closes the dialog`() = runTest {
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddDialog)

        viewModel.saveMedication("Losartan", "losartan potassico", "50 mg", "1 volta al giorno", "", start, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showAddDialog)
        assertNull(state.editingMedication)

        coVerify {
            medicationRepository.upsertMedication(match {
                it.name == "Losartan" &&
                    it.activeIngredient == "losartan potassico" &&
                    it.dosage == "50 mg" &&
                    it.frequency == "1 volta al giorno"
            })
        }
        coVerify {
            settingsDao.setSetting(match { it.value.contains("Farmaco aggiunto: Losartan") })
        }
    }

    @Test
    fun `saveMedication edits an existing medication keeping its id`() = runTest {
        val existing = Medication(
            id = "m1", username = "test", name = "Vecchio",
            activeIngredient = "", dosage = "20 mg", startDate = start
        )
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showEditDialog(existing)
        viewModel.saveMedication("Nuovo", "principio", "40 mg", "2 volte al giorno", "", start, null)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            medicationRepository.upsertMedication(match {
                it.id == "m1" && it.name == "Nuovo" && it.activeIngredient == "principio"
            })
        }
        coVerify {
            settingsDao.setSetting(match { it.value.contains("Farmaco modificato: Nuovo") })
        }
    }

    @Test
    fun `stopMedication sets end date and logs interruption`() = runTest {
        val medication = Medication(
            id = "m1", username = "test", name = "Losartan",
            activeIngredient = "", dosage = "50 mg", startDate = start
        )
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.stopMedication(medication)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            medicationRepository.upsertMedication(match {
                it.id == "m1" && it.endDate != null
            })
        }
        coVerify {
            settingsDao.setSetting(match { it.value.contains("Farmaco interrotto: Losartan") })
        }
    }

    @Test
    fun `deleteMedication removes and logs removal`() = runTest {
        val medication = Medication(
            id = "m1", username = "test", name = "Losartan",
            activeIngredient = "", dosage = "50 mg", startDate = start
        )
        every { medicationRepository.getMedications("test") } returns flowOf(listOf(medication))
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMedication("m1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { medicationRepository.deleteMedication("m1") }
        coVerify {
            settingsDao.setSetting(match { it.value.contains("Farmaco rimosso: Losartan") })
        }
    }
}

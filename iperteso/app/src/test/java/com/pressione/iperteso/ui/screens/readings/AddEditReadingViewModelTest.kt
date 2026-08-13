package com.pressione.iperteso.ui.screens.readings

import com.pressione.iperteso.R
import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditReadingViewModelTest {

    private lateinit var readingRepository: ReadingRepository
    private lateinit var settingsDao: SettingsDao
    private lateinit var viewModel: AddEditReadingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        readingRepository = mockk()
        settingsDao = mockk()
        coEvery { settingsDao.getSetting(any(), any()) } returns null
        coEvery { settingsDao.deleteSetting(any(), any()) } returns Unit
        viewModel = AddEditReadingViewModel(readingRepository, settingsDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initializeForNew sets empty state`() {
        viewModel.initializeForNew("testuser")

        val state = viewModel.uiState.value
        assertEquals("", state.systolic)
        assertEquals("", state.diastolic)
        assertEquals("", state.heartRate)
        assertEquals("", state.notes)
        assertFalse(state.isEditing)
        assertNull(state.category)
    }

    @Test
    fun `initializeForEdit loads existing data`() {
        val reading = Reading(
            id = "r1",
            username = "testuser",
            systolic = 140,
            diastolic = 90,
            heartRate = 80,
            timestamp = Instant.parse("2026-08-12T08:30:00Z"),
            notes = "Post colazione",
            category = Category.GRADE_1
        )

        viewModel.initializeForEdit("testuser", reading)

        val state = viewModel.uiState.value
        assertEquals("140", state.systolic)
        assertEquals("90", state.diastolic)
        assertEquals("80", state.heartRate)
        assertEquals("Post colazione", state.notes)
        assertTrue(state.isEditing)
        assertEquals("r1", state.editingId)
        assertEquals(Category.GRADE_1, state.category)
    }

    @Test
    fun `updateSystolic clears errors and updates category`() {
        viewModel.initializeForNew("testuser")
        viewModel.updateDiastolic("95")

        viewModel.updateSystolic("150")

        val state = viewModel.uiState.value
        assertEquals("150", state.systolic)
        assertNull(state.systolicError)
        // 150/95 → GRADE_1
        assertEquals(Category.GRADE_1, state.category)
    }

    @Test
    fun `validation fails for invalid systolic`() {
        viewModel.initializeForNew("testuser")
        viewModel.updateDiastolic("80")

        viewModel.updateSystolic("0")

        val state = viewModel.uiState.value
        // Error only set on save
        assertNull(state.systolicError)
    }

    @Test
    fun `save validates range and sets errors`() = runTest {
        viewModel.initializeForNew("testuser")
        viewModel.updateSystolic("500")
        viewModel.updateDiastolic("80")
        viewModel.updateHeartRate("70")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.systolicError != null)
    }

    @Test
    fun `save validates diastolic less than systolic`() = runTest {
        viewModel.initializeForNew("testuser")
        viewModel.updateSystolic("100")
        viewModel.updateDiastolic("120")
        viewModel.updateHeartRate("70")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.diastolicError != null)
        assertEquals(R.string.add_edit_error_dia_less, state.diastolicError)
    }

    @Test
    fun `save succeeds with valid data`() = runTest {
        coEvery { readingRepository.findDuplicate("testuser", any()) } returns null
        coEvery { readingRepository.upsertReading(any()) } returns Result.success(
            Reading("new", "testuser", 120, 80, 72, Instant.now(), "", Category.OPTIMAL)
        )

        viewModel.initializeForNew("testuser")
        viewModel.updateSystolic("120")
        viewModel.updateDiastolic("80")
        viewModel.updateHeartRate("72")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.saved)
    }

    @Test
    fun `save blocks duplicates within 10 minutes`() = runTest {
        val duplicate = Reading("existing", "testuser", 120, 80, 72, Instant.now())
        coEvery { readingRepository.findDuplicate("testuser", any()) } returns duplicate

        viewModel.initializeForNew("testuser")
        viewModel.updateSystolic("120")
        viewModel.updateDiastolic("80")
        viewModel.updateHeartRate("72")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.duplicateError != null)
        assertEquals(R.string.add_edit_error_duplicate, state.duplicateError)
        assertFalse(state.saved)
    }
}

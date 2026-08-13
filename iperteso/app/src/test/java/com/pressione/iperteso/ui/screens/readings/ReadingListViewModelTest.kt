package com.pressione.iperteso.ui.screens.readings

import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingListViewModelTest {

    private lateinit var readingRepository: ReadingRepository
    private lateinit var viewModel: ReadingListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        readingRepository = mockk()
        viewModel = ReadingListViewModel(readingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testReadings() = listOf(
        Reading("r1", "test", 110, 70, 65, Instant.now(), "", Category.OPTIMAL),
        Reading("r2", "test", 140, 90, 72, Instant.now(), "", Category.GRADE_1),
        Reading("r3", "test", 125, 82, 70, Instant.now(), "", Category.NORMAL)
    )

    @Test
    fun `initialize loads readings`() = runTest {
        val readings = testReadings()
        coEvery { readingRepository.getReadings("test") } returns flowOf(readings)

        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.readings.size)
    }

    @Test
    fun `category filter filters readings`() = runTest {
        val readings = testReadings()
        coEvery { readingRepository.getReadings("test") } returns flowOf(readings)
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // GRADE_1 triggers hypertension group filter (GRADE_1 + GRADE_2 + GRADE_3)
        viewModel.setCategoryFilter(Category.GRADE_1)

        val state = viewModel.uiState.value
        assertEquals(1, state.readings.size)
        assertEquals(Category.GRADE_1, state.readings[0].category)
    }

    @Test
    fun `clear category filter shows all`() = runTest {
        val readings = testReadings()
        coEvery { readingRepository.getReadings("test") } returns flowOf(readings)
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // OPTIMAL triggers normotension group filter (OPTIMAL + NORMAL + HIGH_NORMAL)
        viewModel.setCategoryFilter(Category.OPTIMAL)
        assertEquals(2, viewModel.uiState.value.readings.size) // OPTIMAL + NORMAL

        viewModel.setCategoryFilter(null)
        assertEquals(3, viewModel.uiState.value.readings.size)
    }

    @Test
    fun `search query filters by notes`() = runTest {
        val readings = listOf(
            Reading("r1", "test", 120, 80, 72, Instant.now(), "prima di colazione", Category.NORMAL),
            Reading("r2", "test", 130, 85, 75, Instant.now(), "dopo cena", Category.HIGH_NORMAL)
        )
        coEvery { readingRepository.getReadings("test") } returns flowOf(readings)
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSearchQuery("colazione")

        val state = viewModel.uiState.value
        assertEquals(1, state.readings.size)
        assertTrue(state.readings[0].notes.contains("colazione"))
    }

    @Test
    fun `search query filters by systolic value`() = runTest {
        val readings = testReadings()
        coEvery { readingRepository.getReadings("test") } returns flowOf(readings)
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSearchQuery("140")

        val state = viewModel.uiState.value
        assertEquals(1, state.readings.size)
        assertEquals(140, state.readings[0].systolic)
    }

    @Test
    fun `combined filter and search works`() = runTest {
        val readings = listOf(
            Reading("r1", "test", 110, 70, 65, Instant.now(), "mattina", Category.OPTIMAL),
            Reading("r2", "test", 145, 92, 78, Instant.now(), "mattina", Category.GRADE_1),
            Reading("r3", "test", 140, 90, 72, Instant.now(), "sera", Category.GRADE_1)
        )
        coEvery { readingRepository.getReadings("test") } returns flowOf(readings)
        viewModel.initialize("test")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setCategoryFilter(Category.GRADE_1)
        viewModel.setSearchQuery("mattina")

        val state = viewModel.uiState.value
        assertEquals(1, state.readings.size)
        assertEquals(Category.GRADE_1, state.readings[0].category)
        assertTrue(state.readings[0].notes.contains("mattina"))
    }
}

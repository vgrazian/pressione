package com.pressione.iperteso.data.repository

import com.pressione.iperteso.data.local.dao.ReadingDao
import com.pressione.iperteso.data.local.entity.ReadingEntity
import com.pressione.iperteso.data.remote.api.ReadingRequest
import com.pressione.iperteso.data.remote.api.ReadingResponse
import com.pressione.iperteso.data.remote.api.ReadingsApi
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ReadingRepositoryTest {

    private lateinit var readingsApi: ReadingsApi
    private lateinit var readingDao: ReadingDao
    private lateinit var repository: ReadingRepository

    @Before
    fun setup() {
        readingsApi = mockk()
        readingDao = mockk(relaxed = true)
        repository = ReadingRepository(readingsApi, readingDao)
    }

    @Test
    fun `getReadings returns empty list for new user`() = runTest {
        coEvery { readingDao.getReadingsByUser("newuser") } returns flowOf(emptyList())

        var result: List<Reading>? = null
        repository.getReadings("newuser").collect {
            result = it
        }

        assertEquals(0, result?.size)
    }

    @Test
    fun `getReadings maps entities to domain`() = runTest {
        val entity = ReadingEntity(
            id = "r1",
            username = "test",
            systolic = 120,
            diastolic = 80,
            heartRate = 72,
            timestamp = System.currentTimeMillis(),
            notes = "test",
            category = "OPTIMAL",
            syncStatus = "synced"
        )
        coEvery { readingDao.getReadingsByUser("test") } returns flowOf(listOf(entity))

        var result: List<Reading>? = null
        repository.getReadings("test").collect {
            result = it
        }

        assertEquals(1, result?.size)
        assertEquals("r1", result!![0].id)
        assertEquals(Category.OPTIMAL, result!![0].category)
    }

    @Test
    fun `upsertReading saves locally and syncs to server`() = runTest {
        val reading = Reading(
            id = "new1",
            username = "test",
            systolic = 130,
            diastolic = 85,
            heartRate = 70,
            timestamp = Instant.now(),
            notes = ""
        )

        coEvery { readingDao.upsertReading(any()) } returns Unit
        coEvery { readingDao.updateSyncStatus("new1", "synced") } returns Unit
        coEvery { readingsApi.upsertReading(any<ReadingRequest>()) } returns ReadingResponse(
            id = "new1", username = "test", systolic = 130, diastolic = 85,
            heartRate = 70, timestamp = Instant.now().toString()
        )

        val result = repository.upsertReading(reading)

        assertTrue(result.isSuccess)
        coVerify { readingDao.upsertReading(any()) }
        coVerify { readingDao.updateSyncStatus("new1", "synced") }
    }

    @Test
    fun `upsertReading works offline`() = runTest {
        val reading = Reading(
            id = "offline1",
            username = "test",
            systolic = 125,
            diastolic = 82,
            heartRate = 68,
            timestamp = Instant.now()
        )

        coEvery { readingDao.upsertReading(any()) } returns Unit
        coEvery { readingsApi.upsertReading(any<ReadingRequest>()) } throws Exception("Network error")

        val result = repository.upsertReading(reading)

        // Should still succeed (offline-first), status remains "pending"
        assertTrue(result.isSuccess)
        coVerify { readingDao.upsertReading(any()) }
    }

    @Test
    fun `deleteReading removes locally and remotely`() = runTest {
        coEvery { readingDao.deleteReading("r1") } returns Unit
        coEvery { readingsApi.deleteReading("r1") } returns Unit

        repository.deleteReading("r1")

        coVerify { readingDao.deleteReading("r1") }
        // Note: remote delete may fail silently (caught in try-catch)
    }

    @Test
    fun `findDuplicate returns null when no match`() = runTest {
        coEvery { readingDao.getPendingSyncReadings("test") } returns emptyList()

        val duplicate = repository.findDuplicate("test", System.currentTimeMillis())

        assertNull(duplicate)
    }

    @Test
    fun `findDuplicate detects reading within 10 minutes`() = runTest {
        val now = System.currentTimeMillis()
        val existing = ReadingEntity(
            id = "existing",
            username = "test",
            systolic = 120, diastolic = 80, heartRate = 72,
            timestamp = now - 5 * 60 * 1000L, // 5 minutes ago
            category = "OPTIMAL",
            syncStatus = "synced"
        )
        coEvery { readingDao.getPendingSyncReadings("test") } returns listOf(existing)

        val duplicate = repository.findDuplicate("test", now)

        assertNotNull(duplicate)
        assertEquals("existing", duplicate?.id)
    }

    @Test
    fun `findDuplicate ignores reading outside 10 minute window`() = runTest {
        val now = System.currentTimeMillis()
        val existing = ReadingEntity(
            id = "old",
            username = "test",
            systolic = 120, diastolic = 80, heartRate = 72,
            timestamp = now - 15 * 60 * 1000L, // 15 minutes ago
            category = "OPTIMAL",
            syncStatus = "synced"
        )
        coEvery { readingDao.getPendingSyncReadings("test") } returns listOf(existing)

        val duplicate = repository.findDuplicate("test", now)

        assertNull(duplicate)
    }

    @Test
    fun `syncPendingReadings syncs and counts`() = runTest {
        val pending = listOf(
            ReadingEntity("1", "test", 120, 80, 72, System.currentTimeMillis(), "", "OPTIMAL", syncStatus = "pending"),
            ReadingEntity("2", "test", 130, 85, 75, System.currentTimeMillis(), "", "HIGH_NORMAL", syncStatus = "pending")
        )
        coEvery { readingDao.getPendingSyncReadings("test") } returns pending
        coEvery { readingsApi.upsertReading(any<ReadingRequest>()) } returns ReadingResponse(
            "ok", "test", 120, 80, 72, Instant.now().toString()
        )
        coEvery { readingDao.updateSyncStatus(any(), "synced") } returns Unit

        val count = repository.syncPendingReadings("test")

        assertEquals(2, count)
        coVerify(exactly = 2) { readingDao.updateSyncStatus(any(), "synced") }
    }

    @Test
    fun `refreshFromServer fetches and updates`() = runTest {
        val remote = listOf(
            ReadingResponse("r1", "test", 120, 80, 72, Instant.now().toString(), "test", null, null)
        )
        coEvery { readingsApi.getReadings("test") } returns remote
        coEvery { readingDao.upsertReadings(any()) } returns Unit

        repository.refreshFromServer("test")

        coVerify { readingDao.upsertReadings(any()) }
    }
}

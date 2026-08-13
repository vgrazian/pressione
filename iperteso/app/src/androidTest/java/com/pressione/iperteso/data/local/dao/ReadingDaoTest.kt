package com.pressione.iperteso.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pressione.iperteso.data.local.AppDatabase
import com.pressione.iperteso.data.local.entity.ReadingEntity
import com.pressione.iperteso.data.local.entity.SettingEntity
import com.pressione.iperteso.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var readingDao: ReadingDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        readingDao = database.readingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        val reading = ReadingEntity(
            id = "r1", username = "test", systolic = 120, diastolic = 80,
            heartRate = 72, timestamp = 1000L, category = "OPTIMAL", syncStatus = "pending"
        )
        readingDao.upsertReading(reading)

        val result = readingDao.getReadingById("r1")
        assertNotNull(result)
        assertEquals("r1", result?.id)
        assertEquals(120, result?.systolic)
        assertEquals("OPTIMAL", result?.category)
    }

    @Test
    fun upsertReplacesExisting() = runTest {
        val original = ReadingEntity(
            "r1", "test", 120, 80, 72, 1000L, "old", "OPTIMAL", syncStatus = "synced"
        )
        readingDao.upsertReading(original)

        val updated = ReadingEntity(
            "r1", "test", 130, 85, 75, 2000L, "new", "HIGH_NORMAL", syncStatus = "pending"
        )
        readingDao.upsertReading(updated)

        val result = readingDao.getReadingById("r1")
        assertEquals(130, result?.systolic)
        assertEquals("new", result?.notes)
        assertEquals("pending", result?.syncStatus)
    }

    @Test
    fun deleteRemovesRecord() = runTest {
        val reading = ReadingEntity(
            "r1", "test", 120, 80, 72, 1000L, category = "OPTIMAL", syncStatus = "synced"
        )
        readingDao.upsertReading(reading)
        assertNotNull(readingDao.getReadingById("r1"))

        readingDao.deleteReading("r1")

        assertNull(readingDao.getReadingById("r1"))
    }

    @Test
    fun getReadingsByUserOrdersByTimestampDesc() = runTest {
        val r1 = ReadingEntity("r1", "test", 110, 70, 65, 1000L, category = "OPTIMAL", syncStatus = "synced")
        val r2 = ReadingEntity("r2", "test", 120, 80, 72, 3000L, category = "NORMAL", syncStatus = "synced")
        val r3 = ReadingEntity("r3", "test", 130, 85, 75, 2000L, category = "HIGH_NORMAL", syncStatus = "synced")

        readingDao.upsertReading(r1)
        readingDao.upsertReading(r2)
        readingDao.upsertReading(r3)

        val readings = readingDao.getReadingsByUser("test").first()
        assertEquals(3, readings.size)
        assertEquals("r2", readings[0].id) // Most recent first
        assertEquals("r3", readings[1].id)
        assertEquals("r1", readings[2].id)
    }

    @Test
    fun getRecentReadingsLimitsResults() = runTest {
        for (i in 1..10) {
            readingDao.upsertReading(
                ReadingEntity("r$i", "test", 120, 80, 72, i * 1000L, category = "OPTIMAL", syncStatus = "synced")
            )
        }

        val readings = readingDao.getRecentReadings("test", 3).first()
        assertEquals(3, readings.size)
    }

    @Test
    fun getPendingSyncReadingsReturnsOnlyPending() = runTest {
        readingDao.upsertReading(
            ReadingEntity("r1", "test", 120, 80, 72, 1000L, category = "OPTIMAL", syncStatus = "synced")
        )
        readingDao.upsertReading(
            ReadingEntity("r2", "test", 130, 85, 75, 2000L, category = "HIGH_NORMAL", syncStatus = "pending")
        )
        readingDao.upsertReading(
            ReadingEntity("r3", "test", 140, 90, 78, 3000L, category = "GRADE_1", syncStatus = "failed")
        )

        val pending = readingDao.getPendingSyncReadings("test")
        assertEquals(1, pending.size)
        assertEquals("r2", pending[0].id)
    }

    @Test
    fun updateSyncStatus() = runTest {
        val reading = ReadingEntity(
            "r1", "test", 120, 80, 72, 1000L, category = "OPTIMAL", syncStatus = "pending"
        )
        readingDao.upsertReading(reading)

        readingDao.updateSyncStatus("r1", "synced")

        val result = readingDao.getReadingById("r1")
        assertEquals("synced", result?.syncStatus)
    }

    @Test
    fun deleteAllForUser() = runTest {
        readingDao.upsertReading(ReadingEntity("r1", "userA", 120, 80, 72, 1000L, category = "OPTIMAL", syncStatus = "synced"))
        readingDao.upsertReading(ReadingEntity("r2", "userA", 130, 85, 75, 2000L, category = "NORMAL", syncStatus = "synced"))
        readingDao.upsertReading(ReadingEntity("r3", "userB", 140, 90, 78, 3000L, category = "GRADE_1", syncStatus = "synced"))

        readingDao.deleteAllForUser("userA")

        assertEquals(1, readingDao.getReadingCount("userB"))
    }

    @Test
    fun findDuplicateMatchesTimestampWithinRange() = runTest {
        readingDao.upsertReading(
            ReadingEntity("r1", "test", 120, 80, 72, 1_000_000L, category = "OPTIMAL", syncStatus = "synced")
        )

        // Same timestamp
        val dup = readingDao.findDuplicate("test", 1_000_000L)
        assertNotNull(dup)
    }
}

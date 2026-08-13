package com.pressione.iperteso.data.repository

import com.pressione.iperteso.data.local.dao.ReadingDao
import com.pressione.iperteso.data.local.entity.ReadingEntity
import com.pressione.iperteso.data.remote.api.ReadingRequest
import com.pressione.iperteso.data.remote.api.ReadingsApi
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * ReadingRepository — offline-first: writes go to Room first, then sync to Supabase.
 * Mirrors the web app's dataService.js.
 */
class ReadingRepository(
    private val readingsApi: ReadingsApi,
    private val readingDao: ReadingDao
) {
    /**
     * Get all readings for a user, from local Room database.
     */
    fun getReadings(username: String): Flow<List<Reading>> {
        return readingDao.getReadingsByUser(username).map { entities ->
            entities.map { it.toDomainReading() }
        }
    }

    /**
     * Get recent readings (last N).
     */
    fun getRecentReadings(username: String, limit: Int = 5): Flow<List<Reading>> {
        return readingDao.getRecentReadings(username, limit).map { entities ->
            entities.map { it.toDomainReading() }
        }
    }

    /**
     * Get readings in a date range.
     */
    fun getReadingsInRange(username: String, start: Long, end: Long): Flow<List<Reading>> {
        return readingDao.getReadingsInRange(username, start, end).map { entities ->
            entities.map { it.toDomainReading() }
        }
    }

    /**
     * Upsert a reading: save locally first, then sync to server.
     */
    suspend fun upsertReading(reading: Reading): Result<Reading> {
        val category = Category.classify(reading.systolic, reading.diastolic)
        val entity = reading.toEntity(category.name, "pending")

        // Save locally
        readingDao.upsertReading(entity)

        // Try to sync
        try {
            val request = reading.toApiRequest()
            readingsApi.upsertReading(request)
            readingDao.updateSyncStatus(reading.id, "synced")
        } catch (e: Exception) {
            // Will be synced later by WorkManager
        }

        return Result.success(entity.toDomainReading())
    }

    /**
     * Delete a reading locally and remotely.
     */
    suspend fun deleteReading(id: String) {
        readingDao.deleteReading(id)
        try {
            readingsApi.deleteReading(id)
        } catch (_: Exception) { }
    }

    /**
     * Delete all readings for a user.
     */
    suspend fun deleteAllForUser(username: String) {
        readingDao.deleteAllForUser(username)
        try {
            readingsApi.deleteAllForUser(username)
        } catch (_: Exception) { }
    }

    /**
     * Check for duplicate readings within 10 minutes.
     */
    suspend fun findDuplicate(username: String, timestamp: Long): Reading? {
        // Check within ±10 minutes
        val window = 10 * 60 * 1000L
        val entities = readingDao.getPendingSyncReadings(username)
        return entities.find { entity ->
            kotlin.math.abs(entity.timestamp - timestamp) < window
        }?.toDomainReading()
    }

    /**
     * Sync pending readings to Supabase.
     */
    suspend fun syncPendingReadings(username: String): Int {
        val pending = readingDao.getPendingSyncReadings(username)
        var synced = 0
        for (entity in pending) {
            try {
                val request = entity.toApiRequest()
                readingsApi.upsertReading(request)
                readingDao.updateSyncStatus(entity.id, "synced")
                synced++
            } catch (_: Exception) {
                readingDao.updateSyncStatus(entity.id, "failed")
            }
        }
        return synced
    }

    /**
     * Fetch readings from Supabase and merge into Room.
     */
    suspend fun refreshFromServer(username: String) {
        try {
            val remote = readingsApi.getReadings(username)
            val entities = remote.map { it.toEntity() }
            readingDao.upsertReadings(entities)
        } catch (_: Exception) { }
    }
}

// ── Mappers ────────────────────────────────────────────────

fun Reading.toEntity(category: String, syncStatus: String) = ReadingEntity(
    id = id,
    username = username,
    systolic = systolic,
    diastolic = diastolic,
    heartRate = heartRate,
    timestamp = timestamp.toEpochMilli(),
    notes = notes,
    category = category,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    syncStatus = syncStatus
)

fun Reading.toApiRequest() = ReadingRequest(
    id = id,
    username = username,
    systolic = systolic,
    diastolic = diastolic,
    heartRate = heartRate,
    timestamp = timestamp.toString(),
    notes = notes,
    updatedAt = updatedAt.toString()
)

fun ReadingEntity.toDomainReading() = Reading(
    id = id,
    username = username,
    systolic = systolic,
    diastolic = diastolic,
    heartRate = heartRate,
    timestamp = Instant.ofEpochMilli(timestamp),
    notes = notes,
    category = try { Category.valueOf(category) } catch (_: Exception) {
        Category.classify(systolic, diastolic)
    },
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun ReadingEntity.toApiRequest() = ReadingRequest(
    id = id,
    username = username,
    systolic = systolic,
    diastolic = diastolic,
    heartRate = heartRate,
    timestamp = Instant.ofEpochMilli(timestamp).toString(),
    notes = notes,
    updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

fun com.pressione.iperteso.data.remote.api.ReadingResponse.toEntity() = ReadingEntity(
    id = id,
    username = username,
    systolic = systolic,
    diastolic = diastolic,
    heartRate = heartRate,
    timestamp = Instant.parse(timestamp).toEpochMilli(),
    notes = notes ?: "",
    category = "",
    createdAt = createdAt?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
    updatedAt = updatedAt?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
    syncStatus = "synced"
)

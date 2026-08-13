package com.pressione.iperteso.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pressione.iperteso.data.local.entity.ReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Query("SELECT * FROM readings WHERE username = :username ORDER BY timestamp DESC")
    fun getReadingsByUser(username: String): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE username = :username ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(username: String, limit: Int = 5): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun getReadingById(id: String): ReadingEntity?

    @Query("SELECT * FROM readings WHERE username = :username AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getReadingsInRange(username: String, start: Long, end: Long): Flow<List<ReadingEntity>>

    @Query("SELECT COUNT(*) FROM readings WHERE username = :username")
    suspend fun getReadingCount(username: String): Int

    @Query("SELECT * FROM readings WHERE username = :username AND sync_status = 'pending'")
    suspend fun getPendingSyncReadings(username: String): List<ReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReading(reading: ReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadings(readings: List<ReadingEntity>)

    @Update
    suspend fun updateReading(reading: ReadingEntity)

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun deleteReading(id: String)

    @Query("DELETE FROM readings WHERE username = :username")
    suspend fun deleteAllForUser(username: String)

    @Query("UPDATE readings SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT * FROM readings WHERE username = :username AND timestamp = :timestamp")
    suspend fun findDuplicate(username: String, timestamp: Long): ReadingEntity?
}

package com.pressione.iperteso.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pressione.iperteso.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications WHERE username = :username ORDER BY start_date DESC")
    fun getMedicationsByUser(username: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE username = :username AND end_date IS NULL ORDER BY start_date DESC")
    fun getActiveMedications(username: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedication(medication: MedicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedications(medications: List<MedicationEntity>)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: String)

    @Query("DELETE FROM medications WHERE username = :username")
    suspend fun deleteAllForUser(username: String)

    @Query("SELECT * FROM medications WHERE username = :username AND sync_status = 'pending'")
    suspend fun getPendingSyncMedications(username: String): List<MedicationEntity>

    @Query("UPDATE medications SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}

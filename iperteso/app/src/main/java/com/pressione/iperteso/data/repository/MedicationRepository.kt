package com.pressione.iperteso.data.repository

import com.pressione.iperteso.data.local.dao.MedicationDao
import com.pressione.iperteso.data.local.entity.MedicationEntity
import com.pressione.iperteso.data.remote.api.MedicationApi
import com.pressione.iperteso.data.remote.api.MedicationRequest
import com.pressione.iperteso.domain.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class MedicationRepository(
    private val medicationApi: MedicationApi,
    private val medicationDao: MedicationDao
) {
    fun getMedications(username: String): Flow<List<Medication>> {
        return medicationDao.getMedicationsByUser(username).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActiveMedications(username: String): Flow<List<Medication>> {
        return medicationDao.getActiveMedications(username).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun upsertMedication(medication: Medication): Result<Medication> {
        val entity = medication.toEntity("pending")
        medicationDao.upsertMedication(entity)

        try {
            val request = medication.toApiRequest()
            medicationApi.upsertMedication(request)
            medicationDao.updateSyncStatus(medication.id, "synced")
        } catch (_: Exception) { }

        return Result.success(entity.toDomain())
    }

    suspend fun deleteMedication(id: String) {
        medicationDao.deleteMedication(id)
        try { medicationApi.deleteMedication(id) } catch (_: Exception) { }
    }

    suspend fun deleteAllForUser(username: String) {
        medicationDao.deleteAllForUser(username)
    }

    suspend fun refreshFromServer(username: String) {
        try {
            val remote = medicationApi.getMedications(username)
            val entities = remote.map { it.toEntity() }
            medicationDao.upsertMedications(entities)
        } catch (_: Exception) { }
    }

    suspend fun syncPending(username: String): Int {
        val pending = medicationDao.getPendingSyncMedications(username)
        var synced = 0
        for (entity in pending) {
            try {
                medicationApi.upsertMedication(entity.toDomain().toApiRequest())
                medicationDao.updateSyncStatus(entity.id, "synced")
                synced++
            } catch (_: Exception) {
                medicationDao.updateSyncStatus(entity.id, "failed")
            }
        }
        return synced
    }
}

// ── Mappers ────────────────────────────────────────────────

fun Medication.toEntity(syncStatus: String) = MedicationEntity(
    id = id, username = username, name = name,
    activeIngredient = activeIngredient,
    dosage = dosage, frequency = frequency, notes = notes,
    startDate = startDate.toEpochMilli(),
    endDate = endDate?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    syncStatus = syncStatus
)

fun Medication.toApiRequest() = MedicationRequest(
    id = id, username = username, name = name,
    activeIngredient = activeIngredient,
    dosage = dosage, frequency = frequency, notes = notes,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    updatedAt = updatedAt.toString()
)

fun MedicationEntity.toDomain() = Medication(
    id = id, username = username, name = name,
    activeIngredient = activeIngredient,
    dosage = dosage, frequency = frequency, notes = notes,
    startDate = Instant.ofEpochMilli(startDate),
    endDate = endDate?.let { Instant.ofEpochMilli(it) },
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun com.pressione.iperteso.data.remote.api.MedicationResponse.toEntity() = MedicationEntity(
    id = id, username = username, name = name,
    activeIngredient = activeIngredient ?: "",
    dosage = dosage ?: "", frequency = frequency ?: "", notes = notes ?: "",
    startDate = Instant.parse(startDate).toEpochMilli(),
    endDate = endDate?.let { Instant.parse(it)?.toEpochMilli() },
    createdAt = createdAt?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
    updatedAt = updatedAt?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
    syncStatus = "synced"
)

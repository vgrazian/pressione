package com.pressione.iperteso.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for medications, mirroring Supabase schema.
 */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "dosage")
    val dosage: String = "",

    @ColumnInfo(name = "frequency")
    val frequency: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "start_date")
    val startDate: Long, // epoch millis

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null, // null = still active

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending"
)

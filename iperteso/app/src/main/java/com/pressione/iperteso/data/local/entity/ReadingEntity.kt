package com.pressione.iperteso.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for readings. Mirrors the Supabase public.readings table.
 */
@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "systolic")
    val systolic: Int,

    @ColumnInfo(name = "diastolic")
    val diastolic: Int,

    @ColumnInfo(name = "heart_rate")
    val heartRate: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // epoch millis

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "category")
    val category: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending" // "pending", "synced", "failed"
)

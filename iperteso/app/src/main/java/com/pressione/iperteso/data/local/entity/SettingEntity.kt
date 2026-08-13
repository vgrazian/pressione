package com.pressione.iperteso.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Room entity for user settings (key-value pairs).
 * Mirrors the Supabase public.settings table.
 */
@Entity(
    tableName = "settings",
    primaryKeys = ["username", "key"]
)
data class SettingEntity(
    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

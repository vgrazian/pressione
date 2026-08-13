package com.pressione.iperteso.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pressione.iperteso.data.local.entity.SettingEntity

@Dao
interface SettingsDao {

    @Query("SELECT value FROM settings WHERE username = :username AND `key` = :key")
    suspend fun getSetting(username: String, key: String): String?

    @Query("SELECT * FROM settings WHERE username = :username")
    suspend fun getUserSettings(username: String): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE username = :username AND `key` = :key")
    suspend fun deleteSetting(username: String, key: String)

    @Query("DELETE FROM settings WHERE username = :username")
    suspend fun deleteAllForUser(username: String)
}

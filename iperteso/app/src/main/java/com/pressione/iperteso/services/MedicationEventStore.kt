package com.pressione.iperteso.services

import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.local.entity.SettingEntity

/**
 * Pending medication-change events, surfaced as a note on the next reading.
 * When a medication is added, modified, stopped, or removed, an event is queued
 * per user; the first new reading consumes and clears them into its notes field.
 */
object MedicationEventStore {

    private const val KEY = "_med_pending_events"

    suspend fun append(username: String, event: String, settingsDao: SettingsDao) {
        val existing = settingsDao.getSetting(username, KEY) ?: ""
        settingsDao.setSetting(
            SettingEntity(
                username = username,
                key = KEY,
                value = existing + event + "\n"
            )
        )
    }

    suspend fun peekPending(username: String, settingsDao: SettingsDao): List<String> {
        val existing = settingsDao.getSetting(username, KEY) ?: return emptyList()
        return existing.trim().lines().map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun clearPending(username: String, settingsDao: SettingsDao) {
        settingsDao.deleteSetting(username, KEY)
    }

    suspend fun takePending(username: String, settingsDao: SettingsDao): List<String> {
        val events = peekPending(username, settingsDao)
        clearPending(username, settingsDao)
        return events
    }
}

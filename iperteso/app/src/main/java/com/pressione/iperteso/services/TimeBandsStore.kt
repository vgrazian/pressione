package com.pressione.iperteso.services

import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.local.entity.SettingEntity
import com.pressione.iperteso.domain.model.TimeBand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Persists configurable time bands per user (key-value settings).
 * Mirrors the web app's `timeBands.js`.
 */
object TimeBandsStore {

    private const val KEY = "_timeBands"

    suspend fun load(username: String, settingsDao: SettingsDao): List<TimeBand> {
        val stored = settingsDao.getSetting(username, KEY) ?: return TimeBand.defaults()
        return try {
            val arr = Json.parseToJsonElement(stored).jsonArray
            if (arr.size != 4) return TimeBand.defaults()
            arr.map { el ->
                val o = el.jsonObject
                TimeBand(
                    key = o["key"]?.jsonPrimitive?.contentOrNull ?: "MORNING",
                    label = o["label"]?.jsonPrimitive?.contentOrNull ?: "Mattina",
                    startHour = o["start"]?.jsonPrimitive?.int ?: 6,
                    endHour = o["end"]?.jsonPrimitive?.int ?: 12
                )
            }
        } catch (_: Exception) {
            TimeBand.defaults()
        }
    }

    suspend fun save(username: String, bands: List<TimeBand>, settingsDao: SettingsDao) {
        val json = buildJsonArray {
            bands.forEach { b ->
                add(buildJsonObject {
                    put("key", b.key)
                    put("label", b.label)
                    put("start", b.startHour)
                    put("end", b.endHour)
                })
            }
        }.toString()
        settingsDao.setSetting(SettingEntity(username, KEY, json))
    }
}

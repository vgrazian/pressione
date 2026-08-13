package com.pressione.iperteso.services

import com.pressione.iperteso.data.local.AppDatabase
import com.pressione.iperteso.data.repository.MedicationRepository
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * JSON backup / restore of readings + medications + settings.
 * Mirrors the web app's `backupData()` / `restoreData()`.
 */
object BackupService {

    suspend fun export(username: String, db: AppDatabase): String {
        val readings = db.readingDao().getReadingsByUser(username).first()
        val medications = db.medicationDao().getMedicationsByUser(username).first()
        val settings = db.settingsDao().getUserSettings(username)

        val root = buildJsonObject {
            put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("username", username)
            put("readings", buildJsonArray {
                readings.forEach { r ->
                    add(buildJsonObject {
                        put("id", r.id)
                        put("systolic", r.systolic)
                        put("diastolic", r.diastolic)
                        put("heartRate", r.heartRate)
                        put("timestamp", r.timestamp)
                        put("notes", r.notes)
                        put("category", r.category)
                    })
                }
            })
            put("medications", buildJsonArray {
                medications.forEach { m ->
                    add(buildJsonObject {
                        put("id", m.id)
                        put("name", m.name)
                        put("dosage", m.dosage)
                        put("frequency", m.frequency)
                        put("notes", m.notes)
                        put("startDate", m.startDate)
                        m.endDate?.let { put("endDate", it) }
                    })
                }
            })
            put("settings", buildJsonArray {
                settings.forEach { s ->
                    add(buildJsonObject {
                        put("key", s.key)
                        put("value", s.value)
                    })
                }
            })
        }
        return root.toString()
    }

    suspend fun restore(
        username: String,
        json: String,
        readingRepository: ReadingRepository,
        medicationRepository: MedicationRepository,
        db: AppDatabase
    ): Int {
        val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return 0
        var count = 0

        root["readings"]?.jsonArray?.forEach { el ->
            runCatching {
                val o = el.jsonObject
                val ts = o["timestamp"]?.jsonPrimitive?.longOrNull ?: return@runCatching
                readingRepository.upsertReading(
                    Reading(
                        id = o["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString(),
                        username = username,
                        systolic = o["systolic"]?.jsonPrimitive?.contentOrNull?.toInt() ?: 0,
                        diastolic = o["diastolic"]?.jsonPrimitive?.contentOrNull?.toInt() ?: 0,
                        heartRate = o["heartRate"]?.jsonPrimitive?.contentOrNull?.toInt() ?: 0,
                        timestamp = Instant.ofEpochMilli(ts),
                        notes = o["notes"]?.jsonPrimitive?.contentOrNull ?: "",
                        category = o["category"]?.jsonPrimitive?.contentOrNull
                            ?.let { runCatching { Category.valueOf(it) }.getOrNull() } ?: Category.OPTIMAL
                    )
                )
                count++
            }
        }

        root["medications"]?.jsonArray?.forEach { el ->
            runCatching {
                val o = el.jsonObject
                medicationRepository.upsertMedication(
                    Medication(
                        id = o["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString(),
                        username = username,
                        name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching,
                        dosage = o["dosage"]?.jsonPrimitive?.contentOrNull ?: "",
                        frequency = o["frequency"]?.jsonPrimitive?.contentOrNull ?: "",
                        notes = o["notes"]?.jsonPrimitive?.contentOrNull ?: "",
                        startDate = o["startDate"]?.jsonPrimitive?.longOrNull?.let { Instant.ofEpochMilli(it) } ?: Instant.now(),
                        endDate = o["endDate"]?.jsonPrimitive?.longOrNull?.let { Instant.ofEpochMilli(it) }
                    )
                )
                count++
            }
        }

        root["settings"]?.jsonArray?.forEach { el ->
            runCatching {
                val o = el.jsonObject
                val key = o["key"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
                val value = o["value"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
                db.settingsDao().setSetting(
                    com.pressione.iperteso.data.local.entity.SettingEntity(username, key, value)
                )
            }
        }

        return count
    }
}

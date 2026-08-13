package com.pressione.iperteso.data.remote.api

import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Serializes readings to/from the `report_data` JSONB column of shared_reports.
 * Uses manual builders because Reading contains Instant + Category which are not
 * directly @Serializable.
 */
object ReadingReportJson {

    fun readingsToJson(readings: List<Reading>, anonymize: Boolean = false): JsonElement = buildJsonObject {
        put("username", if (anonymize) "" else readings.firstOrNull()?.username ?: "")
        put("anonymize", anonymize)
        put("readings", buildJsonArray {
            readings.forEach { r ->
                add(buildJsonObject {
                    put("id", r.id)
                    put("systolic", r.systolic)
                    put("diastolic", r.diastolic)
                    put("heartRate", r.heartRate)
                    put("timestamp", r.timestamp.toEpochMilli())
                    put("notes", r.notes)
                    put("category", webCategory(r.systolic, r.diastolic))
                })
            }
        })
    }

    fun jsonToReadings(json: JsonElement): Pair<String, List<Reading>> {
        val obj = try { json.jsonObject } catch (_: Exception) { return "" to emptyList() }
        val username = obj["username"]?.jsonPrimitive?.contentOrNull ?: ""
        val arr = obj["readings"]?.jsonArray ?: return username to emptyList()

        val readings = arr.mapNotNull { el ->
            try {
                val o = el.jsonObject
                val ts = parseTimestamp(o["timestamp"]?.jsonPrimitive?.contentOrNull) ?: return@mapNotNull null
                Reading(
                    id = o["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString(),
                    username = username,
                    systolic = o["systolic"]?.jsonPrimitive?.int ?: 0,
                    diastolic = o["diastolic"]?.jsonPrimitive?.int ?: 0,
                    heartRate = o["heartRate"]?.jsonPrimitive?.int
                        ?: o["heart_rate"]?.jsonPrimitive?.int ?: 0,
                    timestamp = ts,
                    notes = o["notes"]?.jsonPrimitive?.contentOrNull ?: "",
                    category = Category.classify(
                        o["systolic"]?.jsonPrimitive?.int ?: 0,
                        o["diastolic"]?.jsonPrimitive?.int ?: 0
                    )
                )
            } catch (_: Exception) { null }
        }
        return username to readings
    }

    /** Accepts either epoch-millis (Long) or an ISO-8601 string timestamp. */
    private fun parseTimestamp(raw: String?): Instant? {
        if (raw == null) return null
        raw.toLongOrNull()?.let { return Instant.ofEpochMilli(it) }
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    /**
     * Maps a reading to the web app's category key (ESC/ESH classification used
     * by the GitHub Pages report viewer). The Android app uses a different enum
     * (OPTIMAL/HIGH_NORMAL/GRADE_*), so we recompute the web category from the
     * raw systolic/diastolic values to keep the two apps interoperable.
     */
    private fun webCategory(systolic: Int, diastolic: Int): String = when {
        systolic >= 180 || diastolic >= 120 -> "HYPERTENSIVE_CRISIS"
        systolic >= 140 || diastolic >= 90 -> "HYPERTENSION_STAGE_2"
        systolic >= 130 || diastolic >= 80 -> "HYPERTENSION_STAGE_1"
        systolic >= 120 && diastolic < 80 -> "ELEVATED"
        systolic < 90 || diastolic < 60 -> "HYPOTENSION"
        systolic < 120 && diastolic < 80 -> "NORMAL"
        else -> "UNCLASSIFIED"
    }
}

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

    fun readingsToJson(readings: List<Reading>): JsonElement = buildJsonObject {
        put("username", readings.firstOrNull()?.username ?: "")
        put("readings", buildJsonArray {
            readings.forEach { r ->
                add(buildJsonObject {
                    put("id", r.id)
                    put("systolic", r.systolic)
                    put("diastolic", r.diastolic)
                    put("heartRate", r.heartRate)
                    put("timestamp", r.timestamp.toEpochMilli())
                    put("notes", r.notes)
                    put("category", r.category.name)
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
                val ts = o["timestamp"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                Reading(
                    id = o["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString(),
                    username = username,
                    systolic = o["systolic"]?.jsonPrimitive?.int ?: 0,
                    diastolic = o["diastolic"]?.jsonPrimitive?.int ?: 0,
                    heartRate = o["heartRate"]?.jsonPrimitive?.int ?: 0,
                    timestamp = Instant.ofEpochMilli(ts),
                    notes = o["notes"]?.jsonPrimitive?.contentOrNull ?: "",
                    category = o["category"]?.jsonPrimitive?.contentOrNull
                        ?.let { runCatching { Category.valueOf(it) }.getOrNull() }
                        ?: Category.classify(o["systolic"]?.jsonPrimitive?.int ?: 0, o["diastolic"]?.jsonPrimitive?.int ?: 0)
                )
            } catch (_: Exception) { null }
        }
        return username to readings
    }
}

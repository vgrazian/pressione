package com.pressione.iperteso.data.remote.api

import com.pressione.iperteso.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Readings API — CRUD operations on public.readings table.
 * Mirrors the web app's dataService.js.
 */
class ReadingsApi {

    private val client = SupabaseClientProvider.client

    suspend fun getReadings(username: String): List<ReadingResponse> {
        return client.from("readings")
            .select {
                filter { eq("username", username) }
                order("timestamp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<ReadingResponse>()
    }

    suspend fun upsertReading(reading: ReadingRequest): ReadingResponse? {
        return client.from("readings")
            .upsert(reading)
            .decodeList<ReadingResponse>()
            .firstOrNull()
    }

    suspend fun deleteReading(id: String) {
        client.from("readings")
            .delete {
                filter { eq("id", id) }
            }
    }

    suspend fun deleteAllForUser(username: String) {
        client.from("readings")
            .delete {
                filter { eq("username", username) }
            }
    }
}

@Serializable
data class ReadingResponse(
    val id: String,
    val username: String,
    @SerialName("systolic")
    val systolic: Int,
    @SerialName("diastolic")
    val diastolic: Int,
    @SerialName("heart_rate")
    val heartRate: Int,
    val timestamp: String, // ISO 8601
    val notes: String? = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ReadingRequest(
    val id: String,
    val username: String,
    @SerialName("systolic")
    val systolic: Int,
    @SerialName("diastolic")
    val diastolic: Int,
    @SerialName("heart_rate")
    val heartRate: Int,
    val timestamp: String, // ISO 8601
    val notes: String? = "",
    @SerialName("updated_at")
    val updatedAt: String? = null
)

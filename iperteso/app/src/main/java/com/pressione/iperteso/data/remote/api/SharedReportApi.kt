package com.pressione.iperteso.data.remote.api

import com.pressione.iperteso.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class SharedReportApi {

    private val client = SupabaseClientProvider.client

    suspend fun getSharedReports(username: String): List<SharedReportResponse> {
        return client.from("shared_reports")
            .select {
                filter { eq("username", username) }
            }
            .decodeList<SharedReportResponse>()
    }

    suspend fun createSharedReport(request: SharedReportRequest): SharedReportResponse? {
        return client.from("shared_reports")
            .insert(request)
            .decodeList<SharedReportResponse>()
            .firstOrNull()
    }

    suspend fun revokeSharedReport(token: String) {
        client.from("shared_reports")
            .update({ set("revoked", true) }) {
                filter { eq("token", token) }
            }
    }

    suspend fun getSharedReportByToken(token: String): SharedReportResponse? {
        return client.from("shared_reports")
            .select {
                filter {
                    eq("token", token)
                    eq("revoked", false)
                }
            }
            .decodeList<SharedReportResponse>()
            .firstOrNull()
    }
}

@Serializable
data class SharedReportResponse(
    val id: String,
    val username: String,
    val token: String,
    @SerialName("report_data")
    val reportData: JsonElement? = null,
    val pin: String? = null,
    @SerialName("pin_hash")
    val pinHash: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("expires_at")
    val expiresAt: String,
    val revoked: Boolean = false
)

@Serializable
data class SharedReportRequest(
    val username: String,
    val token: String,
    @SerialName("report_data")
    val reportData: JsonElement,
    val pin: String? = null,
    @SerialName("pin_hash")
    val pinHash: String? = null,
    @SerialName("expires_at")
    val expiresAt: String
)

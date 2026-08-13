package com.pressione.iperteso.data.remote.api

import com.pressione.iperteso.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Shared-report links, stored in the `settings` table exactly like the web app:
 *   key   = "_share_<token>"
 *   value = JSON: { reportData, pinHash, expiresAt, revoked }
 * This keeps Android and web fully interoperable (a link created on one
 * platform can be opened on the other).
 */
class SharedReportApi {

    private val client = SupabaseClientProvider.client
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun createSharedReport(request: SharedReportRequest) {
        val stored = StoredShareLink(
            reportData = request.reportData,
            pinHash = request.pinHash,
            expiresAt = request.expiresAt,
            revoked = false
        )
        client.from("settings").upsert(
            SettingRow(
                username = request.username,
                key = "_share_" + request.token,
                value = json.encodeToString(stored),
                updatedAt = java.time.Instant.now().toString()
            )
        )
    }

    suspend fun getSharedReports(username: String): List<SharedReportResponse> {
        val rows = client.from("settings")
            .select { filter { eq("username", username) } }
            .decodeList<SettingRow>()
        return rows.mapNotNull { row ->
            if (!row.key.startsWith("_share_")) return@mapNotNull null
            val stored = runCatching { json.decodeFromString<StoredShareLink>(row.value) }.getOrNull()
                ?: return@mapNotNull null
            SharedReportResponse(
                id = row.key,
                username = row.username ?: "",
                token = row.key.removePrefix("_share_"),
                reportData = stored.reportData,
                pinHash = stored.pinHash,
                createdAt = row.updatedAt ?: "",
                expiresAt = stored.expiresAt,
                revoked = stored.revoked
            )
        }
    }

    suspend fun revokeSharedReport(username: String, token: String) {
        val key = "_share_" + token
        val row = client.from("settings")
            .select { filter { eq("username", username); eq("key", key) } }
            .decodeList<SettingRow>()
            .firstOrNull() ?: return
        val stored = runCatching { json.decodeFromString<StoredShareLink>(row.value) }.getOrNull() ?: return
        client.from("settings").upsert(
            row.copy(
                value = json.encodeToString(stored.copy(revoked = true)),
                updatedAt = java.time.Instant.now().toString()
            )
        )
    }

    suspend fun getSharedReportByToken(token: String): SharedReportResponse? {
        val row = client.from("settings")
            .select { filter { eq("key", "_share_" + token) } }
            .decodeList<SettingRow>()
            .firstOrNull() ?: return null
        val stored = runCatching { json.decodeFromString<StoredShareLink>(row.value) }.getOrNull() ?: return null
        if (stored.revoked) return null
        return SharedReportResponse(
            id = row.key,
            username = row.username ?: "",
            token = token,
            reportData = stored.reportData,
            pinHash = stored.pinHash,
            createdAt = row.updatedAt ?: "",
            expiresAt = stored.expiresAt,
            revoked = false
        )
    }
}

@Serializable
data class SettingRow(
    val username: String? = null,
    val key: String,
    val value: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class StoredShareLink(
    val reportData: JsonElement,
    val pinHash: String? = null,
    val expiresAt: String,
    val revoked: Boolean = false
)

@Serializable
data class SharedReportResponse(
    val id: String,
    val username: String,
    val token: String,
    @SerialName("report_data")
    val reportData: JsonElement? = null,
    @SerialName("pin_hash")
    val pinHash: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
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
    @SerialName("pin_hash")
    val pinHash: String? = null,
    @SerialName("expires_at")
    val expiresAt: String
)


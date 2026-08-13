package com.pressione.iperteso.data.remote.api

import com.pressione.iperteso.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MedicationApi {

    private val client = SupabaseClientProvider.client

    suspend fun getMedications(username: String): List<MedicationResponse> {
        return client.from("medications")
            .select {
                filter { eq("username", username) }
                order("start_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<MedicationResponse>()
    }

    suspend fun upsertMedication(medication: MedicationRequest): MedicationResponse? {
        return client.from("medications")
            .upsert(medication)
            .decodeList<MedicationResponse>()
            .firstOrNull()
    }

    suspend fun deleteMedication(id: String) {
        client.from("medications")
            .delete {
                filter { eq("id", id) }
            }
    }
}

@Serializable
data class MedicationResponse(
    val id: String,
    val username: String,
    val name: String,
    val dosage: String? = "",
    val frequency: String? = "",
    val notes: String? = "",
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class MedicationRequest(
    val id: String,
    val username: String,
    val name: String,
    val dosage: String? = "",
    val frequency: String? = "",
    val notes: String? = "",
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

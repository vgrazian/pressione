package com.pressione.iperteso.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model for a medication/prescription record.
 * Tracks what medication the patient takes, when they started/stopped,
 * and dosage — so reports can show medication changes over time.
 */
data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val name: String,               // e.g. "Losartan" (product name)
    val activeIngredient: String = "", // e.g. "losartan potassico"
    val dosage: String = "",        // e.g. "50 mg"
    val frequency: String = "", // e.g. "1 volta al giorno"
    val notes: String = "",     // e.g. "dopo colazione"
    val startDate: Instant,     // when the patient started this medication
    val endDate: Instant? = null, // null = still taking it
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isActive: Boolean get() = endDate == null
}

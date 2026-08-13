package com.pressione.iperteso.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model for a blood pressure reading.
 * Mirrors the web app's reading structure exactly.
 */
data class Reading(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val timestamp: Instant,
    val notes: String = "",
    val category: Category = Category.classify(systolic, diastolic),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

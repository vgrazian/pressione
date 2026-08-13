package com.pressione.iperteso.domain.model

/**
 * Time band definition for day-part grouping.
 * Matches the web app's configurable time bands.
 */
data class TimeBand(
    val key: String,        // "MORNING", "AFTERNOON", "EVENING", "NIGHT"
    val label: String,
    val startHour: Int,     // inclusive
    val endHour: Int        // exclusive
) {
    fun contains(hour: Int): Boolean {
        return if (startHour <= endHour) {
            hour in startHour until endHour
        } else {
            // Overnight band (e.g., 22-6)
            hour >= startHour || hour < endHour
        }
    }

    companion object {
        fun defaults() = listOf(
            TimeBand("MORNING", "Mattina", 6, 12),
            TimeBand("AFTERNOON", "Pomeriggio", 12, 18),
            TimeBand("EVENING", "Sera", 18, 22),
            TimeBand("NIGHT", "Notte", 22, 6)
        )
    }
}

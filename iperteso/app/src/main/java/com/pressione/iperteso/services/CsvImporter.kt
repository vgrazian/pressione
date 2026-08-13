package com.pressione.iperteso.services

import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV import — compatible with both IperTeso and bp-tracker export formats.
 * Mirrors the web app's `importCSV()` in dataService.js.
 */
object CsvImporter {

    data class ImportResult(
        val imported: Int = 0,
        val skipped: Int = 0,
        val overwritten: Int = 0,
        val errors: List<String> = emptyList()
    )

    enum class Mode { ADD, SKIP, OVERWRITE }

    suspend fun import(
        username: String,
        text: String,
        repository: ReadingRepository,
        mode: Mode = Mode.ADD
    ): ImportResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return ImportResult(errors = listOf("CSV vuoto"))

        val header = lines[0].lowercase()
        val isPressione = header.contains("sistolica")
        // bp-tracker: Note before Categoria (col 5 = Note) · IperTeso: Categoria before Note (col 6 = Note)
        val noteCol = if (header.contains("pulsazioni")) 5 else 6

        val existingTimestamps = mutableSetOf<Long>()
        if (mode == Mode.SKIP || mode == Mode.OVERWRITE) {
            val existing = repository.getReadings(username).first()
            existing.forEach { existingTimestamps.add(it.timestamp.toEpochMilli()) }
        }

        var imported = 0
        var skipped = 0
        var overwritten = 0
        val errors = mutableListOf<String>()

        for (i in 1 until lines.size) {
            val cols = lines[i].split(',').map { it.trim().removeSurrounding("\"") }
            if (cols.size < 3) continue
            try {
                var sys: Int; var dia: Int; var hr: Int
                var timestamp: Long
                var notes = ""

                if (isPressione) {
                    val date = cols[0]
                    val time = cols.getOrNull(1) ?: "12:00"
                    sys = cols[2].toInt()
                    dia = cols[3].toInt()
                    hr = cols[4].toInt()
                    notes = cols.getOrNull(noteCol) ?: ""
                    timestamp = parseTimestamp(date, time)
                } else {
                    if (cols[0].contains('/') || cols[0].contains('-')) {
                        sys = cols[2].toInt()
                        dia = cols[3].toInt()
                        hr = cols[4].toInt()
                        notes = cols.getOrNull(5) ?: ""
                        timestamp = parseTimestamp(cols[0], cols.getOrNull(1) ?: "12:00")
                    } else {
                        val ts = parseTimestamp(cols[0])
                        sys = cols[1].toInt()
                        dia = cols[2].toInt()
                        hr = cols[3].toInt()
                        notes = cols.getOrNull(4) ?: ""
                        timestamp = ts
                    }
                }

                if (sys < 1 || sys > 300 || dia < 1 || dia > 200 || hr < 1 || hr > 300) {
                    errors.add("Riga $i: fuori range $sys/$dia $hr")
                    continue
                }

                if (mode == Mode.SKIP && existingTimestamps.contains(timestamp)) {
                    skipped++
                    continue
                }
                if (mode == Mode.OVERWRITE && existingTimestamps.contains(timestamp)) {
                    overwritten++
                }

                repository.upsertReading(
                    Reading(
                        username = username,
                        systolic = sys,
                        diastolic = dia,
                        heartRate = hr,
                        timestamp = java.time.Instant.ofEpochMilli(timestamp),
                        notes = notes
                    )
                )
                imported++
            } catch (e: Exception) {
                errors.add("Riga $i: ${e.message}")
            }
        }

        return ImportResult(imported, skipped, overwritten, errors)
    }

    private fun parseTimestamp(date: String, time: String): Long {
        val d = parseDate(date)
        val t = try { LocalTime.parse(time) } catch (_: Exception) { LocalTime.NOON }
        return LocalDateTime.of(d, t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseTimestamp(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            val d = parseDate(iso)
            d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    private fun parseDate(s: String): LocalDate {
        return try {
            LocalDate.parse(s)
        } catch (_: Exception) {
            // dd/MM/yyyy or dd-MM-yyyy
            val parts = s.split('/', '-').map { it.toInt() }
            LocalDate.of(parts[2], parts[1], parts[0])
        }
    }
}

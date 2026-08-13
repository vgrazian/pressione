package com.pressione.iperteso.domain.statistics

import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.domain.model.TimeBand
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Pure statistics computations, mirroring the web app's `statistics.js`.
 * Extracted so the logic is unit-testable on Android, like on the web.
 */
data class Statistics(
    val avgSystolic: Float = 0f,
    val avgDiastolic: Float = 0f,
    val avgHeartRate: Float = 0f,
    val minSystolic: Int = 0,
    val maxSystolic: Int = 0,
    val minDiastolic: Int = 0,
    val maxDiastolic: Int = 0,
    val minHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val readingsCount: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val timeOfDayDistribution: Map<String, Int> = emptyMap(),
    val averageMorningSystolic: Float? = null,
    val averageAfternoonSystolic: Float? = null,
    val averageEveningSystolic: Float? = null,
    val averageNightSystolic: Float? = null,
    // Convenience aggregates (kept for the Analysis screen)
    val morningSurge: Float = 0f,
    val hypertensiveLoad: Float = 0f,
    val hrv: Float = 0f
)

data class AlarmSegment(
    val index: Int,
    val timestamp: Long,
    val rate: Float,
    val systolic: Int,
    val diastolic: Int
)

data class DerivativesResult(
    val systolic: List<Float> = emptyList(),
    val diastolic: List<Float> = emptyList(),
    val timestamps: List<Long> = emptyList(),
    val maxRate: Float = 0f,
    val maxPositiveRate: Float = 0f,
    val maxNegativeRate: Float = 0f,
    val alarmSegments: List<AlarmSegment> = emptyList()
)

data class MorningSurgeResult(
    val morningAvg: Float? = null,
    val eveningAvg: Float? = null,
    val delta: Float? = null,
    val morningCount: Int = 0,
    val eveningCount: Int = 0,
    val alert: Boolean = false
)

data class HypertensiveLoadResult(
    val percentage: Int = 0,
    val normal: Int = 0,
    val abnormal: Int = 0,
    val total: Int = 0
)

object StatisticsCalculator {

    /** Find the time band for a given hour (NIGHT wraps midnight). */
    fun getBandForHour(hour: Int, bands: List<TimeBand>): TimeBand {
        return bands.firstOrNull { it.contains(hour) } ?: bands.last()
    }

    fun computeStatistics(readings: List<Reading>, bands: List<TimeBand> = TimeBand.defaults()): Statistics {
        if (readings.isEmpty()) return Statistics()

        val systolic = readings.map { it.systolic }
        val diastolic = readings.map { it.diastolic }
        val heartRates = readings.map { it.heartRate }

        val catDist = readings.groupingBy { it.category.name }.eachCount()

        val todDist = linkedMapOf<String, Int>()
        val todSystolic = linkedMapOf<String, MutableList<Int>>()
        for (b in bands) { todDist[b.key] = 0; todSystolic[b.key] = mutableListOf() }
        readings.forEach { r ->
            val hour = r.timestamp.atZone(ZoneId.systemDefault()).hour
            val band = getBandForHour(hour, bands)
            todDist[band.key] = (todDist[band.key] ?: 0) + 1
            todSystolic[band.key]?.add(r.systolic)
        }

        fun avgOrNull(key: String): Float? {
            val values = todSystolic[key] ?: return null
            return if (values.isEmpty()) null else round(values.average().toFloat() * 10) / 10
        }

        val surge = computeMorningSurge(readings, bands)
        val load = computeHypertensiveLoad(readings)
        val hrv = computeHRV(readings) ?: 0f

        return Statistics(
            avgSystolic = round(systolic.average().toFloat() * 10) / 10,
            avgDiastolic = round(diastolic.average().toFloat() * 10) / 10,
            avgHeartRate = round(heartRates.average().toFloat() * 10) / 10,
            minSystolic = systolic.minOrNull() ?: 0,
            maxSystolic = systolic.maxOrNull() ?: 0,
            minDiastolic = diastolic.minOrNull() ?: 0,
            maxDiastolic = diastolic.maxOrNull() ?: 0,
            minHeartRate = heartRates.minOrNull() ?: 0,
            maxHeartRate = heartRates.maxOrNull() ?: 0,
            readingsCount = readings.size,
            categoryDistribution = catDist,
            timeOfDayDistribution = todDist,
            averageMorningSystolic = avgOrNull("MORNING"),
            averageAfternoonSystolic = avgOrNull("AFTERNOON"),
            averageEveningSystolic = avgOrNull("EVENING"),
            averageNightSystolic = avgOrNull("NIGHT"),
            morningSurge = surge.delta ?: 0f,
            hypertensiveLoad = load.percentage.toFloat(),
            hrv = hrv
        )
    }

    /**
     * Discrete derivative dP/dt (mmHg/hour) with 3-point moving-average smoothing.
     * An alarm is raised when |dS/dt| > 10 mmHg/h.
     */
    fun computeDerivatives(readings: List<Reading>): DerivativesResult {
        if (readings.size < 2) return DerivativesResult()

        val sorted = readings.sortedBy { it.timestamp.toEpochMilli() }
        val smoothSys = movingAverage(sorted.map { it.systolic.toFloat() }, 3)
        val smoothDia = movingAverage(sorted.map { it.diastolic.toFloat() }, 3)

        val systolic = mutableListOf<Float>()
        val diastolic = mutableListOf<Float>()
        val timestamps = mutableListOf<Long>()
        val alarms = mutableListOf<AlarmSegment>()
        var maxRate = 0f
        var maxPositive = 0f
        var maxNegative = 0f

        for (i in 1 until sorted.size) {
            val dtHours = (sorted[i].timestamp.toEpochMilli() - sorted[i - 1].timestamp.toEpochMilli()) / (1000.0 * 3600.0)
            if (dtHours <= 0) continue

            val ds = ((smoothSys[i] - smoothSys[i - 1]) / dtHours).toFloat()
            val dd = ((smoothDia[i] - smoothDia[i - 1]) / dtHours).toFloat()

            systolic.add(round(ds * 10) / 10)
            diastolic.add(round(dd * 10) / 10)
            timestamps.add(sorted[i].timestamp.toEpochMilli())
            maxRate = max(maxRate, max(abs(ds), abs(dd)))
            if (ds > 0) maxPositive = max(maxPositive, ds) else maxNegative = minOf(maxNegative, ds)

            if (abs(ds) > 10f) {
                alarms.add(
                    AlarmSegment(
                        index = i - 1,
                        timestamp = sorted[i].timestamp.toEpochMilli(),
                        rate = round(ds * 10) / 10,
                        systolic = sorted[i].systolic,
                        diastolic = sorted[i].diastolic
                    )
                )
            }
        }

        return DerivativesResult(systolic, diastolic, timestamps, maxRate, maxPositive, maxNegative, alarms)
    }

    /**
     * Morning surge: average systolic in the morning band vs evening band.
     */
    fun computeMorningSurge(readings: List<Reading>, bands: List<TimeBand> = TimeBand.defaults()): MorningSurgeResult {
        val morningBand = bands.find { it.key == "MORNING" } ?: bands.firstOrNull()
        val eveningBand = bands.find { it.key == "EVENING" } ?: bands.getOrNull(2)
        if (morningBand == null || eveningBand == null) return MorningSurgeResult()

        val morning = readings.filter { morningBand.contains(it.timestamp.atZone(ZoneId.systemDefault()).hour) }
        val evening = readings.filter { eveningBand.contains(it.timestamp.atZone(ZoneId.systemDefault()).hour) }

        val morningAvg = morning.map { it.systolic }.averageOrNull()
        val eveningAvg = evening.map { it.systolic }.averageOrNull()

        val delta = if (morningAvg != null && eveningAvg != null) round(morningAvg - eveningAvg) else null
        return MorningSurgeResult(
            morningAvg = morningAvg,
            eveningAvg = eveningAvg,
            delta = delta,
            morningCount = morning.size,
            eveningCount = evening.size,
            alert = (delta ?: 0f) > 10f
        )
    }

    /**
     * Hypertensive load: % of readings outside the normal range
     * (systolic >= 140 or diastolic >= 90, or hypotensive <90/<60).
     */
    fun computeHypertensiveLoad(readings: List<Reading>): HypertensiveLoadResult {
        if (readings.isEmpty()) return HypertensiveLoadResult()
        val abnormal = readings.count {
            it.systolic >= 140 || it.diastolic >= 90 || it.systolic < 90 || it.diastolic < 60
        }
        return HypertensiveLoadResult(
            percentage = ((abnormal.toFloat() / readings.size) * 100).toInt(),
            normal = readings.size - abnormal,
            abnormal = abnormal,
            total = readings.size
        )
    }

    /** Simplified HRV: standard deviation of heart rate. */
    fun computeHRV(readings: List<Reading>): Float? {
        if (readings.size < 2) return null
        val hr = readings.map { it.heartRate.toFloat() }
        val mean = hr.average()
        val variance = hr.sumOf { (it - mean) * (it - mean) } / hr.size
        return round(sqrt(variance).toFloat() * 10) / 10
    }

    /** 3-point moving average smoothing. */
    fun movingAverage(values: List<Float>, window: Int = 3): List<Float> {
        if (values.isEmpty()) return emptyList()
        return values.indices.map { i ->
            val start = max(0, i - window / 2)
            val end = min(values.size, i + window / 2 + 1)
            values.subList(start, end).average().toFloat()
        }
    }

    /** Linear regression; returns (slope, intercept) or null if < 2 points. */
    fun linearRegression(points: List<Pair<Float, Float>>): Pair<Float, Float>? {
        val n = points.size
        if (n < 2) return null
        var sumX = 0f; var sumY = 0f; var sumXY = 0f; var sumX2 = 0f
        for ((x, y) in points) {
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x
        }
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        return slope to intercept
    }

    private fun List<Int>.averageOrNull(): Float? =
        if (isEmpty()) null else round(average().toFloat())
}

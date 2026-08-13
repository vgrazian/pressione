package com.pressione.iperteso.domain.statistics

import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.domain.model.TimeBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class StatisticsCalculatorTest {

    private fun reading(
        id: String,
        systolic: Int,
        diastolic: Int,
        heartRate: Int,
        timestamp: Instant
    ) = Reading(
        id = id,
        username = "test",
        systolic = systolic,
        diastolic = diastolic,
        heartRate = heartRate,
        timestamp = timestamp
    )

    private fun at(hour: Int): Instant =
        LocalDateTime.of(2026, 8, 13, hour, 0).atZone(ZoneId.systemDefault()).toInstant()

    // ── computeStatistics ─────────────────────────────────────

    @Test
    fun `empty readings return zeroed statistics`() {
        val stats = StatisticsCalculator.computeStatistics(emptyList())
        assertEquals(0, stats.readingsCount)
        assertEquals(0f, stats.avgSystolic)
        assertEquals(0f, stats.avgDiastolic)
        assertEquals(0, stats.minSystolic)
        assertEquals(0, stats.maxSystolic)
    }

    @Test
    fun `averages are rounded to one decimal`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(9)),
            reading("2", 130, 85, 75, at(10)),
            reading("3", 140, 90, 80, at(11))
        )
        val stats = StatisticsCalculator.computeStatistics(readings)
        assertEquals(130.0f, stats.avgSystolic)
        assertEquals(85.0f, stats.avgDiastolic)
        assertEquals(75.0f, stats.avgHeartRate)
        assertEquals(3, stats.readingsCount)
    }

    @Test
    fun `min and max values are computed`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(9)),
            reading("2", 160, 100, 90, at(10)),
            reading("3", 110, 70, 60, at(11))
        )
        val stats = StatisticsCalculator.computeStatistics(readings)
        assertEquals(110, stats.minSystolic)
        assertEquals(160, stats.maxSystolic)
        assertEquals(70, stats.minDiastolic)
        assertEquals(100, stats.maxDiastolic)
        assertEquals(60, stats.minHeartRate)
        assertEquals(90, stats.maxHeartRate)
    }

    @Test
    fun `category distribution counts each category`() {
        val readings = listOf(
            reading("1", 110, 70, 65, at(9)),   // OPTIMAL
            reading("2", 125, 82, 70, at(10)),  // NORMAL
            reading("3", 135, 88, 75, at(11))   // HIGH_NORMAL
        )
        val stats = StatisticsCalculator.computeStatistics(readings)
        assertEquals(1, stats.categoryDistribution[Category.OPTIMAL.name])
        assertEquals(1, stats.categoryDistribution[Category.NORMAL.name])
        assertEquals(1, stats.categoryDistribution[Category.HIGH_NORMAL.name])
    }

    @Test
    fun `time of day distribution uses configured bands`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(7)),    // MORNING
            reading("2", 120, 80, 70, at(14)),   // AFTERNOON
            reading("3", 120, 80, 70, at(20))    // EVENING
        )
        val stats = StatisticsCalculator.computeStatistics(readings, TimeBand.defaults())
        assertEquals(1, stats.timeOfDayDistribution["MORNING"])
        assertEquals(1, stats.timeOfDayDistribution["AFTERNOON"])
        assertEquals(1, stats.timeOfDayDistribution["EVENING"])
        assertEquals(0, stats.timeOfDayDistribution["NIGHT"])
    }

    @Test
    fun `night band wraps around midnight`() {
        val bands = TimeBand.defaults()
        val night = bands.first { it.key == "NIGHT" }
        assertTrue(night.contains(23))
        assertTrue(night.contains(0))
        assertTrue(night.contains(5))
        assertFalse(night.contains(12))
    }

    @Test
    fun `average morning systolic is computed`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(7)),
            reading("2", 140, 85, 75, at(8))
        )
        val stats = StatisticsCalculator.computeStatistics(readings, TimeBand.defaults())
        assertEquals(130.0f, stats.averageMorningSystolic)
        assertNull(stats.averageEveningSystolic)
    }

    // ── computeMorningSurge ───────────────────────────────────

    @Test
    fun `morning surge computes delta between morning and evening`() {
        val readings = listOf(
            reading("1", 145, 90, 80, at(7)),
            reading("2", 145, 90, 80, at(8)),
            reading("3", 120, 80, 70, at(20)),
            reading("4", 120, 80, 70, at(21))
        )
        val surge = StatisticsCalculator.computeMorningSurge(readings)
        assertNotNull(surge.delta)
        assertEquals(25f, surge.delta!!)
        assertTrue(surge.alert)
        assertEquals(2, surge.morningCount)
        assertEquals(2, surge.eveningCount)
    }

    @Test
    fun `morning surge is null when no evening readings`() {
        val readings = listOf(reading("1", 145, 90, 80, at(7)))
        val surge = StatisticsCalculator.computeMorningSurge(readings)
        assertNull(surge.delta)
        assertFalse(surge.alert)
    }

    @Test
    fun `morning surge does not alert below threshold`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(7)),
            reading("2", 118, 80, 70, at(20))
        )
        val surge = StatisticsCalculator.computeMorningSurge(readings)
        assertFalse(surge.alert)
    }

    // ── computeHypertensiveLoad ───────────────────────────────

    @Test
    fun `hypertensive load counts out-of-range readings`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(9)),
            reading("2", 150, 95, 80, at(10)),
            reading("3", 110, 70, 65, at(11)),
            reading("4", 85, 55, 60, at(12)) // hypotensive
        )
        val load = StatisticsCalculator.computeHypertensiveLoad(readings)
        assertEquals(50, load.percentage)
        assertEquals(2, load.abnormal)
        assertEquals(2, load.normal)
        assertEquals(4, load.total)
    }

    @Test
    fun `hypertensive load is zero for empty readings`() {
        val load = StatisticsCalculator.computeHypertensiveLoad(emptyList())
        assertEquals(0, load.percentage)
        assertEquals(0, load.total)
    }

    // ── computeHRV ────────────────────────────────────────────

    @Test
    fun `hrv is null for fewer than two readings`() {
        assertNull(StatisticsCalculator.computeHRV(listOf(reading("1", 120, 80, 70, at(9)))))
        assertNull(StatisticsCalculator.computeHRV(emptyList()))
    }

    @Test
    fun `hrv computes standard deviation of heart rate`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(9)),
            reading("2", 120, 80, 74, at(10)),
            reading("3", 120, 80, 72, at(11))
        )
        val hrv = StatisticsCalculator.computeHRV(readings)!!
        // mean=72, variance = (4 + 4 + 0)/3 = 2.6667 → sd ≈ 1.6
        assertEquals(1.6f, hrv)
    }

    // ── computeDerivatives ────────────────────────────────────

    @Test
    fun `derivatives are empty for fewer than two readings`() {
        val result = StatisticsCalculator.computeDerivatives(listOf(reading("1", 120, 80, 70, at(9))))
        assertTrue(result.systolic.isEmpty())
        assertTrue(result.alarmSegments.isEmpty())
    }

    @Test
    fun `derivatives compute positive rate`() {
        val readings = listOf(
            reading("1", 120, 80, 70, at(8)),
            reading("2", 125, 85, 75, at(9)),
            reading("3", 130, 90, 80, at(10))
        )
        val result = StatisticsCalculator.computeDerivatives(readings)
        assertEquals(2, result.systolic.size)
        assertTrue(result.maxPositiveRate > 0f)
        assertEquals(0f, result.maxNegativeRate)
        assertTrue(result.alarmSegments.isEmpty()) // ~2.5 mmHg/h < 10
    }

    @Test
    fun `derivatives compute negative rate`() {
        val readings = listOf(
            reading("1", 130, 85, 75, at(8)),
            reading("2", 125, 80, 70, at(9)),
            reading("3", 120, 75, 65, at(10))
        )
        val result = StatisticsCalculator.computeDerivatives(readings)
        assertTrue(result.maxNegativeRate < 0f)
        assertEquals(0f, result.maxPositiveRate)
    }

    @Test
    fun `derivatives flag alarm above 10 mmHg per hour`() {
        val readings = listOf(
            reading("1", 100, 70, 65, at(8)),
            reading("2", 130, 80, 70, at(9)),
            reading("3", 160, 90, 75, at(10))
        )
        val result = StatisticsCalculator.computeDerivatives(readings)
        assertTrue(result.alarmSegments.isNotEmpty())
        assertTrue(result.maxRate > 10f)
    }

    @Test
    fun `derivatives ignore zero time gaps`() {
        val t = Instant.parse("2026-08-13T08:00:00Z")
        val readings = listOf(
            reading("1", 120, 80, 70, t),
            reading("2", 130, 85, 75, t)
        )
        val result = StatisticsCalculator.computeDerivatives(readings)
        assertTrue(result.systolic.isEmpty())
    }

    // ── movingAverage ─────────────────────────────────────────

    @Test
    fun `moving average smooths a window of three`() {
        val values = listOf(1f, 2f, 3f, 4f, 5f)
        val result = StatisticsCalculator.movingAverage(values, 3)
        assertEquals(5, result.size)
        assertEquals(1.5f, result[0], 0.01f) // (1+2)/2
        assertEquals(2.0f, result[1], 0.01f) // (1+2+3)/3
        assertEquals(3.0f, result[2], 0.01f)
        assertEquals(4.0f, result[3], 0.01f)
        assertEquals(4.5f, result[4], 0.01f) // (4+5)/2
    }

    @Test
    fun `moving average of empty list is empty`() {
        assertTrue(StatisticsCalculator.movingAverage(emptyList()).isEmpty())
    }

    // ── linearRegression ──────────────────────────────────────

    @Test
    fun `linear regression returns null for fewer than two points`() {
        assertNull(StatisticsCalculator.linearRegression(listOf(1f to 2f)))
        assertNull(StatisticsCalculator.linearRegression(emptyList()))
    }

    @Test
    fun `linear regression computes slope and intercept`() {
        val points = listOf(1f to 2f, 2f to 4f, 3f to 6f)
        val (slope, intercept) = StatisticsCalculator.linearRegression(points)!!
        assertEquals(2.0f, slope, 0.001f)
        assertEquals(0.0f, intercept, 0.001f)
    }

    // ── getBandForHour ────────────────────────────────────────

    @Test
    fun `getBandForHour falls back to last band`() {
        val bands = listOf(TimeBand("MORNING", "Mattina", 6, 12))
        val band = StatisticsCalculator.getBandForHour(3, bands)
        assertEquals("MORNING", band.key)
    }
}

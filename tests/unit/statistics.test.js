import { describe, it, expect } from 'vitest'
import { computeStatistics, linearRegression, movingAverage } from '@/services/statistics.js'
import { classifyReading } from '@/services/categories.js'

function makeReading(sys, dia, hr, ts) {
    return {
        systolic: sys,
        diastolic: dia,
        heartRate: hr,
        timestamp: ts || new Date('2026-01-01T10:00:00').toISOString(),
        category: classifyReading(sys, dia)
    }
}

describe('computeStatistics', () => {
    it('returns empty stats for empty array', () => {
        const stats = computeStatistics([])
        expect(stats.readingsCount).toBe(0)
        expect(stats.avgSystolic).toBe(0)
    })

    it('computes averages correctly', () => {
        const readings = [
            makeReading(120, 80, 70),
            makeReading(130, 85, 75),
            makeReading(110, 75, 65)
        ]
        const stats = computeStatistics(readings)
        expect(stats.avgSystolic).toBe(120)
        expect(stats.avgDiastolic).toBe(80)
        expect(stats.avgHeartRate).toBe(70)
        expect(stats.readingsCount).toBe(3)
    })

    it('computes min/max correctly', () => {
        const readings = [
            makeReading(140, 90, 80),
            makeReading(100, 60, 60),
            makeReading(120, 80, 70)
        ]
        const stats = computeStatistics(readings)
        expect(stats.minSystolic).toBe(100)
        expect(stats.maxSystolic).toBe(140)
        expect(stats.minDiastolic).toBe(60)
        expect(stats.maxDiastolic).toBe(90)
        expect(stats.minHeartRate).toBe(60)
        expect(stats.maxHeartRate).toBe(80)
    })

    it('computes category distribution', () => {
        const readings = [
            makeReading(110, 70, 65),  // NORMAL
            makeReading(110, 70, 65),  // NORMAL
            makeReading(150, 95, 75)   // STAGE 2
        ]
        const stats = computeStatistics(readings)
        expect(stats.categoryDistribution['NORMAL']).toBe(2)
        expect(stats.categoryDistribution['HYPERTENSION_STAGE_2']).toBe(1)
    })

    it('computes time of day distribution', () => {
        const readings = [
            makeReading(120, 80, 70, '2026-01-01T08:00:00'),  // MORNING
            makeReading(120, 80, 70, '2026-01-01T14:00:00'),  // AFTERNOON
            makeReading(120, 80, 70, '2026-01-01T20:00:00'),  // EVENING
            makeReading(120, 80, 70, '2026-01-01T02:00:00')   // NIGHT
        ]
        const stats = computeStatistics(readings)
        expect(stats.timeOfDayDistribution.MORNING).toBe(1)
        expect(stats.timeOfDayDistribution.AFTERNOON).toBe(1)
        expect(stats.timeOfDayDistribution.EVENING).toBe(1)
        expect(stats.timeOfDayDistribution.NIGHT).toBe(1)
    })
})

describe('linearRegression', () => {
    it('returns null for less than 2 points', () => {
        expect(linearRegression([])).toBeNull()
        expect(linearRegression([{ x: 0, y: 1 }])).toBeNull()
    })

    it('computes slope and intercept', () => {
        const result = linearRegression([
            { x: 0, y: 0 },
            { x: 1, y: 1 },
            { x: 2, y: 2 }
        ])
        expect(result.slope).toBeCloseTo(1)
        expect(result.intercept).toBeCloseTo(0)
    })

    it('handles flat line', () => {
        const result = linearRegression([
            { x: 0, y: 5 },
            { x: 1, y: 5 },
            { x: 2, y: 5 }
        ])
        expect(result.slope).toBeCloseTo(0)
        expect(result.intercept).toBeCloseTo(5)
    })
})

describe('movingAverage', () => {
    it('smooths values', () => {
        const result = movingAverage([1, 2, 3, 4, 5], 3)
        expect(result[0]).toBeCloseTo(1.5)
        expect(result[1]).toBeCloseTo(2)
        expect(result[2]).toBeCloseTo(3)
        expect(result[3]).toBeCloseTo(4)
        expect(result[4]).toBeCloseTo(4.5)
    })
})

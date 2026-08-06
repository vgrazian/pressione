import { describe, it, expect } from 'vitest'
import {
    getDefaultBands,
    getBandForHour,
    buildTimeOfDayDistribution,
    groupReadingsByDayAndBand
} from '@/services/timeBands.js'

describe('getDefaultBands', () => {
    it('returns 4 bands', () => {
        const bands = getDefaultBands()
        expect(bands).toHaveLength(4)
    })

    it('bands have required keys', () => {
        const bands = getDefaultBands()
        const keys = bands.map(b => b.key)
        expect(keys).toEqual(['MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'])
    })

    it('bands have Italian labels', () => {
        const labels = getDefaultBands().map(b => b.label)
        expect(labels).toEqual(['Mattina', 'Pomeriggio', 'Sera', 'Notte'])
    })

    it('day bands have start < end', () => {
        const bands = getDefaultBands()
        for (const b of bands) {
            if (b.key !== 'NIGHT') {
                expect(b.start).toBeLessThan(b.end)
            }
        }
    })

    it('NIGHT band wraps midnight (start > end)', () => {
        const night = getDefaultBands().find(b => b.key === 'NIGHT')
        expect(night.start).toBeGreaterThan(night.end)
        expect(night.start).toBe(22)
        expect(night.end).toBe(6)
    })
})

describe('getBandForHour', () => {
    const defaults = getDefaultBands()

    it('returns MORNING for morning hours', () => {
        expect(getBandForHour(6, defaults).key).toBe('MORNING')
        expect(getBandForHour(8, defaults).key).toBe('MORNING')
        expect(getBandForHour(11, defaults).key).toBe('MORNING')
    })

    it('returns AFTERNOON for afternoon hours', () => {
        expect(getBandForHour(12, defaults).key).toBe('AFTERNOON')
        expect(getBandForHour(14, defaults).key).toBe('AFTERNOON')
        expect(getBandForHour(16, defaults).key).toBe('AFTERNOON')
    })

    it('returns EVENING for evening hours', () => {
        expect(getBandForHour(17, defaults).key).toBe('EVENING')
        expect(getBandForHour(19, defaults).key).toBe('EVENING')
        expect(getBandForHour(21, defaults).key).toBe('EVENING')
    })

    it('returns NIGHT for night hours (wrapping midnight)', () => {
        expect(getBandForHour(22, defaults).key).toBe('NIGHT')
        expect(getBandForHour(0, defaults).key).toBe('NIGHT')
        expect(getBandForHour(3, defaults).key).toBe('NIGHT')
        expect(getBandForHour(5, defaults).key).toBe('NIGHT')
    })

    it('works with custom bands', () => {
        const custom = [
            { key: 'DAY', label: 'Giorno', start: 8, end: 20 },
            { key: 'NIGHT', label: 'Notte', start: 20, end: 8 }
        ]
        expect(getBandForHour(12, custom).key).toBe('DAY')
        expect(getBandForHour(22, custom).key).toBe('NIGHT')
        expect(getBandForHour(4, custom).key).toBe('NIGHT')
    })

    it('returns last band as fallback for empty bands', () => {
        const single = [{ key: 'ALL', label: 'Tutto', start: 0, end: 24 }]
        expect(getBandForHour(15, single).key).toBe('ALL')
    })
})

describe('buildTimeOfDayDistribution', () => {
    const bands = getDefaultBands()

    it('returns zero counts for empty readings', () => {
        const { dist } = buildTimeOfDayDistribution([], bands)
        expect(dist.MORNING).toBe(0)
        expect(dist.AFTERNOON).toBe(0)
        expect(dist.EVENING).toBe(0)
        expect(dist.NIGHT).toBe(0)
    })

    it('counts readings in correct bands', () => {
        const readings = [
            { timestamp: '2026-01-01T08:00:00', systolic: 120 },
            { timestamp: '2026-01-01T08:30:00', systolic: 125 },
            { timestamp: '2026-01-01T14:00:00', systolic: 130 },
            { timestamp: '2026-01-01T23:00:00', systolic: 115 },
        ]
        const { dist, systolicByBand } = buildTimeOfDayDistribution(readings, bands)
        expect(dist.MORNING).toBe(2)
        expect(dist.AFTERNOON).toBe(1)
        expect(dist.NIGHT).toBe(1)
        expect(dist.EVENING).toBe(0)
        expect(systolicByBand.MORNING).toEqual([120, 125])
    })
})

describe('groupReadingsByDayAndBand', () => {
    const bands = getDefaultBands()

    it('returns empty array for empty readings', () => {
        expect(groupReadingsByDayAndBand([], bands)).toEqual([])
    })

    it('groups readings by date and band', () => {
        const readings = [
            { timestamp: '2026-01-01T08:00:00', systolic: 120, diastolic: 80, heartRate: 70, id: '1' },
            { timestamp: '2026-01-01T14:00:00', systolic: 130, diastolic: 85, heartRate: 72, id: '2' },
            { timestamp: '2026-01-02T08:00:00', systolic: 118, diastolic: 78, heartRate: 68, id: '3' },
        ]
        const grouped = groupReadingsByDayAndBand(readings, bands)

        expect(grouped).toHaveLength(2)

        // Find entries by date
        const day1 = grouped.find(g => g.date === new Date('2026-01-01').toLocaleDateString('it-IT'))
        const day2 = grouped.find(g => g.date === new Date('2026-01-02').toLocaleDateString('it-IT'))

        expect(day1).toBeDefined()
        expect(day2).toBeDefined()
        expect(day1.bands.MORNING).toHaveLength(1)
        expect(day1.bands.AFTERNOON).toHaveLength(1)
        expect(day2.bands.MORNING).toHaveLength(1)
    })

    it('sorts readings newest first within groups', () => {
        const readings = [
            { timestamp: '2026-01-01T08:00:00', systolic: 120, diastolic: 80, heartRate: 70, id: 'old' },
            { timestamp: '2026-01-01T09:00:00', systolic: 125, diastolic: 82, heartRate: 72, id: 'new' },
        ]
        const grouped = groupReadingsByDayAndBand(readings, bands)
        const date = new Date('2026-01-01').toLocaleDateString('it-IT')
        const dayEntry = grouped.find(g => g.date === date)

        expect(dayEntry).toBeDefined()
        // Should be sorted newest first
        expect(dayEntry.bands.MORNING[0].id).toBe('new')
        expect(dayEntry.bands.MORNING[1].id).toBe('old')
    })
})

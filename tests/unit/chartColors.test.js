import { describe, it, expect } from 'vitest'
import { getChartColors } from '@/services/chartColors.js'

describe('getChartColors', () => {
    it('returns an object with all expected keys', () => {
        const colors = getChartColors()
        expect(colors).toHaveProperty('systolic')
        expect(colors).toHaveProperty('diastolic')
        expect(colors).toHaveProperty('bpm')
        expect(colors).toHaveProperty('systolicBg')
        expect(colors).toHaveProperty('diastolicBg')
        expect(colors).toHaveProperty('bpmBg')
        expect(colors).toHaveProperty('textPrimary')
        expect(colors).toHaveProperty('textSecondary')
        expect(colors).toHaveProperty('targetZoneBg')
        expect(colors).toHaveProperty('sys140Line')
        expect(colors).toHaveProperty('categoryMap')
        expect(colors).toHaveProperty('isDark')
    })

    it('systolic color is not empty', () => {
        const colors = getChartColors()
        expect(colors.systolic).toBeTruthy()
        expect(typeof colors.systolic).toBe('string')
    })

    it('diastolic color is not empty', () => {
        const colors = getChartColors()
        expect(colors.diastolic).toBeTruthy()
        expect(typeof colors.diastolic).toBe('string')
    })

    it('bpm color is not empty', () => {
        const colors = getChartColors()
        expect(colors.bpm).toBeTruthy()
        expect(typeof colors.bpm).toBe('string')
    })

    it('background colors have rgba format', () => {
        const colors = getChartColors()
        expect(colors.systolicBg).toMatch(/^rgba\(/)
        expect(colors.diastolicBg).toMatch(/^rgba\(/)
        expect(colors.bpmBg).toMatch(/^rgba\(/)
    })

    it('target zone background has rgba format', () => {
        const colors = getChartColors()
        expect(colors.targetZoneBg).toMatch(/^rgba\(/)
    })

    it('threshold line is a valid CSS color', () => {
        const colors = getChartColors()
        expect(colors.sys140Line).toBeTruthy()
        expect(typeof colors.sys140Line).toBe('string')
    })

    it('text colors are non-empty strings', () => {
        const colors = getChartColors()
        expect(colors.textPrimary).toBeTruthy()
        expect(colors.textSecondary).toBeTruthy()
        expect(typeof colors.textPrimary).toBe('string')
        expect(typeof colors.textSecondary).toBe('string')
    })

    it('returns consistent results on multiple calls', () => {
        const a = getChartColors()
        const b = getChartColors()
        expect(a.systolic).toBe(b.systolic)
        expect(a.diastolic).toBe(b.diastolic)
        expect(a.bpm).toBe(b.bpm)
    })

    it('categoryMap has all 6 categories', () => {
        const { categoryMap } = getChartColors()
        expect(Object.keys(categoryMap)).toHaveLength(6)
        expect(categoryMap.NORMAL).toBeTruthy()
        expect(categoryMap.HYPERTENSION_STAGE_2).toBeTruthy()
        expect(categoryMap.HYPERTENSIVE_CRISIS).toBeTruthy()
    })
})

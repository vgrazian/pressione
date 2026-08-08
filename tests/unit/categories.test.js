import { describe, it, expect } from 'vitest'
import { classifyReading, getCategoryLabel, getCategoryColor, ReadingCategory } from '@/services/categories.js'

describe('classifyReading', () => {
    it('classifies normal blood pressure', () => {
        expect(classifyReading(110, 70)).toBe(ReadingCategory.NORMAL)
        expect(classifyReading(119, 79)).toBe(ReadingCategory.NORMAL)
    })

    it('classifies elevated blood pressure', () => {
        expect(classifyReading(120, 79)).toBe(ReadingCategory.ELEVATED)
        expect(classifyReading(129, 70)).toBe(ReadingCategory.ELEVATED)
    })

    it('classifies hypertension stage 1', () => {
        expect(classifyReading(130, 80)).toBe(ReadingCategory.HYPERTENSION_STAGE_1)
        expect(classifyReading(135, 85)).toBe(ReadingCategory.HYPERTENSION_STAGE_1)
        expect(classifyReading(130, 70)).toBe(ReadingCategory.HYPERTENSION_STAGE_1)
        expect(classifyReading(120, 85)).toBe(ReadingCategory.HYPERTENSION_STAGE_1)
    })

    it('classifies hypertension stage 2', () => {
        expect(classifyReading(140, 90)).toBe(ReadingCategory.HYPERTENSION_STAGE_2)
        expect(classifyReading(160, 95)).toBe(ReadingCategory.HYPERTENSION_STAGE_2)
        expect(classifyReading(140, 80)).toBe(ReadingCategory.HYPERTENSION_STAGE_2)
        expect(classifyReading(120, 95)).toBe(ReadingCategory.HYPERTENSION_STAGE_2)
    })

    it('classifies hypertensive crisis', () => {
        expect(classifyReading(180, 110)).toBe(ReadingCategory.HYPERTENSIVE_CRISIS)
        expect(classifyReading(200, 120)).toBe(ReadingCategory.HYPERTENSIVE_CRISIS)
        expect(classifyReading(180, 80)).toBe(ReadingCategory.HYPERTENSIVE_CRISIS)
        expect(classifyReading(120, 120)).toBe(ReadingCategory.HYPERTENSIVE_CRISIS)
    })

    it('classifies hypotension', () => {
        expect(classifyReading(85, 55)).toBe(ReadingCategory.HYPOTENSION)
        expect(classifyReading(80, 70)).toBe(ReadingCategory.HYPOTENSION)
        expect(classifyReading(110, 55)).toBe(ReadingCategory.HYPOTENSION)
    })

    it('returns UNCLASSIFIED for invalid inputs', () => {
        expect(classifyReading(0, 0)).toBe(ReadingCategory.UNCLASSIFIED)
        expect(classifyReading(null, 80)).toBe(ReadingCategory.UNCLASSIFIED)
        expect(classifyReading(120, undefined)).toBe(ReadingCategory.UNCLASSIFIED)
    })
})

describe('getCategoryLabel', () => {
    it('returns Italian labels', () => {
        expect(getCategoryLabel(ReadingCategory.NORMAL)).toBe('Normale')
        expect(getCategoryLabel(ReadingCategory.ELEVATED)).toBe('Elevata')
        expect(getCategoryLabel(ReadingCategory.HYPERTENSION_STAGE_1)).toBe('Ipertensione Stadio 1')
    })
})

describe('getCategoryColor', () => {
    it('returns hex colors', () => {
        expect(getCategoryColor(ReadingCategory.NORMAL)).toBe('#3B5D45')
        expect(getCategoryColor(ReadingCategory.HYPERTENSIVE_CRISIS)).toBe('#690005')
    })
})

import { describe, it, expect } from 'vitest'
import { AppError, SyncError, ValidationError, formatUserError } from '@/services/errorHandling.js'

describe('AppError', () => {
    it('creates with message and code', () => {
        const err = new AppError('test', 'TEST_CODE')
        expect(err.message).toBe('test')
        expect(err.code).toBe('TEST_CODE')
        expect(err.name).toBe('AppError')
    })
})

describe('ValidationError', () => {
    it('includes field in details', () => {
        const err = new ValidationError('Invalid value', 'systolic')
        expect(err.message).toBe('Invalid value')
        expect(err.details.field).toBe('systolic')
    })
})

describe('formatUserError', () => {
    it('formats ValidationError', () => {
        expect(formatUserError(new ValidationError('Campo non valido'))).toBe('Campo non valido')
    })

    it('formats SyncError', () => {
        expect(formatUserError(new SyncError('Rete assente'))).toContain('Errore di sincronizzazione')
    })

    it('handles unknown errors', () => {
        expect(formatUserError(null)).toBe('Si è verificato un errore imprevisto.')
        expect(formatUserError('string error')).toBe('Si è verificato un errore imprevisto.')
    })
})

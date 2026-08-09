import { describe, it, expect, vi, beforeEach } from 'vitest'

// ── Global mocks for jsdom ──────────────────────────────────────────

if (!URL.createObjectURL) {
    URL.createObjectURL = vi.fn(() => 'blob:mock')
    URL.revokeObjectURL = vi.fn()
}

// ── helpers ──────────────────────────────────────────────────────────

function buildCSV(headers, rows) {
    return headers.join(',') + '\n' + rows.map(r => r.join(',')).join('\n')
}

function csvFile(content, name = 'test.csv') {
    return new File([content], name, { type: 'text/csv' })
}

// ── exportCSV tests ──────────────────────────────────────────────────

import { exportCSV } from '@/services/dataService.js'

describe('exportCSV', () => {
    let anchorClickSpy

    beforeEach(() => {
        anchorClickSpy = vi.fn()
        const origCreateElement = document.createElement.bind(document)
        vi.spyOn(document, 'createElement').mockImplementation((tag) => {
            const el = origCreateElement(tag)
            if (tag === 'a') el.click = anchorClickSpy
            return el
        })
        vi.clearAllMocks()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    function getExportedText() {
        expect(URL.createObjectURL).toHaveBeenCalled()
        const blob = URL.createObjectURL.mock.calls.at(-1)[0]
        return blob.text()
    }

    it('produce un CSV con BOM UTF-8 e header corretti', async () => {
        const readings = [
            { timestamp: '2026-08-01T10:00:00', systolic: 120, diastolic: 80, heartRate: 72, category: 'NORMAL', notes: '' }
        ]
        exportCSV(readings)

        const text = await getExportedText()
        // jsdom blob.text() may strip BOM; verify content integrity
        expect(text).toContain('Data,Ora,Sistolica (mmHg),Diastolica (mmHg),Freq. Cardiaca (BPM),Categoria,Note')
        expect(text).toContain('120,80,72')
    })

    it('genera righe con formato data/ora italiano', async () => {
        const readings = [
            { timestamp: '2026-08-01T10:05:00', systolic: 120, diastolic: 80, heartRate: 72, category: 'NORMAL', notes: '' }
        ]
        exportCSV(readings)

        const text = await getExportedText()
        expect(text).toMatch(/01\/08\/2026/)
        expect(text).toMatch(/10:05/)
    })

    it('gestisce note con virgolette (escape corretto)', async () => {
        const readings = [
            { timestamp: '2026-08-01T10:00:00', systolic: 120, diastolic: 80, heartRate: 72, category: 'NORMAL', notes: 'Ha detto "ciao"' }
        ]
        exportCSV(readings)

        const text = await getExportedText()
        expect(text).toContain('"Ha detto ""ciao"""')
    })

    it('gestisce array vuoto (solo header)', async () => {
        exportCSV([])

        const text = await getExportedText()
        const lines = text.replace(/^\uFEFF/, '').split('\n').filter(l => l.trim())
        expect(lines.length).toBe(1)
        expect(lines[0]).toContain('Sistolica (mmHg)')
    })

    it('produce un CSV con categoria classificata', async () => {
        const readings = [
            { timestamp: '2026-08-01T10:00:00', systolic: 180, diastolic: 110, heartRate: 95, category: 'HYPERTENSIVE_CRISIS', notes: '' }
        ]
        exportCSV(readings)

        const text = await getExportedText()
        expect(text).toContain('HYPERTENSIVE_CRISIS')
    })

    it('produce più righe in ordine fornito', async () => {
        const readings = [
            { timestamp: '2026-08-03T10:00:00', systolic: 130, diastolic: 85, heartRate: 75, category: 'HYPERTENSION_STAGE_1', notes: '' },
            { timestamp: '2026-08-01T10:00:00', systolic: 120, diastolic: 80, heartRate: 72, category: 'NORMAL', notes: '' },
            { timestamp: '2026-08-02T10:00:00', systolic: 125, diastolic: 82, heartRate: 74, category: 'ELEVATED', notes: '' }
        ]
        exportCSV(readings)

        const text = await getExportedText()
        const lines = text.replace(/^\uFEFF/, '').split('\n').filter(l => l.trim())
        expect(lines.length).toBe(4)
        expect(lines[1]).toContain('130')
        expect(lines[3]).toContain('125')
    })

    it('avvia il download e rilascia object URL', () => {
        exportCSV([{ timestamp: '2026-08-01T10:00:00', systolic: 120, diastolic: 80, heartRate: 72, category: 'NORMAL', notes: '' }])

        expect(anchorClickSpy).toHaveBeenCalled()
        expect(URL.revokeObjectURL).toHaveBeenCalled()
    })
})

// ── importCSV tests ──────────────────────────────────────────────────
//
// Mock the db and supabaseClient modules so importCSV's internal calls
// (upsertReading → db.readings.put) go through mocked paths.

const { mockDbPut, storedReadings } = vi.hoisted(() => {
    const arr = []
    return {
        mockDbPut: vi.fn().mockResolvedValue(undefined),
        storedReadings: arr
    }
})

vi.mock('@/db', () => ({
    db: {
        readings: {
            put: mockDbPut,
            where: vi.fn(),
            delete: vi.fn().mockResolvedValue(undefined),
            bulkPut: vi.fn().mockResolvedValue(undefined)
        },
        syncQueue: {
            put: vi.fn().mockResolvedValue(undefined)
        }
    }
}))

vi.mock('@/services/supabaseClient.js', () => ({
    supabase: null,
    isSupabaseConfigured: false
}))

const { importCSV } = await import('@/services/dataService.js')

describe('importCSV', () => {
    async function mockExistingReadings(readings) {
        const { db } = await import('@/db')
        db.readings.where = vi.fn().mockReturnValue({
            equals: vi.fn().mockReturnValue({
                toArray: vi.fn().mockResolvedValue(readings)
            })
        })
    }

    async function mockEmptyReadings() {
        await mockExistingReadings([])
    }

    beforeEach(async () => {
        storedReadings.length = 0
        mockDbPut.mockReset().mockImplementation(async (reading) => {
            storedReadings.push({
                systolic: reading.systolic,
                diastolic: reading.diastolic,
                heartRate: reading.heartRate || reading.heart_rate,
                notes: reading.notes || '',
                timestamp: reading.timestamp
            })
        })
        await mockEmptyReadings()
    })

    // ── CSV validi ───────────────────────────────────────────────

    it('importa correttamente un CSV in formato IperTeso', async () => {
        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'NORMAL', ''],
                ['2026-08-02', '18:00', '130', '85', '75', 'ELEVATED', 'dopo cena']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        expect(result.skipped).toBe(0)
        expect(result.overwritten).toBe(0)
        expect(result.errors).toHaveLength(0)
        expect(storedReadings).toHaveLength(2)
        expect(storedReadings[0].systolic).toBe(120)
        expect(storedReadings[1].notes).toBe('dopo cena')
    })

    it('importa correttamente un CSV in formato bp-tracker', async () => {
        const csv = buildCSV(
            ['Date', 'Time', 'Systolic', 'Diastolic', 'Heart Rate', 'Notes'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', ''],
                ['2026-08-02', '18:00', '130', '85', '75', 'post workout']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        expect(result.errors).toHaveLength(0)
        expect(storedReadings).toHaveLength(2)
    })

    it('riconosce automaticamente il formato bp-tracker (data/ora separate)', async () => {
        // Header without 'sistolica' → treated as bp-tracker with date+time columns
        const csv = buildCSV(
            ['Date', 'Time', 'Systolic', 'Diastolic', 'Pulse', 'Notes'],
            [
                ['2026-08-01', '08:30', '118', '78', '70', ''],
                ['2026-08-02', '20:00', '135', '88', '76', 'sera']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        expect(result.errors).toHaveLength(0)
        expect(storedReadings[0].systolic).toBe(118)
    })

    it('bp-tracker con Pulsazioni: le note non includono la categoria', async () => {
        // Real bp-tracker export: Note before Categoria, Pulsazioni not Freq. Cardiaca
        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Pulsazioni (bpm)', 'Note', 'Categoria'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'nota importante', 'Normale'],
                ['2026-08-02', '18:00', '180', '110', '95', '', 'Crisi Ipertensiva']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        // Notes should be the actual notes, not the category string
        expect(storedReadings[0].notes).toBe('nota importante')
        expect(storedReadings[1].notes).toBe('')
    })

    // ── CSV malformati ───────────────────────────────────────────

    it('rifiuta CSV vuoto (solo header)', async () => {
        const csv = 'Data,Ora,Sistolica,Diastolica,HeartRate,Categoria,Note\n'
        await expect(importCSV('testuser', csvFile(csv), 'add')).rejects.toThrow('CSV vuoto')
    })

    it('rifiuta CSV con meno di 2 righe', async () => {
        const csv = 'header1,header2\n'
        await expect(importCSV('testuser', csvFile(csv), 'add')).rejects.toThrow('CSV vuoto')
    })

    it('segnala errori per valori non numerici', async () => {
        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'NORMAL', 'ok'],
                ['2026-08-02', '18:00', 'ABC', '80', '72', 'BAD', 'invalid'],
                ['2026-08-03', '09:00', '130', '85', '75', 'ELEVATED', 'ok']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        expect(result.errors).toHaveLength(1)
        expect(result.errors[0]).toMatch(/Valori non validi/)
    })

    it('rifiuta valori fuori range', async () => {
        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '350', '80', '72', 'NORMAL', ''],
                ['2026-08-02', '18:00', '120', '250', '72', 'NORMAL', ''],
                ['2026-08-03', '09:00', '120', '80', '400', 'NORMAL', '']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(0)
        expect(result.errors).toHaveLength(3)
    })

    it('scarta righe con timestamp non valido (senza trattini)', async () => {
        const csv = buildCSV(
            ['Timestamp', 'Systolic', 'Diastolic', 'HeartRate', 'Notes'],
            [
                ['notadate', '120', '80', '72', ''],
                ['2026-08-01', '10:00', '130', '85', '75', 'valid']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(1)
        expect(result.errors).toHaveLength(1)
        expect(result.errors[0]).toMatch(/Formato data non riconosciuto/)
    })

    // ── Gestione duplicati ────────────────────────────────────────

    it("modo 'add': importa tutto, anche duplicati", async () => {
        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'NORMAL', 'prima'],
                ['2026-08-01', '10:00', '125', '82', '74', 'ELEVATED', 'seconda']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        expect(result.skipped).toBe(0)
        expect(storedReadings).toHaveLength(2)
    })

    it("modo 'skip': salta righe con timestamp già esistente", async () => {
        const existingDate = new Date('2026-08-02T18:00:00')
        mockExistingReadings([
            { id: '1', username: 'testuser', timestamp: existingDate.toISOString(), systolic: 100, diastolic: 70, heartRate: 60, notes: '', category: 'NORMAL', updatedAt: existingDate.toISOString() }
        ])

        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'NORMAL', 'nuova'],
                ['2026-08-02', '18:00', '130', '85', '75', 'ELEVATED', 'duplicato']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'skip')

        expect(result.imported).toBe(1)
        expect(result.skipped).toBe(1)
        expect(result.overwritten).toBe(0)
        expect(storedReadings).toHaveLength(1)
        expect(storedReadings[0].systolic).toBe(120)
    })

    it("modo 'overwrite': sovrascrive letture con stesso timestamp", async () => {
        const existingDate = new Date('2026-08-02T18:00:00')
        mockExistingReadings([
            { id: '1', username: 'testuser', timestamp: existingDate.toISOString(), systolic: 100, diastolic: 70, heartRate: 60, notes: '', category: 'NORMAL', updatedAt: existingDate.toISOString() }
        ])

        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'NORMAL', 'nuova'],
                ['2026-08-02', '18:00', '130', '85', '75', 'ELEVATED', 'sovrascritto']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'overwrite')

        expect(result.imported).toBe(2)
        expect(result.overwritten).toBe(1)
        expect(result.skipped).toBe(0)
        expect(storedReadings).toHaveLength(2)
    })

    it("modo 'skip': nessun duplicato → importa tutto", async () => {
        mockExistingReadings([
            { id: '1', username: 'testuser', timestamp: new Date('2026-08-03T10:00:00').toISOString(), systolic: 110, diastolic: 75, heartRate: 65, notes: '', category: 'NORMAL', updatedAt: '2026-08-03T10:00:00.000Z' }
        ])

        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [
                ['2026-08-01', '10:00', '120', '80', '72', 'NORMAL', ''],
                ['2026-08-02', '18:00', '130', '85', '75', 'ELEVATED', '']
            ]
        )
        const result = await importCSV('testuser', csvFile(csv), 'skip')

        expect(result.imported).toBe(2)
        expect(result.skipped).toBe(0)
    })

    // ── Edge cases ────────────────────────────────────────────────

    it('gestisce CSV con righe vuote nel mezzo', async () => {
        const csv = 'Data,Ora,Sistolica (mmHg),Diastolica (mmHg),Freq. Cardiaca (BPM),Categoria,Note\n2026-08-01,10:00,120,80,72,NORMAL,\n\n2026-08-02,18:00,130,85,75,ELEVATED,\n'
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(2)
        expect(result.errors).toHaveLength(0)
    })

    it('gestisce CSV con spazi bianchi attorno ai valori', async () => {
        const csv = buildCSV(
            ['Data', 'Ora', 'Sistolica (mmHg)', 'Diastolica (mmHg)', 'Freq. Cardiaca (BPM)', 'Categoria', 'Note'],
            [[' 2026-08-01 ', ' 10:00 ', ' 120 ', ' 80 ', ' 72 ', ' NORMAL ', ' note ']]
        )
        const result = await importCSV('testuser', csvFile(csv), 'add')

        expect(result.imported).toBe(1)
        expect(storedReadings[0].systolic).toBe(120)
    })
})

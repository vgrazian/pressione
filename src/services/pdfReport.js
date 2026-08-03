/**
 * PDF Report Generator — Medical-grade, pdf-pro design system.
 *
 * Target: clinicians. ESC/ESH classification, risk stratification,
 * trend arrows, reference ranges, page numbering, medical disclaimer.
 */
import { jsPDF } from 'jspdf'
import {
    computeStatistics,
    computeDerivatives,
    computeMorningSurge,
    computeHypertensiveLoad,
    computeHRV
} from '@/services/statistics.js'
import { getCategoryLabel, classifyReading } from '@/services/categories.js'

// ── Design Tokens ──────────────────────────────────────────────
const C = {
    brand: [0, 108, 76],   // #006C4C — primary medical green
    brandBg: [232, 245, 233],// #E8F5E9 — tint
    text: [51, 51, 51],   // #333
    body: [85, 85, 85],   // #555
    muted: [136, 136, 136],// #888
    white: [255, 255, 255],
    error: [186, 26, 26],  // #BA1A1A — dangerous values
    warning: [239, 108, 0],  // #EF6C00 — caution zone
    critical: [211, 47, 47],  // crisis red
    ok: [0, 108, 76],   // normal range
    surface: [248, 249, 247],
}
const S = { m: 14, gap: 8, in: 4, rh: 5.5, hh: 5.5 }
const T = { h1: 12, h2: 10.5, h3: 9, body: 8.5, sm: 7.5, xs: 6.5 }

function genderLabel(g) { return g === 'male' ? 'M' : g === 'female' ? 'F' : '' }

// ── Chart helpers ──────────────────────────────────────────────
async function renderChart(config, w, h) {
    const c = document.createElement('canvas')
    c.width = w; c.height = h; c.style.display = 'none'
    document.body.appendChild(c)
    try {
        const { Chart, registerables } = await import('chart.js')
        Chart.register(...registerables)
        try { const a = await import('chartjs-plugin-annotation'); Chart.register(a.default) } catch { }
        new Chart(c.getContext('2d'), config)
        await new Promise(r => setTimeout(r, 300))
        return c.toDataURL('image/png')
    } finally { document.body.removeChild(c) }
}

function makeLineChart(readings) {
    const sorted = [...readings].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    const labels = sorted.map(r => new Date(r.timestamp).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' }))
    return {
        type: 'line',
        data: {
            labels,
            datasets: [
                { label: 'Sistolica', data: sorted.map(r => r.systolic), borderColor: '#BA1A1A', backgroundColor: 'rgba(186,26,26,0.06)', tension: 0.35, pointRadius: 2, borderWidth: 2 },
                { label: 'Diastolica', data: sorted.map(r => r.diastolic), borderColor: '#1976D2', backgroundColor: 'rgba(25,118,210,0.06)', tension: 0.35, pointRadius: 2, borderWidth: 2 }
            ]
        },
        options: {
            responsive: false, animation: false,
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 9 }, padding: 14, usePointStyle: true } },
                annotation: {
                    annotations: {
                        zoneNormal: { type: 'box', yMin: 90, yMax: 140, backgroundColor: 'rgba(0,108,76,0.04)', borderWidth: 0 },
                        line140: { type: 'line', yMin: 140, yMax: 140, borderColor: 'rgba(186,26,26,0.3)', borderWidth: 1, borderDash: [4, 4], label: { display: true, content: '140', position: 'end', font: { size: 7 }, backgroundColor: 'rgba(255,255,255,0.7)' } },
                        line90: { type: 'line', yMin: 90, yMax: 90, borderColor: 'rgba(25,118,210,0.3)', borderWidth: 1, borderDash: [4, 4], label: { display: true, content: '90', position: 'end', font: { size: 7 }, backgroundColor: 'rgba(255,255,255,0.7)' } }
                    }
                }
            },
            scales: {
                x: { ticks: { maxTicksLimit: 14, font: { size: 8 } }, grid: { display: false } },
                y: { min: 0, max: 200, ticks: { stepSize: 20, font: { size: 8 } }, title: { display: true, text: 'mmHg', font: { size: 8 } } }
            }
        }
    }
}

function makeDoughnutChart(stats) {
    const dist = stats.categoryDistribution || {}
    const keys = ['NORMAL', 'ELEVATED', 'HYPERTENSION_STAGE_1', 'HYPERTENSION_STAGE_2', 'HYPERTENSIVE_CRISIS', 'HYPOTENSION']
    const colors = ['#006C4C', '#F9A825', '#EF6C00', '#D32F2F', '#7B1FA2', '#1976D2']
    const data = keys.map(k => dist[k] || 0)
    const total = data.reduce((a, b) => a + b, 0)
    return {
        type: 'doughnut',
        data: {
            labels: keys.map((k, i) => total ? `${getCategoryLabel(k)} (${Math.round(data[i] / total * 100)}%)` : getCategoryLabel(k)),
            datasets: [{ data, backgroundColor: colors, borderWidth: 1, borderColor: '#fff' }]
        },
        options: {
            responsive: false, animation: false, cutout: '60%',
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 8, font: { size: 7.5 }, padding: 5 } }
            }
        }
    }
}

// ── Section: Report Header ─────────────────────────────────────
function addHeader(doc, opts) {
    const pw = doc.internal.pageSize.getWidth()

    // Top brand bar
    doc.setFillColor(...C.brand)
    doc.rect(0, 0, pw, 14, 'F')
    doc.setFontSize(10)
    doc.setTextColor(...C.white)
    doc.text('REPORT PRESSIONE ARTERIOSA', S.m, 9)

    // Patient info line
    let y = 20
    doc.setFontSize(T.sm)
    doc.setTextColor(...C.muted)
    const from = opts.data.length ? new Date(opts.data[opts.data.length - 1].timestamp).toLocaleDateString('it-IT') : 'N/D'
    const to = opts.data.length ? new Date(opts.data[0].timestamp).toLocaleDateString('it-IT') : 'N/D'
    const patient = opts.anonymize ? 'Anonimo' : (opts.username || '—')
    const ageStr = !opts.anonymize && opts.age ? `, ${opts.age} anni` : ''
    const genderStr = !opts.anonymize && opts.gender ? `, ${genderLabel(opts.gender)}` : ''
    doc.text(`Paziente: ${patient}${ageStr}${genderStr}    •    Periodo: ${from} – ${to}    •    ${opts.data.length} misurazioni    •    Generato: ${new Date().toLocaleDateString('it-IT')}`, S.m, y)

    return y + S.gap
}

// ── Section: Clinical Summary ──────────────────────────────────
function addClinicalSummary(doc, y, stats, readings, opts) {
    const surge = computeMorningSurge(readings)
    const load = computeHypertensiveLoad(readings)
    const hrv = computeHRV(readings)

    // Determine overall classification
    const avgSys = stats.avgSystolic
    const avgDia = stats.avgDiastolic
    const classification = classifyReading(avgSys, avgDia)
    const classLabel = getCategoryLabel(classification)

    doc.setFontSize(T.h2)
    doc.setTextColor(...C.brand)
    doc.text('1. Riepilogo Clinico', S.m, y)
    y += S.in + 2

    // Classification badge
    const badgeColors = {
        'NORMAL': C.ok, 'ELEVATED': C.warning,
        'HYPERTENSION_STAGE_1': C.warning, 'HYPERTENSION_STAGE_2': C.error,
        'HYPERTENSIVE_CRISIS': C.critical, 'HYPOTENSION': [25, 118, 210],
        'UNCLASSIFIED': C.muted
    }
    const badgeColor = badgeColors[classification] || C.brand
    doc.setFontSize(T.sm)
    const badgeText = `Classificazione: ${classLabel}`
    const badgeW = doc.getTextWidth(badgeText) + 10
    doc.setFillColor(...badgeColor)
    doc.roundedRect(S.m, y, badgeW, 7, 2, 2, 'F')
    doc.setTextColor(...C.white)
    doc.text(badgeText, S.m + 5, y + 4.8)

    // Key vitals cards (3 across)
    const cardW = (doc.internal.pageSize.getWidth() - S.m * 2 - S.in * 2) / 3
    const cardY = y + 10
    const cards = [
        { label: 'Pressione media', value: `${avgSys}/${avgDia}`, unit: 'mmHg', alert: classification !== 'Normale' },
        { label: 'Frequenza cardiaca', value: String(stats.avgHeartRate), unit: 'BPM', alert: stats.avgHeartRate > 100 || stats.avgHeartRate < 50 },
        { label: 'Letture totali', value: String(stats.readingsCount), unit: 'misurazioni', alert: false }
    ]
    cards.forEach((c, i) => {
        const cx = S.m + i * (cardW + S.in)
        doc.setFillColor(250, 250, 250)
        doc.roundedRect(cx, cardY, cardW, 16, 2.5, 2.5, 'F')
        doc.setFontSize(T.xs)
        doc.setTextColor(...C.muted)
        doc.text(c.label, cx + 3, cardY + 4)
        doc.setFontSize(T.h1)
        const alertColor = c.alert ? C.error : C.text
        doc.setTextColor(...alertColor)
        doc.text(c.value, cx + 3, cardY + 12)
        doc.setFontSize(6)
        doc.setTextColor(...C.muted)
        doc.text(c.unit, cx + 3 + doc.getTextWidth(c.value) + 2, cardY + 11)
    })
    y = cardY + 19

    // Risk indicators
    y += 2
    doc.setFontSize(T.sm)
    const risks = []
    if (surge.alert) risks.push(`⚠ Picco mattutino elevato: +${surge.delta} mmHg`)
    if (load.percentage > 30) risks.push(`⚠ Carico ipertensivo: ${load.percentage}%`)
    if (hrv !== null && hrv > 15) risks.push(`⚠ HRV elevata: ${hrv} BPM`)
    if (classification === 'HYPERTENSIVE_CRISIS') risks.push('⚠ ATTENZIONE: valori in zona crisi ipertensiva')
    if (classification === 'HYPERTENSION_STAGE_2') risks.push('⚠ Valori in ipertensione di grado 2')

    if (risks.length > 0) {
        risks.forEach(r => {
            doc.setTextColor(...C.error)
            doc.text(r, S.m + 2, y)
            y += 5
        })
    } else {
        doc.setTextColor(...C.ok)
        doc.text('✓ Nessun indicatore di rischio rilevato nel periodo.', S.m + 2, y)
        y += 5
    }

    // Age-specific note
    if (opts.age) {
        y += 2
        doc.setFontSize(T.xs)
        doc.setTextColor(...C.muted)
        if (opts.age >= 65) {
            doc.text(`Nota: per età ≥65 anni, il target pressorio raccomandato è <140/90 mmHg (ESC/ESH 2024).`, S.m + 2, y)
            y += 4
        } else if (opts.age < 18) {
            doc.text(`Nota: per età <18 anni i riferimenti ESC/ESH standard potrebbero non applicarsi. Consultare il pediatra.`, S.m + 2, y)
            y += 4
        }
    }

    return y + S.gap
}

// ── Section: Detailed Stats ────────────────────────────────────
function addStatsTable(doc, y, r7, r30, data) {
    const s7 = computeStatistics(r7), s30 = computeStatistics(r30), s = computeStatistics(data)

    doc.setFontSize(T.h2)
    doc.setTextColor(...C.brand)
    doc.text('2. Dettaglio Multi-Periodo', S.m, y)
    y += S.in + 2

    // Columns: Label | 7gg | 30gg | Selez.
    const lw = 34, cw = 46, x0 = S.m, xl = x0, x1 = x0 + lw, x2 = x1 + cw, x3 = x2 + cw, tw = lw + cw * 3

    // Header
    doc.setFillColor(...C.brand)
    doc.rect(x0, y, tw, S.hh, 'F')
    doc.setFontSize(T.sm)
    doc.setTextColor(...C.white)
    doc.text('7 giorni', x1, y + 3.8)
    doc.text('30 giorni', x2, y + 3.8)
    doc.text('Periodo selez.', x3, y + 3.8)
    y += S.hh

    const der7 = computeDerivatives(r7), der30 = computeDerivatives(r30), der = computeDerivatives(data)
    const rows = [
        ['Letture (n.)', s7.readingsCount, s30.readingsCount, s.readingsCount],
        ['SYS/DIA media', `${s7.avgSystolic}/${s7.avgDiastolic}`, `${s30.avgSystolic}/${s30.avgDiastolic}`, `${s.avgSystolic}/${s.avgDiastolic}`],
        ['Freq. cardiaca', `${s7.avgHeartRate} BPM`, `${s30.avgHeartRate} BPM`, `${s.avgHeartRate} BPM`],
        ['SYS min – max', `${s7.minSystolic} – ${s7.maxSystolic}`, `${s30.minSystolic} – ${s30.maxSystolic}`, `${s.minSystolic} – ${s.maxSystolic}`],
        ['DIA min – max', `${s7.minDiastolic} – ${s7.maxDiastolic}`, `${s30.minDiastolic} – ${s30.maxDiastolic}`, `${s.minDiastolic} – ${s.maxDiastolic}`],
        ['dP/dt max', `${Math.round(der7.maxRate)} mmHg/h`, `${Math.round(der30.maxRate)} mmHg/h`, `${Math.round(der.maxRate)} mmHg/h`],
        ['Allarmi dP/dt', String(der7.alarmSegments.length), String(der30.alarmSegments.length), String(der.alarmSegments.length)],
        ['Carico ipertensivo', `${computeHypertensiveLoad(r7).percentage}%`, `${computeHypertensiveLoad(r30).percentage}%`, `${computeHypertensiveLoad(data).percentage}%`],
        ['Picco mattutino', morningSurgeStr(r7), morningSurgeStr(r30), morningSurgeStr(data)]
    ]

    doc.setFontSize(T.sm)
    for (let i = 0; i < rows.length; i++) {
        if (i % 2 === 0) { doc.setFillColor(...C.brandBg); doc.rect(x0, y - 1, tw, S.rh, 'F') }
        doc.setTextColor(...C.muted)
        doc.text(rows[i][0], xl + 1, y + 3.2)
        doc.setTextColor(...C.body)
        for (let j = 1; j <= 3; j++) {
            const cx = [x1, x2, x3][j - 1]
            // Numeric rows: 0 (Letture), 5 (dP/dt), 6 (Allarmi), 7 (Carico)
            const isNumeric = i === 0 || i === 5 || i === 6 || i === 7
            const align = isNumeric ? 'right' : 'left'
            doc.text(String(rows[i][j]), align === 'right' ? cx + cw - 1 : cx + 1, y + 3.2, align === 'right' ? { align: 'right' } : undefined)
        }
        y += S.rh
    }
    return y + S.gap
}

function morningSurgeStr(readings) {
    const surge = computeMorningSurge(readings)
    if (surge.delta === null) return 'N/D'
    return `${surge.delta > 0 ? '+' : ''}${surge.delta} mmHg`
}

// ── Section: Charts ────────────────────────────────────────────
async function addCharts(doc, y, readings, stats) {
    if (readings.length < 2) return y

    doc.setFontSize(T.h2)
    doc.setTextColor(...C.brand)
    doc.text('3. Andamento e Distribuzione', S.m, y)
    y += S.in + 2

    const pw = doc.internal.pageSize.getWidth()
    const iw = pw - S.m * 2

    // Line chart
    doc.setFontSize(T.sm)
    doc.setTextColor(...C.body)
    const sysRange = `(${stats.minSystolic} – ${stats.maxSystolic})`
    doc.text(`Andamento pressione arteriosa  —  Media ${stats.avgSystolic}/${stats.avgDiastolic} mmHg  ${sysRange}`, S.m, y)
    y += 2.5

    const lineImg = await renderChart(makeLineChart(readings), 800, 340)
    doc.addImage(lineImg, 'PNG', S.m - 1, y, iw + 2, 60)
    y += 64

    if (y > 190) { doc.addPage(); y = 20 }

    // Doughnut
    doc.setFontSize(T.sm)
    doc.setTextColor(...C.body)
    const dist = stats.categoryDistribution || {}
    const entries = Object.entries(dist).sort((a, b) => b[1] - a[1])
    const topCat = entries[0]
    const topLabel = topCat ? getCategoryLabel(topCat[0]) : ''
    const insight = topCat ? `${topLabel}: ${topCat[1]} misurazioni (${Math.round(topCat[1] / stats.readingsCount * 100)}%)` : ''
    doc.text(`Distribuzione categorie ESC/ESH  —  ${insight}`, S.m, y)
    y += 2.5

    const doughImg = await renderChart(makeDoughnutChart(stats), 600, 380)
    doc.addImage(doughImg, 'PNG', S.m - 1, y, iw + 2, 56)
    y += 60

    return y + S.gap
}

// ── Section: Reference Ranges ──────────────────────────────────
function addReferenceRanges(doc, y) {
    const pw = doc.internal.pageSize.getWidth()

    doc.setFontSize(T.h2)
    doc.setTextColor(...C.brand)
    doc.text('4. Riferimenti Clinici ESC/ESH', S.m, y)
    y += S.in + 2

    const ranges = [
        ['Ottimale', '< 120 / < 80', C.ok],
        ['Normale', '120–129 / 80–84', C.ok],
        ['Elevata', '130–139 / 85–89', C.warning],
        ['Ipert. Grado 1', '140–159 / 90–99', C.error],
        ['Ipert. Grado 2', '160–179 / 100–109', C.critical],
        ['Ipert. Grado 3', '≥ 180 / ≥ 110', C.critical],
        ['Ipert. Sist. Isolata', '≥ 140 / < 90', C.warning]
    ]

    const colW = [40, 70, 60]
    const cols = [S.m, S.m + colW[0], S.m + colW[0] + colW[1]]
    const tw = colW[0] + colW[1] + colW[2]

    // Header
    doc.setFillColor(...C.brand)
    doc.rect(S.m, y, tw, S.hh, 'F')
    doc.setFontSize(7)
    doc.setTextColor(...C.white)
    doc.text('Categoria', cols[0] + 2, y + 3.8)
    doc.text('Sistolica / Diastolica (mmHg)', cols[1] + 2, y + 3.8)
    doc.text('Rischio', cols[2] + 2, y + 3.8)
    y += S.hh

    const riskLabels = ['Basso', 'Basso', 'Moderato', 'Alto', 'Molto alto', 'Molto alto', 'Moderato']

    doc.setFontSize(7)
    ranges.forEach((r, i) => {
        if (i % 2 === 0) { doc.setFillColor(...C.brandBg); doc.rect(S.m, y - 1, tw, S.rh, 'F') }
        doc.setTextColor(...r[2])
        doc.text(r[0], cols[0] + 2, y + 3.2)
        doc.setTextColor(...C.body)
        doc.text(r[1], cols[1] + 2, y + 3.2)
        doc.setTextColor(...C.muted)
        doc.text(riskLabels[i], cols[2] + 2, y + 3.2)
        // Color dot
        doc.setFillColor(...r[2])
        doc.circle(cols[0] - 1, y + 1.5, 1.2, 'F')
        y += S.rh
    })

    return y + S.gap
}

// ── Section: History Table ─────────────────────────────────────
function addHistoryTable(doc, y, data) {
    if (data.length === 0) return y

    doc.setFontSize(T.h2)
    doc.setTextColor(...C.brand)
    doc.text('5. Storico Misurazioni', S.m, y)
    y += S.in + 2

    const pw = doc.internal.pageSize.getWidth()
    const tw = pw - S.m * 2
    const colX = [S.m, 42, 60, 76, 92, 108, 136, 174]
    const headers = ['Data', 'Ora', 'SYS', 'DIA', 'BPM', 'Categoria', 'Note']

    // Header
    doc.setFillColor(...C.brand)
    doc.rect(S.m, y, tw, S.hh, 'F')
    doc.setFontSize(7)
    doc.setTextColor(...C.white)
    headers.forEach((h, i) => {
        const align = (h === 'SYS' || h === 'DIA' || h === 'BPM') ? 'right' : 'left'
        doc.text(h, align === 'right' ? colX[i + 1] : colX[i], y + 3.8, align === 'right' ? { align: 'right' } : undefined)
    })
    y += S.hh

    const catColors = {
        'Normale': C.ok, 'Elevata': C.warning, 'Ipertensione Grado 1': C.warning,
        'Ipertensione Grado 2': C.error, 'Crisi Ipertensiva': C.critical, 'Ipotensione': [25, 118, 210]
    }

    doc.setFontSize(7)
    for (let idx = 0; idx < data.length; idx++) {
        const r = data[idx]
        if (y > 275) { doc.addPage(); y = 20 }

        if (idx % 2 === 0) { doc.setFillColor(...C.brandBg); doc.rect(S.m, y - 1, tw, S.rh, 'F') }

        const d = new Date(r.timestamp)
        doc.setTextColor(...C.body)
        doc.text(d.toLocaleDateString('it-IT'), colX[0], y + 3.2)
        doc.setTextColor(...C.muted)
        doc.text(d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }), colX[1], y + 3.2)

        // SYS — clinical color
        // SYS — clinical color
        const sysCol = r.systolic >= 140 ? C.error : r.systolic >= 130 ? C.warning : C.text
        doc.setTextColor(...sysCol)
        doc.text(String(r.systolic), colX[3], y + 3.2, { align: 'right' })

        // DIA
        // DIA
        const diaCol = r.diastolic >= 90 ? C.error : r.diastolic >= 85 ? C.warning : C.text
        doc.setTextColor(...diaCol)
        doc.text(String(r.diastolic), colX[4], y + 3.2, { align: 'right' })

        // BPM
        doc.setTextColor(...C.body)
        doc.text(String(r.heartRate || '–'), colX[5], y + 3.2, { align: 'right' })

        // Category
        const cc = catColors[r.category] || C.muted
        doc.setTextColor(...cc)
        doc.text(getCategoryLabel(r.category) || r.category || '–', colX[6], y + 3.2)

        // Notes
        doc.setTextColor(...C.muted)
        doc.text((r.notes || '').slice(0, 28), colX[7], y + 3.2)
        y += S.rh
    }

    return y + S.in
}

// ── Footer with page numbers ───────────────────────────────────
function addFooter(doc, pageNum, totalPages) {
    doc.setFontSize(6)
    doc.setTextColor(180, 180, 180)
    const pw = doc.internal.pageSize.getWidth()
    doc.text(`Report Pressione App — Non costituisce diagnosi medica. Consultare sempre un medico.`, S.m, 290)
    doc.text(`Pagina ${pageNum} di ${totalPages}`, pw - S.m, 290, { align: 'right' })
}

// ── Main ───────────────────────────────────────────────────────
export async function generatePDF({ data, readings7, readings30, username, age, gender, anonymize, includeCharts, includeHistory }) {
    const doc = new jsPDF({ unit: 'mm', format: 'a4' })
    const stats = computeStatistics(data)
    const opts = { data, username, age, gender, anonymize }

    let y = addHeader(doc, opts)
    y = addClinicalSummary(doc, y, stats, data, opts)
    y = addStatsTable(doc, y, readings7, readings30, data)

    if (includeCharts && data.length >= 2) {
        if (y > 140) { doc.addPage(); y = 20 }
        y = await addCharts(doc, y, data, stats)
    }

    // Reference ranges (always included — medical context)
    if (y > 180) { doc.addPage(); y = 20 }
    y = addReferenceRanges(doc, y)

    if (includeHistory && data.length > 0) {
        if (y > 195) { doc.addPage(); y = 20 }
        y = addHistoryTable(doc, y, data)
    }

    // Footer on all pages with page numbers
    const total = doc.getNumberOfPages()
    for (let i = 1; i <= total; i++) {
        doc.setPage(i)
        addFooter(doc, i, total)
    }

    doc.save(`pressione_report_${new Date().toISOString().slice(0, 10)}.pdf`)
}

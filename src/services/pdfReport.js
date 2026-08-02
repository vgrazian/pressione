/**
 * PDF Report Generator — pdf-pro design system applied
 * 
 * Design tokens, typographic scale, spacing rhythm, accessible colors,
 * right-aligned numbers, semantic table coloring, chart insight titles.
 */
import { jsPDF } from 'jspdf'
import {
    computeStatistics,
    computeDerivatives,
    computeMorningSurge,
    computeHypertensiveLoad,
    computeHRV
} from '@/services/statistics.js'
import { getCategoryLabel } from '@/services/categories.js'

// ── Design Tokens ──────────────────────────────────────────────
const C = {
    brand:        [0, 108, 76],   // #006C4C
    brandLight:   [0, 108, 76],
    textStrong:   [51, 51, 51],   // #333
    textBody:     [85, 85, 85],   // #555
    textMuted:    [136, 136, 136],// #888
    white:        [255, 255, 255],
    error:        [186, 26, 26],  // #BA1A1A
    warning:      [239, 108, 0],  // #EF6C00
    rowEven:      [232, 245, 233],// #E8F5E9 (brand tint)
    rowHeader:    [0, 108, 76],
    divider:      [220, 220, 220],
}

const S = {
    margin:       14,     // page margin (mm)
    sectionGap:   8,      // gap between sections
    innerGap:     4,      // gap inside a section
    rowH:         5.5,    // table row height
    headerH:      5.5,    // table header height
}

const T = {
    sectionTitle: 11,     // section headings
    body:         8.5,    // body text
    small:        7.5,    // table cells, captions
    tiny:         6.5,    // footnotes, footer
}

// ── Chart helpers ──────────────────────────────────────────────

async function renderChart(config, w, h) {
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    canvas.style.display = 'none'
    document.body.appendChild(canvas)
    try {
        const { Chart, registerables } = await import('chart.js')
        Chart.register(...registerables)
        try {
            const a = await import('chartjs-plugin-annotation')
            Chart.register(a.default)
        } catch {}
        new Chart(canvas.getContext('2d'), config)
        await new Promise(r => setTimeout(r, 300))
        return canvas.toDataURL('image/png')
    } finally {
        document.body.removeChild(canvas)
    }
}

function buildLineChart(readings) {
    const sorted = [...readings].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    const labels = sorted.map(r => new Date(r.timestamp).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' }))
    return {
        type: 'line',
        data: {
            labels,
            datasets: [
                { label: 'Sistolica (mmHg)', data: sorted.map(r => r.systolic), borderColor: '#BA1A1A', backgroundColor: 'rgba(186,26,26,0.08)', tension: 0.3, pointRadius: 2, borderWidth: 2 },
                { label: 'Diastolica (mmHg)', data: sorted.map(r => r.diastolic), borderColor: '#1976D2', backgroundColor: 'rgba(25,118,210,0.08)', tension: 0.3, pointRadius: 2, borderWidth: 2 }
            ]
        },
        options: {
            responsive: false, animation: false,
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 9 }, padding: 12, usePointStyle: true } },
                annotation: { annotations: { zone: { type: 'box', yMin: 90, yMax: 140, backgroundColor: 'rgba(0,108,76,0.05)', borderWidth: 0 } } }
            },
            scales: {
                x: { ticks: { maxTicksLimit: 14, font: { size: 8 } }, grid: { display: false } },
                y: { min: 0, max: 200, ticks: { font: { size: 8 } }, title: { display: true, text: 'mmHg', font: { size: 8 } } }
            }
        }
    }
}

function buildDoughnutChart(stats) {
    const dist = stats.categoryDistribution || {}
    const keys = ['Normale', 'Elevata', 'Ipertensione Grado 1', 'Ipertensione Grado 2', 'Crisi Ipertensiva', 'Ipotensione']
    const colors = ['#006C4C', '#F9A825', '#EF6C00', '#D32F2F', '#7B1FA2', '#1976D2']
    const data = keys.map(k => dist[k] || 0)
    const total = data.reduce((a, b) => a + b, 0)
    return {
        type: 'doughnut',
        data: {
            labels: keys.map((k, i) => total ? `${k} (${Math.round(data[i] / total * 100)}%)` : k),
            datasets: [{ data, backgroundColor: colors, borderWidth: 1, borderColor: '#fff' }]
        },
        options: {
            responsive: false, animation: false,
            plugins: { legend: { position: 'bottom', labels: { boxWidth: 8, font: { size: 8 }, padding: 6 } } }
        }
    }
}

// ── Section builders ───────────────────────────────────────────

function addHeader(doc, opts) {
    const pw = doc.internal.pageSize.getWidth()
    // Brand bar
    doc.setFillColor(...C.brand)
    doc.rect(0, 0, pw, 16, 'F')
    doc.setFontSize(12)
    doc.setTextColor(...C.white)
    doc.text('Pressione  —  Report Pressione Arteriosa', S.margin, 10.5)

    let y = 22
    doc.setFontSize(T.small)
    doc.setTextColor(...C.textMuted)
    const from = opts.data.length ? new Date(opts.data[opts.data.length - 1].timestamp).toLocaleDateString('it-IT') : 'N/D'
    const to = opts.data.length ? new Date(opts.data[0].timestamp).toLocaleDateString('it-IT') : 'N/D'
    doc.text(`Periodo: ${from} – ${to}  •  ${opts.data.length} misurazioni  •  ${new Date().toLocaleDateString('it-IT')}`, S.margin, y)
    if (!opts.anonymize) {
        y += 4
        doc.text(`Paziente: ${opts.username}`, S.margin, y)
    }
    return y + S.sectionGap
}

function addMultiPeriodComparison(doc, y, r7, r30, data) {
    const s7 = computeStatistics(r7), s30 = computeStatistics(r30), sSel = computeStatistics(data)

    // Section title
    doc.setFontSize(T.sectionTitle)
    doc.setTextColor(...C.brand)
    doc.text('Confronto Multi-Periodo', S.margin, y)
    y += S.innerGap + 2

    const lw = 34, cw = 46, x0 = S.margin, xl = x0, x1 = x0 + lw, x2 = x1 + cw, x3 = x2 + cw, tw = lw + cw * 3

    // Header row
    doc.setFillColor(...C.brand)
    doc.rect(x0, y, tw, S.headerH, 'F')
    doc.setFontSize(T.small)
    doc.setTextColor(...C.white)
    doc.text('7 Giorni', x1, y + 3.8)
    doc.text('30 Giorni', x2, y + 3.8)
    doc.text('Periodo selez.', x3, y + 3.8)
    y += S.headerH

    const rows = [
        ['Letture', s7.readingsCount, s30.readingsCount, sSel.readingsCount],
        ['SYS/DIA media', `${s7.avgSystolic}/${s7.avgDiastolic}`, `${s30.avgSystolic}/${s30.avgDiastolic}`, `${sSel.avgSystolic}/${sSel.avgDiastolic}`],
        ['BPM medio', s7.avgHeartRate, s30.avgHeartRate, sSel.avgHeartRate],
        ['SYS min–max', `${s7.minSystolic}–${s7.maxSystolic}`, `${s30.minSystolic}–${s30.maxSystolic}`, `${sSel.minSystolic}–${sSel.maxSystolic}`],
        ['DIA min–max', `${s7.minDiastolic}–${s7.maxDiastolic}`, `${s30.minDiastolic}–${s30.maxDiastolic}`, `${sSel.minDiastolic}–${sSel.maxDiastolic}`],
        ['Carico iper.', `${computeHypertensiveLoad(r7).percentage}%`, `${computeHypertensiveLoad(r30).percentage}%`, `${computeHypertensiveLoad(data).percentage}%`]
    ]

    doc.setFontSize(T.small)
    for (let i = 0; i < rows.length; i++) {
        if (i % 2 === 0) { doc.setFillColor(...C.rowEven); doc.rect(x0, y - 1, tw, S.rowH, 'F') }
        doc.setTextColor(...C.textMuted)
        doc.text(String(rows[i][0]), xl + 1, y + 3.2)
        doc.setTextColor(...C.textBody)
        // Right-align numbers, left-align text labels
        const isNumeric = typeof rows[i][1] === 'number'
        for (let j = 1; j <= 3; j++) {
            const colX = [x1, x2, x3][j - 1]
            const val = String(rows[i][j])
            doc.text(val, isNumeric ? colX + cw - 2 : colX + 1, y + 3.2, isNumeric ? { align: 'right' } : undefined)
        }
        y += S.rowH
    }
    return y + S.sectionGap
}

function addAdvancedStats(doc, y, readings) {
    const surge = computeMorningSurge(readings)
    const load = computeHypertensiveLoad(readings)
    const hrv = computeHRV(readings)

    doc.setFontSize(T.sectionTitle)
    doc.setTextColor(...C.brand)
    doc.text('Statistiche Avanzate', S.margin, y)
    y += S.innerGap + 2

    doc.setFontSize(T.small)

    // Draw as compact cards
    const pw = doc.internal.pageSize.getWidth()
    const cardW = (pw - S.margin * 2 - S.innerGap * 2) / 3
    const cards = [
        { label: 'Picco Mattutino', value: surge.delta !== null ? `${surge.delta > 0 ? '+' : ''}${surge.delta} mmHg` : 'N/D', alert: surge.alert },
        { label: 'Carico Ipertensivo', value: `${load.percentage}%`, sub: `${load.abnormal}/${load.total} fuori norma`, alert: load.percentage > 30 },
        { label: 'HRV', value: hrv !== null ? `${hrv} BPM` : 'N/D', sub: 'Dev. standard FC', alert: false }
    ]

    const cardY = y
    cards.forEach((c, i) => {
        const cx = S.margin + i * (cardW + S.innerGap)
        // Card background
        doc.setFillColor(250, 250, 250)
        doc.roundedRect(cx, cardY, cardW, 14, 2, 2, 'F')
        // Label
        doc.setFontSize(6.5)
        doc.setTextColor(...C.textMuted)
        doc.text(c.label, cx + 3, cardY + 4)
        // Value
        doc.setFontSize(T.sectionTitle)
        const valColor = c.alert ? C.error : C.textStrong
        doc.setTextColor(...valColor)
        doc.text(c.value, cx + 3, cardY + 10)
        if (c.sub) {
            doc.setFontSize(6)
            doc.setTextColor(...C.textMuted)
            doc.text(c.sub, cx + 3, cardY + 13)
        }
    })
    y = cardY + 16

    // Trend line
    if (readings.length >= 3) {
        const derivs = computeDerivatives(readings)
        y += 2
        doc.setFontSize(7.5)
        doc.setTextColor(...C.textMuted)
        const avgRate = derivs.systolic.length > 0
            ? Math.round(derivs.systolic.reduce((a, b) => a + b, 0) / derivs.systolic.length * 10) / 10 : 0
        const trend = avgRate > 2 ? 'in aumento ↑' : avgRate < -2 ? 'in diminuzione ↓' : 'stabile →'
        doc.text(`Tendenza: ${trend}  (${avgRate > 0 ? '+' : ''}${avgRate} mmHg/h)`, S.margin, y)
        if (derivs.alarmSegments.length > 0) {
            y += 4
            doc.setTextColor(...C.error)
            doc.text(`⚠ ${derivs.alarmSegments.length} episodi di variazione rapida (>10 mmHg/h)`, S.margin, y)
        }
    }
    return y + S.sectionGap
}

async function addCharts(doc, y, readings, stats) {
    if (readings.length < 2) return y

    doc.setFontSize(T.sectionTitle)
    doc.setTextColor(...C.brand)
    doc.text('Grafici', S.margin, y)
    y += S.innerGap + 2

    const pw = doc.internal.pageSize.getWidth()
    const imgW = pw - S.margin * 2

    // Line chart with insight title
    doc.setFontSize(T.small)
    doc.setTextColor(...C.textBody)
    const s = stats
    doc.text(`Andamento pressione: media ${s.avgSystolic}/${s.avgDiastolic} mmHg`, S.margin, y)
    y += 3

    const lineImg = await renderChart(buildLineChart(readings), 800, 350)
    doc.addImage(lineImg, 'PNG', S.margin - 2, y, imgW + 4, 62)
    y += 66

    if (y > 195) { doc.addPage(); y = 20 }

    // Doughnut chart with insight
    doc.setFontSize(T.small)
    doc.setTextColor(...C.textBody)
    const dist = stats.categoryDistribution || {}
    const dominant = Object.entries(dist).sort((a, b) => b[1] - a[1])[0]
    const insight = dominant ? `Categoria prevalente: ${dominant[0]} (${dominant[1]} letture)` : ''
    doc.text(`Distribuzione categorie  •  ${insight}`, S.margin, y)
    y += 3

    const doughImg = await renderChart(buildDoughnutChart(stats), 600, 400)
    doc.addImage(doughImg, 'PNG', S.margin - 2, y, imgW + 4, 58)
    y += 62

    return y + S.innerGap
}

function addHistoryTable(doc, y, data) {
    if (data.length === 0) return y

    doc.setFontSize(T.sectionTitle)
    doc.setTextColor(...C.brand)
    doc.text('Storico Misurazioni', S.margin, y)
    y += S.innerGap + 2

    const pw = doc.internal.pageSize.getWidth()
    const tw = pw - S.margin * 2
    // Columns: Data | Ora | SYS | DIA | BPM | Categoria | Note
    const colX = [S.margin, 43, 62, 77, 92, 107, 134, 174]
    const colAlign = ['left', 'left', 'right', 'right', 'right', 'left', 'left']
    const headers = ['Data', 'Ora', 'SYS', 'DIA', 'BPM', 'Categoria', 'Note']

    // Header
    doc.setFillColor(...C.brand)
    doc.rect(S.margin, y, tw, S.headerH, 'F')
    doc.setFontSize(7)
    doc.setTextColor(...C.white)
    headers.forEach((h, i) => doc.text(h, colAlign[i] === 'right' ? colX[i + 1] : colX[i], y + 3.8, colAlign[i] === 'right' ? { align: 'right' } : undefined))
    y += S.headerH

    const catColors = {
        'Normale': [0, 108, 76], 'Elevata': [249, 168, 37], 'Ipertensione Grado 1': [239, 108, 0],
        'Ipertensione Grado 2': [211, 47, 47], 'Crisi Ipertensiva': [123, 31, 162], 'Ipotensione': [25, 118, 210]
    }

    doc.setFontSize(7)
    for (let idx = 0; idx < data.length; idx++) {
        const r = data[idx]
        if (y > 276) { doc.addPage(); y = 20; /* repeat header on new page */ }

        if (idx % 2 === 0) { doc.setFillColor(...C.rowEven); doc.rect(S.margin, y - 1, tw, S.rowH, 'F') }

        const d = new Date(r.timestamp)
        doc.setTextColor(...C.textBody)
        doc.text(d.toLocaleDateString('it-IT'), colX[0], y + 3.2)
        doc.setTextColor(...C.textMuted)
        doc.text(d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }), colX[1], y + 3.2)

        // SYS — color by clinical range
        const sysColor = r.systolic >= 140 ? C.error : r.systolic >= 130 ? C.warning : C.textStrong
        doc.setTextColor(...sysColor)
        doc.text(String(r.systolic), colX[3], y + 3.2, { align: 'right' })

        // DIA
        const diaColor = r.diastolic >= 90 ? C.error : r.diastolic >= 85 ? C.warning : C.textStrong
        doc.setTextColor(...diaColor)
        doc.text(String(r.diastolic), colX[4], y + 3.2, { align: 'right' })

        // BPM
        doc.setTextColor(...C.textBody)
        doc.text(String(r.heartRate || '–'), colX[5], y + 3.2, { align: 'right' })

        // Category
        const cc = catColors[r.category] || C.textMuted
        doc.setTextColor(...cc)
        doc.text(getCategoryLabel(r.category) || r.category || '–', colX[6], y + 3.2)

        // Notes
        doc.setTextColor(...C.textMuted)
        doc.text((r.notes || '').slice(0, 28), colX[7], y + 3.2)
        y += S.rowH
    }

    return y + S.innerGap
}

function addFooter(doc) {
    doc.setFontSize(6)
    doc.setTextColor(180, 180, 180)
    doc.text('Report generato da Pressione App — Non costituisce diagnosi medica. Consultare sempre un medico.', S.margin, 290)
}

// ── Main ───────────────────────────────────────────────────────

export async function generatePDF({ data, readings7, readings30, username, anonymize, includeCharts, includeHistory }) {
    const doc = new jsPDF({ unit: 'mm', format: 'a4' })
    const stats = computeStatistics(data)

    let y = addHeader(doc, { data, username, anonymize })
    y = addMultiPeriodComparison(doc, y, readings7, readings30, data)
    y = addAdvancedStats(doc, y, data)

    if (includeCharts && data.length >= 2) {
        if (y > 170) { doc.addPage(); y = 20 }
        y = await addCharts(doc, y, data, stats)
    }

    if (includeHistory && data.length > 0) {
        if (y > 195) { doc.addPage(); y = 20 }
        y = addHistoryTable(doc, y, data)
    }

    // Footer on all pages
    for (let i = 1; i <= doc.getNumberOfPages(); i++) {
        doc.setPage(i)
        addFooter(doc)
    }

    doc.save(`pressione_report_${new Date().toISOString().slice(0, 10)}.pdf`)
}

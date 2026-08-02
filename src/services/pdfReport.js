import { jsPDF } from 'jspdf'
import {
    computeStatistics,
    computeDerivatives,
    computeMorningSurge,
    computeHypertensiveLoad,
    computeHRV
} from '@/services/statistics.js'
import { getCategoryLabel } from '@/services/categories.js'

// --- Chart rendering helpers ---

/**
 * Render a Chart.js config to an off-screen canvas and return base64 PNG.
 */
async function renderChartToImage(config, width, height) {
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    canvas.style.display = 'none'
    document.body.appendChild(canvas)

    try {
        // Dynamic import of Chart.js
        const { Chart, registerables } = await import('chart.js')
        Chart.register(...registerables)

        // Import annotation plugin
        try {
            const annotationPlugin = await import('chartjs-plugin-annotation')
            Chart.register(annotationPlugin.default)
        } catch { /* annotation plugin optional */ }

        const ctx = canvas.getContext('2d')
        new Chart(ctx, config)

        // Wait for chart to render
        await new Promise(r => setTimeout(r, 300))

        const dataUrl = canvas.toDataURL('image/png')
        return dataUrl
    } finally {
        document.body.removeChild(canvas)
    }
}

/**
 * Build SYS/DIA line chart config with OMS safety zones.
 */
function buildLineChart(readings) {
    const sorted = [...readings].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    const labels = sorted.map(r => new Date(r.timestamp).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' }))
    const sysData = sorted.map(r => r.systolic)
    const diaData = sorted.map(r => r.diastolic)

    return {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: 'Sistolica',
                    data: sysData,
                    borderColor: '#BA1A1A',
                    backgroundColor: 'rgba(186,26,26,0.1)',
                    tension: 0.3,
                    pointRadius: 2,
                    borderWidth: 2
                },
                {
                    label: 'Diastolica',
                    data: diaData,
                    borderColor: '#1976D2',
                    backgroundColor: 'rgba(25,118,210,0.1)',
                    tension: 0.3,
                    pointRadius: 2,
                    borderWidth: 2
                }
            ]
        },
        options: {
            responsive: false,
            animation: false,
            plugins: {
                legend: { display: true, position: 'bottom', labels: { boxWidth: 12, font: { size: 10 } } },
                annotation: {
                    annotations: {
                        sysHigh: {
                            type: 'box',
                            yMin: 90, yMax: 140,
                            backgroundColor: 'rgba(0,108,76,0.07)',
                            borderWidth: 0,
                            label: { display: true, content: 'Normale OMS', position: 'start', font: { size: 8 } }
                        }
                    }
                }
            },
            scales: {
                x: { ticks: { maxTicksLimit: 12, font: { size: 8 } }, grid: { display: false } },
                y: { min: 0, max: 200, ticks: { font: { size: 8 } } }
            }
        }
    }
}

/**
 * Build category doughnut chart.
 */
function buildDoughnutChart(stats) {
    const dist = stats.categoryDistribution || {}
    const categories = ['Normale', 'Elevata', 'Ipertensione Grado 1', 'Ipertensione Grado 2', 'Crisi Ipertensiva', 'Ipotensione']
    const keys = ['Normale', 'Elevata', 'Ipertensione Grado 1', 'Ipertensione Grado 2', 'Crisi Ipertensiva', 'Ipotensione']
    const colors = ['#006C4C', '#F9A825', '#EF6C00', '#D32F2F', '#7B1FA2', '#1976D2']

    const data = keys.map(k => dist[k] || 0)

    return {
        type: 'doughnut',
        data: {
            labels: categories,
            datasets: [{
                data,
                backgroundColor: colors,
                borderWidth: 0
            }]
        },
        options: {
            responsive: false,
            animation: false,
            plugins: {
                legend: { display: true, position: 'right', labels: { boxWidth: 8, font: { size: 8 }, padding: 8 } }
            }
        }
    }
}

// --- Section builders ---

function addHeader(doc, options) {
    const pageW = doc.internal.pageSize.getWidth()

    // Green header bar
    doc.setFillColor(0, 108, 76)
    doc.rect(0, 0, pageW, 18, 'F')

    // Title
    doc.setFontSize(13)
    doc.setTextColor(255, 255, 255)
    doc.text('Pressione  —  Report Pressione Arteriosa', 14, 11)

    // Subtitle
    doc.setFontSize(8)
    doc.setTextColor(100, 100, 100)
    let y = 24
    const from = options.data.length
        ? new Date(options.data[options.data.length - 1].timestamp).toLocaleDateString('it-IT')
        : 'N/D'
    const to = options.data.length
        ? new Date(options.data[0].timestamp).toLocaleDateString('it-IT')
        : 'N/D'
    doc.text(`Periodo: ${from} – ${to}  •  ${options.data.length} misurazioni  •  Generato: ${new Date().toLocaleDateString('it-IT')}`, 14, y)
    if (!options.anonymize) {
        y += 4
        doc.text(`Paziente: ${options.username}`, 14, y)
    }
    return y + 8
}

function addMultiPeriodComparison(doc, y, readings7, readings30, data) {
    const s7 = computeStatistics(readings7)
    const s30 = computeStatistics(readings30)
    const sSel = computeStatistics(data)

    doc.setFontSize(12)
    doc.setTextColor(0, 108, 76)
    doc.text('Confronto Multi-Periodo', 14, y)
    y += 8

    // Table layout: label col + 3 data columns
    const labelW = 32
    const colW = 45
    const x0 = 14
    const xLabel = x0
    const x1 = x0 + labelW
    const x2 = x1 + colW
    const x3 = x2 + colW
    const tableW = labelW + colW * 3

    doc.setFontSize(8)
    doc.setTextColor(0, 0, 0)

    // Headers
    doc.setFillColor(0, 108, 76, 0.1)
    doc.rect(x0, y, tableW, 5, 'F')
    doc.setTextColor(0, 108, 76)
    doc.setFontSize(7)
    doc.text('', xLabel + 2, y + 3.5)  // empty label column
    doc.text('7 Giorni', x1 + 2, y + 3.5)
    doc.text('30 Giorni', x2 + 2, y + 3.5)
    doc.text('Periodo selez.', x3 + 2, y + 3.5)
    y += 6

    // Rows
    const rows = [
        ['Letture', String(s7.readingsCount), String(s30.readingsCount), String(sSel.readingsCount)],
        ['SYS/DIA media', `${s7.avgSystolic}/${s7.avgDiastolic}`, `${s30.avgSystolic}/${s30.avgDiastolic}`, `${sSel.avgSystolic}/${sSel.avgDiastolic}`],
        ['BPM medio', String(s7.avgHeartRate), String(s30.avgHeartRate), String(sSel.avgHeartRate)],
        ['SYS min–max', `${s7.minSystolic}–${s7.maxSystolic}`, `${s30.minSystolic}–${s30.maxSystolic}`, `${sSel.minSystolic}–${sSel.maxSystolic}`],
        ['DIA min–max', `${s7.minDiastolic}–${s7.maxDiastolic}`, `${s30.minDiastolic}–${s30.maxDiastolic}`, `${sSel.minDiastolic}–${sSel.maxDiastolic}`]
    ]

    doc.setFontSize(7)
    doc.setTextColor(60, 60, 60)
    for (let i = 0; i < rows.length; i++) {
        const isEven = i % 2 === 0
        if (isEven) {
            doc.setFillColor(245, 245, 245)
            doc.rect(x0, y - 1, tableW, 5, 'F')
        }
        doc.setTextColor(100, 100, 100)
        doc.text(rows[i][0], xLabel + 2, y + 2.5)
        doc.setTextColor(60, 60, 60)
        doc.text(rows[i][1], x1 + 2, y + 2.5)
        doc.text(rows[i][2], x2 + 2, y + 2.5)
        doc.text(rows[i][3], x3 + 2, y + 2.5)
        y += 5
    }

    return y + 6
}

function addAdvancedStats(doc, y, readings) {
    const surge = computeMorningSurge(readings)
    const load = computeHypertensiveLoad(readings)
    const hrv = computeHRV(readings)

    doc.setFontSize(12)
    doc.setTextColor(0, 108, 76)
    doc.text('Statistiche Avanzate', 14, y)
    y += 8

    doc.setFontSize(9)
    doc.setTextColor(60, 60, 60)

    const items = [
        { label: 'Picco Mattutino', value: surge.delta !== null ? `${surge.delta > 0 ? '+' : ''}${surge.delta} mmHg` : 'N/D', alert: surge.alert },
        { label: 'Carico Ipertensivo', value: `${load.percentage}% (${load.abnormal}/${load.total})`, alert: load.percentage > 30 },
        { label: 'HRV (dev. std.)', value: hrv !== null ? `${hrv} BPM` : 'N/D', alert: false }
    ]

    for (const item of items) {
        doc.setFontSize(9)
        doc.setTextColor(60, 60, 60)
        doc.text(`• ${item.label}:`, 18, y)
        doc.setTextColor(item.alert ? 186 : 60, item.alert ? 26 : 60, item.alert ? 26 : 60)
        doc.text(item.value, 80, y)
        if (item.alert) {
            doc.setFontSize(6)
            doc.setTextColor(186, 26, 26)
            doc.text('⚠', 76, y)
        }
        y += 6
    }

    // Trend interpretation
    if (readings.length >= 3) {
        const derivs = computeDerivatives(readings)
        y += 3
        doc.setFontSize(8)
        doc.setTextColor(100, 100, 100)
        const avgRate = derivs.systolic.length > 0
            ? Math.round(derivs.systolic.reduce((a, b) => a + b, 0) / derivs.systolic.length * 10) / 10
            : 0
        const trendText = avgRate > 2 ? 'in aumento' : avgRate < -2 ? 'in diminuzione' : 'stabile'
        doc.text(`Tendenza sistolica: ${trendText} (variazione media ${avgRate > 0 ? '+' : ''}${avgRate} mmHg/h)`, 18, y)
        if (derivs.alarmSegments.length > 0) {
            y += 5
            doc.setTextColor(186, 26, 26)
            doc.text(`⚠ ${derivs.alarmSegments.length} episodi di variazione rapida (>10 mmHg/h) rilevati`, 18, y)
        }
    }

    return y + 6
}

async function addCharts(doc, y, readings, stats) {
    if (readings.length < 2) return y

    doc.setFontSize(12)
    doc.setTextColor(0, 108, 76)
    doc.text('Grafici', 14, y)
    y += 6

    const pageW = doc.internal.pageSize.getWidth()
    const chartW = (pageW - 30) / 2

    // Line chart
    doc.setFontSize(8)
    doc.setTextColor(100, 100, 100)
    doc.text('Andamento Pressione (OMS)', 14, y)
    y += 3

    const lineConfig = buildLineChart(readings)
    const lineImg = await renderChartToImage(lineConfig, 800, 350)
    doc.addImage(lineImg, 'PNG', 12, y, pageW - 24, 60)
    y += 63

    // Check if we need a new page for remaining charts
    if (y > 200) { doc.addPage(); y = 20 }

    // Doughnut chart
    doc.setFontSize(8)
    doc.setTextColor(100, 100, 100)
    doc.text('Distribuzione Categorie OMS', 14, y)
    y += 3

    const doughnutConfig = buildDoughnutChart(stats)
    const doughnutImg = await renderChartToImage(doughnutConfig, 600, 400)
    doc.addImage(doughnutImg, 'PNG', 12, y, pageW - 24, 55)
    y += 58

    return y + 4
}

function addHistoryTable(doc, y, data) {
    if (data.length === 0) return y

    doc.setFontSize(12)
    doc.setTextColor(0, 108, 76)
    doc.text('Storico Misurazioni', 14, y)
    y += 8

    // Table header
    const pageW = doc.internal.pageSize.getWidth()
    doc.setFillColor(0, 108, 76, 0.12)
    doc.rect(14, y, pageW - 28, 5, 'F')
    doc.setFontSize(7)
    doc.setTextColor(0, 108, 76)

    const colPositions = [16, 48, 78, 98, 118, 136, 160]
    const colHeaders = ['Data', 'Ora', 'Sistolica', 'Diastolica', 'BPM', 'Categoria', 'Note']
    colHeaders.forEach((h, i) => doc.text(h, colPositions[i], y + 3.5))
    y += 6

    // Category colors
    const catColors = {
        'Normale': [0, 108, 76],
        'Elevata': [249, 168, 37],
        'Ipertensione Grado 1': [239, 108, 0],
        'Ipertensione Grado 2': [211, 47, 47],
        'Crisi Ipertensiva': [123, 31, 162],
        'Ipotensione': [25, 118, 210]
    }

    doc.setFontSize(7)
    for (const r of data) {
        if (y > 275) { doc.addPage(); y = 20 }

        const isEven = data.indexOf(r) % 2 === 0
        if (isEven) {
            doc.setFillColor(250, 250, 250)
            doc.rect(14, y - 1, pageW - 28, 5, 'F')
        }

        const d = new Date(r.timestamp)
        doc.setTextColor(100, 100, 100)
        doc.text(d.toLocaleDateString('it-IT'), colPositions[0], y + 2.5)
        doc.text(d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }), colPositions[1], y + 2.5)

        // Systolic: color based on value
        const sysColor = r.systolic >= 140 ? [186, 26, 26] : r.systolic >= 130 ? [239, 108, 0] : [60, 60, 60]
        doc.setTextColor(...sysColor)
        doc.text(String(r.systolic), colPositions[2], y + 2.5)

        // Diastolic: color based on value
        const diaColor = r.diastolic >= 90 ? [186, 26, 26] : r.diastolic >= 85 ? [239, 108, 0] : [60, 60, 60]
        doc.setTextColor(...diaColor)
        doc.text(String(r.diastolic), colPositions[3], y + 2.5)

        doc.setTextColor(60, 60, 60)
        doc.text(String(r.heartRate || '–'), colPositions[4], y + 2.5)

        // Category with color
        const catColor = catColors[r.category] || [100, 100, 100]
        doc.setTextColor(...catColor)
        doc.text(getCategoryLabel(r.category) || r.category || '–', colPositions[5], y + 2.5)

        doc.setTextColor(130, 130, 130)
        doc.text((r.notes || '').slice(0, 22), colPositions[6], y + 2.5)
        y += 5
    }

    return y + 4
}

function addFooter(doc) {
    const pageW = doc.internal.pageSize.getWidth()
    doc.setFontSize(6)
    doc.setTextColor(160, 160, 160)
    doc.text('Report generato da Pressione App — Non costituisce diagnosi medica. Consultare sempre un medico.', 14, 290)
}

// --- Main export ---

/**
 * Generate a professional PDF report with charts and multi-period stats.
 */
export async function generatePDF({ data, readings7, readings30, username, anonymize, includeCharts, includeHistory }) {
    const doc = new jsPDF({ unit: 'mm', format: 'a4' })
    const stats = computeStatistics(data)

    let y = addHeader(doc, { data, username, anonymize })

    // Multi-period comparison (always included)
    y = addMultiPeriodComparison(doc, y, readings7, readings30, data)

    // Advanced stats
    y = addAdvancedStats(doc, y, data)

    // Charts
    if (includeCharts && data.length >= 2) {
        // Check page break before charts
        if (y > 180) { doc.addPage(); y = 20 }
        y = await addCharts(doc, y, data, stats)
    }

    // History table
    if (includeHistory && data.length > 0) {
        if (y > 200) { doc.addPage(); y = 20 }
        y = addHistoryTable(doc, y, data)
    }

    // Footer on all pages
    const pageCount = doc.getNumberOfPages()
    for (let i = 1; i <= pageCount; i++) {
        doc.setPage(i)
        addFooter(doc)
    }

    doc.save(`pressione_report_${new Date().toISOString().slice(0, 10)}.pdf`)
}

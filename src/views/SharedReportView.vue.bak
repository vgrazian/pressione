<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { supabase } from '@/services/supabaseClient.js'
import { computeStatistics, computeDerivatives, computeMorningSurge, computeHypertensiveLoad } from '@/services/statistics.js'
import { getCategoryLabel, classifyReading } from '@/services/categories.js'
import { getDefaultBands, getBandForHour } from '@/services/timeBands.js'
import { getChartColors } from '@/services/chartColors.js'
import { Chart, registerables } from 'chart.js'
import annotationPlugin from 'chartjs-plugin-annotation'

Chart.register(...registerables, annotationPlugin)

const route = useRoute()
const report = ref(null)
const error = ref('')
const isLoading = ref(true)
const needsPin = ref(false)
const pinInput = ref('')
const pinError = ref('')
const bpChartEl = ref(null)
const derivChartEl = ref(null)
const pieChartEl = ref(null)
let bpChart = null, derivChart = null, pieChart = null
const dateFilter = ref('all')

const bands = getDefaultBands()

const filteredReadings = computed(() => {
  if (!report.value?.readings) return []
  let result = [...report.value.readings].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
  if (dateFilter.value === '7') {
    const c = new Date(Date.now() - 7 * 86400000)
    result = result.filter(r => new Date(r.timestamp) >= c)
  } else if (dateFilter.value === '30') {
    const c = new Date(Date.now() - 30 * 86400000)
    result = result.filter(r => new Date(r.timestamp) >= c)
  }
  return result
})

const stats = computed(() => computeStatistics(filteredReadings.value, bands))
const derivatives = computed(() => computeDerivatives(filteredReadings.value))
const morningSurge = computed(() => computeMorningSurge(filteredReadings.value, bands))
const htnLoad = computed(() => computeHypertensiveLoad(filteredReadings.value))
const classification = computed(() => classifyReading(stats.value.avgSystolic, stats.value.avgDiastolic))

async function hashPin(pin) {
  const d = new TextEncoder().encode(pin)
  const h = await crypto.subtle.digest('SHA-256', d)
  return Array.from(new Uint8Array(h)).map(b => b.toString(16).padStart(2, '0')).join('')
}

async function loadReport(pinHash = null) {
  isLoading.value = true; error.value = ''
  try {
    const token = route.params.token
    const { data, err } = await supabase.from('settings').select('value').eq('key', '_share_' + token).limit(1)
    if (err || !data || data.length === 0) throw new Error('not found')
    const stored = JSON.parse(data[0].value)
    if (stored.revoked || new Date(stored.expiresAt) < new Date()) throw new Error('expired')
    if (stored.pinHash) {
      if (!pinHash) { needsPin.value = true; isLoading.value = false; return }
      if (pinHash !== stored.pinHash) throw new Error('wrong pin')
    }
    report.value = stored.reportData
  } catch (e) { error.value = 'Link non valido, scaduto o revocato.' }
  finally {
    isLoading.value = false
    await nextTick()
    renderCharts()
  }
}

async function submitPin() {
  pinError.value = ''
  if (pinInput.value.length !== 4) { pinError.value = 'Inserisci 4 cifre'; return }
  await loadReport(await hashPin(pinInput.value))
}

onMounted(() => loadReport())

// Re-render charts when date filter changes
watch(dateFilter, async () => { await nextTick(); renderCharts() })

function renderCharts() { renderBPChart(); renderDerivChart(); renderPieChart() }

function renderBPChart() {
  if (bpChart) { bpChart.destroy(); bpChart = null }
  if (!bpChartEl.value) return
  const data = filteredReadings.value
  if (!data.length) return
  const C = getChartColors()
  const labels = data.map(r => new Date(r.timestamp).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' }))
  bpChart = new Chart(bpChartEl.value, {
    type: 'line', data: { labels, datasets: [
      { label: 'Sistolica', data: data.map(r => r.systolic), borderColor: C.systolic, backgroundColor: C.systolicBg, borderWidth: 2, pointRadius: 3, pointHoverRadius: 6, tension: 0.35, fill: false },
      { label: 'Diastolica', data: data.map(r => r.diastolic), borderColor: C.diastolic, backgroundColor: C.diastolicBg, borderWidth: 2, pointRadius: 3, pointHoverRadius: 6, tension: 0.35, fill: false },
      { label: 'BPM', data: data.map(r => r.heartRate), borderColor: C.bpm, borderWidth: 1, pointRadius: 1, borderDash: [4, 3], tension: 0.35, fill: false, yAxisID: 'y1' }
    ] },
    options: {
      responsive: true, maintainAspectRatio: false, interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16, font: { size: 11 }, color: C.textSecondary } },
        annotation: { annotations: {
          goalZone: { type: 'box', yMin: 90, yMax: 140, backgroundColor: C.targetZoneBg, borderColor: C.targetZoneBorder, borderWidth: 1, borderDash: [6, 3], label: { display: true, content: 'Target <140/90', position: 'start', font: { size: 9 }, backgroundColor: C.targetLabelBg, color: C.targetLabelText } },
          sys140: { type: 'line', yMin: 140, yMax: 140, borderColor: 'rgba(186,26,26,0.4)', borderWidth: 1, borderDash: [5, 5] }
        } },
        tooltip: { callbacks: {
          title: (ctx) => data[ctx[0].dataIndex] ? new Date(data[ctx[0].dataIndex].timestamp).toLocaleString('it-IT', { dateStyle: 'medium', timeStyle: 'short' }) : '',
          label: (ctx) => { const r = data[ctx.dataIndex]; if (!r) return ''; if (ctx.datasetIndex === 0) return `Sistolica: ${r.systolic} mmHg`; if (ctx.datasetIndex === 1) return `Diastolica: ${r.diastolic} mmHg`; return `BPM: ${r.heartRate}` },
          afterLabel: (ctx) => { const r = data[ctx.dataIndex]; if (!r || ctx.datasetIndex > 1) return ''; return `Categoria: ${getCategoryLabel(r.category || classifyReading(r.systolic, r.diastolic))}` }
        } }
      },
      scales: {
        x: { ticks: { maxTicksLimit: 14, font: { size: 10 } }, grid: { display: false } },
        y: { type: 'linear', position: 'left', min: 40, max: 200, ticks: { stepSize: 20, font: { size: 10 } }, title: { display: true, text: 'mmHg', font: { size: 10 } } },
        y1: { type: 'linear', position: 'right', min: 40, max: 140, ticks: { stepSize: 20, font: { size: 10 } }, title: { display: true, text: 'BPM', font: { size: 10 } }, grid: { drawOnChartArea: false } }
      }
    }
  })
}

function renderDerivChart() {
  if (derivChart) { derivChart.destroy(); derivChart = null }
  if (!derivChartEl.value) return
  const d = derivatives.value
  if (!d.timestamps.length) return
  const labels = d.timestamps.map(t => new Date(t).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' }))
  const C2 = getChartColors()
  derivChart = new Chart(derivChartEl.value, {
    type: 'bar', data: { labels, datasets: [{ label: 'dS/dt', data: d.systolic, backgroundColor: d.systolic.map(v => Math.abs(v) > 10 ? C2.derivAlarm : v > 0 ? C2.derivPositive : C2.derivNegative), borderWidth: 0, borderRadius: 2 }] },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => `Δ ${ctx.raw > 0 ? '+' : ''}${ctx.raw} mmHg/ora` } } },
      scales: {
        x: { ticks: { maxTicksLimit: 12, font: { size: 10 } }, grid: { display: false } },
        y: { min: -Math.max(d.maxRate + 5, 20), max: Math.max(d.maxRate + 5, 20), ticks: { font: { size: 10 } }, title: { display: true, text: 'mmHg/ora', font: { size: 10 } } }
      }
    }
  })
}

function renderPieChart() {
  if (pieChart) { pieChart.destroy(); pieChart = null }
  if (!pieChartEl.value) return
  const data = filteredReadings.value
  if (!data.length) return
  const cats = {}
  data.forEach(r => { const l = getCategoryLabel(r.category || classifyReading(r.systolic, r.diastolic)); cats[l] = (cats[l] || 0) + 1 })
  const C3 = getChartColors()
  const colors = { 'Normale': C3.catNormal, 'Elevata': C3.catElevated, 'Ipert. Stadio 1': C3.catStage1, 'Ipert. Stadio 2': C3.catStage2, 'Crisi Ipertensiva': C3.catCrisis, 'Ipotensione': C3.catHypotension }
  pieChart = new Chart(pieChartEl.value, {
    type: 'doughnut', data: { labels: Object.keys(cats), datasets: [{ data: Object.values(cats), backgroundColor: Object.keys(cats).map(k => colors[k] || '#999'), borderWidth: 1, borderColor: C3.surfaceRaised }] },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, padding: 12, font: { size: 10 } } } } }
  })
}

const readingsByTimeOfDay = computed(() => {
  const m = {}
  for (const b of bands) m[b.key] = { label: b.label, icon: b.icon, count: 0, systolic: [] }
  for (const r of filteredReadings.value) {
    const band = getBandForHour(new Date(r.timestamp).getHours(), bands)
    if (m[band.key]) { m[band.key].count++; m[band.key].systolic.push(r.systolic) }
  }
  return Object.values(m).map(b => ({ ...b, avgSys: b.systolic.length ? Math.round(b.systolic.reduce((s, v) => s + v, 0) / b.systolic.length) : null }))
})
</script>

<template>
  <div style="max-width:900px;margin:0 auto;padding:2rem 1.5rem;font-family:Inter,sans-serif;">
    <div v-if="isLoading" style="text-align:center;padding:4rem;"><p>Caricamento...</p></div>

    <div v-else-if="needsPin" style="text-align:center;padding:4rem;">
      <h2 style="color:#006C4C;margin-bottom:1rem">🔒 Report Protetto</h2>
      <p style="color:#666;margin-bottom:1.5rem">Inserisci il PIN di 4 cifre fornito dal paziente.</p>
      <input v-model="pinInput" type="text" inputmode="numeric" maxlength="4" placeholder="1234" style="font-size:2rem;text-align:center;width:120px;padding:8px;border:2px solid #006C4C;border-radius:8px;letter-spacing:8px" />
      <div v-if="pinError" style="color:#BA1A1A;margin-top:8px;font-size:0.875rem">{{ pinError }}</div>
      <button @click="submitPin" style="margin-top:1rem;padding:8px 24px;background:#006C4C;color:white;border:none;border-radius:8px;font-size:1rem;cursor:pointer">Sblocca</button>
    </div>

    <div v-else-if="error" style="text-align:center;padding:4rem;"><h2 style="color:#BA1A1A">⚠️ {{ error }}</h2></div>

    <template v-else-if="report && filteredReadings.length">
      <!-- Header -->
      <div style="background:#006C4C;color:white;padding:1.5rem;border-radius:12px 12px 0 0">
        <h1 style="margin:0 0 4px;font-size:1.5rem">📊 Report Pressione Arteriosa</h1>
        <p style="margin:0;opacity:0.85;font-size:0.875rem">{{ filteredReadings.length }} misurazioni — {{ new Date(filteredReadings[filteredReadings.length-1].timestamp).toLocaleDateString('it-IT') }} – {{ new Date(filteredReadings[0].timestamp).toLocaleDateString('it-IT') }}</p>
      </div>

      <!-- Clinical Summary -->
      <div style="background:white;border:1px solid #E0E0E0;border-top:none;padding:1.5rem;border-radius:0 0 12px 12px;margin-bottom:1.5rem">
        <div style="display:inline-block;padding:4px 14px;border-radius:20px;font-size:0.875rem;font-weight:600;color:white;margin-bottom:1rem" :style="{ background: getChartColors().categoryMap[classification] }">{{ getCategoryLabel(classification) }}</div>

        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin-bottom:1rem">
          <div style="background:#F8F9F7;border-radius:8px;padding:12px;text-align:center">
            <div style="font-size:0.6875rem;color:#888;text-transform:uppercase">Media SYS/DIA</div>
            <div style="font-size:1.5rem;font-weight:700;color:#333">{{ stats.avgSystolic }}/{{ stats.avgDiastolic }}</div><div style="font-size:0.6875rem;color:#888">mmHg</div>
          </div>
          <div style="background:#F8F9F7;border-radius:8px;padding:12px;text-align:center">
            <div style="font-size:0.6875rem;color:#888;text-transform:uppercase">BPM Medio</div>
            <div style="font-size:1.5rem;font-weight:700;color:#333">{{ stats.avgHeartRate }}</div><div style="font-size:0.6875rem;color:#888">battiti/min</div>
          </div>
          <div style="background:#F8F9F7;border-radius:8px;padding:12px;text-align:center">
            <div style="font-size:0.6875rem;color:#888;text-transform:uppercase">Carico Ipertensivo</div>
            <div style="font-size:1.5rem;font-weight:700" :style="{ color: htnLoad.percentage > 30 ? '#D32F2F' : '#006C4C' }">{{ htnLoad.percentage }}%</div><div style="font-size:0.6875rem;color:#888">{{ htnLoad.abnormal }}/{{ htnLoad.total }} anomale</div>
          </div>
          <div style="background:#F8F9F7;border-radius:8px;padding:12px;text-align:center">
            <div style="font-size:0.6875rem;color:#888;text-transform:uppercase">Picco Mattutino</div>
            <div style="font-size:1.5rem;font-weight:700" :style="{ color: morningSurge.alert ? '#D32F2F' : '#333' }">{{ morningSurge.delta !== null ? (morningSurge.delta > 0 ? '+' : '') + morningSurge.delta : 'N/D' }}</div><div style="font-size:0.6875rem;color:#888">mmHg Δ</div>
          </div>
        </div>

        <div v-if="morningSurge.alert" style="background:#FFF3E0;border-left:3px solid #EF6C00;padding:8px 12px;margin-bottom:8px;border-radius:4px;font-size:0.8125rem">⚠️ Picco mattutino elevato: Δ {{ morningSurge.delta }} mmHg — Rischio cardiovascolare aumentato (ESC/ESH 2024)</div>
        <div v-if="htnLoad.percentage > 30" style="background:#FFEBEE;border-left:3px solid #D32F2F;padding:8px 12px;margin-bottom:8px;border-radius:4px;font-size:0.8125rem">⚠️ Carico ipertensivo &gt;30%: {{ htnLoad.abnormal }} letture fuori range su {{ htnLoad.total }}</div>
        <div v-if="derivatives.alarmSegments.length > 0" style="background:#FFF8E1;border-left:3px solid #F9A825;padding:8px 12px;margin-bottom:8px;border-radius:4px;font-size:0.8125rem">⚠️ {{ derivatives.alarmSegments.length }} episodi di variazione rapida (&gt;10 mmHg/ora)</div>
        <div v-if="!morningSurge.alert && htnLoad.percentage <= 30 && derivatives.alarmSegments.length === 0" style="background:#E8F5E9;border-left:3px solid #006C4C;padding:8px 12px;border-radius:4px;font-size:0.8125rem;color:#006C4C">✅ Nessun indicatore di rischio critico rilevato nel periodo.</div>
      </div>

      <!-- Date filter -->
      <div style="display:flex;gap:8px;margin-bottom:1rem">
        <button v-for="f in [{k:'all',l:'Tutto'},{k:'30',l:'30 giorni'},{k:'7',l:'7 giorni'}]" :key="f.k" @click="dateFilter = f.k" :style="{ padding:'5px 16px',borderRadius:'20px',border:'1px solid '+(dateFilter===f.k?'#006C4C':'#CCC'),background:dateFilter===f.k?'#006C4C':'white',color:dateFilter===f.k?'white':'#666',fontSize:'0.8125rem',cursor:'pointer',fontWeight:dateFilter===f.k?'600':'400' }">{{ f.l }}</button>
      </div>

      <!-- BP Chart -->
      <div style="background:white;border:1px solid #E0E0E0;border-radius:12px;padding:1.5rem;margin-bottom:1rem">
        <h3 style="margin:0 0 1rem;font-size:1rem;color:#333">Andamento Pressione</h3>
        <div style="position:relative;height:300px"><canvas ref="bpChartEl"></canvas></div>
        <p style="font-size:0.6875rem;color:#999;margin-top:8px">Zona verde tratteggiata: range target ESC/ESH (&lt;140/90 mmHg). Passa il mouse sui punti per i dettagli.</p>
      </div>

      <!-- Derivative + Pie -->
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem;margin-bottom:1rem">
        <div v-if="derivatives.timestamps.length" style="background:white;border:1px solid #E0E0E0;border-radius:12px;padding:1.5rem">
          <h3 style="margin:0 0 0.5rem;font-size:0.9375rem;color:#333">Variazione oraria (dP/dt)</h3>
          <p style="font-size:0.6875rem;color:#999;margin-bottom:0.75rem">mmHg/ora — rosso &gt;10</p>
          <div style="position:relative;height:200px"><canvas ref="derivChartEl"></canvas></div>
        </div>
        <div style="background:white;border:1px solid #E0E0E0;border-radius:12px;padding:1.5rem">
          <h3 style="margin:0 0 0.5rem;font-size:0.9375rem;color:#333">Distribuzione categorie</h3>
          <div style="position:relative;height:200px"><canvas ref="pieChartEl"></canvas></div>
        </div>
      </div>

      <!-- Time of day -->
      <div style="background:white;border:1px solid #E0E0E0;border-radius:12px;padding:1.5rem;margin-bottom:1rem">
        <h3 style="margin:0 0 0.75rem;font-size:0.9375rem;color:#333">Per fascia oraria</h3>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px">
          <div v-for="b in readingsByTimeOfDay" :key="b.label" style="background:#F8F9F7;border-radius:8px;padding:10px;text-align:center" :style="{ opacity: b.count > 0 ? 1 : 0.4 }">
            <div style="font-size:1.25rem">{{ b.icon }}</div>
            <div style="font-size:0.6875rem;color:#888;margin-top:2px">{{ b.label }}</div>
            <div v-if="b.avgSys" style="font-size:0.9375rem;font-weight:600;margin-top:4px">{{ b.avgSys }} mmHg</div>
            <div style="font-size:0.6875rem;color:#888">{{ b.count }} letture</div>
          </div>
        </div>
      </div>

      <!-- Readings table -->
      <div style="background:white;border:1px solid #E0E0E0;border-radius:12px;padding:1.5rem;margin-bottom:1rem">
        <h3 style="margin:0 0 0.75rem;font-size:0.9375rem;color:#333">Misurazioni <span style="font-weight:400;color:#999;font-size:0.8125rem">({{ filteredReadings.length }} totali)</span></h3>
        <div style="overflow-x:auto">
          <table style="width:100%;border-collapse:collapse;font-size:0.8125rem">
            <thead><tr style="background:#F8F9F7;border-bottom:2px solid #E0E0E0">
              <th style="padding:8px;text-align:left;font-size:0.6875rem;color:#888">Data</th><th style="padding:8px;text-align:right;font-size:0.6875rem;color:#888">SYS</th><th style="padding:8px;text-align:right;font-size:0.6875rem;color:#888">DIA</th><th style="padding:8px;text-align:right;font-size:0.6875rem;color:#888">BPM</th><th style="padding:8px;text-align:left;font-size:0.6875rem;color:#888">Categoria</th>
            </tr></thead>
            <tbody>
              <tr v-for="(r, i) in filteredReadings" :key="i" style="border-bottom:1px solid #F0F0F0" :style="{ borderLeft: '3px solid ' + getChartColors().categoryMap[r.category || classifyReading(r.systolic, r.diastolic)] }">
                <td style="padding:6px 8px">{{ new Date(r.timestamp).toLocaleDateString('it-IT') }} <span style="color:#999;font-size:0.6875rem">{{ new Date(r.timestamp).toLocaleTimeString('it-IT', { hour:'2-digit', minute:'2-digit' }) }}</span></td>
                <td style="padding:6px 8px;text-align:right;font-weight:600" :style="{ color: r.systolic >= 140 ? '#D32F2F' : r.systolic >= 130 ? '#EF6C00' : '#333' }">{{ r.systolic }}</td>
                <td style="padding:6px 8px;text-align:right;font-weight:600" :style="{ color: r.diastolic >= 90 ? '#D32F2F' : r.diastolic >= 85 ? '#EF6C00' : '#333' }">{{ r.diastolic }}</td>
                <td style="padding:6px 8px;text-align:right">{{ r.heartRate }}</td>
                <td style="padding:6px 8px;font-size:0.75rem">{{ getCategoryLabel(r.category || classifyReading(r.systolic, r.diastolic)) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <p style="color:#999;font-size:0.75rem;margin:2rem 0;text-align:center">Report generato da Pressione App — Autodistruzione 48 ore<br>Riferimenti: linee guida ESC/ESH 2024</p>
    </template>

    <template v-else-if="report">
      <div style="text-align:center;padding:4rem;color:#666"><p>Nessuna misurazione disponibile.</p></div>
    </template>
  </div>
</template>

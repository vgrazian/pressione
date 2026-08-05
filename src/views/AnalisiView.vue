<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, computeMorningSurge, computeHypertensiveLoad, computeDerivatives, computeHRV } from '@/services/statistics.js'
import { getCategoryLabel, classifyReading } from '@/services/categories.js'
import { getUserBands, getDefaultBands, getBandForHour, groupReadingsByDayAndBand } from '@/services/timeBands.js'
import { generatePDF as generatePDFReport, generatePDFBlob } from '@/services/pdfReport.js'
import { supabase } from '@/services/supabaseClient.js'
import { getChartColors } from '@/services/chartColors.js'
import { useTheme } from '@/services/theme.js'
import { Chart, registerables } from 'chart.js'
import annotationPlugin from 'chartjs-plugin-annotation'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import AppIcon from '@/components/AppIcon.vue'

Chart.register(...registerables, annotationPlugin)

const router = useRouter()
const { user } = useAuth()
const { theme } = useTheme()
const readings = ref([])
const isLoading = ref(true)
const dateRange = ref('30')
const customFrom = ref('')
const customTo = ref('')
const chartTab = ref('bp')
const viewMode = ref('list')
const userBands = ref(getDefaultBands())

const includeCharts = ref(true)
const includeHistory = ref(true)
const anonymize = ref(false)
const anonymizeReportLink = ref(false)
const shareLink = ref(null)
const sharePin = ref('')
const showPin = ref('')
const linkMessage = ref('')
const activeLinks = ref([])
const generatingAction = ref(null)

const bpChartEl = ref(null)
const derivChartEl = ref(null)
const pieChartEl = ref(null)
let bpChart = null, derivChart = null, pieChart = null

const periods = [
  { value: '7', label: '7 Giorni' },
  { value: '30', label: '30 Giorni' },
  { value: 'custom', label: 'Personalizzato' }
]

const chartTabs = [
  { key: 'bp', label: 'Andamento', icon: 'chart' },
  { key: 'deriv', label: 'Variazioni', icon: 'trending-up' },
  { key: 'dist', label: 'Distribuzione', icon: 'pie-chart' }
]

function fmtDateShort(ts) {
  const d = new Date(ts)
  return d.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' })
}

onMounted(async () => {
  isLoading.value = true
  shareLink.value = null; showPin.value = ''; linkMessage.value = ''
  try {
    await refreshFromServer(user.value.username)
    readings.value = await getReadings(user.value.username)
    userBands.value = await getUserBands(user.value.username)
    await loadActiveLinks()
  } finally {
    isLoading.value = false
    await nextTick()
    renderActiveChart()
  }
})

const filteredReadings = computed(() => {
  let result = [...readings.value]
  if (dateRange.value === '7') {
    const cutoff = new Date(Date.now() - 7 * 86400000)
    result = result.filter(r => new Date(r.timestamp) >= cutoff)
  } else if (dateRange.value === '30') {
    const cutoff = new Date(Date.now() - 30 * 86400000)
    result = result.filter(r => new Date(r.timestamp) >= cutoff)
  } else if (dateRange.value === 'custom' && customFrom.value && customTo.value) {
    result = result.filter(r => {
      const t = new Date(r.timestamp)
      return t >= new Date(customFrom.value) && t <= new Date(customTo.value + 'T23:59:59')
    })
  }
  result.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
  return result
})

const stats = computed(() => computeStatistics(filteredReadings.value, userBands.value))
const morningSurge = computed(() => computeMorningSurge(filteredReadings.value, userBands.value))
const htnLoad = computed(() => computeHypertensiveLoad(filteredReadings.value))
const derivatives = computed(() => computeDerivatives(filteredReadings.value))
const hrv = computed(() => computeHRV(filteredReadings.value))

const readings7 = computed(() => {
  const cutoff = new Date(Date.now() - 7 * 86400000)
  return readings.value.filter(r => new Date(r.timestamp) >= cutoff)
})
const readings30 = computed(() => {
  const cutoff = new Date(Date.now() - 30 * 86400000)
  return readings.value.filter(r => new Date(r.timestamp) >= cutoff)
})
const stats7 = computed(() => computeStatistics(readings7.value, userBands.value))
const stats30 = computed(() => computeStatistics(readings30.value, userBands.value))
const derivatives7 = computed(() => computeDerivatives(readings7.value))
const derivatives30 = computed(() => computeDerivatives(readings30.value))
const htnLoad7 = computed(() => computeHypertensiveLoad(readings7.value))
const htnLoad30 = computed(() => computeHypertensiveLoad(readings30.value))
const surge7 = computed(() => computeMorningSurge(readings7.value, userBands.value))
const surge30 = computed(() => computeMorningSurge(readings30.value, userBands.value))

const readingsByTimeOfDay = computed(() => {
  const bandMap = {}
  for (const b of userBands.value) {
    bandMap[b.key] = { label: `${b.label} (${String(b.start).padStart(2, '0')}:00-${String(b.end).padStart(2, '0')}:00)`, readings: [], icon: b.icon, key: b.key }
  }
  for (const r of filteredReadings.value) {
    const hour = new Date(r.timestamp).getHours()
    const band = getBandForHour(hour, userBands.value)
    if (bandMap[band.key]) bandMap[band.key].readings.push(r)
  }
  return Object.values(bandMap).map(b => ({
    ...b, count: b.readings.length,
    avgSys: b.readings.length ? Math.round(b.readings.reduce((s, r) => s + r.systolic, 0) / b.readings.length) : null,
    avgDia: b.readings.length ? Math.round(b.readings.reduce((s, r) => s + r.diastolic, 0) / b.readings.length) : null,
    avgHR: b.readings.length ? Math.round(b.readings.reduce((s, r) => s + r.heartRate, 0) / b.readings.length) : null
  }))
})

const groupedReadings = computed(() => groupReadingsByDayAndBand(filteredReadings.value, userBands.value))

const titleSuffix = computed(() => anonymize.value ? '' : ` - ${user.value?.username}`)
const displayName = computed(() => {
  const u = user.value; if (!u) return ''
  const first = (u.firstName || '').trim(); const last = (u.lastName || '').trim()
  return first && last ? `${first} ${last}` : (first || last || u.username)
})

function renderActiveChart() {
  if (chartTab.value === 'bp') renderBPChart()
  else if (chartTab.value === 'deriv') renderDerivChart()
  else renderPieChart()
}

function renderBPChart() {
  if (bpChart) { bpChart.destroy(); bpChart = null }
  if (!bpChartEl.value) return
  const data = filteredReadings.value
  if (!data.length) return
  const C = getChartColors()
  const labels = data.map(r => new Date(r.timestamp).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' }))
  bpChart = new Chart(bpChartEl.value, {
    type: 'line', data: { labels, datasets: [
      { label: 'Sistolica', data: data.map(r => r.systolic), borderColor: C.systolic, backgroundColor: C.systolicBg, borderWidth: 2, pointRadius: 2, pointHoverRadius: 5, tension: 0.35, fill: false },
      { label: 'Diastolica', data: data.map(r => r.diastolic), borderColor: C.diastolic, backgroundColor: C.diastolicBg, borderWidth: 2, pointRadius: 2, pointHoverRadius: 5, tension: 0.35, fill: false },
      { label: 'BPM', data: data.map(r => r.heartRate), borderColor: C.bpm, borderWidth: 1, pointRadius: 1, borderDash: [4, 3], tension: 0.35, fill: false, yAxisID: 'y1' }
    ] },
    options: {
      responsive: true, maintainAspectRatio: false, interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16, font: { size: 11 }, color: C.textSecondary } },
        annotation: { annotations: {
          goalZone: { type: 'box', yMin: 90, yMax: 140, backgroundColor: C.targetZoneBg, borderColor: C.targetZoneBorder, borderWidth: 1, borderDash: [6, 3], label: { display: true, content: 'Target <140/90', position: 'start', font: { size: 9 }, backgroundColor: C.targetLabelBg, color: C.targetLabelText } },
          sys140: { type: 'line', yMin: 140, yMax: 140, borderColor: C.sys140Line, borderWidth: 1, borderDash: [5, 5] }
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
  const C = getChartColors()
  const labels = d.timestamps.map(t => new Date(t).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' }))
  derivChart = new Chart(derivChartEl.value, {
    type: 'bar', data: { labels, datasets: [
      { label: 'dS/dt', data: d.systolic, backgroundColor: d.systolic.map(v => Math.abs(v) > 10 ? C.derivAlarm : v > 0 ? C.derivPositive : C.derivNegative), borderWidth: 0, borderRadius: 2 }
    ] },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => `Variazione: ${ctx.raw} mmHg/ora` } } },
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
  const C = getChartColors()
  const cats = { 'Normale': 0, 'Elevata': 0, 'Stadio 1': 0, 'Stadio 2+': 0 }
  data.forEach(r => {
    if (r.systolic < 120 && r.diastolic < 80) cats['Normale']++
    else if (r.systolic < 130 && r.diastolic < 80) cats['Elevata']++
    else if (r.systolic < 140 && r.diastolic < 90) cats['Stadio 1']++
    else if (r.systolic < 90 || r.diastolic < 60) cats['Normale']++
    else cats['Stadio 2+']++
  })
  pieChart = new Chart(pieChartEl.value, {
    type: 'doughnut', data: { labels: Object.keys(cats), datasets: [{ data: Object.values(cats), backgroundColor: [C.catNormal, C.catElevated, C.catStage1, C.catStage2], borderWidth: 0 }] },
    options: {
      responsive: true, maintainAspectRatio: false,
      onClick: (_event, elements) => { if (elements.length > 0) router.push({ name: 'readingList', query: { category: Object.keys(cats)[elements[0].index] } }) },
      plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, padding: 12, font: { size: 10 } } } }
    }
  })
}

watch(chartTab, () => { nextTick(() => renderActiveChart()) })
watch(filteredReadings, () => { nextTick(() => renderActiveChart()) }, { flush: 'post' })
watch(userBands, () => { nextTick(() => renderActiveChart()) }, { flush: 'post' })
watch(theme, () => { nextTick(() => renderActiveChart()) })

async function generatePDF() {
  generatingAction.value = 'pdf'
  try {
    await generatePDFReport({
      data: filteredReadings.value, readings7: readings7.value, readings30: readings30.value,
      username: user.value?.username, displayName: displayName.value,
      birthDate: user.value?.birthDate || null, gender: user.value?.gender || null,
      anonymize: anonymize.value, includeCharts: includeCharts.value, includeHistory: includeHistory.value
    })
  } catch (e) { linkMessage.value = 'Errore nella generazione PDF: ' + e.message }
  finally { generatingAction.value = null }
}

async function getPDFFile() {
  return await generatePDFBlob({
    data: filteredReadings.value, readings7: readings7.value, readings30: readings30.value,
    username: user.value?.username, birthDate: user.value?.birthDate || null,
    gender: user.value?.gender || null, anonymize: anonymize.value,
    includeCharts: includeCharts.value, includeHistory: includeHistory.value
  })
}

async function loadActiveLinks() {
  try {
    const { data } = await supabase.from('settings').select('key, value').eq('username', user.value.username).like('key', '_share_%')
    if (data) {
      activeLinks.value = data.filter(s => {
        try { const v = JSON.parse(s.value); return !v.revoked && new Date(v.expiresAt) > new Date() } catch { return false }
      }).map(s => ({ token: s.key.replace('_share_', ''), ...JSON.parse(s.value) }))
    }
  } catch { activeLinks.value = [] }
}

async function hashPin(pin) {
  const encoder = new TextEncoder(); const data = encoder.encode(pin)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('')
}

async function generateShareLink() {
  linkMessage.value = ''
  const readingsData = filteredReadings.value
  if (!readingsData.length) { linkMessage.value = 'Nessun dato da condividere'; return }
  try {
    const reportData = {
      stats: stats.value, readings: readingsData.slice(0, 100).map(r => ({
        systolic: r.systolic, diastolic: r.diastolic, heartRate: r.heartRate,
        timestamp: r.timestamp, notes: r.notes, category: r.category
      })), anonymize: anonymizeReportLink.value,
      displayName: anonymizeReportLink.value ? null : displayName.value,
      birthDate: anonymizeReportLink.value ? null : (user.value?.birthDate || null),
      gender: anonymizeReportLink.value ? null : (user.value?.gender || null)
    }
    const token = Array.from(crypto.getRandomValues(new Uint8Array(16))).map(b => b.toString(16).padStart(2, '0')).join('')
    let pinHash = null, pinClear = ''
    if (sharePin.value) {
      pinClear = String(Math.floor(1000 + Math.random() * 9000))
      pinHash = await hashPin(pinClear)
    }
    const expiresAt = new Date(Date.now() + 48 * 3600000).toISOString()
    const { error } = await supabase.from('settings').upsert({
      username: user.value.username, key: '_share_' + token,
      value: JSON.stringify({ reportData, pinHash, expiresAt, revoked: false }),
      updated_at: new Date().toISOString()
    })
    if (error) throw error
    shareLink.value = `https://vgrazian.github.io/pressione/#/share/${token}`
    showPin.value = pinClear
    const expiryStr = new Date(expiresAt).toLocaleString('it-IT')
    linkMessage.value = pinClear ? `PIN: ${pinClear} (comunicalo al medico). Scade il ${expiryStr}` : `Link generato! Scade il ${expiryStr}`
    await loadActiveLinks()
  } catch (e) { linkMessage.value = 'Errore: ' + e.message }
}

function copyLink() {
  if (shareLink.value) {
    const text = showPin.value ? `${shareLink.value}\nPIN: ${showPin.value}` : shareLink.value
    navigator.clipboard.writeText(text); linkMessage.value = 'Copiato!'
    setTimeout(() => linkMessage.value = '', 3000)
  }
}

async function revokeLink(token) {
  try {
    const key = '_share_' + token
    const { data } = await supabase.from('settings').select('value').eq('username', user.value.username).eq('key', key).single()
    if (data) { const v = JSON.parse(data.value); v.revoked = true; await supabase.from('settings').upsert({ username: user.value.username, key, value: JSON.stringify(v), updated_at: new Date().toISOString() }) }
    activeLinks.value = activeLinks.value.filter(l => l.token !== token)
    shareLink.value = null; showPin.value = ''; linkMessage.value = 'Link revocato.'
    setTimeout(() => linkMessage.value = '', 3000)
  } catch (e) { linkMessage.value = 'Errore: ' + e.message }
}

function copyActiveLink(token) {
  const url = `https://vgrazian.github.io/pressione/#/share/${token}`
  navigator.clipboard.writeText(url); linkMessage.value = 'Link copiato!'
  setTimeout(() => linkMessage.value = '', 2000)
}

function applyCustomRange() { if (customFrom.value && customTo.value) {} }
</script>

<template>
  <div class="page">
    <div class="page-header"><h1>Analisi</h1></div>

    <div class="card mb-md">
      <div class="flex gap-sm mb-sm flex-wrap items-center">
        <button v-for="p in periods" :key="p.value" class="chip" :class="{ 'chip--active': dateRange === p.value }"
          @click="dateRange = p.value">{{ p.label }}</button>
        <template v-if="dateRange === 'custom'">
          <input type="date" v-model="customFrom" class="form-input" style="width:140px" @change="applyCustomRange" />
          <span class="text-secondary">—</span>
          <input type="date" v-model="customTo" class="form-input" style="width:140px" @change="applyCustomRange" />
        </template>
      </div>
      <label class="flex items-center gap-sm mb-sm" style="cursor:pointer;font-size:0.8125rem">
        <input type="checkbox" v-model="includeCharts" /> Includi grafici nel PDF
      </label>
      <label class="flex items-center gap-sm" style="cursor:pointer;font-size:0.8125rem">
        <input type="checkbox" v-model="anonymize" /> Anonimizza report
      </label>
    </div>

    <div v-if="isLoading" class="p-lg"><SkeletonLoader type="text" :count="8" /></div>

    <div v-else-if="!filteredReadings.length" class="empty-state">
      <AppIcon name="chart" :size="40" color="var(--color-text-tertiary)" />
      <h3>Nessun dato nel periodo</h3>
      <p>Aggiungi misurazioni per visualizzare le analisi.</p>
    </div>

    <template v-else>
      <div class="stats-grid mb-md">
        <div class="stat-card card card--flat">
          <span class="stat-card__label">SYS/DIA media</span>
          <span class="stat-card__value">{{ stats.avgSystolic }}/{{ stats.avgDiastolic }}</span>
          <span class="stat-card__unit">mmHg</span>
        </div>
        <div class="stat-card card card--flat">
          <span class="stat-card__label">BPM medio</span>
          <span class="stat-card__value">{{ stats.avgHeartRate }}</span>
          <span class="stat-card__unit">BPM</span>
        </div>
        <div class="stat-card card card--flat">
          <span class="stat-card__label">Misurazioni</span>
          <span class="stat-card__value">{{ stats.readingsCount }}</span>
        </div>
        <div class="stat-card card card--flat">
          <span class="stat-card__label">Carico Ipert.</span>
          <span class="stat-card__value" :class="{ 'text-error': htnLoad.percentage > 30 }">{{ htnLoad.percentage }}%</span>
        </div>
      </div>

      <div v-if="derivatives.alarmSegments.length > 0" class="alert-box mb-md">
        ⚠ {{ derivatives.alarmSegments.length }} episodi di variazione rapida (>10 mmHg/h) rilevati
      </div>

      <div v-if="filteredReadings.length >= 2" class="card mb-md">
        <div class="chart-tabs mb-sm">
          <button v-for="t in chartTabs" :key="t.key" class="chart-tab"
            :class="{ 'chart-tab--active': chartTab === t.key }"
            @click="chartTab = t.key">
            <AppIcon :name="t.icon" :size="14" />
            <span>{{ t.label }}</span>
          </button>
        </div>
        <div class="chart-wrap">
          <canvas v-show="chartTab === 'bp'" ref="bpChartEl"></canvas>
          <canvas v-show="chartTab === 'deriv'" ref="derivChartEl"></canvas>
          <canvas v-show="chartTab === 'dist'" ref="pieChartEl"></canvas>
        </div>
        <p class="text-secondary mt-sm" style="font-size:0.6875rem">Zona verde: target ESC/ESH (&lt;140/90 mmHg).</p>
      </div>

      <div class="card mb-md">
        <h3 class="mb-sm">Confronto 7 / 30 giorni</h3>
        <div class="comparison-table">
          <table>
            <thead><tr><th></th><th>7 giorni</th><th>30 giorni</th></tr></thead>
            <tbody>
              <tr><td>Letture</td><td>{{ stats7.readingsCount }}</td><td>{{ stats30.readingsCount }}</td></tr>
              <tr><td>SYS/DIA media</td><td>{{ stats7.avgSystolic }}/{{ stats7.avgDiastolic }}</td><td>{{ stats30.avgSystolic }}/{{ stats30.avgDiastolic }}</td></tr>
              <tr><td>BPM medio</td><td>{{ stats7.avgHeartRate }}</td><td>{{ stats30.avgHeartRate }}</td></tr>
              <tr><td>Variazione max ↑</td><td class="text-error">{{ derivatives7.maxPositiveRate > 0 ? '+' + Math.round(derivatives7.maxPositiveRate) : '0' }} mmHg/h</td><td class="text-error">{{ derivatives30.maxPositiveRate > 0 ? '+' + Math.round(derivatives30.maxPositiveRate) : '0' }} mmHg/h</td></tr>
              <tr><td>Variazione max ↓</td><td class="text-warning">{{ derivatives7.maxNegativeRate < 0 ? Math.round(derivatives7.maxNegativeRate) : '0' }} mmHg/h</td><td class="text-warning">{{ derivatives30.maxNegativeRate < 0 ? Math.round(derivatives30.maxNegativeRate) : '0' }} mmHg/h</td></tr>
              <tr><td>Allarmi dP/dt</td><td :class="{ 'text-error': derivatives7.alarmSegments.length > 0 }">{{ derivatives7.alarmSegments.length }}</td><td :class="{ 'text-error': derivatives30.alarmSegments.length > 0 }">{{ derivatives30.alarmSegments.length }}</td></tr>
              <tr><td>Carico ipertensivo</td><td :class="{ 'text-error': htnLoad7.percentage > 30 }">{{ htnLoad7.percentage }}%</td><td :class="{ 'text-error': htnLoad30.percentage > 30 }">{{ htnLoad30.percentage }}%</td></tr>
              <tr><td>Picco mattutino</td><td>{{ surge7.delta !== null ? (surge7.delta > 0 ? '+' : '') + surge7.delta + ' mmHg' : 'N/D' }}</td><td>{{ surge30.delta !== null ? (surge30.delta > 0 ? '+' : '') + surge30.delta + ' mmHg' : 'N/D' }}</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card mb-md">
        <h3 class="mb-sm">Per fascia oraria</h3>
        <div class="timeofday-grid">
          <div v-for="band in readingsByTimeOfDay" :key="band.key" class="tod-card" :class="{ 'tod-card--empty': band.count === 0 }">
            <span class="tod-icon">{{ band.icon }}</span>
            <span class="tod-label">{{ band.label }}</span>
            <span v-if="band.count > 0" class="tod-avg">{{ band.avgSys }}/{{ band.avgDia }} mmHg</span>
            <span v-if="band.count > 0" class="tod-bpm">{{ band.avgHR }} BPM</span>
            <span class="tod-count">{{ band.count }} letture</span>
          </div>
        </div>
        <div v-if="morningSurge.delta !== null" class="mt-md" style="font-size:0.8125rem">
          <span :class="morningSurge.alert ? 'text-error' : 'text-secondary'">
            {{ morningSurge.alert ? '⚠' : 'ℹ' }} Picco mattutino: {{ morningSurge.delta > 0 ? '+' : '' }}{{ morningSurge.delta }} mmHg
          </span>
        </div>
      </div>

      <div v-if="includeHistory" class="card mb-md">
        <div class="flex justify-between items-center mb-sm">
          <h3>Storico</h3>
          <div class="flex gap-sm">
            <button class="chip" :class="{ 'chip--active': viewMode === 'list' }" @click="viewMode = 'list'">Lista</button>
            <button class="chip" :class="{ 'chip--active': viewMode === 'grouped' }" @click="viewMode = 'grouped'">Per fascia</button>
          </div>
        </div>
        <div v-if="viewMode === 'list'" class="table-scroll">
          <table class="preview-table">
            <thead><tr><th>Data</th><th>Ora</th><th>SYS</th><th>DIA</th><th>BPM</th><th class="col-category">Categoria</th></tr></thead>
            <tbody>
              <tr v-for="r in filteredReadings.slice(0, 30)" :key="r.id" :style="{ borderLeft: '3px solid ' + getChartColors().categoryMap[r.category] }">
                <td class="col-date">{{ fmtDateShort(r.timestamp) }}</td>
                <td>{{ new Date(r.timestamp).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }) }}</td>
                <td :class="{ 'text-error': r.systolic >= 140, 'text-warning': r.systolic >= 130 && r.systolic < 140 }">{{ r.systolic }}</td>
                <td :class="{ 'text-error': r.diastolic >= 90, 'text-warning': r.diastolic >= 85 && r.diastolic < 90 }">{{ r.diastolic }}</td>
                <td>{{ r.heartRate }}</td>
                <td class="col-category"><small>{{ getCategoryLabel(r.category) }}</small></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="viewMode === 'list' && filteredReadings.length > 30" class="text-secondary mt-sm" style="font-size:0.75rem">
          ...e altre {{ filteredReadings.length - 30 }} misurazioni (scarica il PDF per lo storico completo)
        </p>
        <div v-if="viewMode === 'grouped'" class="banded-table-wrapper">
          <table class="banded-table" v-for="day of groupedReadings.slice(0, 14)" :key="day.date">
            <thead><tr><th colspan="6" class="banded-date">{{ day.date }}</th></tr></thead>
            <tbody>
              <template v-for="band of userBands" :key="band.key">
                <template v-if="day.bands[band.key] && day.bands[band.key].length">
                  <tr class="band-label-row"><td colspan="6"><span class="band-label">{{ band.icon }} {{ band.label }} ({{ day.bands[band.key].length }} letture)</span></td></tr>
                  <tr v-for="r in day.bands[band.key]" :key="r.id" :style="{ borderLeft: '3px solid ' + getChartColors().categoryMap[r.category] }">
                    <td></td>
                    <td>{{ new Date(r.timestamp).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }) }}</td>
                    <td :class="{ 'text-error': r.systolic >= 140, 'text-warning': r.systolic >= 130 && r.systolic < 140 }">{{ r.systolic }}</td>
                    <td :class="{ 'text-error': r.diastolic >= 90, 'text-warning': r.diastolic >= 85 && r.diastolic < 90 }">{{ r.diastolic }}</td>
                    <td>{{ r.heartRate }}</td>
                    <td><small>{{ getCategoryLabel(r.category) }}</small></td>
                  </tr>
                </template>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card mb-md">
        <h3 class="mb-sm">Scarica / Condividi</h3>
        <button class="btn btn-primary" @click="generatePDF" :disabled="generatingAction !== null">
          <AppIcon name="copy" :size="16" /> {{ generatingAction === 'pdf' ? 'Generazione...' : 'Scarica PDF' }}
        </button>
      </div>

      <div class="card mb-md">
        <h3 class="mb-sm">Link Temporaneo (48h)</h3>
        <p class="text-secondary mb-sm" style="font-size:0.8125rem">Genera un link web per il medico. Con PIN opzionale per maggiore privacy.</p>
        <div class="flex gap-sm mb-sm items-center flex-wrap">
          <label class="flex items-center gap-sm" style="font-size:0.8125rem;cursor:pointer">
            <input type="checkbox" v-model="sharePin" /> Proteggi con PIN
          </label>
          <label class="flex items-center gap-sm" style="font-size:0.8125rem;cursor:pointer">
            <input type="checkbox" v-model="anonymizeReportLink" /> Anonimizza dati
          </label>
          <button class="btn btn-primary btn-sm" @click="generateShareLink">Genera Link</button>
        </div>
        <div v-if="shareLink" class="share-link-box mb-sm">
          <code>{{ shareLink }}</code>
          <button class="btn btn-sm btn-ghost" @click="copyLink">Copia</button>
        </div>
        <div v-if="linkMessage" class="form-success mb-sm">{{ linkMessage }}</div>
        <div v-if="activeLinks.length > 0" class="mt-md">
          <h4 class="mb-sm" style="font-size:0.875rem">Link Attivi</h4>
          <div v-for="link in activeLinks" :key="link.token" class="active-link-row">
            <code style="font-size:0.6875rem">{{ link.token.slice(0, 12) }}...</code>
            <span style="font-size:0.6875rem;color:var(--color-text-tertiary)">Scade {{ new Date(link.expiresAt).toLocaleString('it-IT') }}</span>
            <div class="flex gap-sm">
              <button class="btn btn-sm btn-ghost" @click="copyActiveLink(link.token)" style="font-size:0.6875rem;padding:2px 8px;min-height:24px">Copia</button>
              <button class="btn btn-sm btn-error" @click="revokeLink(link.token)" style="font-size:0.6875rem;padding:2px 8px;min-height:24px">Revoca</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.chart-tabs { display: flex; gap: 4px; background: var(--color-surface-overlay); border-radius: var(--radius-sm); padding: 3px; }
.chart-tab { display: flex; align-items: center; gap: 4px; padding: 6px 14px; border: none; background: transparent; border-radius: 6px; font-size: 0.75rem; font-family: var(--font-sans); color: var(--color-text-secondary); cursor: pointer; transition: all 0.15s; white-space: nowrap; }
.chart-tab--active { background: var(--color-surface-raised); color: var(--color-accent); font-weight: 600; box-shadow: var(--shadow-sm); }
.chart-wrap { position: relative; height: 260px; width: 100%; }
@media (max-width: 480px) { .chart-wrap { height: 200px; } }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm); }
@media (max-width: 480px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-card { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 2px; padding: var(--space-sm) var(--space-md); }
.stat-card__label { font-size: 0.625rem; color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.03em; }
.stat-card__value { font-size: 1.25rem; font-weight: 700; font-variant-numeric: tabular-nums; }
.stat-card__unit { font-size: 0.625rem; color: var(--color-text-tertiary); }

.alert-box { background: var(--color-error-muted); padding: var(--space-sm) var(--space-md); border-radius: var(--radius-sm); color: var(--color-error); font-size: 0.8125rem; line-height: 1.4; }

.comparison-table table { width: 100%; border-collapse: collapse; font-size: 0.8125rem; }
.comparison-table th { text-align: left; padding: 4px 8px; background: var(--color-surface-overlay); color: var(--color-text-secondary); font-weight: 600; font-size: 0.75rem; }
.comparison-table td { padding: 5px 8px; border-bottom: 1px solid var(--color-border); font-variant-numeric: tabular-nums; }
.comparison-table td:first-child { color: var(--color-text-secondary); }
@media (max-width: 480px) { .comparison-table table { font-size: 0.75rem; } }

.timeofday-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm); }
@media (max-width: 480px) { .timeofday-grid { grid-template-columns: repeat(2, 1fr); } }
.tod-card { background: var(--color-surface-overlay); border-radius: var(--radius-sm); padding: var(--space-sm); text-align: center; display: flex; flex-direction: column; align-items: center; gap: 2px; }
.tod-card--empty { opacity: 0.4; }
.tod-icon { font-size: 1.25rem; }
.tod-label { font-size: 0.625rem; color: var(--color-text-tertiary); }
.tod-avg { font-size: 0.875rem; font-weight: 600; }
.tod-bpm { font-size: 0.6875rem; color: var(--color-text-secondary); }
.tod-count { font-size: 0.625rem; color: var(--color-text-tertiary); }

.table-scroll { width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch; }
.preview-table { width: 100%; border-collapse: collapse; font-size: 0.75rem; white-space: nowrap; }
.preview-table th, .preview-table td { padding: 5px 8px; text-align: left; border-bottom: 1px solid var(--color-border); }
.preview-table th { color: var(--color-text-secondary); font-weight: 600; font-size: 0.6875rem; }
@media (max-width: 400px) { .col-category { display: none; } }

.share-link-box { display: flex; align-items: center; gap: var(--space-sm); background: var(--color-surface-overlay); padding: var(--space-sm) var(--space-md); border-radius: var(--radius-sm); }
.share-link-box code { font-size: 0.75rem; word-break: break-all; flex: 1; }
.active-link-row { display: flex; align-items: center; gap: var(--space-sm); padding: 4px 0; flex-wrap: wrap; }
.active-link-row code { flex: 1; min-width: 100px; }

.banded-table-wrapper { max-height: 60vh; overflow-y: auto; }
.banded-table { width: 100%; border-collapse: collapse; font-size: 0.75rem; margin-bottom: var(--space-md); }
.banded-date { text-align: left; padding: 6px 8px; background: var(--color-accent-muted); color: var(--color-accent); font-weight: 600; font-size: 0.8125rem; border-radius: var(--radius-sm) var(--radius-sm) 0 0; }
.band-label-row td { padding: 3px 8px; background: var(--color-surface-raised); border-bottom: 1px solid var(--color-border); }
.band-label { font-size: 0.6875rem; color: var(--color-text-secondary); font-weight: 500; }
.banded-table tbody tr td { padding: 4px 8px; border-bottom: 1px solid var(--color-border); font-size: 0.75rem; }

.text-error { color: var(--color-error); }
.text-warning { color: #EF6C00; }
</style>

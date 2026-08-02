<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, computeDerivatives, computeMorningSurge, computeHypertensiveLoad, computeHRV } from '@/services/statistics.js'
import { getCategoryLabel } from '@/services/categories.js'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import { Chart, registerables } from 'chart.js'
import annotationPlugin from 'chartjs-plugin-annotation'

Chart.register(...registerables, annotationPlugin)

const router = useRouter()
const { user } = useAuth()

const readings = ref([])
const isLoading = ref(true)
const dateRange = ref('30')
const customFrom = ref('')
const customTo = ref('')
const bpChartEl = ref(null)
const derivChartEl = ref(null)
const pieChartEl = ref(null)
let bpChart = null
let derivChart = null
let pieChart = null

const periods = [
  { value: '7', label: '7 Giorni' },
  { value: '30', label: '30 Giorni' },
  { value: 'custom', label: 'Personalizzato' }
]

onMounted(async () => {
  await loadData()
})

watch(dateRange, () => { if (dateRange.value !== 'custom') renderCharts() })

async function loadData() {
  isLoading.value = true
  try {
    await refreshFromServer(user.value.username)
    readings.value = await getReadings(user.value.username)
    await nextTick()
    renderCharts()
  } finally { isLoading.value = false }
}

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
  result.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
  return result
})

const stats = computed(() => computeStatistics(filteredReadings.value))
const derivatives = computed(() => computeDerivatives(filteredReadings.value))
const morningSurge = computed(() => computeMorningSurge(filteredReadings.value))
const htnLoad = computed(() => computeHypertensiveLoad(filteredReadings.value))
const hrv = computed(() => computeHRV(filteredReadings.value))

function applyCustomRange() {
  if (customFrom.value && customTo.value) renderCharts()
}

function renderCharts() {
  setTimeout(() => {
    renderBPChart()
    renderDerivChart()
    renderPieChart()
  }, 100)
}

// --- Main BP Chart ---
function renderBPChart() {
  if (bpChart) { bpChart.destroy(); bpChart = null }
  if (!bpChartEl.value) return
  const data = filteredReadings.value
  if (!data.length) return

  const labels = data.map(r => {
    const d = new Date(r.timestamp)
    return d.toLocaleDateString('it-IT', { day: 'numeric', month: 'short' })
  })

  bpChart = new Chart(bpChartEl.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'Sistolica',
          data: data.map(r => r.systolic),
          borderColor: '#E63946',
          backgroundColor: 'rgba(230,57,70,0.1)',
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
          tension: 0.1,
          fill: false
        },
        {
          label: 'Diastolica',
          data: data.map(r => r.diastolic),
          borderColor: '#457B9D',
          backgroundColor: 'rgba(69,123,157,0.1)',
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
          tension: 0.1,
          fill: false
        },
        {
          label: 'BPM',
          data: data.map(r => r.heartRate),
          borderColor: '#6C757D',
          borderWidth: 1,
          pointRadius: 1,
          borderDash: [3, 3],
          tension: 0.1,
          fill: false,
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16, font: { size: 11 } } },
        annotation: {
          annotations: {
            sysSafe: { type: 'box', yMin: 90, yMax: 140, backgroundColor: 'rgba(0,108,76,0.04)', borderColor: 'transparent' },
            diaSafe: { type: 'box', yMin: 60, yMax: 90, backgroundColor: 'rgba(0,108,76,0.06)', borderColor: 'transparent' }
          }
        },
        tooltip: {
          callbacks: {
            title: (ctx) => data[ctx[0].dataIndex] ? new Date(data[ctx[0].dataIndex].timestamp).toLocaleString('it-IT') : '',
            label: (ctx) => {
              const r = data[ctx.dataIndex]
              if (!r) return ''
              if (ctx.datasetIndex === 0) return `Sistolica: ${r.systolic} mmHg`
              if (ctx.datasetIndex === 1) return `Diastolica: ${r.diastolic} mmHg`
              return `BPM: ${r.heartRate}`
            }
          }
        }
      },
      scales: {
        x: { ticks: { maxTicksLimit: 12, font: { size: 10 } }, grid: { display: false } },
        y: {
          type: 'linear',
          position: 'left',
          min: 40, max: 180,
          ticks: { stepSize: 20, font: { size: 10 } },
          title: { display: true, text: 'mmHg', font: { size: 10 } }
        },
        y1: {
          type: 'linear',
          position: 'right',
          min: 40, max: 120,
          ticks: { stepSize: 20, font: { size: 10 } },
          title: { display: true, text: 'BPM', font: { size: 10 } },
          grid: { drawOnChartArea: false }
        }
      }
    }
  })
}

// --- Derivative Chart ---
function renderDerivChart() {
  if (derivChart) { derivChart.destroy(); derivChart = null }
  if (!derivChartEl.value) return
  const d = derivatives.value
  if (!d.timestamps.length) return

  const labels = d.timestamps.map(t => new Date(t).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' }))

  derivChart = new Chart(derivChartEl.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [
        {
          label: 'dS/dt',
          data: d.systolic,
          backgroundColor: d.systolic.map(v => Math.abs(v) > 10 ? '#D90429' : v > 0 ? '#E6394680' : '#457B9D80'),
          borderWidth: 0,
          borderRadius: 2
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => `Variazione: ${ctx.raw} mmHg/ora`
          }
        }
      },
      scales: {
        x: { ticks: { maxTicksLimit: 12, font: { size: 10 } }, grid: { display: false } },
        y: {
          min: -Math.max(d.maxRate + 5, 20),
          max: Math.max(d.maxRate + 5, 20),
          ticks: { font: { size: 10 } },
          title: { display: true, text: 'mmHg/ora', font: { size: 10 } }
        }
      }
    }
  })
}

// --- Pie Chart (OMS Distribution) ---
function renderPieChart() {
  if (pieChart) { pieChart.destroy(); pieChart = null }
  if (!pieChartEl.value) return
  const data = filteredReadings.value
  if (!data.length) return

  const cats = { 'Normale': 0, 'Elevata': 0, 'Stadio 1': 0, 'Stadio 2+': 0 }
  data.forEach(r => {
    if (r.systolic < 120 && r.diastolic < 80) cats['Normale']++
    else if (r.systolic < 130 && r.diastolic < 80) cats['Elevata']++
    else if (r.systolic < 140 && r.diastolic < 90) cats['Stadio 1']++
    else if (r.systolic < 90 || r.diastolic < 60) cats['Normale']++
    else cats['Stadio 2+']++
  })

  pieChart = new Chart(pieChartEl.value, {
    type: 'doughnut',
    data: {
      labels: Object.keys(cats),
      datasets: [{
        data: Object.values(cats),
        backgroundColor: ['#006C4C', '#FFC107', '#FF9800', '#E63946'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      onClick: (_event, elements) => {
        if (elements.length > 0) {
          const idx = elements[0].index
          const label = Object.keys(cats)[idx]
          // Navigate to list filtered by category
          router.push({ name: 'readingList', query: { category: label } })
        }
      },
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 10, padding: 12, font: { size: 10 } } }
      }
    }
  })
}

// Navigation
function goToAdd() { router.push('/add') }
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Statistiche</h1>
    </div>

    <!-- Period Selector -->
    <div class="flex gap-sm mb-md flex-wrap items-center">
      <button v-for="p in periods" :key="p.value" class="chip" :class="{ 'chip--active': dateRange === p.value }"
        @click="dateRange = p.value">{{ p.label }}</button>
      <template v-if="dateRange === 'custom'">
        <input type="date" v-model="customFrom" class="form-input" style="width:140px" @change="applyCustomRange" />
        <span class="text-secondary">—</span>
        <input type="date" v-model="customTo" class="form-input" style="width:140px" @change="applyCustomRange" />
      </template>
    </div>

    <div v-if="isLoading" class="p-lg">
      <SkeletonLoader type="stats" class="mb-md" />
      <SkeletonLoader type="chart" class="mb-md" />
    </div>

    <template v-else-if="filteredReadings.length === 0">
      <div class="empty-state">
        <span style="font-size:2rem;opacity:0.4;">📊</span>
        <h3>Nessun dato nel periodo</h3>
        <p>Seleziona un altro periodo o aggiungi una misurazione.</p>
        <button class="btn btn-primary mt-md" @click="goToAdd">Aggiungi Misurazione</button>
      </div>
    </template>

    <template v-else>
      <!-- KPI Cards 2x2 -->
      <div class="kpi-grid mb-lg">
        <div class="kpi-card card card--flat">
          <span class="kpi-label">Media Sistolica / Diastolica</span>
          <span class="kpi-value">{{ stats.avgSystolic }} / {{ stats.avgDiastolic }} <small>mmHg</small></span>
        </div>
        <div class="kpi-card card card--flat">
          <span class="kpi-label">Morning Surge (06-09 vs 20-23)</span>
          <span class="kpi-value" v-if="morningSurge.delta !== null">
            Δ {{ morningSurge.delta > 0 ? '+' : '' }}{{ morningSurge.delta }}
            <span v-if="morningSurge.alert" class="badge" style="background:var(--color-error-muted);color:var(--color-error)">⚠️ Rischio</span>
          </span>
          <span class="kpi-value text-secondary" v-else>Dati insufficienti</span>
        </div>
        <div class="kpi-card card card--flat">
          <span class="kpi-label">Carico Ipertensivo</span>
          <span class="kpi-value">
            {{ htnLoad.percentage }}%
            <div class="progress-bar"><div class="progress-fill" :style="{ width: htnLoad.percentage + '%' }"></div></div>
          </span>
        </div>
        <div class="kpi-card card card--flat">
          <span class="kpi-label">Variabilità FC (HRV)</span>
          <span class="kpi-value">{{ hrv ?? '—' }}<small v-if="hrv"> σ BPM</small></span>
        </div>
      </div>

      <!-- Main BP Line Chart -->
      <div class="card mb-md">
        <h3 class="mb-sm">Andamento Pressione</h3>
        <div class="chart-wrap"><canvas ref="bpChartEl"></canvas></div>
      </div>

      <!-- Derivative Chart -->
      <div class="card mb-md" v-if="derivatives.timestamps.length > 0">
        <h3 class="mb-sm">Velocità di Variazione (dP/dt)</h3>
        <p class="text-secondary mb-sm" style="font-size:0.75rem">mmHg/ora — barre rosse indicano variazioni &gt;10 mmHg/ora</p>
        <div class="chart-wrap chart-wrap--sm"><canvas ref="derivChartEl"></canvas></div>
        <!-- Alarm list -->
        <div v-if="derivatives.alarmSegments.length" class="mt-sm">
          <div v-for="(seg, i) in derivatives.alarmSegments.slice(0, 3)" :key="i" class="alarm-item">
            ⚠️ {{ new Date(seg.timestamp).toLocaleDateString('it-IT') }} — Variazione {{ seg.rate > 0 ? '+' : '' }}{{ seg.rate }} mmHg/ora ({{ seg.systolic }}/{{ seg.diastolic }})
          </div>
        </div>
      </div>

      <!-- Pie Chart + Distribution -->
      <div class="card mb-md">
        <h3 class="mb-sm">Distribuzione Categorie (OMS)</h3>
        <div class="chart-wrap chart-wrap--sm"><canvas ref="pieChartEl"></canvas></div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.kpi-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-sm); }
.kpi-card { display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center; }
.kpi-label { font-size: 0.6875rem; color: var(--color-text-secondary); text-transform: uppercase; }
.kpi-value { font-size: 1.25rem; font-weight: 700; }
.kpi-value small { font-size: 0.625rem; color: var(--color-text-tertiary); font-weight: 400; }

.chart-wrap { position: relative; height: 260px; width: 100%; }
.chart-wrap--sm { height: 180px; }

.badge { display: inline-block; padding: 1px 6px; border-radius: var(--radius-full); font-size: 0.625rem; margin-left: 4px; }

.progress-bar { width: 100%; height: 6px; background: var(--color-surface-overlay); border-radius: 3px; margin-top: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--color-accent); border-radius: 3px; transition: width 0.3s; max-width: 100%; }

.alarm-item { font-size: 0.75rem; color: var(--color-error); padding: 4px 0; border-bottom: 1px solid var(--color-border); }
.alarm-item:last-child { border-bottom: none; }

@media (max-width: 480px) {
  .chart-wrap { height: 200px; }
  .chart-wrap--sm { height: 150px; }
}
</style>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, linearRegression, movingAverage } from '@/services/statistics.js'
import { getCategoryLabel, ALL_CATEGORIES } from '@/services/categories.js'
import AppIcon from '@/components/AppIcon.vue'
import SkeletonLoader from '@/components/SkeletonLoader.vue'

const { user } = useAuth()

const readings = ref([])
const stats = ref(null)
const isLoading = ref(true)
const dateRange = ref('all')
const trendType = ref('none') // 'none', 'linear', 'movingAverage'

const dateRangeOptions = [
  { value: '7d', label: '7 giorni' },
  { value: '30d', label: '30 giorni' },
  { value: '90d', label: '3 mesi' },
  { value: '180d', label: '6 mesi' },
  { value: '365d', label: '1 anno' },
  { value: 'all', label: 'Tutto' }
]

const trendOptions = [
  { value: 'none', label: 'Nessuno' },
  { value: 'linear', label: 'Lineare' },
  { value: 'movingAverage', label: 'Media mobile' }
]

onMounted(async () => {
  await loadData()
})

async function loadData() {
  isLoading.value = true
  try {
    await refreshFromServer(user.value.username)
    const all = await getReadings(user.value.username)
    readings.value = all
    stats.value = computeStatistics(all)
  } catch (e) {
    console.error('Load error:', e)
  } finally {
    isLoading.value = false
  }
}

const filteredReadings = computed(() => {
  let result = [...readings.value]
  if (dateRange.value !== 'all') {
    const days = parseInt(dateRange.value)
    const cutoff = new Date(Date.now() - days * 24 * 60 * 60 * 1000)
    result = result.filter(r => new Date(r.timestamp) >= cutoff)
  }
  // Sort chronologically for chart
  result.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
  return result
})

const filteredStats = computed(() => computeStatistics(filteredReadings.value))

const chartData = computed(() => {
  return filteredReadings.value.map((r, i) => ({
    x: i,
    ySystolic: r.systolic,
    yDiastolic: r.diastolic,
    yHeartRate: r.heartRate,
    label: new Date(r.timestamp).toLocaleDateString('it-IT', { day: 'numeric', month: 'short' }),
    timestamp: r.timestamp
  }))
})

const trendLine = computed(() => {
  if (chartData.value.length < 2) return null
  const points = chartData.value.map(d => ({ x: d.x, y: d.ySystolic }))
  return linearRegression(points)
})

const maLine = computed(() => {
  if (chartData.value.length < 3) return null
  const values = chartData.value.map(d => d.ySystolic)
  return movingAverage(values, 3)
})

const maxSystolic = computed(() => Math.max(...chartData.value.map(d => d.ySystolic), 0) + 20)
const maxDiastolic = computed(() => Math.max(...chartData.value.map(d => d.yDiastolic), 0) + 20)

// Aggregate data for charts with many points (max ~50 points)
const chartPoints = computed(() => {
  const data = chartData.value
  if (data.length <= 50) return data
  // Aggregate by grouping every N points
  const groupSize = Math.ceil(data.length / 50)
  const result = []
  for (let i = 0; i < data.length; i += groupSize) {
    const slice = data.slice(i, i + groupSize)
    result.push({
      x: i,
      label: slice[0].label,
      ySystolic: Math.round(slice.reduce((s, d) => s + d.ySystolic, 0) / slice.length),
      yDiastolic: Math.round(slice.reduce((s, d) => s + d.yDiastolic, 0) / slice.length)
    })
  }
  return result
})

// SVG polyline points (invert Y since SVG Y goes down)
const sysPoints = computed(() => {
  if (chartPoints.value.length < 1) return ''
  const n = chartPoints.value.length - 1
  return chartPoints.value.map((d, i) => {
    const y = 100 - ((d.ySystolic / maxSystolic.value) * 100)
    return `${(i / Math.max(n, 1)) * 100},${Math.max(2, Math.min(98, y))}`
  }).join(' ')
})

const diaPoints = computed(() => {
  if (chartPoints.value.length < 1) return ''
  const n = chartPoints.value.length - 1
  return chartPoints.value.map((d, i) => {
    const y = 100 - ((d.yDiastolic / maxSystolic.value) * 100)
    return `${(i / Math.max(n, 1)) * 100},${Math.max(2, Math.min(98, y))}`
  }).join(' ')
})

// Trend points in SVG coordinates
const trendPoints = computed(() => {
  if (!trendLine.value || chartPoints.value.length < 2) return ''
  const n = chartPoints.value.length - 1
  return chartPoints.value.map((d, i) => {
    const y = trendLine.value.slope * i + trendLine.value.intercept
    const svgY = 100 - ((y / maxSystolic.value) * 100)
    return `${(i / Math.max(n, 1)) * 100},${Math.max(1, Math.min(99, svgY))}`
  }).join(' ')
})

const maPoints = computed(() => {
  if (!maLine.value || chartPoints.value.length < 3) return ''
  const n = chartPoints.value.length - 1
  return maLine.value.map((v, i) => {
    const svgY = 100 - ((v / maxSystolic.value) * 100)
    return `${(i / Math.max(n, 1)) * 100},${Math.max(1, Math.min(99, svgY))}`
  }).join(' ')
})
const maxHR = computed(() => Math.max(...chartData.value.map(d => d.yHeartRate), 0) + 20)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Statistiche</h1>
    </div>

    <!-- Date Range -->
    <div class="date-range-chips flex gap-sm mb-md" style="flex-wrap: wrap;">
      <button
        v-for="opt in dateRangeOptions"
        :key="opt.value"
        class="chip"
        :class="{ 'chip--active': dateRange === opt.value }"
        @click="dateRange = opt.value"
      >{{ opt.label }}</button>
    </div>

    <div v-if="isLoading" class="p-lg">
      <SkeletonLoader type="stats" class="mb-md" />
      <SkeletonLoader type="chart" class="mb-md" />
      <SkeletonLoader type="card" :count="3" height="40px" />
    </div>

    <template v-else-if="filteredReadings.length > 0">
      <!-- Summary Cards -->
      <div class="stats-grid mb-lg">
        <div class="stat-card card card--flat">
          <span class="stat-card__label">Media Sistolica</span>
          <span class="stat-card__value">{{ filteredStats.avgSystolic }}</span>
        </div>
        <div class="stat-card card card--flat">
          <span class="stat-card__label">Media Diastolica</span>
          <span class="stat-card__value">{{ filteredStats.avgDiastolic }}</span>
        </div>
        <div class="stat-card card card--flat">
          <span class="stat-card__label">Min-Max Sistolica</span>
          <span class="stat-card__value">{{ filteredStats.minSystolic }}-{{ filteredStats.maxSystolic }}</span>
        </div>
        <div class="stat-card card card--flat">
          <span class="stat-card__label">Totale</span>
          <span class="stat-card__value">{{ filteredStats.readingsCount }}</span>
        </div>
      </div>

      <!-- BP Chart -->
      <div class="chart-container card mb-md">
        <div class="flex justify-between items-center mb-sm">
          <h3>Andamento Pressione</h3>
          <div class="flex gap-sm">
            <button v-for="opt in trendOptions" :key="opt.value"
              class="chip" :class="{ 'chip--active': trendType === opt.value }"
              @click="trendType = opt.value">{{ opt.label }}</button>
          </div>
        </div>
        <div class="chart bp-chart">
          <div class="chart-y-axis">
            <span>{{ maxSystolic }}</span>
            <span>{{ Math.round(maxSystolic / 2) }}</span>
            <span>0</span>
          </div>
          <div class="chart-area">
            <!-- SVG Line Chart -->
            <svg class="chart-line-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
              <!-- Grid lines -->
              <line x1="0" y1="50" x2="100" y2="50" stroke="var(--color-border)" stroke-width="0.3" />
              <!-- Diastolic line -->
              <polyline v-if="diaPoints" :points="diaPoints" fill="none" stroke="var(--color-accent)" stroke-width="1.5" opacity="0.7" />
              <!-- Systolic line -->
              <polyline v-if="sysPoints" :points="sysPoints" fill="none" stroke="#BA1A1A" stroke-width="2" />
              <!-- Trend line -->
              <polyline v-if="trendType === 'linear' && trendPoints" :points="trendPoints" fill="none" stroke="var(--color-accent)" stroke-width="1" stroke-dasharray="3,3" opacity="0.6" />
              <polyline v-if="trendType === 'movingAverage' && maPoints" :points="maPoints" fill="none" stroke="var(--color-accent)" stroke-width="1.5" stroke-dasharray="2,2" opacity="0.5" />
            </svg>
            <!-- Hover tooltip area -->
            <div class="chart-dots">
              <div v-for="(p, i) in chartPoints" :key="i" class="chart-dot"
                :style="{ left: (i / Math.max(chartPoints.length - 1, 1)) * 100 + '%', bottom: ((p.ySystolic / maxSystolic) * 100) + '%' }"
                :title="p.label + ': ' + p.ySystolic + '/' + p.yDiastolic">
              </div>
            </div>
          </div>
        </div>
        <div class="chart-legend">
          <span><span class="legend-dot sys"></span> Sistolica</span>
          <span><span class="legend-dot dia"></span> Diastolica</span>
        </div>
      </div>

      <!-- Category Distribution -->
      <div class="card mb-md">
        <h3 class="mb-sm">Distribuzione Categorie</h3>
        <div v-if="filteredStats.categoryDistribution" class="cat-distribution">
          <div
            v-for="(count, cat) in filteredStats.categoryDistribution"
            :key="cat"
            class="cat-row"
          >
            <span class="cat-label">{{ getCategoryLabel(cat) }}</span>
            <div class="cat-bar-container">
              <div class="cat-bar" :style="{ width: (count / filteredStats.readingsCount * 100) + '%' }"></div>
            </div>
            <span class="cat-count">{{ count }}</span>
          </div>
        </div>
      </div>

      <!-- Time of Day Distribution -->
      <div class="card">
        <h3 class="mb-sm">Distribuzione Oraria</h3>
        <div class="tod-grid">
          <div class="tod-item">
            <AppIcon name="sun" :size="24" />
            <span class="tod-label">Mattina</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.MORNING || 0 }}</span>
          </div>
          <div class="tod-item">
            <AppIcon name="sun" :size="24" />
            <span class="tod-label">Pomeriggio</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.AFTERNOON || 0 }}</span>
          </div>
          <div class="tod-item">
            <AppIcon name="sun" :size="24" />
            <span class="tod-label">Sera</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.EVENING || 0 }}</span>
          </div>
          <div class="tod-item">
            <AppIcon name="moon" :size="24" />
            <span class="tod-label">Notte</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.NIGHT || 0 }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <AppIcon name="chart" :size="48" color="var(--color-text-tertiary)" class="empty-state__icon" />
      <h3>Nessun dato</h3>
      <p>Aggiungi misurazioni per vedere le statistiche.</p>
    </div>
  </div>
</template>

<style scoped>
.chip {
  background: var(--color-surface-overlay);
  color: var(--color-text-primary);
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
}

.chip--active {
  background: var(--color-accent-muted);
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-card__label {
  font-size: 0.6875rem;
  color: var(--color-text-secondary);
  text-transform: uppercase;
}

.stat-card__value {
  font-size: 1.5rem;
  font-weight: 700;
}

.chart-container h3 {
  margin-bottom: var(--space-md);
}

.bp-chart {
  display: flex;
  gap: var(--space-sm);
  height: 200px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 0.625rem;
  color: var(--color-text-secondary);
  width: 30px;
  text-align: right;
}

.chart-area {
  flex: 1;
  position: relative;
  border-bottom: 1px solid var(--color-border-strong);
  border-left: 1px solid var(--color-border-strong);
  overflow: hidden;
}

.chart-line-svg {
  position: absolute;
  inset: 5px 0 0 0;
  width: 100%;
  height: calc(100% - 5px);
  pointer-events: none;
}

.chart-dots { position: absolute; inset: 0; }
.chart-dot {
  position: absolute;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #BA1A1A;
  transform: translate(-50%, 50%);
  opacity: 0;
  transition: opacity 0.15s;
  cursor: pointer;
}
.chart-dot:hover { opacity: 1; }

.chart-bar-group {
  position: absolute;
  bottom: 0;
  display: flex;
  gap: 1px;
  transform: translateX(-50%);
}

.chart-bar {
  width: 8px;
  border-radius: 2px 2px 0 0;
  transition: height 0.3s;
}

.sys-bar { background: #BA1A1A; }
.dia-bar { background: #006C4C; }

.chart-legend {
  display: flex;
  gap: var(--space-md);
  margin-top: var(--space-sm);
  font-size: 0.75rem;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}

.legend-dot.sys { background: #BA1A1A; }
.legend-dot.dia { background: #006C4C; }

.cat-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.cat-label {
  width: 140px;
  font-size: 0.8125rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-bar-container {
  flex: 1;
  height: 8px;
  background: var(--color-surface-overlay);
  border-radius: 4px;
  overflow: hidden;
}

.cat-bar {
  height: 100%;
  background: var(--color-accent);
  border-radius: 4px;
}

.cat-count {
  font-size: 0.8125rem;
  font-weight: 600;
  width: 30px;
  text-align: right;
}

.tod-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.tod-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: var(--space-md);
  background: var(--color-surface-overlay);
  border-radius: var(--radius-md);
}

.tod-icon { font-size: 1.5rem; }
.tod-label { font-size: 0.75rem; color: var(--color-text-secondary); }
.tod-value { font-size: 1.25rem; font-weight: 700; }
</style>

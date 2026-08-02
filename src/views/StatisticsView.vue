<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, linearRegression, movingAverage } from '@/services/statistics.js'
import { getCategoryLabel, ALL_CATEGORIES } from '@/services/categories.js'

const { user } = useAuth()

const readings = ref([])
const stats = ref(null)
const isLoading = ref(true)
const dateRange = ref('all') // '7d', '30d', '90d', '180d', '365d', 'all'
const trendType = ref('linear')

const dateRangeOptions = [
  { value: '7d', label: '7 giorni' },
  { value: '30d', label: '30 giorni' },
  { value: '90d', label: '3 mesi' },
  { value: '180d', label: '6 mesi' },
  { value: '365d', label: '1 anno' },
  { value: 'all', label: 'Tutto' }
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

    <div v-if="isLoading" class="empty-state"><p>Caricamento...</p></div>

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

      <!-- BP Chart (Simple ASCII-style visualization) -->
      <div class="chart-container card mb-md">
        <h3>Andamento Pressione</h3>
        <div class="chart bp-chart">
          <div class="chart-y-axis">
            <span>{{ maxSystolic }}</span>
            <span>{{ Math.round(maxSystolic / 2) }}</span>
            <span>0</span>
          </div>
          <div class="chart-area">
            <div
              v-for="(point, i) in chartData"
              :key="i"
              class="chart-bar-group"
              :style="{ left: (i / Math.max(chartData.length - 1, 1)) * 100 + '%' }"
            >
              <div class="chart-bar sys-bar"
                :style="{ height: (point.ySystolic / maxSystolic) * 100 + '%' }"
                title="SYS: {{ point.ySystolic }}"></div>
              <div class="chart-bar dia-bar"
                :style="{ height: (point.yDiastolic / maxSystolic) * 100 + '%' }"
                title="DIA: {{ point.yDiastolic }}"></div>
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
            <span class="tod-icon">🌅</span>
            <span class="tod-label">Mattina</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.MORNING || 0 }}</span>
          </div>
          <div class="tod-item">
            <span class="tod-icon">☀️</span>
            <span class="tod-label">Pomeriggio</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.AFTERNOON || 0 }}</span>
          </div>
          <div class="tod-item">
            <span class="tod-icon">🌆</span>
            <span class="tod-label">Sera</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.EVENING || 0 }}</span>
          </div>
          <div class="tod-item">
            <span class="tod-icon">🌙</span>
            <span class="tod-label">Notte</span>
            <span class="tod-value">{{ filteredStats.timeOfDayDistribution?.NIGHT || 0 }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <span class="empty-state__icon">📊</span>
      <h3>Nessun dato</h3>
      <p>Aggiungi misurazioni per vedere le statistiche.</p>
    </div>
  </div>
</template>

<style scoped>
.chip {
  background: var(--color-surface-container);
  color: var(--color-on-surface);
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
}

.chip--active {
  background: var(--color-primary-container);
  color: var(--color-on-primary-container);
  border-color: var(--color-primary);
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
  color: var(--color-on-surface-variant);
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
  color: var(--color-on-surface-variant);
  width: 30px;
  text-align: right;
}

.chart-area {
  flex: 1;
  position: relative;
  border-bottom: 1px solid var(--color-outline-variant);
  border-left: 1px solid var(--color-outline-variant);
}

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
  background: var(--color-surface-container);
  border-radius: 4px;
  overflow: hidden;
}

.cat-bar {
  height: 100%;
  background: var(--color-primary);
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
  background: var(--color-surface-container);
  border-radius: var(--radius-md);
}

.tod-icon { font-size: 1.5rem; }
.tod-label { font-size: 0.75rem; color: var(--color-on-surface-variant); }
.tod-value { font-size: 1.25rem; font-weight: 700; }
</style>

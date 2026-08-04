<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer, retrySyncQueue } from '@/services/dataService.js'
import { computeStatistics, computeDerivatives } from '@/services/statistics.js'
import { getCategoryColor } from '@/services/categories.js'
import ReadingCard from '@/components/ReadingCard.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import AppIcon from '@/components/AppIcon.vue'
import SkeletonLoader from '@/components/SkeletonLoader.vue'

const router = useRouter()
const { user } = useAuth()

const latestReading = ref(null)
const statistics = ref(null)
const weeklyTrend = ref(null)
const recentReadings = ref([])
const isLoading = ref(true)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  isLoading.value = true
  try {
    await retrySyncQueue(user.value.username)
    await refreshFromServer(user.value.username)

    const allReadings = await getReadings(user.value.username)
    if (allReadings.length > 0) {
      latestReading.value = allReadings[0]
      statistics.value = computeStatistics(allReadings)
      recentReadings.value = allReadings.slice(0, 5)

      // Weekly trend: compute derivative on last 7 days
      const weekAgo = new Date(Date.now() - 7 * 86400000)
      const weekReadings = allReadings.filter(r => new Date(r.timestamp) >= weekAgo)
      if (weekReadings.length >= 2) {
        const deriv = computeDerivatives(weekReadings)
        const avgDs = deriv.systolic.length ? deriv.systolic.reduce((a, b) => a + b, 0) / deriv.systolic.length : 0
        const avgDd = deriv.diastolic.length ? deriv.diastolic.reduce((a, b) => a + b, 0) / deriv.diastolic.length : 0
        weeklyTrend.value = {
          systolicRate: Math.round(avgDs * 10) / 10,
          diastolicRate: Math.round(avgDd * 10) / 10,
          alarmCount: deriv.alarmSegments.length
        }
      }
    }
  } catch (e) {
    console.error('Load error:', e)
  } finally {
    isLoading.value = false
  }
}

function goToAdd() {
  router.push('/add')
}

function editReading(reading) {
  router.push(`/edit/${reading.id}`)
}

const latestCategoryColor = computed(() => {
  if (!latestReading.value) return ''
  return getCategoryColor(latestReading.value.category)
})
</script>

<template>
  <div class="page">
    <div class="page-header flex justify-between items-center">
      <div>
        <h1>Ciao, {{ user?.username }}</h1>
        <p class="text-secondary" v-if="latestReading">Ultima misurazione: {{ new Date(latestReading.timestamp).toLocaleDateString('it-IT') }}</p>
      </div>
      <button class="btn btn-primary" @click="goToAdd">+ Nuova</button>
    </div>

    <!-- Latest Reading Card -->
    <div v-if="latestReading" class="latest-card card mb-md" :style="{ borderLeftColor: latestCategoryColor }">
      <div class="latest-card__header">
        <h2>Ultima Misurazione</h2>
        <CategoryBadge :category="latestReading.category" />
      </div>
      <div class="latest-card__values">
        <div class="latest-value">
          <span class="latest-value__number">{{ latestReading.systolic }}</span>
          <span class="latest-value__label">Sistolica</span>
        </div>
        <span class="latest-value__sep">/</span>
        <div class="latest-value">
          <span class="latest-value__number">{{ latestReading.diastolic }}</span>
          <span class="latest-value__label">Diastolica</span>
        </div>
        <div class="latest-value">
          <span class="latest-value__number">{{ latestReading.heartRate }}</span>
          <span class="latest-value__label"><AppIcon name="heart" :size="12" /> BPM</span>
        </div>
      </div>
      <div class="latest-card__info">
        {{ new Date(latestReading.timestamp).toLocaleString('it-IT') }}
        <span v-if="latestReading.notes"> — {{ latestReading.notes }}</span>
      </div>
    </div>

    <!-- Statistics Summary -->
    <div v-if="statistics && statistics.readingsCount > 0" class="stats-grid mb-md">
      <div class="stat-card card card--flat">
        <span class="stat-card__label">Media Sistolica</span>
        <span class="stat-card__value">{{ statistics.avgSystolic }}</span>
        <span class="stat-card__unit">mmHg</span>
      </div>
      <div class="stat-card card card--flat">
        <span class="stat-card__label">Media Diastolica</span>
        <span class="stat-card__value">{{ statistics.avgDiastolic }}</span>
        <span class="stat-card__unit">mmHg</span>
      </div>
      <div class="stat-card card card--flat">
        <span class="stat-card__label">Media BPM</span>
        <span class="stat-card__value">{{ statistics.avgHeartRate }}</span>
        <span class="stat-card__unit">BPM</span>
      </div>
      <div class="stat-card card card--flat">
        <span class="stat-card__label">Totale Misurazioni</span>
        <span class="stat-card__value">{{ statistics.readingsCount }}</span>
      </div>
    </div>

    <!-- Weekly Trend -->
    <div v-if="weeklyTrend" class="trend-card card mb-md">
      <h3 class="mb-sm">Trend Settimanale (7 giorni)</h3>
      <div class="trend-row">
        <div class="trend-item">
          <span class="trend-label">Sistolica</span>
          <span class="trend-value" :class="{ 'trend-up': weeklyTrend.systolicRate > 2, 'trend-down': weeklyTrend.systolicRate < -2 }">
            {{ weeklyTrend.systolicRate > 0 ? '+' : '' }}{{ weeklyTrend.systolicRate }}
          </span>
          <span class="trend-unit">mmHg/giorno</span>
        </div>
        <div class="trend-item">
          <span class="trend-label">Diastolica</span>
          <span class="trend-value" :class="{ 'trend-up': weeklyTrend.diastolicRate > 1.5, 'trend-down': weeklyTrend.diastolicRate < -1.5 }">
            {{ weeklyTrend.diastolicRate > 0 ? '+' : '' }}{{ weeklyTrend.diastolicRate }}
          </span>
          <span class="trend-unit">mmHg/giorno</span>
        </div>
      </div>
      <div v-if="weeklyTrend.alarmCount > 0" class="trend-alert mt-sm">
        ⚠️ {{ weeklyTrend.alarmCount }} {{ weeklyTrend.alarmCount === 1 ? 'picco pressorio' : 'picchi pressori' }} nella settimana
      </div>
      <p v-else class="trend-stable mt-sm">✅ Andamento stabile</p>
    </div>

    <!-- Recent Readings -->
    <div v-if="recentReadings.length > 0" class="mb-md">
      <div class="flex justify-between items-center mb-sm">
        <h3>Recenti</h3>
        <router-link to="/list" class="btn btn-sm btn-outline">Vedi tutte</router-link>
      </div>
      <div class="recent-list flex flex-col gap-sm">
        <ReadingCard
          v-for="reading in recentReadings"
          :key="reading.id"
          :reading="reading"
          compact
          @edit="editReading"
        />
      </div>
    </div>

    <!-- Loading -->
    <div v-if="isLoading">
      <SkeletonLoader type="card" :count="2" height="140px" class="mb-md" />
      <SkeletonLoader type="stats" class="mb-md" />
      <SkeletonLoader type="card" :count="3" height="80px" />
    </div>

    <!-- Empty State -->
    <div v-if="!isLoading && !latestReading" class="empty-state">
      <AppIcon name="heart" :size="48" color="var(--color-text-tertiary)" class="empty-state__icon" />
      <h3>Nessuna misurazione</h3>
      <p>Inizia a monitorare la tua pressione aggiungendo la prima misurazione.</p>
      <button class="btn btn-primary mt-md" @click="goToAdd">Aggiungi Misurazione</button>
    </div>
  </div>
</template>

<style scoped>
.text-secondary {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.latest-card {
  border-left: 4px solid var(--color-accent);
}

.latest-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}

.latest-card__header h2 {
  font-size: 1rem;
  color: var(--color-text-secondary);
}

.latest-card__values {
  display: flex;
  align-items: flex-end;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.latest-value {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.latest-value__number {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
}

.latest-value__label {
  font-size: 0.6875rem;
  color: var(--color-text-secondary);
  text-transform: uppercase;
}

.latest-value__sep {
  font-size: 2rem;
  color: var(--color-text-tertiary);
  padding-bottom: 1.25rem;
}

.latest-card__info {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
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

.stat-card__unit {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

.trend-card { border-left: 4px solid var(--color-accent); }
.trend-row { display: flex; gap: var(--space-xl); }
.trend-item { display: flex; flex-direction: column; }
.trend-label { font-size: 0.6875rem; color: var(--color-text-secondary); text-transform: uppercase; }
.trend-value { font-size: 1.25rem; font-weight: 700; color: var(--color-text-primary); }
.trend-value.trend-up { color: #BA1A1A; }
.trend-value.trend-down { color: #006C4C; }
.trend-unit { font-size: 0.625rem; color: var(--color-text-tertiary); }
.trend-alert { font-size: 0.8125rem; color: var(--color-error); font-weight: 500; }
.trend-stable { font-size: 0.8125rem; color: var(--color-accent); }
</style>

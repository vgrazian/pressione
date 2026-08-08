<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer, retrySyncQueue } from '@/services/dataService.js'
import { computeStatistics, computeDerivatives } from '@/services/statistics.js'
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
const syncStatus = ref('idle')
const syncError = ref('')

const displayName = computed(() => {
  return user.value?.firstName?.trim() || user.value?.username || ''
})

const showWelcome = ref(localStorage.getItem('pressione_welcome_dismissed') !== 'true')

const needsSetup = computed(() => {
  return !user.value?.email || user.value.email === user.value.username
})

function dismissWelcome() {
  showWelcome.value = false
  localStorage.setItem('pressione_welcome_dismissed', 'true')
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return 'Buonanotte'
  if (h < 12) return 'Buongiorno'
  if (h < 18) return 'Buon pomeriggio'
  if (h < 22) return 'Buonasera'
  return 'Buonanotte'
})

const wellnessMessage = computed(() => {
  if (!latestReading.value) return null
  const cat = latestReading.value.category
  const messages = {
    optimal: { text: 'La tua pressione è ottimale', icon: '✅', tone: 'positive' },
    normal: { text: 'La tua pressione è nella norma', icon: '👍', tone: 'positive' },
    elevated: { text: 'La pressione è leggermente elevata', icon: '💡', tone: 'caution' },
    hypertension_stage1: { text: 'Pressione alta — monitora con attenzione', icon: '⚠️', tone: 'warning' },
    hypertension_stage2: { text: 'Pressione molto alta — consulta il medico', icon: '🔴', tone: 'serious' },
    hypertensive_crisis: { text: 'Valori critici — se hai sintomi contatta subito un medico. Altrimenti ripeti la misurazione dopo 5 minuti di riposo.', icon: '🆘', tone: 'crisis' },
    hypotension: { text: 'La pressione è più bassa del normale', icon: 'ℹ️', tone: 'info' }
  }
  return messages[cat] || messages.normal
})

const trendMessage = computed(() => {
  if (!weeklyTrend.value) return null
  const s = weeklyTrend.value.systolicRate
  const d = weeklyTrend.value.diastolicRate
  if (Math.abs(s) < 1 && Math.abs(d) < 1) return { text: 'Andamento stabile ✅', tone: 'stable' }
  if (s < -2 || d < -1.5) return { text: 'In miglioramento 👍', tone: 'improving' }
  if (s > 2 || d > 1.5) return { text: 'In lieve aumento — tienila d\'occhio', tone: 'watching' }
  return { text: 'Andamento stabile ✅', tone: 'stable' }
})

const daysMonitored = computed(() => {
  if (!statistics.value) return 0
  return statistics.value.readingsCount > 0 ? Math.max(1, Math.round(statistics.value.readingsCount / 2)) : 0
})

onMounted(async () => {
  await loadData()
})

async function loadData() {
  isLoading.value = true
  syncStatus.value = 'syncing'
  syncError.value = ''
  try {
    await retrySyncQueue(user.value.username)
    await refreshFromServer(user.value.username)
    syncStatus.value = 'idle'

    const allReadings = await getReadings(user.value.username)
    if (allReadings.length > 0) {
      latestReading.value = allReadings[0]
      statistics.value = computeStatistics(allReadings)
      recentReadings.value = allReadings.slice(0, 5)

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
    syncStatus.value = 'error'
    syncError.value = e.message || 'Errore di sincronizzazione'
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
</script>

<template>
  <div class="page">
    <!-- Greeting -->
    <div class="greeting mb-lg">
      <h1>{{ greeting }}, {{ displayName }}</h1>
      <p class="greeting__sub" v-if="latestReading">
        Ultima lettura: {{ new Date(latestReading.timestamp).toLocaleString('it-IT', { weekday: 'long', hour: '2-digit', minute: '2-digit' }) }}
      </p>
    </div>

    <!-- Welcome / First-time setup -->
    <div v-if="showWelcome && needsSetup" class="welcome-card mb-lg">
      <div class="flex justify-between items-start mb-sm">
        <h3 style="font-size:1rem;font-weight:600">👋 Benvenuto in Pressione!</h3>
        <button class="btn btn-sm btn-ghost" @click="dismissWelcome" title="Non mostrare più">×</button>
      </div>
      <p class="text-secondary mb-sm" style="font-size:0.875rem">
        Per usare l'app al meglio e poter recuperare la password in futuro, completa questi due passaggi:
      </p>
      <div class="welcome-steps">
        <div class="welcome-step">
          <span class="welcome-step__num">1</span>
          <div>
            <strong>Imposta la tua email</strong>
            <p class="text-secondary" style="font-size:0.8125rem">Serve per il recupero password. Ora è vuota o uguale allo username.</p>
          </div>
          <router-link to="/settings" class="btn btn-sm btn-primary" style="flex-shrink:0">Vai alle impostazioni</router-link>
        </div>
        <div class="welcome-step">
          <span class="welcome-step__num">2</span>
          <div>
            <strong>Cambia la password</strong>
            <p class="text-secondary" style="font-size:0.8125rem">Scegli una password personale per proteggere i tuoi dati.</p>
          </div>
          <router-link to="/settings" class="btn btn-sm btn-outline" style="flex-shrink:0">Cambia password</router-link>
        </div>
      </div>
    </div>

    <!-- Sync Status -->
    <div v-if="syncStatus === 'syncing'" class="sync-banner mb-md">
      Sincronizzazione in corso...
    </div>
    <div v-if="syncStatus === 'error'" class="sync-banner sync-banner--error mb-md">
      <span>{{ syncError }}</span>
      <button class="btn btn-sm btn-ghost" @click="loadData">Riprova</button>
    </div>

    <!-- Wellness Status Card -->
    <div v-if="latestReading && wellnessMessage" class="wellness-card card mb-md" :class="`wellness-card--${wellnessMessage.tone}`">
      <div class="wellness-card__header">
        <span class="wellness-card__icon">{{ wellnessMessage.icon }}</span>
        <CategoryBadge :category="latestReading.category" />
      </div>
      <p class="wellness-card__message">{{ wellnessMessage.text }}</p>
      <div class="wellness-card__values">
        <div class="wellness-value">
          <span class="wellness-value__num">{{ latestReading.systolic }}</span>
          <span class="wellness-value__lbl">SYS</span>
        </div>
        <span class="wellness-value__sep">/</span>
        <div class="wellness-value">
          <span class="wellness-value__num">{{ latestReading.diastolic }}</span>
          <span class="wellness-value__lbl">DIA</span>
        </div>
        <div class="wellness-value wellness-value--bpm">
          <span class="wellness-value__num">{{ latestReading.heartRate }}</span>
          <span class="wellness-value__lbl"><AppIcon name="heart" :size="14" /> BPM</span>
        </div>
      </div>
    </div>

    <!-- Quick Add -->
    <button class="btn btn-primary btn-block mb-md" @click="goToAdd">
      <AppIcon name="plus" :size="16" /> Registra una misurazione
    </button>

    <!-- Insights Row -->
    <div v-if="statistics && statistics.readingsCount > 0" class="insights-row mb-md">
      <div class="insight-chip">
        <span class="insight-chip__val">{{ statistics.avgSystolic }}/{{ statistics.avgDiastolic }}</span>
        <span class="insight-chip__lbl">Media pressione</span>
      </div>
      <div class="insight-chip">
        <span class="insight-chip__val">{{ statistics.readingsCount }}</span>
        <span class="insight-chip__lbl">Misurazioni totali</span>
      </div>
      <div class="insight-chip">
        <span class="insight-chip__val">{{ statistics.avgHeartRate }}</span>
        <span class="insight-chip__lbl">BPM medi</span>
      </div>
    </div>

    <!-- Trend -->
    <div v-if="trendMessage" class="trend-line mb-md" :class="`trend-line--${trendMessage.tone}`">
      <span>{{ trendMessage.text }}</span>
      <span v-if="weeklyTrend" class="trend-line__detail">
        SYS {{ weeklyTrend.systolicRate > 0 ? '+' : '' }}{{ weeklyTrend.systolicRate }} · DIA {{ weeklyTrend.diastolicRate > 0 ? '+' : '' }}{{ weeklyTrend.diastolicRate }} mmHg/giorno
      </span>
    </div>

    <!-- Recent Readings -->
    <div v-if="recentReadings.length > 0" class="mb-md">
      <div class="flex justify-between items-center mb-sm">
        <h3>Recenti</h3>
        <router-link to="/list" class="btn btn-sm btn-outline">Vedi tutte</router-link>
      </div>
      <div class="flex flex-col gap-sm">
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
      <div class="empty-state__illustration">
        <AppIcon name="heart" :size="48" color="var(--color-accent-muted)" />
      </div>
      <h3>Inizia a prenderti cura di te</h3>
      <p class="text-secondary">Registra la tua prima misurazione per iniziare a monitorare la pressione.</p>
      <div class="empty-state__steps">
        <div class="empty-step"><span class="empty-step__num">1</span><span>Misura la pressione con lo sfigmomanometro</span></div>
        <div class="empty-step"><span class="empty-step__num">2</span><span>Tocca "Registra una misurazione"</span></div>
        <div class="empty-step"><span class="empty-step__num">3</span><span>Inserisci i valori e salva</span></div>
      </div>
      <button class="btn btn-primary mt-md" @click="goToAdd">Registra la prima misurazione</button>
    </div>
  </div>
</template>

<style scoped>
/* ── Greeting ── */
.greeting { padding-top: var(--space-sm); }
.greeting h1 { font-size: 1.5rem; font-weight: 700; margin-bottom: 2px; }
.greeting__sub { font-size: 0.8125rem; color: var(--color-text-tertiary); }

/* ── Welcome card ── */
.welcome-card {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
}
.welcome-steps { display: flex; flex-direction: column; gap: var(--space-md); }
.welcome-step {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
}
.welcome-step__num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  color: var(--color-on-accent);
  font-size: 0.8125rem;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
}

/* ── Sync banner ── */
.sync-banner { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-sm) var(--space-md); border-radius: var(--radius-sm); background: var(--color-accent-muted); color: var(--color-accent); font-size: 0.8125rem; }
.sync-banner--error { background: var(--color-error-muted); color: var(--color-error); justify-content: space-between; }

/* ── Wellness Card ── */
.wellness-card { padding: var(--space-lg); }
.wellness-card--positive { border-left: 4px solid var(--color-accent); }
.wellness-card--caution { border-left: 4px solid #F9A825; }
.wellness-card--warning { border-left: 4px solid #EF6C00; }
.wellness-card--serious,
.wellness-card--crisis { border-left: 4px solid var(--color-error); }
.wellness-card--info { border-left: 4px solid var(--color-border-strong); }

.wellness-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}
.wellness-card__icon { font-size: 1.5rem; }
.wellness-card__message {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.5;
  margin-bottom: var(--space-md);
}
.wellness-card--crisis .wellness-card__message {
  color: var(--color-error);
}

.wellness-card__values {
  display: flex;
  align-items: flex-end;
  gap: var(--space-md);
}
.wellness-value {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.wellness-value__num {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.wellness-value__lbl {
  font-size: 0.625rem;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.wellness-value__sep {
  font-size: 2rem;
  color: var(--color-text-tertiary);
  padding-bottom: 1.25rem;
}
.wellness-value--bpm .wellness-value__num { font-size: 1.5rem; }

/* ── Insights Row ── */
.insights-row {
  display: flex;
  gap: var(--space-sm);
}
.insight-chip {
  flex: 1;
  background: var(--color-surface-overlay);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.insight-chip__val {
  font-size: 1.125rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.insight-chip__lbl {
  font-size: 0.625rem;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

/* ── Trend Line ── */
.trend-line {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 500;
}
.trend-line--improving { background: var(--color-accent-muted); color: var(--color-accent); }
.trend-line--stable { background: var(--color-surface-overlay); color: var(--color-text-secondary); }
.trend-line--watching { background: #FFF3E0; color: #E65100; }
.trend-line__detail { font-size: 0.75rem; font-weight: 400; opacity: 0.75; }

/* ── Empty State ── */
.empty-state { text-align: center; padding: var(--space-2xl) var(--space-md); }
.empty-state__illustration { margin-bottom: var(--space-md); opacity: 0.6; }
.empty-state h3 { margin-bottom: var(--space-xs); }
.text-secondary { color: var(--color-text-secondary); font-size: 0.875rem; }
.empty-state__steps { display: flex; flex-direction: column; gap: var(--space-sm); margin: var(--space-lg) auto 0; text-align: left; max-width: 280px; }
.empty-step { display: flex; align-items: center; gap: var(--space-sm); font-size: 0.8125rem; color: var(--color-text-secondary); }
.empty-step__num { display: flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: var(--radius-full); background: var(--color-accent-muted); color: var(--color-accent); font-size: 0.75rem; font-weight: 600; flex-shrink: 0; }
</style>

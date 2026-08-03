<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, computeMorningSurge, computeHypertensiveLoad, computeDerivatives, computeHRV } from '@/services/statistics.js'
import { getCategoryLabel } from '@/services/categories.js'

function catColor(category) {
  const map = { 'NORMAL': '#006C4C', 'ELEVATED': '#F9A825', 'HYPERTENSION_STAGE_1': '#EF6C00', 'HYPERTENSION_STAGE_2': '#D32F2F', 'HYPERTENSIVE_CRISIS': '#7B1FA2', 'HYPOTENSION': '#1976D2' }
  return map[category] || '#999'
}
import { generatePDF as generatePDFReport } from '@/services/pdfReport.js'
import { supabase } from '@/services/supabaseClient.js'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import AppIcon from '@/components/AppIcon.vue'

const { user } = useAuth()
const readings = ref([])
const isLoading = ref(true)
const dateRange = ref('30')
const customFrom = ref('')
const customTo = ref('')
const includeCharts = ref(true)
const includeHistory = ref(true)
const anonymize = ref(false)
const shareLink = ref(null)
const sharePin = ref('')
const showPin = ref('')
const linkMessage = ref('')
const activeLinks = ref([])
const generatingPDF = ref(false)

const periods = [
  { value: '7', label: '7 Giorni' },
  { value: '30', label: '30 Giorni' },
  { value: 'custom', label: 'Personalizzato' }
]

onMounted(async () => {
  isLoading.value = true
  try {
    await refreshFromServer(user.value.username)
    readings.value = await getReadings(user.value.username)
    await loadActiveLinks()
  } finally { isLoading.value = false }
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

const stats = computed(() => computeStatistics(filteredReadings.value))
const morningSurge = computed(() => computeMorningSurge(filteredReadings.value))
const htnLoad = computed(() => computeHypertensiveLoad(filteredReadings.value))
const derivatives = computed(() => computeDerivatives(filteredReadings.value))
const hrv = computed(() => computeHRV(filteredReadings.value))

// Derivatives for multi-period comparison
const derivatives7 = computed(() => computeDerivatives(readings7.value))
const derivatives30 = computed(() => computeDerivatives(readings30.value))
const stats7 = computed(() => computeStatistics(readings7.value))
const stats30 = computed(() => computeStatistics(readings30.value))
const htnLoad7 = computed(() => computeHypertensiveLoad(readings7.value))
const htnLoad30 = computed(() => computeHypertensiveLoad(readings30.value))
const surge7 = computed(() => computeMorningSurge(readings7.value))
const surge30 = computed(() => computeMorningSurge(readings30.value))

// Readings grouped by time of day
const readingsByTimeOfDay = computed(() => {
  const bands = { MORNING: { label: 'Mattina (06-09)', readings: [], icon: '☀️' }, AFTERNOON: { label: 'Pomeriggio (12-15)', readings: [], icon: '🌤️' }, EVENING: { label: 'Sera (18-22)', readings: [], icon: '🌅' }, NIGHT: { label: 'Notte (23-05)', readings: [], icon: '🌙' } }
  for (const r of filteredReadings.value) {
    const h = new Date(r.timestamp).getHours()
    if (h >= 6 && h < 9) bands.MORNING.readings.push(r)
    else if (h >= 12 && h < 15) bands.AFTERNOON.readings.push(r)
    else if (h >= 18 && h < 22) bands.EVENING.readings.push(r)
    else bands.NIGHT.readings.push(r)
  }
  return Object.entries(bands).map(([key, b]) => ({
    key, label: b.label, icon: b.icon, readings: b.readings,
    count: b.readings.length,
    avgSys: b.readings.length ? Math.round(b.readings.reduce((s, r) => s + r.systolic, 0) / b.readings.length) : null,
    avgDia: b.readings.length ? Math.round(b.readings.reduce((s, r) => s + r.diastolic, 0) / b.readings.length) : null,
    avgHR: b.readings.length ? Math.round(b.readings.reduce((s, r) => s + r.heartRate, 0) / b.readings.length) : null
  }))
})

const titleSuffix = computed(() => anonymize.value ? '' : ` - ${user.value?.username}`)

// Subsets for multi-period comparison
const readings7 = computed(() => {
  const cutoff = new Date(Date.now() - 7 * 86400000)
  return readings.value.filter(r => new Date(r.timestamp) >= cutoff)
})
const readings30 = computed(() => {
  const cutoff = new Date(Date.now() - 30 * 86400000)
  return readings.value.filter(r => new Date(r.timestamp) >= cutoff)
})

// --- PDF Generation ---
async function generatePDF() {
  generatingPDF.value = true
  try {
    await generatePDFReport({
      data: filteredReadings.value,
      readings7: readings7.value,
      readings30: readings30.value,
      username: user.value?.username,
      age: user.value?.age || null,
      gender: user.value?.gender || null,
      anonymize: anonymize.value,
      includeCharts: includeCharts.value,
      includeHistory: includeHistory.value
    })
  } catch (e) {
    linkMessage.value = 'Errore nella generazione PDF: ' + e.message
  } finally {
    generatingPDF.value = false
  }
}

// --- Sharing ---
function sharePDF() {
  generatePDF()
}

async function shareViaEmail() {
  const s = stats.value
  const body = `REPORT PRESSIONE ARTERIOSA${titleSuffix.value}\n\n` +
    `Media: ${s.avgSystolic}/${s.avgDiastolic} mmHg\n` +
    `BPM medio: ${s.avgHeartRate}\n` +
    `Misurazioni: ${s.readingsCount}\n\n` +
    `Generato da Pressione App`
  window.open(`mailto:?subject=Report Pressione${titleSuffix.value}&body=${encodeURIComponent(body)}`, '_blank')
}

function shareViaWhatsApp() {
  const s = stats.value
  const text = `📊 *Report Pressione Arteriosa*${titleSuffix.value.replace(/-/g, '')}\n\n` +
    `Media: ${s.avgSystolic}/${s.avgDiastolic} mmHg\n` +
    `BPM medio: ${s.avgHeartRate}\n` +
    `Misurazioni: ${s.readingsCount}`
  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank')
}

async function shareNative() {
  const s = stats.value
  const text = `Report Pressione Arteriosa${titleSuffix.value}\nMedia: ${s.avgSystolic}/${s.avgDiastolic} mmHg | BPM: ${s.avgHeartRate} | ${s.readingsCount} misurazioni`
  if (navigator.share) {
    await navigator.share({ title: 'Report Pressione', text })
  } else {
    await navigator.clipboard.writeText(text)
    linkMessage.value = 'Report copiato negli appunti!'
    setTimeout(() => linkMessage.value = '', 3000)
  }
}

// --- Temporary Link ---
async function loadActiveLinks() {
  try {
    const { data } = await supabase.from('settings')
      .select('key, value')
      .eq('username', user.value.username)
      .like('key', '_share_%')
    if (data) {
      activeLinks.value = data
        .filter(s => {
          try { const v = JSON.parse(s.value); return !v.revoked && new Date(v.expiresAt) > new Date() }
          catch { return false }
        })
        .map(s => ({ token: s.key.replace('_share_', ''), ...JSON.parse(s.value) }))
    }
  } catch { activeLinks.value = [] }
}

async function hashPin(pin) {
  const encoder = new TextEncoder()
  const data = encoder.encode(pin)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('')
}

async function generateShareLink() {
  linkMessage.value = ''
  const readingsData = filteredReadings.value
  if (!readingsData.length) { linkMessage.value = 'Nessun dato da condividere'; return }
  try {
    const reportData = {
      stats: stats.value,
      readings: readingsData.slice(0, 100).map(r => ({
        systolic: r.systolic, diastolic: r.diastolic, heartRate: r.heartRate,
        timestamp: r.timestamp, notes: r.notes, category: r.category
      }))
    }
    // Generate random token and optional PIN
    const token = Array.from(crypto.getRandomValues(new Uint8Array(16))).map(b => b.toString(16).padStart(2, '0')).join('')
    let pinHash = null
    let pinClear = ''
    if (sharePin.value) {
      pinClear = String(Math.floor(1000 + Math.random() * 9000))
      pinHash = await hashPin(pinClear)
    }

    // Store in settings table using user's real username as FK
    const expiresAt = new Date(Date.now() + 48 * 3600000).toISOString()
    const { error } = await supabase.from('settings').upsert({
      username: user.value.username,
      key: '_share_' + token,
      value: JSON.stringify({ reportData, pinHash, expiresAt, revoked: false }),
      updated_at: new Date().toISOString()
    })
    if (error) throw error

    shareLink.value = `https://vgrazian.github.io/pressione/#/share/${token}`
    showPin.value = pinClear
    const expiryStr = new Date(expiresAt).toLocaleString('it-IT')
    linkMessage.value = pinClear
      ? `PIN: ${pinClear} (comunicalo al medico). Scade il ${expiryStr}`
      : `Link generato! Scade il ${expiryStr}`
    await loadActiveLinks()
  } catch (e) {
    linkMessage.value = 'Errore: ' + e.message
  }
}

function copyLink() {
  if (shareLink.value) {
    const text = showPin.value ? `${shareLink.value}\nPIN: ${showPin.value}` : shareLink.value
    navigator.clipboard.writeText(text)
    linkMessage.value = 'Copiato!'
    setTimeout(() => linkMessage.value = '', 3000)
  }
}

async function revokeLink(token) {
  try {
    const key = '_share_' + token
    const { data } = await supabase.from('settings').select('value').eq('username', user.value.username).eq('key', key).single()
    if (data) {
      const v = JSON.parse(data.value); v.revoked = true
      await supabase.from('settings').upsert({ username: user.value.username, key, value: JSON.stringify(v), updated_at: new Date().toISOString() })
    }
    activeLinks.value = activeLinks.value.filter(l => l.token !== token)
    linkMessage.value = 'Link revocato.'
    setTimeout(() => linkMessage.value = '', 3000)
  } catch (e) {
    linkMessage.value = 'Errore: ' + e.message
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header"><h1>Report & Condivisione</h1></div>

    <!-- Period + Filters -->
    <div class="card mb-md">
      <h3 class="mb-sm">Filtro Contenuti</h3>
      <div class="flex gap-sm mb-sm flex-wrap">
        <button v-for="p in periods" :key="p.value" class="chip" :class="{ 'chip--active': dateRange === p.value }"
          @click="dateRange = p.value">{{ p.label }}</button>
      </div>
      <div v-if="dateRange === 'custom'" class="flex gap-sm mb-sm">
        <input type="date" v-model="customFrom" class="form-input" style="width:140px" />
        <span class="text-secondary">—</span>
        <input type="date" v-model="customTo" class="form-input" style="width:140px" />
      </div>
      <label class="flex items-center gap-sm mb-sm" style="cursor:pointer">
        <input type="checkbox" v-model="includeCharts" /> Includi grafici
      </label>
      <label class="flex items-center gap-sm mb-sm" style="cursor:pointer">
        <input type="checkbox" v-model="includeHistory" /> Includi storico completo
      </label>
      <label class="flex items-center gap-sm" style="cursor:pointer">
        <input type="checkbox" v-model="anonymize" /> Anonimizza report
      </label>
    </div>

    <div v-if="isLoading" class="p-lg"><SkeletonLoader type="text" :count="8" /></div>

    <div v-else-if="!filteredReadings.length" class="empty-state">
      <AppIcon name="copy" :size="40" color="var(--color-text-tertiary)" />
      <h3>Nessun dato nel periodo</h3>
      <p>Aggiungi misurazioni per generare un report.</p>
    </div>

    <template v-else>
      <!-- Preview: Summary Cards -->
      <div class="card mb-md">
        <h2>Report Pressione Arteriosa{{ titleSuffix }}</h2>
        <p class="text-secondary mb-sm" style="font-size:0.8125rem">
          {{ filteredReadings.length }} misurazioni — 
          {{ new Date(filteredReadings[filteredReadings.length-1]?.timestamp).toLocaleDateString('it-IT') }} / 
          {{ new Date(filteredReadings[0]?.timestamp).toLocaleDateString('it-IT') }}
        </p>
        <div class="preview-stats mb-md">
          <span><strong>{{ stats.avgSystolic }}/{{ stats.avgDiastolic }}</strong> mmHg media</span>
          <span><strong>{{ stats.avgHeartRate }}</strong> BPM medio</span>
          <span><strong>{{ htnLoad.percentage }}%</strong> carico ipertensivo</span>
          <span v-if="hrv !== null"><strong>{{ hrv }}</strong> HRV</span>
        </div>

        <!-- Derivatives alert -->
        <div v-if="derivatives.alarmSegments.length > 0" class="alert-box mb-md" style="background:var(--color-error-muted);padding:var(--space-sm) var(--space-md);border-radius:var(--radius-sm);color:var(--color-error);font-size:0.8125rem">
          ⚠ {{ derivatives.alarmSegments.length }} episodi di variazione rapida (>10 mmHg/h) rilevati
        </div>

        <!-- Multi-period comparison -->
        <h3 class="mb-sm" style="font-size:0.9375rem">Confronto 7 / 30 giorni</h3>
        <div class="comparison-table">
          <table>
            <thead>
              <tr><th></th><th>7 giorni</th><th>30 giorni</th></tr>
            </thead>
            <tbody>
              <tr><td>Letture</td><td>{{ stats7.readingsCount }}</td><td>{{ stats30.readingsCount }}</td></tr>
              <tr><td>SYS/DIA media</td><td>{{ stats7.avgSystolic }}/{{ stats7.avgDiastolic }}</td><td>{{ stats30.avgSystolic }}/{{ stats30.avgDiastolic }}</td></tr>
              <tr><td>BPM medio</td><td>{{ stats7.avgHeartRate }}</td><td>{{ stats30.avgHeartRate }}</td></tr>
              <tr><td>dP/dt max</td><td>{{ Math.round(derivatives7.maxRate) }} mmHg/h</td><td>{{ Math.round(derivatives30.maxRate) }} mmHg/h</td></tr>
              <tr><td>Allarmi dP/dt</td><td :class="{ 'text-error': derivatives7.alarmSegments.length > 0 }">{{ derivatives7.alarmSegments.length }}</td><td :class="{ 'text-error': derivatives30.alarmSegments.length > 0 }">{{ derivatives30.alarmSegments.length }}</td></tr>
              <tr><td>Carico ipertensivo</td><td :class="{ 'text-error': htnLoad7.percentage > 30 }">{{ htnLoad7.percentage }}%</td><td :class="{ 'text-error': htnLoad30.percentage > 30 }">{{ htnLoad30.percentage }}%</td></tr>
              <tr><td>Picco mattutino</td><td>{{ surge7.delta !== null ? (surge7.delta > 0 ? '+' : '') + surge7.delta + ' mmHg' : 'N/D' }}</td><td>{{ surge30.delta !== null ? (surge30.delta > 0 ? '+' : '') + surge30.delta + ' mmHg' : 'N/D' }}</td></tr>
            </tbody>
          </table>
        </div>

        <!-- Time of day breakdown -->
        <h3 class="mb-sm mt-md" style="font-size:0.9375rem">Per fascia oraria</h3>
        <div class="timeofday-grid">
          <div v-for="band in readingsByTimeOfDay" :key="band.key" class="tod-card" :class="{ 'tod-card--empty': band.count === 0 }">
            <span class="tod-icon">{{ band.icon }}</span>
            <span class="tod-label">{{ band.label }}</span>
            <span v-if="band.count > 0" class="tod-avg">{{ band.avgSys }}/{{ band.avgDia }} mmHg</span>
            <span v-if="band.count > 0" class="tod-bpm">{{ band.avgHR }} BPM</span>
            <span class="tod-count">{{ band.count }} letture</span>
          </div>
        </div>

        <!-- Morning surge -->
        <div v-if="morningSurge.delta !== null" class="mt-md" style="font-size:0.8125rem">
          <span :class="morningSurge.alert ? 'text-error' : 'text-secondary'">
            {{ morningSurge.alert ? '⚠' : 'ℹ' }} Picco mattutino: {{ morningSurge.delta > 0 ? '+' : '' }}{{ morningSurge.delta }} mmHg
            (media mattina {{ morningSurge.morningAvg }} vs sera {{ morningSurge.eveningAvg }})
          </span>
        </div>

        <!-- Trend -->
        <div v-if="derivatives.systolic.length > 0" class="mt-sm" style="font-size:0.8125rem">
          <span class="text-secondary">
            Tendenza sistolica:
            <template v-if="derivatives.maxRate > 5">in aumento ↑</template>
            <template v-else-if="derivatives.maxRate < -5">in diminuzione ↓</template>
            <template v-else>stabile →</template>
            (variazione max: {{ Math.round(derivatives.maxRate) }} mmHg/h)
          </span>
        </div>
      </div>

      <!-- History table -->
      <div v-if="includeHistory" class="card mb-md">
        <h3 class="mb-sm">Storico</h3>
        <table class="preview-table">
          <thead><tr><th>Data</th><th>Ora</th><th>SYS</th><th>DIA</th><th>BPM</th><th>Categoria</th></tr></thead>
          <tbody>
            <tr v-for="r in filteredReadings.slice(0, 30)" :key="r.id"
              :style="{ borderLeft: '3px solid ' + catColor(r.category) }">
              <td>{{ new Date(r.timestamp).toLocaleDateString('it-IT') }}</td>
              <td>{{ new Date(r.timestamp).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }) }}</td>
              <td :class="{ 'text-error': r.systolic >= 140, 'text-warning': r.systolic >= 130 && r.systolic < 140 }">{{ r.systolic }}</td>
              <td :class="{ 'text-error': r.diastolic >= 90, 'text-warning': r.diastolic >= 85 && r.diastolic < 90 }">{{ r.diastolic }}</td>
              <td>{{ r.heartRate }}</td>
              <td><small>{{ getCategoryLabel(r.category) }}</small></td>
            </tr>
          </tbody>
        </table>
        <p v-if="filteredReadings.length > 30" class="text-secondary mt-sm" style="font-size:0.75rem">
          ...e altre {{ filteredReadings.length - 30 }} misurazioni (scarica il PDF per lo storico completo)
        </p>
      </div>

      <!-- Share Actions -->
      <div class="card mb-md">
        <h3 class="mb-sm">Condividi</h3>
        <div class="flex gap-sm flex-wrap">
          <button class="btn btn-primary" @click="generatePDF" :disabled="generatingPDF">
            <AppIcon name="copy" :size="16" /> {{ generatingPDF ? 'Generazione...' : 'Scarica PDF' }}
          </button>
          <button class="btn btn-ghost" @click="shareViaEmail">
            <AppIcon name="share" :size="16" /> Email
          </button>
          <button class="btn btn-ghost" @click="shareViaWhatsApp">
            <AppIcon name="share" :size="16" /> WhatsApp
          </button>
          <button class="btn btn-ghost" @click="shareNative">
            <AppIcon name="share" :size="16" /> Condividi
          </button>
        </div>
      </div>

      <!-- Temporary Link -->
      <div class="card mb-md">
        <h3 class="mb-sm">Link Temporaneo (48h)</h3>
        <p class="text-secondary mb-sm" style="font-size:0.8125rem">
          Genera un link web per il medico. Con PIN opzionale per maggiore privacy.
        </p>
        <div class="flex gap-sm mb-sm items-center">
          <label class="flex items-center gap-sm" style="font-size:0.8125rem;cursor:pointer">
            <input type="checkbox" v-model="sharePin" /> Proteggi con PIN
          </label>
          <button class="btn btn-primary btn-sm" @click="generateShareLink">Genera Link</button>
        </div>
        <div v-if="shareLink" class="share-link-box mb-sm">
          <code>{{ shareLink }}</code>
          <button class="btn btn-sm btn-ghost" @click="copyLink">Copia</button>
        </div>
        <div v-if="linkMessage" class="form-success mb-sm">{{ linkMessage }}</div>

        <!-- Active Links -->
        <div v-if="activeLinks.length > 0" class="mt-md">
          <h4 class="mb-sm" style="font-size:0.875rem">Link Attivi</h4>
          <div v-for="link in activeLinks" :key="link.token" class="active-link-row">
            <code style="font-size:0.6875rem">{{ link.token.slice(0, 12) }}...</code>
            <span style="font-size:0.6875rem;color:var(--color-text-tertiary)">
              Scade {{ new Date(link.expiresAt).toLocaleString('it-IT') }}
            </span>
            <button class="btn btn-sm btn-error" @click="revokeLink(link.token)" style="font-size:0.6875rem;padding:2px 8px;min-height:24px">Revoca</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.preview-stats { display: flex; gap: var(--space-lg); font-size: 0.875rem; flex-wrap: wrap; }
.preview-table { width: 100%; border-collapse: collapse; font-size: 0.75rem; }
.preview-table th, .preview-table td { padding: 5px 8px; text-align: left; border-bottom: 1px solid var(--color-border); }
.preview-table th { color: var(--color-text-secondary); font-weight: 600; font-size: 0.6875rem; }
.preview-table td { font-size: 0.75rem; }
.share-link-box { display: flex; align-items: center; gap: var(--space-sm); background: var(--color-surface-overlay); padding: var(--space-sm) var(--space-md); border-radius: var(--radius-sm); }
.share-link-box code { font-size: 0.75rem; word-break: break-all; flex: 1; }
.form-success { color: var(--color-accent); font-size: 0.8125rem; font-weight: 500; }

/* Multi-period comparison table */
.comparison-table table { width: 100%; border-collapse: collapse; font-size: 0.8125rem; }
.comparison-table th { text-align: left; padding: 4px 8px; background: var(--color-surface-overlay); color: var(--color-text-secondary); font-weight: 600; font-size: 0.75rem; }
.comparison-table td { padding: 5px 8px; border-bottom: 1px solid var(--color-border); font-variant-numeric: tabular-nums; }
.comparison-table td:first-child { color: var(--color-text-secondary); }

/* Time of day cards */
.timeofday-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm); }
.tod-card { background: var(--color-surface-overlay); border-radius: var(--radius-sm); padding: var(--space-sm); text-align: center; display: flex; flex-direction: column; align-items: center; gap: 2px; }
.tod-card--empty { opacity: 0.4; }
.tod-icon { font-size: 1.25rem; }
.tod-label { font-size: 0.625rem; color: var(--color-text-tertiary); }
.tod-avg { font-size: 0.875rem; font-weight: 600; }
.tod-bpm { font-size: 0.6875rem; color: var(--color-text-secondary); }
.tod-count { font-size: 0.625rem; color: var(--color-text-tertiary); }

.alert-box { line-height: 1.4; }
.text-error { color: var(--color-error); }
.text-warning { color: #EF6C00; }

@media (max-width: 480px) {
  .timeofday-grid { grid-template-columns: repeat(2, 1fr); }
  .comparison-table table { font-size: 0.75rem; }
}
</style>

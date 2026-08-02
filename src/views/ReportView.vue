<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, computeMorningSurge, computeHypertensiveLoad } from '@/services/statistics.js'
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
      <!-- Preview -->
      <div class="card mb-md" id="report-preview">
        <h2>Report Pressione Arteriosa{{ titleSuffix }}</h2>
        <p class="text-secondary mb-sm">
          {{ filteredReadings.length }} misurazioni — 
          {{ new Date(filteredReadings[filteredReadings.length-1]?.timestamp).toLocaleDateString('it-IT') }} / 
          {{ new Date(filteredReadings[0]?.timestamp).toLocaleDateString('it-IT') }}
        </p>
        <div class="preview-stats mb-sm">
          <span><strong>{{ stats.avgSystolic }}/{{ stats.avgDiastolic }}</strong> mmHg media</span>
          <span><strong>{{ stats.avgHeartRate }}</strong> BPM medio</span>
          <span><strong>{{ htnLoad.percentage }}%</strong> carico ipertensivo</span>
        </div>
        <table v-if="includeHistory" class="preview-table">
          <thead><tr><th>Data</th><th>SYS</th><th>DIA</th><th>BPM</th><th>Categoria</th></tr></thead>
          <tbody>
            <tr v-for="r in filteredReadings.slice(0, 20)" :key="r.id">
              <td>{{ new Date(r.timestamp).toLocaleDateString('it-IT') }}</td>
              <td>{{ r.systolic }}</td>
              <td>{{ r.diastolic }}</td>
              <td>{{ r.heartRate }}</td>
              <td><small>{{ r.category }}</small></td>
            </tr>
          </tbody>
        </table>
        <p v-if="filteredReadings.length > 20" class="text-secondary mt-sm" style="font-size:0.75rem">
          ...e altre {{ filteredReadings.length - 20 }} misurazioni
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
.preview-table th, .preview-table td { padding: 4px 8px; text-align: left; border-bottom: 1px solid var(--color-border); }
.preview-table th { color: var(--color-text-secondary); font-weight: 600; }
.share-link-box { display: flex; align-items: center; gap: var(--space-sm); background: var(--color-surface-overlay); padding: var(--space-sm) var(--space-md); border-radius: var(--radius-sm); }
.share-link-box code { font-size: 0.75rem; word-break: break-all; flex: 1; }
.form-success { color: var(--color-accent); font-size: 0.8125rem; font-weight: 500; }
</style>

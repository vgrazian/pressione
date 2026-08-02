<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics, computeMorningSurge, computeHypertensiveLoad } from '@/services/statistics.js'
import { supabase } from '@/services/supabaseClient.js'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import AppIcon from '@/components/AppIcon.vue'
import { jsPDF } from 'jspdf'

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
const linkMessage = ref('')
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

// --- PDF Generation ---
async function generatePDF() {
  generatingPDF.value = true
  const doc = new jsPDF({ unit: 'mm', format: 'a4' })
  const data = filteredReadings.value
  let y = 20
 
  // Header
  doc.setFontSize(16)
  doc.setTextColor(0, 108, 76)
  doc.text('Report Pressione Arteriosa' + titleSuffix.value, 20, y)
  y += 10
  doc.setFontSize(10)
  doc.setTextColor(100, 100, 100)
  const from = data.length ? new Date(data[data.length-1].timestamp).toLocaleDateString('it-IT') : 'N/D'
  const to = data.length ? new Date(data[0].timestamp).toLocaleDateString('it-IT') : 'N/D'
  doc.text(`Periodo: ${from} - ${to}  |  Misurazioni: ${data.length}`, 20, y)
  y += 12

  // Stats
  doc.setFontSize(12)
  doc.setTextColor(0, 0, 0)
  doc.text('Riepilogo', 20, y)
  y += 7
  doc.setFontSize(10)
  const s = stats.value
  doc.text(`Media: ${s.avgSystolic}/${s.avgDiastolic} mmHg  |  BPM medio: ${s.avgHeartRate}`, 20, y)
  y += 6
  doc.text(`Intervallo: ${s.minSystolic}-${s.maxSystolic} / ${s.minDiastolic}-${s.maxDiastolic} mmHg`, 20, y)
  y += 10

  if (htnLoad.value) {
    doc.text(`Carico Ipertensivo: ${htnLoad.value.percentage}% (${htnLoad.value.abnormal} fuori norma su ${htnLoad.value.total})`, 20, y)
    y += 8
  }

  // History table
  if (includeHistory.value && data.length > 0) {
    y += 4
    doc.setFontSize(11)
    doc.text('Storico Misurazioni', 20, y)
    y += 7
    doc.setFontSize(8)
    // Table header
    doc.setFillColor(240, 240, 240)
    doc.rect(20, y, 170, 6, 'F')
    doc.text('Data', 22, y + 4)
    doc.text('Ora', 45, y + 4)
    doc.text('Sistolica', 65, y + 4)
    doc.text('Diastolica', 85, y + 4)
    doc.text('BPM', 105, y + 4)
    doc.text('Categoria', 125, y + 4)
    doc.text('Note', 155, y + 4)
    y += 7

    for (const r of data) {
      if (y > 270) { doc.addPage(); y = 20 }
      const d = new Date(r.timestamp)
      doc.text(d.toLocaleDateString('it-IT'), 22, y + 4)
      doc.text(d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }), 45, y + 4)
      doc.text(String(r.systolic), 65, y + 4)
      doc.text(String(r.diastolic), 85, y + 4)
      doc.text(String(r.heartRate), 105, y + 4)
      doc.text(r.category || '', 125, y + 4)
      doc.text((r.notes || '').slice(0, 25), 155, y + 4)
      y += 5
    }
  }

  // Footer
  doc.setFontSize(7)
  doc.setTextColor(150, 150, 150)
  doc.text(`Generato il ${new Date().toLocaleDateString('it-IT')} — Pressione App`, 20, 285)

  doc.save(`pressione_report_${new Date().toISOString().slice(0,10)}.pdf`)
  generatingPDF.value = false
}

// --- Sharing ---
function sharePDF() {
  generatePDF().then(() => {
    // After download, no further action needed
  })
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
async function generateShareLink() {
  linkMessage.value = ''
  const data = filteredReadings.value
  if (!data.length) { linkMessage.value = 'Nessun dato da condividere'; return }
  try {
    const reportData = {
      stats: stats.value,
      readings: data.slice(0, 100).map(r => ({
        systolic: r.systolic, diastolic: r.diastolic, heartRate: r.heartRate,
        timestamp: r.timestamp, notes: r.notes, category: r.category
      }))
    }
    const pin = sharePin.value || null
    const { data: result, error } = await supabase.rpc('create_share_link', {
      p_username: user.value.username,
      p_report_data: reportData,
      p_pin: pin || null
    })
    if (error) throw error
    shareLink.value = `https://vgrazian.github.io/pressione/#/share/${result.token}`
    if (pin) shareLink.value += `?pin=${pin}`
    linkMessage.value = 'Link generato! Valido per 48 ore.'
  } catch (e) {
    linkMessage.value = 'Errore: ' + e.message
  }
}

function copyLink() {
  if (shareLink.value) {
    navigator.clipboard.writeText(shareLink.value)
    linkMessage.value = 'Link copiato!'
    setTimeout(() => linkMessage.value = '', 3000)
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
          Genera un link web che il medico può aprire dal browser. Si autodistrugge dopo 48 ore.
        </p>
        <div class="flex gap-sm mb-sm">
          <input v-model="sharePin" type="text" class="form-input" placeholder="PIN 4 cifre (opzionale)" maxlength="4" style="width:160px" />
          <button class="btn btn-primary btn-sm" @click="generateShareLink">Genera Link</button>
        </div>
        <div v-if="shareLink" class="share-link-box">
          <code>{{ shareLink }}</code>
          <button class="btn btn-sm btn-ghost" @click="copyLink">Copia</button>
        </div>
        <div v-if="linkMessage" class="form-success mt-sm">{{ linkMessage }}</div>
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

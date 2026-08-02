<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getReadings, refreshFromServer } from '@/services/dataService.js'
import { computeStatistics } from '@/services/statistics.js'
import { getCategoryLabel } from '@/services/categories.js'

const { user } = useAuth()

const readings = ref([])
const stats = ref(null)
const isLoading = ref(true)
const dateRange = ref('all')

const dateRangeOptions = [
  { value: '7d', label: 'Ultimi 7 giorni' },
  { value: '30d', label: 'Ultimi 30 giorni' },
  { value: '90d', label: 'Ultimi 3 mesi' },
  { value: 'all', label: 'Tutto' }
]

onMounted(async () => {
  await loadData()
})

async function loadData() {
  isLoading.value = true
  try {
    await refreshFromServer(user.value.username)
    readings.value = await getReadings(user.value.username)
    stats.value = computeStatistics(readings.value)
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
  return result
})

const filteredStats = computed(() => computeStatistics(filteredReadings.value))

const reportText = computed(() => {
  const s = filteredStats.value
  const from = filteredReadings.value.length > 0
    ? new Date(filteredReadings.value[filteredReadings.value.length - 1].timestamp).toLocaleDateString('it-IT')
    : 'N/D'
  const to = filteredReadings.value.length > 0
    ? new Date(filteredReadings.value[0].timestamp).toLocaleDateString('it-IT')
    : 'N/D'

  let text = `📊 REPORT PRESSIONE ARTERIOSA\n`
  text += `Utente: ${user.value.username}\n`
  text += `Periodo: ${from} - ${to}\n`
  text += `Misurazioni: ${s.readingsCount}\n\n`
  text += `--- MEDIE ---\n`
  text += `Sistolica: ${s.avgSystolic} mmHg\n`
  text += `Diastolica: ${s.avgDiastolic} mmHg\n`
  text += `Freq. Cardiaca: ${s.avgHeartRate} BPM\n\n`
  text += `--- INTERVALLI ---\n`
  text += `Sistolica: ${s.minSystolic} - ${s.maxSystolic} mmHg\n`
  text += `Diastolica: ${s.minDiastolic} - ${s.maxDiastolic} mmHg\n`
  text += `Freq. Cardiaca: ${s.minHeartRate} - ${s.maxHeartRate} BPM\n\n`
  text += `--- CATEGORIE ---\n`
  for (const [cat, count] of Object.entries(s.categoryDistribution || {})) {
    const pct = ((count / s.readingsCount) * 100).toFixed(1)
    text += `${getCategoryLabel(cat)}: ${count} (${pct}%)\n`
  }
  text += `\n--- DISTRIBUZIONE ORARIA ---\n`
  for (const [tod, count] of Object.entries(s.timeOfDayDistribution || {})) {
    text += `${tod}: ${count}\n`
  }
  return text
})

async function copyToClipboard() {
  try {
    await navigator.clipboard.writeText(reportText.value)
    alert('Report copiato negli appunti!')
  } catch {
    // Fallback
    const textarea = document.createElement('textarea')
    textarea.value = reportText.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    alert('Report copiato negli appunti!')
  }
}

async function shareReport() {
  if (navigator.share) {
    try {
      await navigator.share({ text: reportText.value, title: 'Report Pressione' })
    } catch { /* user cancelled */ }
  } else {
    await copyToClipboard()
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Report</h1>
    </div>

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
      <div class="card mb-md">
        <pre class="report-text">{{ reportText }}</pre>
      </div>

      <div class="flex gap-sm">
        <button class="btn btn-primary" @click="copyToClipboard">📋 Copia</button>
        <button class="btn btn-outline" @click="shareReport">📤 Condividi</button>
      </div>
    </template>

    <div v-else class="empty-state">
      <span class="empty-state__icon">📄</span>
      <h3>Nessun dato disponibile</h3>
      <p>Aggiungi misurazioni per generare un report.</p>
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

.report-text {
  font-family: 'SF Mono', 'Menlo', monospace;
  font-size: 0.8125rem;
  white-space: pre-wrap;
  line-height: 1.6;
  color: var(--color-on-surface);
}
</style>

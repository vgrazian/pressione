<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { upsertReading, getReadingById, getReadings } from '@/services/dataService.js'
import { classifyReading, getCategoryColor, getCategoryLabel } from '@/services/categories.js'

const route = useRoute()
const router = useRouter()
const { user } = useAuth()

const isEdit = computed(() => !!route.params.id)

const systolic = ref('')
const diastolic = ref('')
const heartRate = ref('')
const date = ref('')
const time = ref('')
const notes = ref('')
const errorMessage = ref('')
const isSaving = ref(false)

// Computed category for live preview
const previewCategory = computed(() => {
  const sys = parseInt(systolic.value)
  const dia = parseInt(diastolic.value)
  if (!sys || !dia) return null
  return classifyReading(sys, dia)
})

const previewColor = computed(() => {
  if (!previewCategory.value) return null
  return getCategoryColor(previewCategory.value)
})

const previewLabel = computed(() => {
  if (!previewCategory.value) return null
  return getCategoryLabel(previewCategory.value)
})

onMounted(async () => {
  const now = new Date()
  date.value = now.toISOString().split('T')[0]
  time.value = now.toTimeString().slice(0, 5)

  if (isEdit.value) {
    const reading = await getReadingById(route.params.id)
    if (reading) {
      systolic.value = String(reading.systolic)
      diastolic.value = String(reading.diastolic)
      heartRate.value = String(reading.heartRate)
      notes.value = reading.notes || ''
      const ts = new Date(reading.timestamp)
      date.value = ts.toISOString().split('T')[0]
      time.value = ts.toTimeString().slice(0, 5)
    }
  }
})

function validate() {
  const sys = parseInt(systolic.value)
  const dia = parseInt(diastolic.value)
  const hr = parseInt(heartRate.value)

  if (!sys || sys < 1 || sys > 300) return 'Sistolica non valida (1-300 mmHg)'
  if (!dia || dia < 1 || dia > 200) return 'Diastolica non valida (1-200 mmHg)'
  if (!hr || hr < 1 || hr > 300) return 'Frequenza cardiaca non valida (1-300 BPM)'
  if (dia >= sys) return 'La diastolica deve essere inferiore alla sistolica'
  return null
}

async function checkDuplicate(timestamp, sys, dia, hr) {
  if (isEdit.value) return false
  const recent = await getReadings(user.value.username, { limit: 20 })
  const tenMinAgo = new Date(timestamp).getTime() - 10 * 60 * 1000
  const tenMinAfter = new Date(timestamp).getTime() + 10 * 60 * 1000

  return recent.some(r => {
    const rt = new Date(r.timestamp).getTime()
    return rt >= tenMinAgo && rt <= tenMinAfter &&
      r.systolic === sys && r.diastolic === dia && r.heartRate === hr
  })
}

async function handleSave() {
  errorMessage.value = ''
  const validationError = validate()
  if (validationError) {
    errorMessage.value = validationError
    return
  }

  isSaving.value = true
  try {
    const timestamp = new Date(`${date.value}T${time.value}`).toISOString()
    const sys = parseInt(systolic.value)
    const dia = parseInt(diastolic.value)
    const hr = parseInt(heartRate.value)

    // Duplicate check
    const isDuplicate = await checkDuplicate(timestamp, sys, dia, hr)
    if (isDuplicate) {
      errorMessage.value = 'Hai già inserito una misurazione simile negli ultimi 10 minuti.'
      isSaving.value = false
      return
    }

    const reading = {
      systolic: sys,
      diastolic: dia,
      heartRate: hr,
      timestamp,
      notes: notes.value.trim()
    }

    if (isEdit.value) {
      reading.id = route.params.id
    }

    await upsertReading(reading, user.value.username)
    router.push('/')
  } catch (e) {
    errorMessage.value = e.message || 'Errore nel salvataggio'
  } finally {
    isSaving.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="page">
    <div class="page-header flex items-center gap-md">
      <button class="btn btn-sm btn-outline" @click="goBack">← Indietro</button>
      <h1>{{ isEdit ? 'Modifica' : 'Nuova' }} Misurazione</h1>
    </div>

    <!-- Live Category Preview -->
    <div v-if="previewCategory" class="category-preview card mb-md text-center"
      :style="{ borderColor: previewColor, backgroundColor: previewColor + '10' }">
      <span class="category-preview__label" :style="{ color: previewColor }">
        {{ previewLabel }}
      </span>
    </div>

    <form @submit.prevent="handleSave" class="card">
      <div class="form-row">
        <div class="form-group">
          <label class="form-label" for="systolic">Sistolica (mmHg)</label>
          <input id="systolic" v-model="systolic" type="number" class="form-input"
            placeholder="120" min="1" max="300" inputmode="numeric" />
        </div>
        <div class="form-group">
          <label class="form-label" for="diastolic">Diastolica (mmHg)</label>
          <input id="diastolic" v-model="diastolic" type="number" class="form-input"
            placeholder="80" min="1" max="200" inputmode="numeric" />
        </div>
      </div>

      <div class="form-group">
        <label class="form-label" for="heartRate">Frequenza Cardiaca (BPM)</label>
        <input id="heartRate" v-model="heartRate" type="number" class="form-input"
          placeholder="72" min="1" max="300" inputmode="numeric" />
      </div>

      <div class="form-row">
        <div class="form-group">
          <label class="form-label" for="date">Data</label>
          <input id="date" v-model="date" type="date" class="form-input" />
        </div>
        <div class="form-group">
          <label class="form-label" for="time">Ora</label>
          <input id="time" v-model="time" type="time" class="form-input" />
        </div>
      </div>

      <div class="form-group">
        <label class="form-label" for="notes">Note (opzionale)</label>
        <input id="notes" v-model="notes" type="text" class="form-input"
          placeholder="Es. dopo attività fisica, a riposo..." maxlength="500" />
      </div>

      <div v-if="errorMessage" class="form-error mb-md">{{ errorMessage }}</div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" @click="goBack">Annulla</button>
        <button type="submit" class="btn btn-primary" :disabled="isSaving">
          {{ isSaving ? 'Salvataggio...' : 'Salva' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.category-preview {
  border: 2px solid;
  padding: var(--space-md);
}

.category-preview__label {
  font-size: 1.125rem;
  font-weight: 600;
}

.form-actions {
  display: flex;
  gap: var(--space-sm);
  justify-content: flex-end;
  margin-top: var(--space-md);
}

@media (max-width: 480px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>

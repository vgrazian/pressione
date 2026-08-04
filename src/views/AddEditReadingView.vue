<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { upsertReading, getReadingById, getReadings } from '@/services/dataService.js'
import { classifyReading, getCategoryColor, getCategoryLabel } from '@/services/categories.js'
import Breadcrumbs from '@/components/Breadcrumbs.vue'

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
    <!-- Breadcrumbs -->
    <Breadcrumbs :items="[
      { label: 'Home', to: '/' },
      { label: isEdit ? 'Modifica Misurazione' : 'Nuova Misurazione' }
    ]" />

    <div class="page-header flex items-center gap-md">
      <button class="btn btn-sm btn-outline back-btn" @click="goBack" aria-label="Indietro">
        ←
      </button>
      <h1>{{ isEdit ? 'Modifica' : 'Nuova' }} Misurazione</h1>
    </div>

    <!-- Live Category Preview -->
    <div v-if="previewCategory" class="category-preview card mb-md text-center"
      :style="{ borderColor: previewColor, backgroundColor: previewColor + '10' }">
      <span class="category-preview__label" :style="{ color: previewColor }">
        {{ previewLabel }}
      </span>
    </div>

    <form @submit.prevent="handleSave" class="card add-edit-form">
      <!-- Main values row: SYS / DIA / BPM -->
      <div class="vitals-row">
        <div class="form-group">
          <label class="form-label" for="systolic">Sistolica</label>
          <input id="systolic" v-model="systolic" type="number" class="form-input form-input--lg"
            placeholder="120" min="1" max="300" inputmode="numeric" />
          <span class="form-unit">mmHg</span>
        </div>
        <div class="form-group">
          <label class="form-label" for="diastolic">Diastolica</label>
          <input id="diastolic" v-model="diastolic" type="number" class="form-input form-input--lg"
            placeholder="80" min="1" max="200" inputmode="numeric" />
          <span class="form-unit">mmHg</span>
        </div>
        <div class="form-group">
          <label class="form-label" for="heartRate">BPM</label>
          <input id="heartRate" v-model="heartRate" type="number" class="form-input form-input--lg"
            placeholder="72" min="1" max="300" inputmode="numeric" />
          <span class="form-unit">bpm</span>
        </div>
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
        <label class="form-label" for="notes">Note</label>
        <input id="notes" v-model="notes" type="text" class="form-input"
          placeholder="Es. dopo attività fisica, a riposo..." maxlength="500" />
      </div>

      <div v-if="errorMessage" class="form-error mb-md">{{ errorMessage }}</div>

      <!-- Action buttons -->
      <div class="form-actions">
        <button type="button" class="btn btn-error" @click="goBack">
          ✕ Annulla
        </button>
        <button type="submit" class="btn btn-primary btn--full-mobile" :disabled="isSaving">
          {{ isSaving ? 'Salvataggio...' : '✓ Salva' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
/* Vitals row: SYS / DIA / BPM in a compact row */
.vitals-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.form-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-md);
  align-items: start;
}

.form-row .form-group {
  margin-bottom: 0;
  min-width: 0;
  overflow: hidden;
}

.form-row .form-input {
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
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
  justify-content: space-between;
  margin-top: var(--space-lg);
}

.form-unit {
  display: block;
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.form-input--lg {
  font-size: 1.25rem;
  font-weight: 600;
  text-align: center;
  padding: var(--space-sm) var(--space-xs);
  height: auto;
}

/* Back button */
.back-btn {
  min-width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  border-radius: var(--radius-sm);
}

/* Mobile optimization */
@media (max-width: 480px) {
  .add-edit-form {
    padding: var(--space-md);
  }

  .vitals-row {
    gap: 6px;
  }

  .vitals-row .form-label {
    font-size: 0.6875rem;
  }

  .vitals-row .form-input--lg {
    font-size: 1.125rem;
    padding: 6px 4px;
  }

  .form-row {
    grid-template-columns: 1fr 1fr;
    gap: var(--space-sm);
  }

  .form-actions {
    position: sticky;
    bottom: 0;
    background: var(--color-surface);
    padding: var(--space-sm) 0;
    margin: var(--space-md) calc(-1 * var(--space-md)) calc(-1 * var(--space-md));
    padding-left: var(--space-md);
    padding-right: var(--space-md);
    border-top: 1px solid var(--color-border);
    z-index: 5;
  }

  .btn--full-mobile {
    flex: 1;
  }

  .page-header h1 {
    font-size: 1.0625rem;
  }
}

/* Very small screens (iPhone SE) */
@media (max-width: 375px) {
  .vitals-row .form-input--lg {
    font-size: 1rem;
    padding: 4px 2px;
  }

  .vitals-row {
    gap: 4px;
  }

  .form-label {
    font-size: 0.75rem;
  }
}
</style>

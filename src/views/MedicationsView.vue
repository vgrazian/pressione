<script setup>
import { ref, computed, onMounted, inject, reactive } from 'vue'
import { useAuth } from '@/services/auth.js'
import { getMedications, upsertMedication, stopMedication, deleteMedication } from '@/services/dataService.js'
import { useI18n } from '@/services/i18n.js'
import AppIcon from '@/components/AppIcon.vue'
import SkeletonLoader from '@/components/SkeletonLoader.vue'

const { user } = useAuth()
const { t } = useI18n()
const confirm = inject('confirm-dialog', null)

const medications = ref([])
const isLoading = ref(true)
const showMedForm = ref(false)
const editingMedication = ref(null)
const medMessage = ref('')
const medForm = reactive({ name: '', activeIngredient: '', dosage: '', frequency: '', notes: '', startDate: '', endDate: '', stillTaking: true })

const activeMeds = computed(() => medications.value.filter(m => !m.endDate))
const historicalMeds = computed(() => medications.value.filter(m => m.endDate))

onMounted(async () => {
  await loadMedications()
})

async function loadMedications() {
  isLoading.value = true
  try {
    medications.value = await getMedications(user.value.username)
  } catch {
    medications.value = []
  } finally {
    isLoading.value = false
  }
}

function openAddMedication() {
  editingMedication.value = null
  medForm.name = ''
  medForm.activeIngredient = ''
  medForm.dosage = ''
  medForm.frequency = ''
  medForm.notes = ''
  medForm.startDate = new Date().toISOString().slice(0, 10)
  medForm.endDate = ''
  medForm.stillTaking = true
  medMessage.value = ''
  showMedForm.value = true
}

function openEditMedication(med) {
  editingMedication.value = med
  medForm.name = med.name
  medForm.activeIngredient = med.activeIngredient || ''
  medForm.dosage = med.dosage || ''
  medForm.frequency = med.frequency || ''
  medForm.notes = med.notes || ''
  medForm.startDate = (med.startDate || '').slice(0, 10)
  medForm.endDate = med.endDate ? med.endDate.slice(0, 10) : ''
  medForm.stillTaking = !med.endDate
  medMessage.value = ''
  showMedForm.value = true
}

function cancelMedForm() {
  showMedForm.value = false
  editingMedication.value = null
  medMessage.value = ''
}

async function saveMedication() {
  medMessage.value = ''
  if (!medForm.name.trim()) { medMessage.value = 'Inserisci il nome del farmaco'; return }
  const payload = {
    id: editingMedication.value?.id || null,
    name: medForm.name.trim(),
    activeIngredient: medForm.activeIngredient.trim(),
    dosage: medForm.dosage.trim(),
    frequency: medForm.frequency.trim(),
    notes: medForm.notes.trim(),
    startDate: medForm.startDate ? new Date(medForm.startDate + 'T00:00:00').toISOString() : new Date().toISOString(),
    endDate: !medForm.stillTaking && medForm.endDate ? new Date(medForm.endDate + 'T00:00:00').toISOString() : null
  }
  await upsertMedication(payload, user.value.username)
  await loadMedications()
  cancelMedForm()
}

async function handleStopMedication(med) {
  await stopMedication(med.id, user.value.username)
  await loadMedications()
}

async function handleDeleteMedication(med) {
  let ok = true
  if (confirm) {
    ok = await confirm({ title: t('remove') + ' ' + t('medications').toLowerCase(), message: `"${med.name}"?`, confirmText: t('remove'), variant: 'danger' })
  } else {
    ok = window.confirm(`Eliminare "${med.name}"?`)
  }
  if (!ok) return
  await deleteMedication(med.id, user.value.username)
  await loadMedications()
}

function formatMedDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('it-IT')
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>💊 {{ t('medications') }}</h1>
      <button class="btn btn-primary btn-sm" @click="openAddMedication">
        <AppIcon name="plus" :size="16" /> {{ t('add_medication') }}
      </button>
    </div>

    <div v-if="isLoading" class="card mb-md"><SkeletonLoader /></div>

    <template v-else>
      <!-- Add / edit form -->
      <div v-if="showMedForm" class="card mb-md" style="border-color: var(--color-accent);">
        <h3 class="mb-sm">{{ editingMedication ? t('medication_edit') : t('medication_new') }}</h3>
        <div class="form-group"><label class="form-label">{{ t('medication_name') }}</label><input v-model="medForm.name" type="text" class="form-input" autocomplete="off" /></div>
        <div class="form-group"><label class="form-label">{{ t('medication_ingredient') }}</label><input v-model="medForm.activeIngredient" type="text" class="form-input" autocomplete="off" /></div>
        <div class="flex gap-sm">
          <div class="form-group" style="flex:1"><label class="form-label">{{ t('medication_dosage') }}</label><input v-model="medForm.dosage" type="text" class="form-input" placeholder="50 mg" /></div>
          <div class="form-group" style="flex:1"><label class="form-label">{{ t('medication_frequency') }}</label><input v-model="medForm.frequency" type="text" class="form-input" placeholder="1 al giorno" /></div>
        </div>
        <div class="form-group"><label class="form-label">{{ t('notes_optional') }}</label><input v-model="medForm.notes" type="text" class="form-input" /></div>
        <div class="flex gap-sm items-center">
          <div class="form-group" style="margin-bottom:0"><label class="form-label">{{ t('medication_start') }}</label><input v-model="medForm.startDate" type="date" class="form-input" /></div>
          <label class="flex items-center gap-sm" style="font-size:0.875rem;margin-top:auto">
            <input v-model="medForm.stillTaking" type="checkbox" /> {{ t('medication_still_taking') }}
          </label>
        </div>
        <div v-if="!medForm.stillTaking" class="form-group"><label class="form-label">{{ t('medication_end') }}</label><input v-model="medForm.endDate" type="date" class="form-input" /></div>
        <div v-if="medMessage" class="form-error mb-sm">{{ medMessage }}</div>
        <div class="flex gap-sm">
          <button class="btn btn-sm btn-primary" @click="saveMedication">{{ t('save') }}</button>
          <button class="btn btn-sm btn-ghost" @click="cancelMedForm">{{ t('cancel') }}</button>
        </div>
      </div>

      <div v-if="medications.length === 0 && !showMedForm" class="card text-center p-lg">
        <p class="text-secondary mb-sm">💊</p>
        <p class="text-secondary">{{ t('no_medications') }}</p>
        <button class="btn btn-primary btn-sm mt-sm" @click="openAddMedication">{{ t('add_medication') }}</button>
      </div>

      <!-- Active medications -->
      <section v-if="activeMeds.length" class="mb-md">
        <h3 class="mb-sm">{{ t('medications_active') }}</h3>
        <div v-for="med in activeMeds" :key="med.id" class="card card--flat mb-sm">
          <div class="flex items-center" style="justify-content:space-between">
            <div>
              <strong>{{ med.name }}</strong>
              <span v-if="med.dosage" class="text-secondary"> · {{ med.dosage }}</span>
              <span v-if="med.frequency" class="text-secondary"> · {{ med.frequency }}</span>
              <div v-if="med.activeIngredient" class="text-secondary" style="font-size:0.75rem">{{ med.activeIngredient }}</div>
            </div>
            <span class="chip chip--active">{{ t('medication_ongoing') }}</span>
          </div>
          <div class="text-secondary" style="font-size:0.75rem;margin-top:4px">
            {{ t('medication_start') }}: {{ formatMedDate(med.startDate) }}
          </div>
          <div v-if="med.notes" class="text-secondary" style="font-size:0.75rem;margin-top:2px">{{ med.notes }}</div>
          <div class="flex gap-sm" style="margin-top:8px">
            <button class="btn btn-sm btn-ghost" @click="openEditMedication(med)">{{ t('edit') }}</button>
            <button class="btn btn-sm btn-ghost" @click="handleStopMedication(med)">{{ t('medication_stop') }}</button>
            <button class="btn btn-sm btn-ghost-error btn-icon" @click="handleDeleteMedication(med)" :title="t('remove')">
              <AppIcon name="trash" :size="16" color="currentColor" />
            </button>
          </div>
        </div>
      </section>

      <!-- Historical medications -->
      <section v-if="historicalMeds.length">
        <h3 class="mb-sm">{{ t('medications_history') }}</h3>
        <div v-for="med in historicalMeds" :key="med.id" class="card card--flat mb-sm">
          <div class="flex items-center" style="justify-content:space-between">
            <div>
              <strong>{{ med.name }}</strong>
              <span v-if="med.dosage" class="text-secondary"> · {{ med.dosage }}</span>
              <span v-if="med.frequency" class="text-secondary"> · {{ med.frequency }}</span>
              <div v-if="med.activeIngredient" class="text-secondary" style="font-size:0.75rem">{{ med.activeIngredient }}</div>
            </div>
          </div>
          <div class="text-secondary" style="font-size:0.75rem;margin-top:4px">
            {{ t('medication_start') }}: {{ formatMedDate(med.startDate) }} — {{ t('medication_end') }}: {{ formatMedDate(med.endDate) }}
          </div>
          <div v-if="med.notes" class="text-secondary" style="font-size:0.75rem;margin-top:2px">{{ med.notes }}</div>
          <div class="flex gap-sm" style="margin-top:8px">
            <button class="btn btn-sm btn-ghost" @click="openEditMedication(med)">{{ t('edit') }}</button>
            <button class="btn btn-sm btn-ghost-error btn-icon" @click="handleDeleteMedication(med)" :title="t('remove')">
              <AppIcon name="trash" :size="16" color="currentColor" />
            </button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

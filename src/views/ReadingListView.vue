<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { getReadings, deleteReading, refreshFromServer } from '@/services/dataService.js'
import { ALL_CATEGORIES, getCategoryLabel } from '@/services/categories.js'
import ReadingCard from '@/components/ReadingCard.vue'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const { user } = useAuth()

const readings = ref([])
const isLoading = ref(true)
const searchQuery = ref('')
const categoryFilter = ref('')
const confirmDialog = ref(null)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  isLoading.value = true
  try {
    await refreshFromServer(user.value.username)
    const all = await getReadings(user.value.username)
    readings.value = all
  } catch (e) {
    console.error('Load error:', e)
  } finally {
    isLoading.value = false
  }
}

function editReading(reading) {
  router.push(`/edit/${reading.id}`)
}

async function handleDelete(reading) {
  const confirmed = await confirmDialog.value?.show({
    title: 'Elimina misurazione',
    message: `Eliminare la misurazione di ${new Date(reading.timestamp).toLocaleDateString('it-IT')}?`,
    confirmText: 'Elimina',
    variant: 'danger'
  })
  if (confirmed) {
    await deleteReading(reading.id, user.value.username)
    readings.value = readings.value.filter(r => r.id !== reading.id)
  }
}

function goToAdd() {
  router.push('/add')
}
</script>

<template>
  <div class="page">
    <div class="page-header flex justify-between items-center">
      <h1>Tutte le Misurazioni</h1>
      <button class="btn btn-primary btn-sm" @click="goToAdd">+ Nuova</button>
    </div>

    <!-- Search & Filters -->
    <div class="filters mb-md">
      <input v-model="searchQuery" type="search" class="form-input"
        placeholder="Cerca per note o valori..." />
      <div class="filter-chips flex gap-sm" style="flex-wrap: wrap; margin-top: var(--space-sm);">
        <button
          class="chip"
          :class="{ 'chip--active': !categoryFilter }"
          @click="categoryFilter = ''"
        >Tutte</button>
        <button
          v-for="cat in ALL_CATEGORIES"
          :key="cat"
          class="chip"
          :class="{ 'chip--active': categoryFilter === cat }"
          @click="categoryFilter = categoryFilter === cat ? '' : cat"
        >{{ getCategoryLabel(cat) }}</button>
      </div>
    </div>

    <!-- Readings List -->
    <div v-if="isLoading" class="empty-state">
      <p>Caricamento...</p>
    </div>

    <div v-else-if="readings.length === 0" class="empty-state">
      <AppIcon name="list" :size="48" color="var(--color-text-tertiary)" class="empty-state__icon" />
      <h3>Nessuna misurazione</h3>
      <p>Aggiungi la tua prima misurazione di pressione.</p>
      <button class="btn btn-primary mt-md" @click="goToAdd">Aggiungi Misurazione</button>
    </div>

    <div v-else class="readings-list flex flex-col gap-sm">
      <ReadingCard
        v-for="reading in readings"
        :key="reading.id"
        :reading="reading"
        @edit="editReading"
        @delete="handleDelete"
      />
    </div>
  </div>
</template>

<style scoped>
.filters {
  position: sticky;
  top: 0;
  z-index: 5;
  background: var(--color-surface);
  padding-bottom: var(--space-sm);
}

.filter-chips {
  overflow-x: auto;
}

.chip {
  background: var(--color-surface-overlay);
  color: var(--color-text-primary);
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
}

.chip--active {
  background: var(--color-accent-muted);
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.chip:active {
  opacity: 0.7;
}
</style>

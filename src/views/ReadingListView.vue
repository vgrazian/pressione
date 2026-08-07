<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { getReadings, deleteReading, refreshFromServer } from '@/services/dataService.js'
import { ALL_CATEGORIES, getCategoryLabel } from '@/services/categories.js'
import ReadingCard from '@/components/ReadingCard.vue'
import AppIcon from '@/components/AppIcon.vue'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import CollapsibleSection from '@/components/CollapsibleSection.vue'

const router = useRouter()
const { user } = useAuth()

const confirm = inject('confirm-dialog', null)
const allReadings = ref([])
const isLoading = ref(true)
const searchQuery = ref('')
const categoryFilter = ref('')
const dateFilter = ref('all')
const customFrom = ref('')
const customTo = ref('')

const filteredReadings = computed(() => {
  let result = allReadings.value
  // Date filter
  const df = dateFilter.value
  if (df === '7') {
    const cutoff = new Date(Date.now() - 7 * 86400000)
    result = result.filter(r => new Date(r.timestamp) >= cutoff)
  } else if (df === '30') {
    const cutoff = new Date(Date.now() - 30 * 86400000)
    result = result.filter(r => new Date(r.timestamp) >= cutoff)
  } else if (df === 'custom' && customFrom.value && customTo.value) {
    result = result.filter(r => {
      const t = new Date(r.timestamp)
      return t >= new Date(customFrom.value) && t <= new Date(customTo.value + 'T23:59:59')
    })
  }
  // Category filter
  if (categoryFilter.value) {
    result = result.filter(r => r.category === categoryFilter.value)
  }
  // Search
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(r =>
      (r.notes || '').toLowerCase().includes(q) ||
      String(r.systolic).includes(q) || String(r.diastolic).includes(q) || String(r.heartRate).includes(q)
    )
  }
  return result
})

function setDateFilter(val) {
  dateFilter.value = val
}

onMounted(async () => {
  await loadData()
})

async function loadData() {
  isLoading.value = true
  try {
    await refreshFromServer(user.value.username)
    allReadings.value = await getReadings(user.value.username)
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
  if (!confirm) {
    // Fallback: use native confirm
    if (!window.confirm(`Eliminare la misurazione di ${new Date(reading.timestamp).toLocaleDateString('it-IT')}?`)) return
  } else {
    const confirmed = await confirm({
      title: 'Elimina misurazione',
      message: `Eliminare la misurazione di ${new Date(reading.timestamp).toLocaleDateString('it-IT')}?`,
      confirmText: 'Elimina',
      variant: 'danger'
    })
    if (!confirmed) return
  }
  await deleteReading(reading.id, user.value.username)
  allReadings.value = allReadings.value.filter(r => r.id !== reading.id)
}

function goToAdd() {
  router.push('/add')
}

// --- Swipe-to-delete ---
const swipeState = ref({}) // { [id]: { deltaX: number } }

function onTouchStart(id, e) {
  swipeState.value[id] = { startX: e.touches[0].clientX, deltaX: 0 }
}
function onTouchMove(id, e) {
  const s = swipeState.value[id]
  if (!s) return
  s.deltaX = Math.min(0, e.touches[0].clientX - s.startX)
}
function onTouchEnd(id, reading) {
  const s = swipeState.value[id]
  if (!s) return
  if (s.deltaX < -80) {
    handleDelete(reading)
  }
  delete swipeState.value[id]
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
      <input v-model="searchQuery" type="search" class="form-input" aria-label="Cerca misurazioni"
        placeholder="Cerca per note o valori..." />
      <CollapsibleSection title="Filtri" class="mt-sm">
        <div class="flex gap-sm flex-wrap">
          <button v-for="p in [{v:'all',l:'Tutte'},{v:'7',l:'7gg'},{v:'30',l:'30gg'}]" :key="p.v"
            class="chip" :class="{ 'chip--active': dateFilter === p.v }" @click="setDateFilter(p.v)">{{ p.l }}</button>
        </div>
        <div class="filter-chips flex gap-sm mt-sm" style="flex-wrap: wrap;">
          <button class="chip" :class="{ 'chip--active': !categoryFilter }" @click="categoryFilter = ''">Tutte</button>
          <button v-for="cat in ALL_CATEGORIES" :key="cat" class="chip"
            :class="{ 'chip--active': categoryFilter === cat }"
            @click="categoryFilter = categoryFilter === cat ? '' : cat">{{ getCategoryLabel(cat) }}</button>
        </div>
      </CollapsibleSection>
    </div>

    <!-- Readings List -->
    <div v-if="isLoading" class="p-lg">
      <SkeletonLoader type="card" :count="5" height="110px" />
    </div>

    <div v-else-if="filteredReadings.length === 0" class="empty-state">
      <AppIcon name="list" :size="48" color="var(--color-text-tertiary)" class="empty-state__icon" />
      <h3>Nessuna misurazione</h3>
      <p>Aggiungi la tua prima misurazione di pressione.</p>
      <button class="btn btn-primary mt-md" @click="goToAdd">Aggiungi Misurazione</button>
    </div>

    <div v-else class="readings-list flex flex-col gap-sm">
      <div v-for="reading in filteredReadings" :key="reading.id"
        class="swipe-container"
        @touchstart="onTouchStart(reading.id, $event)"
        @touchmove="onTouchMove(reading.id, $event)"
        @touchend="onTouchEnd(reading.id, reading)"
        :style="{ transform: swipeState[reading.id] ? 'translateX(' + swipeState[reading.id].deltaX + 'px)' : '' }">
        <ReadingCard :reading="reading" @edit="editReading" @delete="handleDelete" />
      </div>
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

.swipe-container {
  transition: transform 0.1s ease-out;
  touch-action: pan-y;
}
</style>

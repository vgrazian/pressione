<script setup>
import CategoryBadge from './CategoryBadge.vue'
import TimeOfDayIcon from './TimeOfDayIcon.vue'

const props = defineProps({
  reading: { type: Object, required: true },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete'])

function formatDate(ts) {
  const d = new Date(ts)
  return d.toLocaleDateString('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatTime(ts) {
  const d = new Date(ts)
  return d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="reading-card" :class="{ 'reading-card--compact': compact }">
    <div class="reading-card__main">
      <div class="reading-card__values">
        <div class="reading-card__bp">
          <span class="bp-sys">{{ reading.systolic }}</span>
          <span class="bp-sep">/</span>
          <span class="bp-dia">{{ reading.diastolic }}</span>
          <span class="bp-unit">mmHg</span>
        </div>
        <div class="reading-card__hr">
          <span class="hr-icon">❤️</span>
          <span>{{ reading.heartRate }} BPM</span>
        </div>
      </div>
      <CategoryBadge :category="reading.category" :small="compact" />
    </div>

    <div class="reading-card__meta">
      <div class="reading-card__time">
        <TimeOfDayIcon :timestamp="reading.timestamp" />
        <span>{{ formatDate(reading.timestamp) }}</span>
        <span>{{ formatTime(reading.timestamp) }}</span>
      </div>
      <p v-if="reading.notes && !compact" class="reading-card__notes">{{ reading.notes }}</p>
    </div>

    <div v-if="!compact" class="reading-card__actions">
      <button class="btn btn-sm btn-outline" @click="$emit('edit', reading)">Modifica</button>
      <button class="btn btn-sm btn-secondary" @click="$emit('delete', reading)">Elimina</button>
    </div>
  </div>
</template>

<style scoped>
.reading-card {
  background: white;
  border-radius: var(--radius-md);
  padding: var(--space-md);
  box-shadow: var(--elevation-1);
  border-left: 4px solid var(--color-primary);
}

.reading-card--compact {
  padding: var(--space-sm) var(--space-md);
}

.reading-card__main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-sm);
}

.reading-card__bp {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.bp-sys {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-on-surface);
}

.bp-sep {
  font-size: 1.25rem;
  color: var(--color-on-surface-variant);
  margin: 0 2px;
}

.bp-dia {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-on-surface);
}

.bp-unit {
  font-size: 0.75rem;
  color: var(--color-on-surface-variant);
  margin-left: 4px;
}

.reading-card__hr {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.875rem;
  color: var(--color-on-surface-variant);
  margin-top: 2px;
}

.hr-icon {
  font-size: 0.75rem;
}

.reading-card__meta {
  font-size: 0.8125rem;
  color: var(--color-on-surface-variant);
}

.reading-card__time {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.reading-card__notes {
  margin-top: var(--space-xs);
  font-style: italic;
  color: var(--color-on-surface-variant);
}

.reading-card__actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-surface-container);
}
</style>

<script setup>
defineProps({
  type: { type: String, default: 'text' }, // 'text' | 'card' | 'chart'
  count: { type: Number, default: 3 },
  height: { type: String, default: '' }
})
</script>

<template>
  <!-- Text skeletons -->
  <div v-if="type === 'text'" class="skeleton-list">
    <div v-for="i in count" :key="i" class="skeleton skeleton-text" :style="{ width: (100 - (i - 1) * 15) + '%' }"></div>
  </div>

  <!-- Card skeletons -->
  <div v-if="type === 'card'" class="skeleton-grid">
    <div v-for="i in count" :key="i" class="skeleton skeleton-card" :style="height ? { height } : {}"></div>
  </div>

  <!-- Chart skeleton -->
  <div v-if="type === 'chart'" class="skeleton skeleton-chart"></div>

  <!-- Stat cards -->
  <div v-if="type === 'stats'" class="skeleton-stats">
    <div v-for="i in 4" :key="i" class="skeleton skeleton-stat"></div>
  </div>
</template>

<style scoped>
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: var(--space-md);
}

.skeleton-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.skeleton-chart {
  height: 200px;
  border-radius: var(--radius-md);
  margin-bottom: var(--space-md);
}

.skeleton-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.skeleton-stat {
  height: 80px;
  border-radius: var(--radius-md);
}
</style>

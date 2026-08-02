<script setup>
import { computed } from 'vue'

const props = defineProps({
  timestamp: { type: String, required: true }
})

const timeOfDay = computed(() => {
  const hour = new Date(props.timestamp).getHours()
  if (hour >= 6 && hour < 12) return { icon: '🌅', label: 'Mattina' }
  if (hour >= 12 && hour < 17) return { icon: '☀️', label: 'Pomeriggio' }
  if (hour >= 17 && hour < 22) return { icon: '🌆', label: 'Sera' }
  return { icon: '🌙', label: 'Notte' }
})
</script>

<template>
  <span class="time-icon" :title="timeOfDay.label">{{ timeOfDay.icon }}</span>
</template>

<style scoped>
.time-icon {
  font-size: 0.875rem;
}
</style>

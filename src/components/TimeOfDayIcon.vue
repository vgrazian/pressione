<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  timestamp: { type: String, required: true }
})

const timeOfDay = computed(() => {
  const hour = new Date(props.timestamp).getHours()
  if (hour >= 6 && hour < 12) return { icon: 'sun', label: 'Mattina' }
  if (hour >= 12 && hour < 17) return { icon: 'sun', label: 'Pomeriggio' }
  if (hour >= 17 && hour < 22) return { icon: 'sun', label: 'Sera' }
  return { icon: 'moon', label: 'Notte' }
})
</script>

<template>
  <span class="time-icon" :title="timeOfDay.label">
    <AppIcon :name="timeOfDay.icon" :size="14" />
  </span>
</template>

<style scoped>
.time-icon {
  display: inline-flex;
  align-items: center;
}
</style>

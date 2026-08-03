<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { getDefaultBands, getBandForHour } from '@/services/timeBands.js'

const props = defineProps({
  timestamp: { type: String, required: true },
  bands: { type: Array, default: null }
})

const timeOfDay = computed(() => {
  const hour = new Date(props.timestamp).getHours()
  const bands = props.bands || getDefaultBands()
  const band = getBandForHour(hour, bands)
  const isNight = band.key === 'NIGHT'
  return { icon: isNight ? 'moon' : 'sun', label: band.label }
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

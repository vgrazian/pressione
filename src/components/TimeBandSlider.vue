<script setup>
import { computed } from 'vue'

const props = defineProps({
  bands: { type: Array, required: true }
})

const emit = defineEmits(['update:bands'])

const colors = ['#FFB347', '#87CEEB', '#DDA0DD', '#2C3E50']
const colorLabels = ['☀️', '🌤️', '🌅', '🌙']

const totalSlots = 24

function segmentStyle(band) {
  const start = Math.max(0, band.start)
  const end = Math.min(totalSlots, band.end)
  let width = end - start
  if (width <= 0) {
    // Handle wrap-around (e.g., night 22-6)
    width = (end + totalSlots) - start
  }
  const left = (start / totalSlots) * 100
  const w = (width / totalSlots) * 100
  return { left: left + '%', width: w + '%' }
}

function onDragStart(idx, side, e) {
  e.preventDefault()
  const startX = e.touches ? e.touches[0].clientX : e.clientX
  const bar = e.target.closest('.band-track')
  const barWidth = bar.offsetWidth

  function onMove(ev) {
    const clientX = ev.touches ? ev.touches[0].clientX : ev.clientX
    const dx = clientX - startX
    const hourDelta = Math.round((dx / barWidth) * totalSlots)

    if (hourDelta === 0) return

    const newBands = props.bands.map(b => ({ ...b }))

    if (side === 'end') {
      // Moving this band's end, adjust next band's start
      const newEnd = Math.min(totalSlots, Math.max(0, props.bands[idx].end + hourDelta))
      if (newEnd <= props.bands[idx].start) return
      newBands[idx].end = newEnd
      // Update next band's start (wrap around)
      const nextIdx = (idx + 1) % props.bands.length
      newBands[nextIdx].start = newEnd % totalSlots
    } else {
      // Moving this band's start, adjust previous band's end
      const newStart = Math.min(totalSlots, Math.max(0, props.bands[idx].start + hourDelta))
      if (newStart >= props.bands[idx].end) return
      newBands[idx].start = newStart
      const prevIdx = (idx - 1 + props.bands.length) % props.bands.length
      newBands[prevIdx].end = newStart
    }

    emit('update:bands', newBands)
  }

  function onEnd() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onEnd)
    document.removeEventListener('touchmove', onMove)
    document.removeEventListener('touchend', onEnd)
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onEnd)
  document.addEventListener('touchmove', onMove)
  document.addEventListener('touchend', onEnd)
}

function startHour(band) { return band.start }
function endHour(band) { return band.end }
</script>

<template>
  <div class="band-slider">
    <div class="band-track">
      <!-- Hour markers -->
      <div class="band-track__hours">
        <span v-for="h in [0,6,12,18]" :key="h" class="band-track__hour"
          :style="{ left: (h / totalSlots) * 100 + '%' }">{{ h }}</span>
      </div>
      <!-- Colored segments -->
      <div v-for="(band, i) in bands" :key="band.key" class="band-segment"
        :style="{ ...segmentStyle(band), backgroundColor: colors[i] }">
        <span class="band-segment__label">{{ colorLabels[i] }} {{ band.start }}-{{ band.end }}</span>
      </div>
      <!-- Draggable dividers (between each band) -->
      <div v-for="(band, i) in bands" :key="'div-' + band.key"
        class="band-divider band-divider--end"
        :style="{ left: ((band.end % totalSlots) / totalSlots) * 100 + '%' }"
        @mousedown.prevent="onDragStart(i, 'end', $event)"
        @touchstart.prevent="onDragStart(i, 'end', $event)">
        <div class="band-divider__handle"></div>
      </div>
    </div>
    <div class="band-legend">
      <div v-for="(band, i) in bands" :key="band.key" class="band-legend__item">
        <span class="band-legend__color" :style="{ backgroundColor: colors[i] }"></span>
        <span>{{ band.label }}</span>
        <span class="band-legend__time">{{ band.start }}:00 – {{ band.end }}:00</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.band-slider {
  margin: var(--space-md) 0;
}

.band-track {
  position: relative;
  height: 48px;
  background: var(--color-surface-overlay);
  border-radius: var(--radius-md);
  overflow: visible;
  cursor: default;
  user-select: none;
  margin-bottom: var(--space-md);
}

.band-track__hours {
  position: absolute;
  top: -16px;
  left: 0;
  right: 0;
  height: 16px;
}

.band-track__hour {
  position: absolute;
  transform: translateX(-50%);
  font-size: 0.625rem;
  color: var(--color-text-tertiary);
}

.band-segment {
  position: absolute;
  top: 0;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: left 0.1s, width 0.1s;
  min-width: 0;
  overflow: hidden;
}

.band-segment__label {
  font-size: 0.625rem;
  font-weight: 600;
  color: rgba(255,255,255,0.9);
  white-space: nowrap;
  text-shadow: 0 1px 2px rgba(0,0,0,0.3);
}

.band-divider {
  position: absolute;
  top: -6px;
  width: 28px;
  height: 60px;
  transform: translateX(-50%);
  cursor: col-resize;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.band-divider__handle {
  width: 4px;
  height: 32px;
  border-radius: 2px;
  background: var(--color-text-primary);
  opacity: 0.6;
  transition: opacity 0.15s;
  pointer-events: none;
}

.band-divider:hover .band-divider__handle,
.band-divider:active .band-divider__handle {
  opacity: 1;
}

.band-legend {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.band-legend__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

.band-legend__color {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.band-legend__time {
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
</style>

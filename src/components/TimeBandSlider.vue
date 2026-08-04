<script setup>
import { computed } from 'vue'

const props = defineProps({
  bands: { type: Array, required: true }
})

const emit = defineEmits(['update:bands'])

const colors = ['#FFB347', '#87CEEB', '#DDA0DD', '#2C3E50']
const labels = ['☀️', '🌤️', '🌅', '🌙']
const totalSlots = 24

// Flatten bands into visual segments: wrap-around bands (start > end) are split
// into two pieces so they appear at both ends of the 24h bar.
const flatSegments = computed(() => {
  const segs = []
  for (let i = 0; i < props.bands.length; i++) {
    const b = props.bands[i]
    if (b.start <= b.end) {
      segs.push({ bandIdx: i, start: b.start, end: b.end })
    } else {
      // Wrap-around: render tail first (0 → end), then head (start → 24)
      segs.push({ bandIdx: i, start: 0, end: b.end })
      segs.push({ bandIdx: i, start: b.start, end: totalSlots })
    }
  }
  return segs
})

function segmentStyle(seg) {
  const width = seg.end - seg.start
  const left = (seg.start / totalSlots) * 100
  const w = (width / totalSlots) * 100
  return { left: left + '%', width: w + '%' }
}

function onDragStart(idx, side, e) {
  e.preventDefault()
  const startX = e.touches ? e.touches[0].clientX : e.clientX
  const bar = e.target.closest('.band-track')
  const barWidth = bar.offsetWidth
  const startBands = props.bands.map(b => ({ ...b }))

  function onMove(ev) {
    const clientX = ev.touches ? ev.touches[0].clientX : ev.clientX
    const dx = clientX - startX
    const hourDelta = Math.round((dx / barWidth) * totalSlots)
    if (hourDelta === 0) return

    const newBands = startBands.map(b => ({ ...b }))
    if (side === 'end') {
      const newEnd = (startBands[idx].end + hourDelta + totalSlots) % totalSlots
      if (newEnd === startBands[idx].start) return
      newBands[idx].end = newEnd
      const nextIdx = (idx + 1) % newBands.length
      newBands[nextIdx].start = newEnd
    } else {
      const newStart = (startBands[idx].start + hourDelta + totalSlots) % totalSlots
      if (newStart === startBands[idx].end) return
      newBands[idx].start = newStart
      const prevIdx = (idx - 1 + newBands.length) % newBands.length
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
</script>

<template>
  <div class="band-slider">
    <div class="band-track">
      <div class="band-track__hours">
        <span v-for="h in [0,6,12,18]" :key="h" class="band-track__hour"
          :style="{ left: (h / totalSlots) * 100 + '%' }">{{ h }}</span>
      </div>
      <div v-for="(seg, i) in flatSegments" :key="'seg-' + i" class="band-segment"
        :style="{ ...segmentStyle(seg), backgroundColor: colors[seg.bandIdx] }">
        <span v-if="seg.start === 0 || seg.end === totalSlots" class="band-segment__label">{{ labels[seg.bandIdx] }}</span>
      </div>
      <div v-for="(band, i) in bands" :key="'div-' + band.key"
        class="band-divider"
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
.band-slider { margin: var(--space-md) 0; }

.band-track {
  position: relative;
  height: 48px;
  background: var(--color-surface-overlay);
  border-radius: var(--radius-md);
  overflow: visible;
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
  pointer-events: none;
}

.band-divider:hover .band-divider__handle { opacity: 1; }

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

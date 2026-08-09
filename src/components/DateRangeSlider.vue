<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  readings: { type: Array, required: true },
  modelValue: { type: Object, required: true } // { from: 'YYYY-MM-DD', to: 'YYYY-MM-DD' }
})

const emit = defineEmits(['update:modelValue'])

const trackEl = ref(null)
const dragging = ref(null) // 'from' | 'to' | null
const trackWidth = ref(0)

// Compute the full date range from readings (or fallback to last 90 days)
const fullRange = computed(() => {
  if (props.readings.length > 0) {
    const sorted = [...props.readings].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    const first = new Date(sorted[0].timestamp)
    const last = new Date()
    // Ensure at least 7 days range
    const minDate = new Date(Math.min(first.getTime(), Date.now() - 7 * 86400000))
    // Round to start of day
    minDate.setHours(0, 0, 0, 0)
    last.setHours(23, 59, 59, 999)
    return { min: minDate, max: last, days: Math.ceil((last - minDate) / 86400000) || 7 }
  }
  const now = new Date()
  const past = new Date(now.getTime() - 90 * 86400000)
  return { min: past, max: now, days: 90 }
})

// Normalize date to YYYY-MM-DD
function toDateStr(d) {
  if (typeof d === 'string') return d.slice(0, 10)
  return d.toISOString().slice(0, 10)
}

// Convert date to percentage position on the track
function dateToPercent(dateStr) {
  const d = new Date(dateStr)
  const range = fullRange.value
  const totalMs = range.max.getTime() - range.min.getTime()
  if (totalMs <= 0) return 0
  return Math.max(0, Math.min(100, ((d.getTime() - range.min.getTime()) / totalMs) * 100))
}

// Convert percentage to date
function percentToDate(pct) {
  const range = fullRange.value
  const totalMs = range.max.getTime() - range.min.getTime()
  const ms = range.min.getTime() + (pct / 100) * totalMs
  const d = new Date(ms)
  d.setHours(0, 0, 0, 0)
  return toDateStr(d)
}

// Current handle positions as percentages
const fromPct = computed(() => dateToPercent(props.modelValue.from))
const toPct = computed(() => dateToPercent(props.modelValue.to))

// Sparkline data: count readings per day
const sparkline = computed(() => {
  const range = fullRange.value
  const days = range.days
  const buckets = new Array(Math.min(days, 60)).fill(0)
  const stepMs = (range.max.getTime() - range.min.getTime()) / buckets.length
  for (const r of props.readings) {
    const idx = Math.floor((new Date(r.timestamp).getTime() - range.min.getTime()) / stepMs)
    if (idx >= 0 && idx < buckets.length) buckets[idx]++
  }
  const maxCount = Math.max(1, ...buckets)
  return buckets.map(c => (c / maxCount) * 100)
})

// Format date for display
function fmtDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('it-IT', { day: 'numeric', month: 'short' })
}

// Drag handlers
function onPointerDown(handle, e) {
  e.preventDefault()
  dragging.value = handle
  const el = trackEl.value
  if (!el) return
  trackWidth.value = el.offsetWidth
  el.setPointerCapture(e.pointerId)

  function onMove(ev) {
    if (!dragging.value || !trackEl.value) return
    const rect = trackEl.value.getBoundingClientRect()
    const pct = Math.max(0, Math.min(100, ((ev.clientX - rect.left) / rect.width) * 100))
    const date = percentToDate(pct)

    if (dragging.value === 'from') {
      if (date >= props.modelValue.to) return
      emit('update:modelValue', { ...props.modelValue, from: date })
    } else {
      if (date <= props.modelValue.from) return
      emit('update:modelValue', { ...props.modelValue, to: date })
    }
  }

  function onUp() {
    dragging.value = null
    trackEl.value?.releasePointerCapture(e.pointerId)
    document.removeEventListener('pointermove', onMove)
    document.removeEventListener('pointerup', onUp)
  }

  document.addEventListener('pointermove', onMove)
  document.addEventListener('pointerup', onUp)
}

// Also handle native touch events for better mobile support
function onTouchStart(handle, e) {
  if (e.touches.length !== 1) return
  e.preventDefault()
  dragging.value = handle
  const el = trackEl.value
  if (!el) return
  trackWidth.value = el.offsetWidth

  function onMove(ev) {
    if (!dragging.value || !trackEl.value) return
    const rect = trackEl.value.getBoundingClientRect()
    const pct = Math.max(0, Math.min(100, ((ev.touches[0].clientX - rect.left) / rect.width) * 100))
    const date = percentToDate(pct)

    if (dragging.value === 'from') {
      if (date >= props.modelValue.to) return
      emit('update:modelValue', { ...props.modelValue, from: date })
    } else {
      if (date <= props.modelValue.from) return
      emit('update:modelValue', { ...props.modelValue, to: date })
    }
  }

  function onEnd() {
    dragging.value = null
    document.removeEventListener('touchmove', onMove)
    document.removeEventListener('touchend', onEnd)
  }

  document.addEventListener('touchmove', onMove, { passive: false })
  document.addEventListener('touchend', onEnd)
}

// Ensure modelValue has both from and to
watch(() => props.modelValue, (val) => {
  if (!val.from && !val.to) {
    const range = fullRange.value
    emit('update:modelValue', {
      from: toDateStr(new Date(range.max.getTime() - 30 * 86400000)),
      to: toDateStr(range.max)
    })
  }
}, { immediate: true })
</script>

<template>
  <div class="date-range-slider">
    <div class="drs-labels">
      <span class="drs-label drs-label--from">{{ fmtDate(modelValue.from) }}</span>
      <span class="drs-label drs-label--to">{{ fmtDate(modelValue.to) }}</span>
    </div>
    <div ref="trackEl" class="drs-track" :class="{ 'drs-track--dragging': dragging }">
      <!-- Sparkline bars -->
      <div class="drs-sparkline">
        <div
          v-for="(h, i) in sparkline"
          :key="i"
          class="drs-sparkline__bar"
          :style="{ height: h + '%' }"
        />
      </div>
      <!-- Selected range highlight -->
      <div
        class="drs-range"
        :style="{ left: fromPct + '%', width: (toPct - fromPct) + '%' }"
      />
      <!-- From handle -->
      <div
        class="drs-handle drs-handle--from"
        :class="{ 'drs-handle--active': dragging === 'from' }"
        :style="{ left: fromPct + '%' }"
        @pointerdown="onPointerDown('from', $event)"
        @touchstart="onTouchStart('from', $event)"
      >
        <div class="drs-handle__grip" />
      </div>
      <!-- To handle -->
      <div
        class="drs-handle drs-handle--to"
        :class="{ 'drs-handle--active': dragging === 'to' }"
        :style="{ left: toPct + '%' }"
        @pointerdown="onPointerDown('to', $event)"
        @touchstart="onTouchStart('to', $event)"
      >
        <div class="drs-handle__grip" />
      </div>
    </div>
    <div class="drs-labels drs-labels--range">
      <span class="drs-label drs-label--min">{{ fmtDate(toDateStr(fullRange.min)) }}</span>
      <span class="drs-label drs-label--max">{{ fmtDate(toDateStr(fullRange.max)) }}</span>
    </div>
  </div>
</template>

<style scoped>
.date-range-slider {
  padding: var(--space-sm) 0;
  user-select: none;
}

.drs-labels {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}

.drs-labels--range {
  margin-top: 2px;
  margin-bottom: 0;
}

.drs-label {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
}

.drs-label--from {
  color: var(--color-accent);
  font-weight: 600;
}
.drs-label--to {
  color: var(--color-accent);
  font-weight: 600;
}

.drs-track {
  position: relative;
  height: 36px;
  background: var(--color-surface-overlay);
  border-radius: var(--radius-sm);
  cursor: pointer;
  overflow: hidden;
  touch-action: none;
}

.drs-track--dragging {
  cursor: grabbing;
}

.drs-sparkline {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  padding: 0 2px;
  gap: 1px;
  opacity: 0.15;
}

.drs-sparkline__bar {
  flex: 1;
  min-width: 1px;
  background: var(--color-text-primary);
  border-radius: 1px 1px 0 0;
  transition: height 0.2s;
}

.drs-range {
  position: absolute;
  top: 0;
  bottom: 0;
  background: var(--color-accent);
  opacity: 0.15;
  border-left: 2px solid var(--color-accent);
  border-right: 2px solid var(--color-accent);
  pointer-events: none;
}

.drs-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 20px;
  margin-left: -10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  touch-action: none;
}

.drs-handle--active {
  cursor: grabbing;
}

.drs-handle__grip {
  width: 4px;
  height: 20px;
  background: var(--color-accent);
  border-radius: 2px;
  box-shadow: 0 0 0 3px var(--color-surface-raised), 0 0 0 4px var(--color-accent);
  transition: height 0.15s;
}

.drs-handle--active .drs-handle__grip {
  height: 28px;
}
</style>

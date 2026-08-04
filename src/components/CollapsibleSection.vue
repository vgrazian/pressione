<script setup>
import { ref } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  open: { type: Boolean, default: false }
})

const isOpen = ref(props.open)

function toggle() {
  isOpen.value = !isOpen.value
}
</script>

<template>
  <div class="collapsible" :class="{ 'collapsible--open': isOpen }">
    <button class="collapsible__header" @click="toggle" :aria-expanded="isOpen">
      <span class="collapsible__title">{{ title }}</span>
      <span class="collapsible__icon" :class="{ 'collapsible__icon--open': isOpen }">▸</span>
    </button>
    <div v-show="isOpen" class="collapsible__body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.collapsible {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: var(--space-sm);
}

.collapsible__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface-overlay);
  border: none;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.collapsible__header:hover {
  background: var(--color-border);
}

.collapsible__icon {
  display: inline-block;
  transition: transform 0.2s;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

.collapsible__icon--open {
  transform: rotate(90deg);
}

.collapsible__body {
  padding: var(--space-md);
  background: var(--color-surface-raised);
}
</style>

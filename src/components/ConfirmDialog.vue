<script setup>
import { ref } from 'vue'

const visible = ref(false)
const title = ref('')
const message = ref('')
const confirmText = ref('Conferma')
const cancelText = ref('Annulla')
const variant = ref('default') // 'default' | 'danger'
let resolvePromise = null

function show(options = {}) {
  title.value = options.title || 'Conferma'
  message.value = options.message || 'Sei sicuro?'
  confirmText.value = options.confirmText || 'Conferma'
  cancelText.value = options.cancelText || 'Annulla'
  variant.value = options.variant || 'default'
  visible.value = true

  return new Promise((resolve) => {
    resolvePromise = resolve
  })
}

function confirm() {
  visible.value = false
  if (resolvePromise) {
    resolvePromise(true)
    resolvePromise = null
  }
}

function cancel() {
  visible.value = false
  if (resolvePromise) {
    resolvePromise(false)
    resolvePromise = null
  }
}

defineExpose({ show })
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="dialog-overlay" @click.self="cancel">
      <div class="dialog" role="dialog" aria-modal="true">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <div class="dialog-actions">
          <button class="btn btn-secondary" @click="cancel">{{ cancelText }}</button>
          <button
            class="btn"
            :class="variant === 'danger' ? 'btn-error' : 'btn-primary'"
            @click="confirm"
          >
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.dialog {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  max-width: 360px;
  width: 100%;
  box-shadow: var(--shadow-lg);
}

.dialog h3 {
  margin-bottom: var(--space-sm);
}

.dialog p {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  margin-bottom: var(--space-lg);
}

.dialog-actions {
  display: flex;
  gap: var(--space-sm);
  justify-content: flex-end;
}
</style>

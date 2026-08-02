<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { isOnline } from '@/services/dataService.js'

const online = ref(true)
let interval = null

onMounted(() => {
  checkConnection()
  interval = setInterval(checkConnection, 30000)
})

onUnmounted(() => clearInterval(interval))

async function checkConnection() {
  online.value = navigator.onLine
  if (online.value) online.value = await isOnline()
}
</script>

<template>
  <div v-if="!online" class="offline-banner">
    <span>⚠️ Offline — i dati saranno sincronizzati quando tornerai online</span>
  </div>
</template>

<style scoped>
.offline-banner {
  position: fixed;
  top: 48px;
  left: 0;
  right: 0;
  z-index: 49;
  background: #FFF3CD;
  color: #856404;
  text-align: center;
  padding: 6px var(--space-md);
  font-size: 0.8125rem;
  font-weight: 500;
}
</style>

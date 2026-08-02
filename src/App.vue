<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { initAuth, useAuth } from '@/services/auth.js'
import { refreshFromServer } from '@/services/dataService.js'
import AppNav from '@/components/AppNav.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const router = useRouter()
const { isAuthenticated, isAuthReady, user } = useAuth()
const isInitializing = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    await initAuth()
  } catch (e) {
    error.value = 'Errore di inizializzazione: ' + e.message
  } finally {
    isInitializing.value = false
  }
})
</script>

<template>
  <div id="pressione-app">
    <div v-if="isInitializing" class="app-loading">
      <div class="spinner"></div>
      <p>Caricamento...</p>
    </div>

    <div v-else-if="error" class="app-error">
      <p>{{ error }}</p>
      <button @click="location.reload()">Riprova</button>
    </div>

    <template v-else>
      <AppNav v-if="isAuthenticated" />
      <main :class="{ 'has-nav': isAuthenticated }">
        <router-view />
      </main>
      <ConfirmDialog />
    </template>
  </div>
</template>

<style scoped>
.app-loading,
.app-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 1rem;
  padding: 2rem;
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--color-surface-overlay);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

main {
  padding-bottom: 1rem;
}

main.has-nav {
  padding-bottom: 5rem;
}
</style>

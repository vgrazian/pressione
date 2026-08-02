<script setup>
import { onMounted, ref, inject } from 'vue'
import { useRouter } from 'vue-router'
import { initAuth, useAuth } from '@/services/auth.js'
import { useTheme } from '@/services/theme.js'
import AppNav from '@/components/AppNav.vue'
import AppIcon from '@/components/AppIcon.vue'
import OfflineBanner from '@/components/OfflineBanner.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const router = useRouter()
const { isAuthenticated, isAuthReady, user, logout } = useAuth()
const { theme, toggle: toggleTheme } = useTheme()
const isInitializing = ref(true)
const error = ref(null)
const confirm = inject('confirm-dialog', null)

onMounted(async () => {
  try {
    await initAuth()
  } catch (e) {
    error.value = 'Errore di inizializzazione: ' + e.message
  } finally {
    isInitializing.value = false
  }
})

async function handleLogout() {
  if (!confirm) {
    await logout()
    router.push('/login')
    return
  }
  const ok = await confirm({
    title: 'Logout',
    message: 'Vuoi effettuare il logout?',
    confirmText: 'Esci'
  })
  if (ok) {
    await logout()
    router.push('/login')
  }
}
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
      <!-- Top Bar -->
      <header v-if="isAuthenticated" class="topbar">
        <span class="topbar-brand">
          <AppIcon name="heart" :size="18" color="var(--color-accent)" />
          Pressione
        </span>
        <div class="topbar-actions">
          <button class="topbar-btn" @click="toggleTheme" :title="'Tema: ' + theme">
            <AppIcon :name="theme === 'dark' ? 'moon' : 'sun'" :size="18" />
          </button>
          <button class="topbar-btn" @click="handleLogout" title="Logout">
            <AppIcon name="logout" :size="18" />
          </button>
        </div>
      </header>

      <OfflineBanner v-if="isAuthenticated" />

      <AppNav v-if="isAuthenticated" />
      <main :class="{ 'has-nav': isAuthenticated, 'has-topbar': isAuthenticated }">
        <router-view />
      </main>

      <!-- Global FAB -->
      <button v-if="isAuthenticated" class="fab" @click="router.push('/add')" title="Nuova misurazione">
        <AppIcon name="plus" :size="24" color="var(--color-on-accent)" />
      </button>
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
  border: 4px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 var(--space-lg);
  background: var(--color-surface-raised);
  border-bottom: 1px solid var(--color-border);
}

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}

.topbar-actions { display: flex; align-items: center; gap: 4px; }
.topbar-btn {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px;
  border: none; border-radius: var(--radius-sm);
  background: transparent; color: var(--color-text-tertiary);
  cursor: pointer; transition: background 0.15s, color 0.15s;
}
.topbar-btn:hover { background: var(--color-surface-overlay); color: var(--color-text-primary); }
.topbar-btn:active { transform: scale(0.95); }

main { padding-bottom: 1rem; }
main.has-topbar { padding-top: 0; }
main.has-nav { padding-bottom: 5rem; }
</style>

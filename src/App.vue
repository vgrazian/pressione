<script setup>
import { onMounted, ref, inject, watch } from 'vue'
import { useRouter } from 'vue-router'
import { initAuth, useAuth } from '@/services/auth.js'
import { useTheme } from '@/services/theme.js'
import { initKeepAlive } from '@/services/keepAlive.js'
import { initPWAInstall } from '@/services/pwaInstall.js'
import AppNav from '@/components/AppNav.vue'
import AppIcon from '@/components/AppIcon.vue'
import OfflineBanner from '@/components/OfflineBanner.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import ProfilePrompt from '@/components/ProfilePrompt.vue'
import { useSWUpdate } from '@/services/swUpdate.js'
import { startReminderScheduler, stopReminderScheduler } from '@/services/reminderService.js'

const router = useRouter()
const { isAuthenticated, isAuthReady, user, logout } = useAuth()
const { theme, resolvedTheme, toggle: toggleTheme } = useTheme()
const isInitializing = ref(true)
const error = ref(null)
const confirm = inject('confirm-dialog', null)
const showProfilePrompt = ref(false)
const { updateAvailable, updateFailed, applyUpdate } = useSWUpdate()

onMounted(async () => {
  initPWAInstall()
  try {
    await initAuth()
  } catch (e) {
    error.value = 'Errore di inizializzazione: ' + e.message
  } finally {
    isInitializing.value = false
  }
})

// Start keep-alive and reminder scheduler when user logs in
watch(() => user.value?.username, (username, oldUsername) => {
  if (username) {
    initKeepAlive(username)
    startReminderScheduler(username)
  } else if (oldUsername) {
    stopReminderScheduler()
  }
}, { immediate: true })

// Show profile prompt only if ALL of: auth ready, user exists, profile not completed,
// user hasn't skipped, AND no profile data is already present (birthDate or gender).
// Uses a short delay to let async profile data (from settings table) arrive first.
let profilePromptTimer = null
watch([() => user.value, () => isAuthReady.value], ([u, ready]) => {
    if (ready && u && u.username && !u.profileCompleted && !u.skipProfilePrompt
        && !u.birthDate && !u.gender) {
        // Delay showing the prompt to allow async profile fetch to complete
        clearTimeout(profilePromptTimer)
        profilePromptTimer = setTimeout(() => {
            // Re-check conditions after delay — profile data may have arrived
            const currentUser = user.value
            if (currentUser && !currentUser.profileCompleted && !currentUser.skipProfilePrompt
                && !currentUser.birthDate && !currentUser.gender) {
                showProfilePrompt.value = true
            }
        }, 1500)
    } else if (u && (u.profileCompleted || u.skipProfilePrompt || u.birthDate || u.gender)) {
        clearTimeout(profilePromptTimer)
        showProfilePrompt.value = false
    }
}, { immediate: true })

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
          <img src="/logo.png" alt="Pressione" class="topbar-logo" />
        </span>
        <div class="topbar-actions">
          <button class="topbar-btn" @click="toggleTheme" :title="'Tema: ' + theme">
            <AppIcon :name="resolvedTheme === 'dark' ? 'moon' : 'sun'" :size="18" />
          </button>
          <button class="topbar-btn" @click="handleLogout" title="Logout">
            <AppIcon name="logout" :size="18" />
          </button>
        </div>
      </header>

      <!-- Update available banner -->
      <div v-if="isAuthenticated && updateAvailable" class="update-banner">
        <template v-if="!updateFailed">
          <span>Nuova versione disponibile</span>
          <button class="btn btn-sm btn-primary" @click="applyUpdate">Aggiorna ora</button>
        </template>
        <template v-else>
          <span>⚠️ Aggiornamento automatico non riuscito. Prova da Impostazioni → Forza aggiornamento.</span>
        </template>
      </div>

      <OfflineBanner v-if="isAuthenticated" />

      <AppNav v-if="isAuthenticated" />
      <main :class="{ 'has-nav': isAuthenticated, 'has-topbar': isAuthenticated }">
        <router-view v-slot="{ Component }">
          <transition name="view-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <!-- Global FAB — hidden on add/edit pages -->
      <button v-if="isAuthenticated && $route.name !== 'addReading' && $route.name !== 'editReading'" class="fab" @click="router.push('/add')" title="Nuova misurazione">
        <AppIcon name="plus" :size="24" color="var(--color-on-accent)" />
      </button>
      <ConfirmDialog />

      <!-- Profile prompt for incomplete profiles -->
      <ProfilePrompt v-if="showProfilePrompt" @close="showProfilePrompt = false" />
    </template>
  </div>
</template>

<style scoped>
/* View transitions */
.view-fade-enter-active,
.view-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.view-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.view-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

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

.topbar-logo {
  width: 24px;
  height: 24px;
  border-radius: 4px;
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

.update-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-accent-muted);
  border-bottom: 1px solid var(--color-accent);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-accent);
  animation: slideDown 0.3s ease;
}
@keyframes slideDown { from { transform: translateY(-100%); } to { transform: translateY(0); } }

main { padding-bottom: 1rem; }
main.has-topbar { padding-top: 48px; }
main.has-nav { padding-bottom: calc(4.5rem + env(safe-area-inset-bottom, 0.5rem)); }
</style>

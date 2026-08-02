<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/services/auth.js'

const { user, fetchUsers, updateUserRole, deactivateUser } = useAuth()

const users = ref([])
const isLoading = ref(true)
const errorMessage = ref('')
const confirmDialog = ref(null)

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  isLoading.value = true
  try {
    users.value = await fetchUsers()
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    isLoading.value = false
  }
}

async function toggleRole(targetUser) {
  if (targetUser.username === user.value.username) {
    errorMessage.value = 'Non puoi cambiare il tuo ruolo'
    return
  }
  const newRole = targetUser.role === 'admin' ? 'user' : 'admin'
  try {
    await updateUserRole(targetUser.username, newRole)
    targetUser.role = newRole
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function handleDeactivate(targetUser) {
  if (targetUser.username === user.value.username) {
    errorMessage.value = 'Non puoi disattivare il tuo account'
    return
  }
  const confirmed = await confirmDialog.value?.show({
    title: 'Disattiva utente',
    message: `Disattivare l'utente "${targetUser.username}"?`,
    confirmText: 'Disattiva',
    variant: 'danger'
  })
  if (confirmed) {
    try {
      await deactivateUser(targetUser.username)
      targetUser.disabled = true
    } catch (e) {
      errorMessage.value = e.message
    }
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Gestione Utenti</h1>
      <router-link to="/settings" class="btn btn-sm btn-outline mt-sm">← Impostazioni</router-link>
    </div>

    <div v-if="errorMessage" class="card mb-md" style="border-color: var(--color-error);">
      <p style="color: var(--color-error);">{{ errorMessage }}</p>
    </div>

    <div v-if="isLoading" class="empty-state"><p>Caricamento...</p></div>

    <div v-else class="card">
      <div class="users-list">
        <div v-for="u in users" :key="u.username" class="user-row">
          <div class="user-info">
            <span class="user-name">{{ u.username }}</span>
            <span class="user-email">{{ u.email }}</span>
            <span class="chip" :class="u.role === 'admin' ? 'chip-admin' : 'chip-user'">
              {{ u.role === 'admin' ? 'Admin' : 'Utente' }}
            </span>
            <span v-if="u.disabled" class="chip chip-inactive">Disattivato</span>
          </div>
          <div class="user-actions flex gap-sm">
            <button
              v-if="!u.disabled"
              class="btn btn-sm btn-outline"
              @click="toggleRole(u)"
            >
              {{ u.role === 'admin' ? 'Rendi Utente' : 'Rendi Admin' }}
            </button>
            <button
              v-if="!u.disabled"
              class="btn btn-sm btn-secondary"
              @click="handleDeactivate(u)"
            >
              Disattiva
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-surface-overlay);
  gap: var(--space-md);
  flex-wrap: wrap;
}

.user-row:last-child {
  border-bottom: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.user-name {
  font-weight: 600;
}

.user-email {
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
}

.chip-admin {
  background: var(--color-accent-muted);
  color: var(--color-accent);
}

.chip-user {
  background: var(--color-surface-overlay);
  color: var(--color-text-primary);
}

.chip-inactive {
  background: var(--color-error-container);
  color: var(--color-on-error-container);
}
</style>

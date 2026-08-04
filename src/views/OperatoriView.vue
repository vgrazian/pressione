<script setup>
import { ref, onMounted, inject } from 'vue'
import { useAuth } from '@/services/auth.js'
import SkeletonLoader from '@/components/SkeletonLoader.vue'

const { user, fetchUsers, updateUserRole, deactivateUser, adminResetUserPassword } = useAuth()

const confirm = inject('confirm-dialog', null)
const users = ref([])
const isLoading = ref(true)
const errorMessage = ref('')
const resetUser = ref(null)
const newPassword = ref('')

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
  if (!confirm) {
    if (!window.confirm(`Disattivare l'utente "${targetUser.username}"?`)) return
  } else {
    const confirmed = await confirm({
      title: 'Disattiva utente',
      message: `Disattivare l'utente "${targetUser.username}"?`,
      confirmText: 'Disattiva',
      variant: 'danger'
    })
    if (!confirmed) return
  }
  try {
    await deactivateUser(targetUser.username)
    targetUser.disabled = true
  } catch (e) {
    errorMessage.value = e.message
  }
}

function startResetPassword(targetUser) {
  resetUser.value = targetUser
  newPassword.value = ''
  errorMessage.value = ''
}

async function handleResetPassword() {
  if (!newPassword.value || newPassword.value.length < 8) {
    errorMessage.value = 'La password deve essere di almeno 8 caratteri'
    return
  }
  try {
    await adminResetUserPassword(resetUser.value.username, newPassword.value)
    errorMessage.value = ''
    resetUser.value = null
    newPassword.value = ''
    alert(`Password reimpostata per ${resetUser.value?.username || 'utente'}`)
  } catch (e) {
    errorMessage.value = e.message
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

    <div v-if="isLoading" class="p-lg">
      <SkeletonLoader type="card" :count="5" height="60px" />
    </div>

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
              class="btn btn-sm btn-ghost"
              @click="startResetPassword(u)"
            >
              Reset PW
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

    <!-- Reset Password Section -->
    <div v-if="resetUser" class="card mt-md" style="border-color: var(--color-accent);">
      <h3 class="mb-sm">Reimposta password per {{ resetUser.username }}</h3>
      <div class="form-group">
        <label class="form-label">Nuova password</label>
        <input v-model="newPassword" type="password" class="form-input" placeholder="Minimo 8 caratteri" />
      </div>
      <div class="flex gap-sm">
        <button class="btn btn-primary btn-sm" @click="handleResetPassword">Reimposta</button>
        <button class="btn btn-sm btn-secondary" @click="resetUser = null">Annulla</button>
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

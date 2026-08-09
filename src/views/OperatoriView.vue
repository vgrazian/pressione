<script setup>
import { ref, onMounted, inject } from 'vue'
import { useAuth } from '@/services/auth.js'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import AppIcon from '@/components/AppIcon.vue'

const { user, fetchUsers, updateUserRole, deactivateUser, activateUser, hardDeleteUser, adminResetUserPassword, adminUpdateUserEmail, register } = useAuth()

const confirm = inject('confirm-dialog', null)
const users = ref([])
const isLoading = ref(true)
const errorMessage = ref('')
const resetUser = ref(null)
const newPassword = ref('')
const editingEmail = ref(null)
const newEmail = ref('')
const successMessage = ref('')
const showNewUserForm = ref(false)
const newUser = ref({ username: '', email: '', password: '', isAdmin: false, isActive: true })
const creatingUser = ref(false)

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

async function toggleAdmin(targetUser) {
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

async function toggleActive(targetUser) {
  if (targetUser.username === user.value.username) {
    errorMessage.value = 'Non puoi disattivare il tuo account'
    return
  }
  const activating = targetUser.disabled
  if (!confirm) {
    const msg = activating
      ? `Riattivare l'utente "${targetUser.username}"?`
      : `Disattivare l'utente "${targetUser.username}"?`
    if (!window.confirm(msg)) return
  } else {
    const confirmed = await confirm({
      title: activating ? 'Riattiva utente' : 'Disattiva utente',
      message: activating
        ? `Riattivare l'utente "${targetUser.username}"? Potrà nuovamente accedere.`
        : `Disattivare l'utente "${targetUser.username}"?`,
      confirmText: activating ? 'Riattiva' : 'Disattiva',
      variant: activating ? 'default' : 'danger'
    })
    if (!confirmed) return
  }
  try {
    if (activating) {
      await activateUser(targetUser.username)
    } else {
      await deactivateUser(targetUser.username)
    }
    // Reload from server to ensure correct state (prevents stale role on reactivation)
    await loadUsers()
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function handleHardDelete(targetUser) {
  if (targetUser.username === user.value.username) {
    errorMessage.value = 'Non puoi eliminare il tuo account'
    return
  }
  if (!confirm) {
    if (!window.confirm(`Eliminare PERMANENTEMENTE l'utente "${targetUser.username}"? Questa azione è irreversibile.`)) return
  } else {
    const confirmed = await confirm({
      title: 'Elimina utente',
      message: `Eliminare PERMANENTEMENTE l'utente "${targetUser.username}"? Tutti i suoi dati andranno persi.`,
      confirmText: 'Elimina',
      variant: 'danger'
    })
    if (!confirmed) return
  }
  try {
    await hardDeleteUser(targetUser.username)
    users.value = users.value.filter(u => u.username !== targetUser.username)
  } catch (e) {
    errorMessage.value = e.message
  }
}

async function handleCreateUser() {
  errorMessage.value = ''
  if (!newUser.value.username || !newUser.value.email || !newUser.value.password) {
    errorMessage.value = 'Compila tutti i campi'
    return
  }
  if (newUser.value.password.length < 8) {
    errorMessage.value = 'La password deve essere di almeno 8 caratteri'
    return
  }
  if (!newUser.value.email.includes('@')) {
    errorMessage.value = 'Inserisci un indirizzo email valido'
    return
  }
  creatingUser.value = true
  try {
    const role = newUser.value.isAdmin ? 'admin' : 'user'
    await register(newUser.value.username, newUser.value.email, newUser.value.password, role)
    // If isActive is false, deactivate the newly created user
    if (!newUser.value.isActive) {
      await deactivateUser(newUser.value.username)
    }
    successMessage.value = `Utente "${newUser.value.username}" creato`
    showNewUserForm.value = false
    newUser.value = { username: '', email: '', password: '', isAdmin: false, isActive: true }
    setTimeout(() => { successMessage.value = '' }, 3000)
    await loadUsers()
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    creatingUser.value = false
  }
}

function startEditEmail(targetUser) {
  editingEmail.value = targetUser.username
  newEmail.value = targetUser.email || ''
  errorMessage.value = ''
}

function cancelEditEmail() {
  editingEmail.value = null
  newEmail.value = ''
}

async function handleUpdateEmail(targetUser) {
  if (!newEmail.value || !newEmail.value.includes('@')) {
    errorMessage.value = 'Inserisci un indirizzo email valido'
    return
  }
  try {
    await adminUpdateUserEmail(targetUser.username, newEmail.value)
    targetUser.email = newEmail.value.toLowerCase().trim()
    editingEmail.value = null
    newEmail.value = ''
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
    successMessage.value = `Password reimpostata per ${resetUser.value.username}`
    errorMessage.value = ''
    resetUser.value = null
    newPassword.value = ''
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (e) {
    errorMessage.value = e.message
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Gestione Utenti</h1>
      <div class="flex gap-sm">
        <button class="btn btn-primary btn-sm" @click="showNewUserForm = !showNewUserForm">
          <AppIcon name="plus" :size="16" /> Nuovo Utente
        </button>
        <router-link to="/settings" class="btn btn-sm btn-outline"><AppIcon name="chevron-left" :size="16" /> Impostazioni</router-link>
      </div>
    </div>

    <!-- New User Form -->
    <div v-if="showNewUserForm" class="card mb-md" style="border-color: var(--color-accent);">
      <h3 class="mb-sm"><AppIcon name="users" :size="18" /> Nuovo Utente</h3>
      <div class="new-user-form">
        <div class="form-group">
          <label class="form-label">Username</label>
          <input v-model="newUser.username" class="form-input" placeholder="nome.cognome" autocomplete="off" />
        </div>
        <div class="form-group">
          <label class="form-label">Email</label>
          <input v-model="newUser.email" type="email" class="form-input" placeholder="nome@email.com" autocomplete="off" />
        </div>
        <div class="form-group">
          <label class="form-label">Password</label>
          <input v-model="newUser.password" type="password" class="form-input" placeholder="Minimo 8 caratteri" autocomplete="new-password" />
        </div>
      </div>
      <div class="flex gap-sm items-center mt-sm">
        <label class="checkbox-label">
          <input type="checkbox" v-model="newUser.isAdmin" />
          <AppIcon name="users" :size="14" /> Admin
        </label>
        <label class="checkbox-label">
          <input type="checkbox" v-model="newUser.isActive" />
          Attivo
        </label>
        <button class="btn btn-sm btn-primary" @click="handleCreateUser" :disabled="creatingUser">
          <AppIcon name="plus" :size="16" /> {{ creatingUser ? 'Creazione...' : 'Crea Utente' }}
        </button>
        <button class="btn btn-sm btn-ghost" @click="showNewUserForm = false">Annulla</button>
      </div>
    </div>

    <div v-if="successMessage" class="card card--success mb-md">
      <p class="text-success">{{ successMessage }}</p>
    </div>

    <div v-if="errorMessage" class="card card--error mb-md">
      <p class="text-error">{{ errorMessage }}</p>
    </div>

    <div v-if="isLoading" class="p-lg">
      <SkeletonLoader type="card" :count="5" height="60px" />
    </div>

    <div v-else class="card">
      <div class="users-list">
        <div v-for="u in users" :key="u.username" class="user-row" :class="{ 'user-row--disabled': u.disabled }">
          <div class="user-info">
            <span class="user-name">{{ u.username }}</span>
            <div class="user-meta">
            <template v-if="!u.disabled && editingEmail === u.username">
              <div class="email-edit-row">
                <input v-model="newEmail" type="email" class="form-input email-edit-input"
                  placeholder="nuova@email.com" @keyup.enter="handleUpdateEmail(u)" />
                <button class="btn btn-xs btn-primary" @click="handleUpdateEmail(u)">Salva</button>
                <button class="btn btn-xs btn-ghost" @click="cancelEditEmail"><AppIcon name="x" :size="14" /></button>
              </div>
            </template>
            <template v-else>
              <span class="user-email" :class="{ 'clickable': !u.disabled }" role="button" tabindex="0" @click="!u.disabled && startEditEmail(u)" @keydown.enter="!u.disabled && startEditEmail(u)">{{ u.email || '—' }}</span>
              <button v-if="!u.disabled" class="btn-edit-email" title="Modifica email" @click="startEditEmail(u)">
                <AppIcon name="edit" :size="12" />
              </button>
            </template>
            <span class="chip" :class="u.role === 'admin' ? 'chip-admin' : 'chip-user'">
              {{ u.role === 'admin' ? 'Admin' : 'Utente' }}
            </span>
            <span v-if="u.disabled" class="chip chip-inactive">Disattivato</span>
            </div>
          </div>
          <div class="user-actions">
            <label class="checkbox-label" :class="{ 'checkbox-label--disabled': u.username === user?.username }" :title="u.username === user?.username ? 'Non puoi cambiare il tuo ruolo' : ''">
              <input type="checkbox" :checked="u.role === 'admin'" :disabled="u.username === user?.username"
                @change="toggleAdmin(u)" />
              <AppIcon name="users" :size="14" /> Admin
            </label>
            <label class="checkbox-label" :class="{ 'checkbox-label--disabled': u.username === user?.username }" :title="u.username === user?.username ? 'Non puoi disattivare il tuo account' : ''">
              <input type="checkbox" :checked="!u.disabled" :disabled="u.username === user?.username"
                @change="toggleActive(u)" />
              Attivo
            </label>
            <button
              v-if="!u.disabled"
              class="btn btn-sm btn-ghost"
              @click="startResetPassword(u)"
            >
              <AppIcon name="refresh" :size="14" /> Reset PW
            </button>
            <button
              v-if="u.username !== user?.username"
              class="btn btn-sm btn-ghost-error"
              @click="handleHardDelete(u)"
            >
              <AppIcon name="trash" :size="14" color="currentColor" /> Elimina
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
.new-user-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

@media (max-width: 480px) {
  .new-user-form {
    grid-template-columns: 1fr;
  }
}

.user-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-border);
}

.user-row:last-child {
  border-bottom: none;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 0.9375rem;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.user-actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 480px) {
  .user-row {
    grid-template-columns: 1fr;
  }
  .user-actions {
    justify-content: flex-start;
  }
}

.user-row--disabled .user-info {
  opacity: 0.55;
}

.user-row--disabled .user-name {
  text-decoration: line-through;
}

.user-email.clickable {
  cursor: pointer;
  color: var(--color-accent);
}

.user-email.clickable:hover {
  text-decoration: underline;
}

.btn-edit-email {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  padding: 0 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.user-row:hover .btn-edit-email {
  opacity: 1;
}

.email-edit-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.email-edit-input {
  max-width: 220px;
  min-width: 140px;
  flex: 1;
  font-size: 0.8125rem;
  padding: 0.25rem 0.5rem;
  min-height: 28px;
}

.card--error {
  border-color: var(--color-error);
}

.card--error p {
  color: var(--color-error);
}

.card--success {
  border-color: var(--color-accent);
}

.card--success p {
  color: var(--color-accent);
}

.text-error {
  color: var(--color-error);
}

.text-success {
  color: var(--color-accent);
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

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8125rem;
  cursor: pointer;
  user-select: none;
  padding: 2px 4px;
  border-radius: var(--radius-sm);
}

.checkbox-label:hover {
  background: var(--color-surface-overlay);
}

.checkbox-label input[type="checkbox"] {
  width: 14px;
  height: 14px;
  accent-color: var(--color-accent);
  cursor: pointer;
  margin: 0;
}

.checkbox-label--disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.checkbox-label--disabled:hover {
  background: transparent;
}

.checkbox-label--disabled input[type="checkbox"] {
  cursor: not-allowed;
}

.items-center {
  align-items: center;
}
</style>

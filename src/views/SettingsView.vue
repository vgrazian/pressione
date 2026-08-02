<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { deleteAllReadings, getReminders, upsertReminder, deleteReminder } from '@/services/dataService.js'
import { isAdmin } from '@/services/rbac.js'

const router = useRouter()
const { user, logout, changePassword } = useAuth()

const confirmDialog = ref(null)
const reminders = ref([])
const showPasswordForm = ref(false)
const currentPassword = ref('')
const newPassword = ref('')
const passwordError = ref('')
const passwordSuccess = ref('')

onMounted(async () => {
  reminders.value = await getReminders(user.value.username)
})

// --- Reminder Management ---
function addReminder() {
  reminders.value.push({
    id: null,
    username: user.value.username,
    enabled: true,
    time: '08:00',
    daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
    isNew: true
  })
}

async function saveReminder(reminder) {
  await upsertReminder(reminder, user.value.username)
  reminder.isNew = false
  reminders.value = await getReminders(user.value.username)
}

async function removeReminder(reminder) {
  const confirmed = await confirmDialog.value?.show({
    title: 'Rimuovi promemoria',
    message: 'Eliminare questo promemoria?',
    confirmText: 'Rimuovi',
    variant: 'danger'
  })
  if (confirmed) {
    await deleteReminder(reminder.id, user.value.username)
    reminders.value = reminders.value.filter(r => r.id !== reminder.id)
  }
}

function toggleDay(reminder, day) {
  const idx = reminder.daysOfWeek.indexOf(day)
  if (idx >= 0) {
    reminder.daysOfWeek.splice(idx, 1)
  } else {
    reminder.daysOfWeek.push(day)
    reminder.daysOfWeek.sort()
  }
}

const dayLabels = { 1: 'L', 2: 'M', 3: 'M', 4: 'G', 5: 'V', 6: 'S', 7: 'D' }

// --- Password Change ---
async function handlePasswordChange() {
  passwordError.value = ''
  passwordSuccess.value = ''
  if (!currentPassword.value || !newPassword.value) {
    passwordError.value = 'Compila tutti i campi'
    return
  }
  if (newPassword.value.length < 8) {
    passwordError.value = 'La password deve essere di almeno 8 caratteri'
    return
  }
  try {
    await changePassword(currentPassword.value, newPassword.value)
    passwordSuccess.value = 'Password aggiornata con successo!'
    currentPassword.value = ''
    newPassword.value = ''
  } catch (e) {
    passwordError.value = e.message
  }
}

// --- Data Management ---
async function handleDeleteAll() {
  const confirmed = await confirmDialog.value?.show({
    title: 'Elimina tutti i dati',
    message: 'Questa operazione è irreversibile. Eliminare TUTTE le misurazioni?',
    confirmText: 'Elimina tutto',
    variant: 'danger'
  })
  if (confirmed) {
    await deleteAllReadings(user.value.username)
    router.push('/')
  }
}

// --- Logout ---
async function handleLogout() {
  const confirmed = await confirmDialog.value?.show({
    title: 'Logout',
    message: 'Vuoi effettuare il logout?',
    confirmText: 'Esci'
  })
  if (confirmed) {
    await logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Impostazioni</h1>
    </div>

    <!-- User Info -->
    <div class="card mb-md">
      <h3 class="mb-sm">Account</h3>
      <p><strong>Username:</strong> {{ user?.username }}</p>
      <p><strong>Email:</strong> {{ user?.email }}</p>
      <p><strong>Ruolo:</strong> {{ user?.role === 'admin' ? 'Amministratore' : 'Utente' }}</p>
    </div>

    <!-- Password -->
    <div class="card mb-md">
      <h3 class="mb-sm" @click="showPasswordForm = !showPasswordForm" style="cursor: pointer;">
        Cambio Password {{ showPasswordForm ? '▾' : '▸' }}
      </h3>
      <div v-if="showPasswordForm">
        <div class="form-group">
          <label class="form-label">Password attuale</label>
          <input v-model="currentPassword" type="password" class="form-input" />
        </div>
        <div class="form-group">
          <label class="form-label">Nuova password</label>
          <input v-model="newPassword" type="password" class="form-input" placeholder="Minimo 8 caratteri" />
        </div>
        <div v-if="passwordError" class="form-error mb-sm">{{ passwordError }}</div>
        <div v-if="passwordSuccess" class="form-success mb-sm">{{ passwordSuccess }}</div>
        <button class="btn btn-primary btn-sm" @click="handlePasswordChange">Aggiorna Password</button>
      </div>
    </div>

    <!-- Reminders -->
    <div class="card mb-md">
      <div class="flex justify-between items-center mb-sm">
        <h3>Promemoria</h3>
        <button class="btn btn-sm btn-outline" @click="addReminder">+ Aggiungi</button>
      </div>

      <div v-if="reminders.length === 0" class="text-center p-md">
        <p style="color: var(--color-text-secondary);">Nessun promemoria configurato</p>
      </div>

      <div v-for="(reminder, i) in reminders" :key="reminder.id || `new-${i}`" class="reminder-item">
        <div class="flex items-center gap-sm mb-sm">
          <input v-model="reminder.time" type="time" class="form-input" style="width: 120px;" />
          <label class="flex items-center gap-sm" style="font-size: 0.875rem;">
            <input v-model="reminder.enabled" type="checkbox" />
            Attivo
          </label>
        </div>
        <div class="flex gap-sm mb-sm" style="flex-wrap: wrap;">
          <button
            v-for="day in [1,2,3,4,5,6,7]"
            :key="day"
            class="day-chip"
            :class="{ active: reminder.daysOfWeek.includes(day) }"
            @click="toggleDay(reminder, day)"
          >{{ dayLabels[day] }}</button>
        </div>
        <div class="flex gap-sm">
          <button class="btn btn-sm btn-primary" @click="saveReminder(reminder)">Salva</button>
          <button v-if="!reminder.isNew" class="btn btn-sm btn-secondary" @click="removeReminder(reminder)">Rimuovi</button>
        </div>
        <hr v-if="i < reminders.length - 1" style="margin: var(--space-md) 0; border-color: var(--color-surface-overlay);" />
      </div>
    </div>

    <!-- Admin -->
    <div v-if="isAdmin(user)" class="card mb-md">
      <h3 class="mb-sm">Amministrazione</h3>
      <router-link to="/operators" class="btn btn-outline btn-sm">Gestione Utenti</router-link>
    </div>

    <!-- Danger Zone -->
    <div class="card mb-md" style="border: 1px solid var(--color-error);">
      <h3 class="mb-sm" style="color: var(--color-error);">Zona Pericolosa</h3>
      <button class="btn btn-error btn-sm" @click="handleDeleteAll">Elimina Tutte le Misurazioni</button>
    </div>

    <!-- Logout -->
    <div class="card mb-md">
      <button class="btn btn-secondary btn-block" @click="handleLogout">Logout</button>
    </div>

    <p class="text-center" style="color: var(--color-text-secondary); font-size: 0.75rem; padding: var(--space-lg);">
      Pressione v1.0.0
    </p>
  </div>
</template>

<style scoped>
.form-success {
  color: var(--color-accent);
  font-size: 0.875rem;
  font-weight: 500;
}

.reminder-item {
  padding: var(--space-sm) 0;
}

.day-chip {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: white;
  font-size: 0.75rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.day-chip.active {
  background: var(--color-accent);
  color: white;
  border-color: var(--color-accent);
}
</style>

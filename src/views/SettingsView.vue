<script setup>
import { ref, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { deleteAllReadings, getReminders, upsertReminder, deleteReminder, getReadings, exportCSV, generateTestData, refreshFromServer, backupData, restoreData } from '@/services/dataService.js'
import { isAdmin } from '@/services/rbac.js'
import { useI18n } from '@/services/i18n.js'

const router = useRouter()
const { user, changePassword, updateUserEmail } = useAuth()
const { t, setLang, currentLang, availableLangs } = useI18n()

const confirm = inject('confirm-dialog')
const reminders = ref([])
const showPasswordForm = ref(false)
const showEmailForm = ref(false)
const currentPassword = ref('')
const newPassword = ref('')
const passwordError = ref('')
const passwordSuccess = ref('')
const newEmail = ref('')
const emailError = ref('')
const emailSuccess = ref('')
const message = ref('')
const restoreInput = ref(null)

onMounted(async () => {
  reminders.value = await getReminders(user.value.username)
})

// --- Language ---
function changeLanguage(lang) { setLang(lang) }

// --- Reminder Management ---
function addReminder() {
  reminders.value.push({
    id: null, username: user.value.username, enabled: true,
    time: '08:00', daysOfWeek: [1,2,3,4,5,6,7], isNew: true
  })
}
async function saveReminder(r) {
  await upsertReminder(r, user.value.username)
  r.isNew = false
  reminders.value = await getReminders(user.value.username)
}
async function removeReminder(r) {
  const ok = await confirm({ title: t('remove') + ' ' + t('reminders').toLowerCase(), message: t('remove') + '?', confirmText: t('remove'), variant: 'danger' })
  if (ok) { await deleteReminder(r.id, user.value.username); reminders.value = reminders.value.filter(x => x.id !== r.id) }
}
function toggleDay(r, day) {
  const idx = r.daysOfWeek.indexOf(day)
  if (idx >= 0) r.daysOfWeek.splice(idx, 1)
  else { r.daysOfWeek.push(day); r.daysOfWeek.sort() }
}

// --- Password ---
async function handlePasswordChange() {
  passwordError.value = ''; passwordSuccess.value = ''
  if (!currentPassword.value || !newPassword.value) { passwordError.value = 'Compila tutti i campi'; return }
  if (newPassword.value.length < 8) { passwordError.value = 'Minimo 8 caratteri'; return }
  try { await changePassword(currentPassword.value, newPassword.value); passwordSuccess.value = 'Password aggiornata!'; currentPassword.value = ''; newPassword.value = '' }
  catch (e) { passwordError.value = e.message }
}

// --- Email ---
async function handleEmailChange() {
  emailError.value = ''; emailSuccess.value = ''
  if (!newEmail.value || !newEmail.value.includes('@')) { emailError.value = 'Email non valida'; return }
  try { await updateUserEmail(newEmail.value); emailSuccess.value = 'Email aggiornata!'; newEmail.value = '' }
  catch (e) { emailError.value = e.message }
}

// --- Data Management ---
async function handleDeleteAll() {
  const ok = await confirm({ title: t('delete_all_data'), message: t('delete_all_confirm'), confirmText: t('delete_all_btn'), variant: 'danger' })
  if (ok) { await deleteAllReadings(user.value.username); router.push('/') }
}

async function handleExportCSV() {
  message.value = ''
  await refreshFromServer(user.value.username)
  const readings = await getReadings(user.value.username)
  if (readings.length === 0) { message.value = 'Nessun dato da esportare'; return }
  exportCSV(readings)
  message.value = `Esportate ${readings.length} misurazioni`
}

async function handleGenerateTestData() {
  message.value = ''
  try {
    await generateTestData(user.value.username, 30)
    await refreshFromServer(user.value.username)
    message.value = t('test_data_generated')
  } catch (e) { message.value = e.message }
}

async function handleBackup() {
  message.value = ''
  try {
    await refreshFromServer(user.value.username)
    const count = await backupData(user.value.username)
    message.value = `Backup creato con ${count} misurazioni`
  } catch (e) { message.value = e.message }
}

function triggerRestore() { restoreInput.value?.click() }

async function handleRestore(e) {
  message.value = ''
  const file = e.target.files?.[0]
  if (!file) return
  const ok = await confirm({ title: 'Ripristina dati', message: 'I dati esistenti verranno uniti al backup. Continuare?', confirmText: 'Ripristina' })
  if (!ok) { e.target.value = ''; return }
  try {
    const count = await restoreData(user.value.username, file)
    await refreshFromServer(user.value.username)
    message.value = `Ripristinate ${count} misurazioni`
  } catch (err) { message.value = err.message }
  e.target.value = ''
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>{{ t('settings') }}</h1>
    </div>

    <!-- Language -->
    <div class="card mb-md">
      <h3 class="mb-sm">🌐 Lingua / Language</h3>
      <div class="flex gap-sm">
        <button v-for="lang in availableLangs" :key="lang" class="chip" :class="{ 'chip--active': currentLang === lang }" @click="changeLanguage(lang)">
          {{ lang === 'it' ? '🇮🇹 Italiano' : '🇬🇧 English' }}
        </button>
      </div>
    </div>

    <!-- Account -->
    <div class="card mb-md">
      <h3 class="mb-sm">{{ t('account') }}</h3>
      <p><strong>{{ t('username') }}:</strong> {{ user?.username }}</p>
      <p><strong>{{ t('email') }}:</strong> {{ user?.email || t('email_not_set') }}</p>
      <p><strong>Ruolo:</strong> {{ user?.role === 'admin' ? t('role_admin') : t('role_user') }}</p>
      <button class="btn btn-sm btn-ghost mt-sm" @click="showEmailForm = !showEmailForm">{{ showEmailForm ? t('cancel') : t('change_email') }}</button>
      <div v-if="showEmailForm" class="mt-md">
        <div class="form-group"><label class="form-label">{{ t('new_email') }}</label><input v-model="newEmail" type="email" class="form-input" /></div>
        <div v-if="emailError" class="form-error mb-sm">{{ emailError }}</div>
        <div v-if="emailSuccess" class="form-success mb-sm">{{ emailSuccess }}</div>
        <button class="btn btn-primary btn-sm" @click="handleEmailChange">{{ t('update_email') }}</button>
      </div>
    </div>

    <!-- Password -->
    <div class="card mb-md">
      <h3 class="mb-sm" @click="showPasswordForm = !showPasswordForm" style="cursor:pointer;">{{ t('change_password') }} {{ showPasswordForm ? '▾' : '▸' }}</h3>
      <div v-if="showPasswordForm">
        <div class="form-group"><label class="form-label">{{ t('current_password') }}</label><input v-model="currentPassword" type="password" class="form-input" /></div>
        <div class="form-group"><label class="form-label">{{ t('new_password') }}</label><input v-model="newPassword" type="password" class="form-input" placeholder="Minimo 8 caratteri" /></div>
        <div v-if="passwordError" class="form-error mb-sm">{{ passwordError }}</div>
        <div v-if="passwordSuccess" class="form-success mb-sm">{{ passwordSuccess }}</div>
        <button class="btn btn-primary btn-sm" @click="handlePasswordChange">{{ t('update_password') }}</button>
      </div>
    </div>

    <!-- Reminders -->
    <div class="card mb-md">
      <div class="flex justify-between items-center mb-sm">
        <h3>{{ t('reminders') }}</h3>
        <button class="btn btn-sm btn-ghost" @click="addReminder">{{ t('add_reminder') }}</button>
      </div>
      <div v-if="reminders.length === 0" class="text-center p-md"><p class="text-secondary">{{ t('no_reminders') }}</p></div>
      <div v-for="(r, i) in reminders" :key="r.id || `new-${i}`" class="reminder-item">
        <div class="flex items-center gap-sm mb-sm">
          <input v-model="r.time" type="time" class="form-input" style="width:120px" />
          <label class="flex items-center gap-sm" style="font-size:0.875rem"><input v-model="r.enabled" type="checkbox" /> {{ t('active') }}</label>
        </div>
        <div class="flex gap-sm mb-sm flex-wrap">
          <button v-for="d in [1,2,3,4,5,6,7]" :key="d" class="day-chip" :class="{ active: r.daysOfWeek.includes(d) }" @click="toggleDay(r, d)">
            {{ t('day_' + ['','mon','tue','wed','thu','fri','sat','sun'][d]) }}
          </button>
        </div>
        <div class="flex gap-sm">
          <button class="btn btn-sm btn-primary" @click="saveReminder(r)">{{ t('save') }}</button>
          <button v-if="!r.isNew" class="btn btn-sm btn-secondary" @click="removeReminder(r)">{{ t('remove') }}</button>
        </div>
        <hr v-if="i < reminders.length - 1" style="margin:var(--space-md) 0;border-color:var(--color-surface-overlay)" />
      </div>
    </div>

    <!-- Data Management -->
    <div class="card mb-md">
      <h3 class="mb-sm">Dati</h3>
      <div class="flex flex-col gap-sm">
        <button class="btn btn-sm btn-ghost" @click="handleExportCSV">{{ t('export_csv') }}</button>
        <button class="btn btn-sm btn-ghost" @click="handleBackup">Backup (JSON)</button>
        <button class="btn btn-sm btn-ghost" @click="triggerRestore">Ripristina Backup</button>
        <input ref="restoreInput" type="file" accept=".json" style="display:none" @change="handleRestore" />
        <button class="btn btn-sm btn-ghost" @click="handleGenerateTestData">{{ t('generate_test_data') }}</button>
      </div>
      <div v-if="message" class="form-success mt-sm">{{ message }}</div>
    </div>

    <!-- Admin -->
    <div v-if="isAdmin(user)" class="card mb-md">
      <h3 class="mb-sm">{{ t('admin') }}</h3>
      <router-link to="/operators" class="btn btn-ghost btn-sm">{{ t('user_management') }}</router-link>
    </div>

    <!-- Danger Zone -->
    <div class="card mb-md" style="border:1px solid var(--color-error)">
      <h3 class="mb-sm" style="color:var(--color-error)">{{ t('danger_zone') }}</h3>
      <button class="btn btn-error btn-sm" @click="handleDeleteAll">{{ t('delete_all_data') }}</button>
    </div>

    <p class="text-center" style="color:var(--color-text-secondary);font-size:0.75rem;padding:var(--space-lg)">{{ t('version') }}</p>
  </div>
</template>

<style scoped>
.form-success { color: var(--color-accent); font-size: 0.875rem; font-weight: 500; }
.reminder-item { padding: var(--space-sm) 0; }
.day-chip {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid var(--color-border-strong); background: var(--color-surface-raised);
  font-size: 0.75rem; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.day-chip.active { background: var(--color-accent); color: var(--color-on-accent); border-color: var(--color-accent); }
</style>

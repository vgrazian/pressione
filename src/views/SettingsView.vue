<script setup>
import { ref, onMounted, computed, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { deleteAllReadings, getReminders, upsertReminder, deleteReminder, getReadings, exportCSV, importCSV, generateTestData, refreshFromServer, backupData, restoreData } from '@/services/dataService.js'
import { isAdmin } from '@/services/rbac.js'
import { useI18n } from '@/services/i18n.js'
import { startKeepAlive, stopKeepAlive, isKeepAliveActive, isKeepAliveEnabled, getStorageInfo, formatBytes } from '@/services/keepAlive.js'
import { promptInstall, isInstallPromptAvailable, isIOS, isStandalone } from '@/services/pwaInstall.js'
import { useSWUpdate } from '@/services/swUpdate.js'
import { APP_VERSION, BUILD_NUMBER, BUILD_TIME } from '@/services/version.js'
import { getUserBands, saveUserBands, getDefaultBands } from '@/services/timeBands.js'
import AppIcon from '@/components/AppIcon.vue'
import TimeBandSlider from '@/components/TimeBandSlider.vue'

const router = useRouter()
const { user, changePassword, updateUserEmail, updateUserProfile } = useAuth()
const { t, setLang, currentLang, availableLangs } = useI18n()
const { forceClearCache } = useSWUpdate()

const confirm = inject('confirm-dialog', null)
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
const keepAliveOn = ref(false)
const storageInfo = ref(null)
const profileBirthDate = ref('')
const profileGender = ref('')
const profileMessage = ref('')
const savedBirthDate = ref('')
const savedGender = ref('')
const importCsvInput = ref(null)

const profileDirty = computed(() => {
  return profileBirthDate.value !== savedBirthDate.value || profileGender.value !== savedGender.value
})

function computeAge(birthDateStr) {
  if (!birthDateStr) return null
  const today = new Date()
  const birth = new Date(birthDateStr)
  let age = today.getFullYear() - birth.getFullYear()
  const m = today.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age
}

onMounted(async () => {
  try {
    reminders.value = await getReminders(user.value.username).catch(() => [])
  } catch { reminders.value = [] }
  keepAliveOn.value = await isKeepAliveEnabled(user.value.username)
  if (keepAliveOn.value) {
    storageInfo.value = await getStorageInfo()
  }
  profileBirthDate.value = user.value?.birthDate || ''
  profileGender.value = user.value?.gender || ''
  savedBirthDate.value = profileBirthDate.value
  savedGender.value = profileGender.value
  timeBands.value = await getUserBands(user.value.username)
  savedBands.value = JSON.parse(JSON.stringify(timeBands.value))
})

// --- Time Bands ---
async function handleSaveBands() {
  timeBandsMessage.value = ''
  await saveUserBands(user.value.username, timeBands.value)
  savedBands.value = JSON.parse(JSON.stringify(timeBands.value))
  timeBandsMessage.value = 'Fasce orarie salvate!'
  setTimeout(() => timeBandsMessage.value = '', 3000)
}

function resetBands() {
  timeBands.value = getDefaultBands()
}

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
  // For unsaved reminders, remove directly without confirmation
  if (!r.id) {
    reminders.value = reminders.value.filter(x => x !== r)
    return
  }
  // For saved reminders, confirm before deleting
  if (confirm) {
    const ok = await confirm({ title: t('remove') + ' ' + t('reminders').toLowerCase(), message: t('remove') + '?', confirmText: t('remove'), variant: 'danger' })
    if (!ok) return
  }
  await deleteReminder(r.id, user.value.username)
  reminders.value = reminders.value.filter(x => x !== r)
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
  try {
    await updateUserEmail(newEmail.value)
    emailSuccess.value = 'Email aggiornata!'
    newEmail.value = ''
    showEmailForm.value = false
    setTimeout(() => emailSuccess.value = '', 3000)
  } catch (e) { emailError.value = e.message }
}

// --- Data Management ---
async function handleDeleteAll() {
  if (!confirm) {
    if (!window.confirm('Eliminare TUTTE le misurazioni? Operazione irreversibile.')) return
  } else {
    const ok = await confirm({ title: t('delete_all_data'), message: t('delete_all_confirm'), confirmText: t('delete_all_btn'), variant: 'danger' })
    if (!ok) return
  }
  await deleteAllReadings(user.value.username)
  router.push('/')
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
  if (!confirm) {
    if (!window.confirm('I dati esistenti verranno uniti al backup. Continuare?')) { e.target.value = ''; return }
  } else {
    const ok = await confirm({ title: 'Ripristina dati', message: 'I dati esistenti verranno uniti al backup. Continuare?', confirmText: 'Ripristina' })
    if (!ok) { e.target.value = ''; return }
  }
  try {
    const count = await restoreData(user.value.username, file)
    await refreshFromServer(user.value.username)
    message.value = `Ripristinate ${count} misurazioni`
  } catch (err) { message.value = err.message }
  e.target.value = ''
}

function triggerImportCsv() { importCsvInput.value?.click() }

async function handleImportCsv(e) {
  message.value = ''
  const file = e.target.files?.[0]
  if (!file) return
  if (!confirm) {
    if (!window.confirm('I dati verranno aggiunti a quelli esistenti. Continuare?')) { e.target.value = ''; return }
  } else {
    const ok = await confirm({ title: 'Importa CSV', message: 'I dati verranno aggiunti a quelli esistenti. Continuare?', confirmText: 'Importa' })
    if (!ok) { e.target.value = ''; return }
  }
  try {
    const result = await importCSV(user.value.username, file)
    await refreshFromServer(user.value.username)
    message.value = `Importate ${result.imported} misurazioni`
    if (result.errors.length > 0) {
      message.value += ` (${result.errors.length} errori)`
    }
  } catch (err) { message.value = err.message }
  e.target.value = ''
}

// --- Profile ---
async function handleSaveProfile() {
  profileMessage.value = ''
  try {
    const bd = profileBirthDate.value || null
    if (bd) {
      const age = computeAge(bd)
      if (age !== null && (age < 1 || age > 120)) {
        profileMessage.value = 'Data di nascita non valida'
        return
      }
      if (new Date(bd) > new Date()) {
        profileMessage.value = 'La data di nascita non può essere futura'
        return
      }
    }
    await updateUserProfile({
      birthDate: bd,
      gender: profileGender.value || null,
      profileCompleted: true
    })
    savedBirthDate.value = profileBirthDate.value
    savedGender.value = profileGender.value
    profileMessage.value = 'Profilo aggiornato!'
    setTimeout(() => profileMessage.value = '', 3000)
  } catch (e) {
    profileMessage.value = e.message
  }
}

// --- Keep-Alive ---
async function toggleKeepAlive() {
  if (keepAliveOn.value) {
    await stopKeepAlive()
    keepAliveOn.value = false
    storageInfo.value = null
  } else {
    await startKeepAlive(user.value.username)
    keepAliveOn.value = true
    storageInfo.value = await getStorageInfo()
  }
}

// --- PWA Install ---
const installAvailable = ref(isInstallPromptAvailable())
const appInstalled = ref(isStandalone())
const installMessage = ref('')
const cacheClearing = ref(false)
const timeBands = ref(getDefaultBands())
const timeBandsMessage = ref('')
const savedBands = ref([])

const bandsDirty = computed(() => {
  if (savedBands.value.length === 0) return false
  return JSON.stringify(timeBands.value) !== JSON.stringify(savedBands.value)
})

async function handleForceClearCache() {
  cacheClearing.value = true
  await forceClearCache()
}

async function handleInstall() {
  installMessage.value = ''
  const accepted = await promptInstall()
  if (accepted) {
    installMessage.value = '✅ App installata!'
    appInstalled.value = true
  } else {
    installMessage.value = 'Installazione annullata. Puoi riprovare in qualsiasi momento.'
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>{{ t('settings') }}</h1>
    </div>

    <!-- Account -->
    <div class="card mb-md">
      <h3 class="mb-sm">{{ t('account') }}</h3>
      <p><strong>{{ t('username') }}:</strong> {{ user?.username }}</p>
      <div class="flex items-center gap-sm">
        <span><strong>{{ t('email') }}:</strong> {{ user?.email || t('email_not_set') }}</span>
        <button class="btn btn-sm btn-ghost" @click="showEmailForm = !showEmailForm">{{ showEmailForm ? t('cancel') : t('change_email') }}</button>
      </div>
      <p><strong>Ruolo:</strong> {{ user?.role === 'admin' ? t('role_admin') : t('role_user') }}</p>
      <div v-if="showEmailForm" class="mt-sm">
        <div class="form-group"><label class="form-label">{{ t('new_email') }}</label><input v-model="newEmail" type="email" class="form-input" /></div>
        <div v-if="emailError" class="form-error mb-sm">{{ emailError }}</div>
        <button class="btn btn-primary btn-sm" @click="handleEmailChange">{{ t('update_email') }}</button>
      </div>
      <div v-if="emailSuccess" class="form-success mt-sm">{{ emailSuccess }}</div>

      <!-- Birth Date & Gender -->
      <hr style="margin:var(--space-md) 0;border-color:var(--color-border)" />
      <p class="text-secondary mb-sm" style="font-size:0.8125rem">Data di nascita e genere aiutano a personalizzare il report con riferimenti clinici adeguati.</p>
      <div class="profile-row mb-sm">
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Data di nascita</label>
          <input v-model="profileBirthDate" type="date" class="form-input" />
          <span v-if="profileBirthDate" class="text-secondary" style="font-size:0.75rem">
            Età: {{ computeAge(profileBirthDate) }} anni
          </span>
        </div>
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Genere</label>
          <select v-model="profileGender" class="form-input">
            <option value="">Non specificato</option>
            <option value="male">Maschio</option>
            <option value="female">Femmina</option>
            <option value="other">Altro</option>
          </select>
        </div>
      </div>
      <button class="btn btn-sm btn-primary" @click="handleSaveProfile" :disabled="!profileDirty">Salva profilo</button>
      <div v-if="profileMessage" class="form-success mt-sm">{{ profileMessage }}</div>
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
          <button class="btn btn-sm btn-error btn-icon" @click="removeReminder(r)" :title="t('remove')">
            <AppIcon name="trash" :size="16" color="currentColor" />
          </button>
        </div>
        <hr v-if="i < reminders.length - 1" style="margin:var(--space-md) 0;border-color:var(--color-surface-overlay)" />
      </div>
    </div>

    <!-- Time Bands Configuration -->
    <div class="card mb-md">
      <h3 class="mb-sm">⏰ Fasce Orarie</h3>
      <p class="text-secondary mb-sm" style="font-size:0.8125rem">
        Trascina i separatori per regolare le fasce. Le fasce non possono sovrapporsi.
      </p>
      <TimeBandSlider :bands="timeBands" @update:bands="timeBands = $event" />
      <div class="flex gap-sm">
        <button class="btn btn-sm btn-primary" @click="handleSaveBands" :disabled="!bandsDirty">Salva fasce</button>
        <button class="btn btn-sm btn-ghost" @click="resetBands">Ripristina default</button>
      </div>
      <div v-if="timeBandsMessage" class="form-success mt-sm">{{ timeBandsMessage }}</div>
    </div>

    <!-- Data Management -->
    <div class="card mb-md">
      <h3 class="mb-sm">Dati</h3>
      <div class="flex flex-col gap-sm">
        <button class="btn btn-sm btn-secondary" @click="handleExportCSV">📥 {{ t('export_csv') }}</button>
        <button class="btn btn-sm btn-secondary" @click="handleBackup">💾 Backup (JSON)</button>
        <button class="btn btn-sm btn-secondary" @click="triggerRestore">📂 Ripristina Backup</button>
        <input ref="restoreInput" type="file" accept=".json" style="display:none" @change="handleRestore" />
        <button class="btn btn-sm btn-secondary" @click="triggerImportCsv">📥 Importa CSV (bp-tracker)</button>
        <input ref="importCsvInput" type="file" accept=".csv" style="display:none" @change="handleImportCsv" />
        <button class="btn btn-sm btn-secondary" @click="handleGenerateTestData"><AppIcon name="robot" :size="16" /> {{ t('generate_test_data') }}</button>
      </div>
      <div v-if="message" class="form-success mt-sm">{{ message }}</div>
    </div>

    <!-- Database Keep-Alive -->
    <div class="card mb-md">
      <div class="flex justify-between items-center mb-sm">
        <h3>🔄 Keep-Alive Database</h3>
        <label class="toggle-switch">
          <input type="checkbox" :checked="keepAliveOn" @change="toggleKeepAlive" />
          <span class="toggle-slider"></span>
        </label>
      </div>
      <p class="text-secondary" style="font-size:0.875rem">
        Mantiene il database attivo con ping periodici a Supabase e richiede archiviazione persistente per evitare che i dati vengano eliminati dal browser.
      </p>
      <div v-if="keepAliveOn && storageInfo" class="mt-sm" style="font-size:0.875rem">
        <p><strong>Stato archiviazione:</strong> {{ storageInfo.persisted ? '✅ Persistente' : '☁️ Sincronizzato su Supabase' }}</p>
        <p class="text-secondary" style="font-size:0.75rem">
          I dati sono sempre salvati in cloud. La cache locale accelera il caricamento.
        </p>
        <p><strong>Spazio usato:</strong> {{ formatBytes(storageInfo.usage) }} / {{ formatBytes(storageInfo.quota) }}
          <span v-if="storageInfo.percent !== null">({{ storageInfo.percent }}%)</span>
        </p>
      </div>
    </div>

    <!-- PWA Install -->
    <div v-if="!appInstalled" class="card mb-md">
      <h3 class="mb-sm">📲 Installa App</h3>
      <p class="text-secondary mb-sm" style="font-size:0.875rem">
        Aggiungi Pressione alla schermata Home per un accesso rapido come una vera app.
      </p>

      <!-- Android / Chrome -->
      <div v-if="installAvailable">
        <button class="btn btn-primary" @click="handleInstall">
          <AppIcon name="copy" :size="16" /> Installa su Home
        </button>
        <div v-if="installMessage" class="form-success mt-sm">{{ installMessage }}</div>
      </div>

      <!-- iOS Safari -->
      <div v-else-if="isIOS()">
        <div class="ios-install-steps">
          <p class="mb-sm" style="font-weight:600">Per installare su iOS:</p>
          <ol style="padding-left:1.25rem;line-height:1.8;font-size:0.875rem">
            <li>Tocca il pulsante <strong>Condividi</strong> <span style="font-size:1.1rem">⎋</span> nella barra di Safari</li>
            <li>Scorri e seleziona <strong>"Aggiungi a Home"</strong></li>
            <li>Tocca <strong>Aggiungi</strong> per confermare</li>
          </ol>
        </div>
      </div>

      <!-- Desktop Chrome -->
      <div v-else>
        <p class="text-secondary" style="font-size:0.8125rem">
          Su desktop, usa l'icona <strong>Installa</strong> nella barra degli indirizzi del browser.
        </p>
      </div>
    </div>

    <!-- Admin -->
    <div v-if="isAdmin(user)" class="card mb-md">
      <h3 class="mb-sm">{{ t('admin') }}</h3>
      <router-link to="/operators" class="btn btn-ghost btn-sm">{{ t('user_management') }}</router-link>
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

    <!-- Cache & Updates -->
    <div class="card mb-md">
      <h3 class="mb-sm">🔄 Cache & Aggiornamenti</h3>
      <p class="text-secondary mb-sm" style="font-size:0.8125rem">
        Se l'app mostra una versione vecchia, svuota la cache per forzare il caricamento dell'ultima versione.
      </p>
      <button class="btn btn-sm btn-secondary" @click="handleForceClearCache" :disabled="cacheClearing">
        {{ cacheClearing ? 'Aggiornamento...' : 'Forza aggiornamento' }}
      </button>
    </div>

    <!-- Danger Zone -->
    <div class="card mb-md" style="border:1px solid var(--color-error)">
      <h3 class="mb-sm" style="color:var(--color-error)">{{ t('danger_zone') }}</h3>
      <button class="btn btn-error btn-sm" @click="handleDeleteAll">{{ t('delete_all_data') }}</button>
    </div>

    <p class="text-center" style="color:var(--color-text-secondary);font-size:0.75rem;padding:var(--space-lg)">
      Pressione v{{ APP_VERSION }} — build {{ BUILD_NUMBER }} — {{ new Date(BUILD_TIME).toLocaleString('it-IT') }}
    </p>
  </div>
</template>

<style scoped>
.profile-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-sm);
  align-items: start;
}
@media (max-width: 400px) {
  .profile-row {
    grid-template-columns: 1fr;
  }
}
.profile-row .form-input { min-width: 0; width: 100%; -webkit-appearance: none; }
.form-success { color: var(--color-accent); font-size: 0.875rem; font-weight: 500; }
.reminder-item { padding: var(--space-sm) 0; }
.day-chip {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid var(--color-border-strong); background: var(--color-surface-raised);
  font-size: 0.75rem; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.day-chip.active { background: var(--color-accent); color: var(--color-on-accent); border-color: var(--color-accent); }

/* Toggle Switch */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 26px;
  cursor: pointer;
}
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider {
  position: absolute;
  inset: 0;
  background: var(--color-border);
  border-radius: 26px;
  transition: background 0.2s;
}
.toggle-slider::before {
  content: '';
  position: absolute;
  width: 20px; height: 20px;
  left: 3px; bottom: 3px;
  background: white;
  border-radius: 50%;
  transition: transform 0.2s;
}
.toggle-switch input:checked + .toggle-slider { background: var(--color-accent); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(22px); }

/* iOS Install Steps */
.ios-install-steps {
  background: var(--color-surface-overlay);
  border-radius: var(--radius-md);
  padding: var(--space-md);
}

.form-success { color: var(--color-accent); font-size: 0.875rem; font-weight: 500; }
</style>

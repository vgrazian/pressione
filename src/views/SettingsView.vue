<script setup>
import { ref, onMounted, computed, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { deleteAllReadings, getReminders, upsertReminder, deleteReminder, getReadings, exportCSV, importCSV, generateTestData, refreshFromServer, backupData, restoreData } from '@/services/dataService.js'
import { useI18n } from '@/services/i18n.js'
import { startKeepAlive, stopKeepAlive, isKeepAliveActive, isKeepAliveEnabled, getStorageInfo, formatBytes } from '@/services/keepAlive.js'
import { promptInstall, isInstallPromptAvailable, isIOS, isStandalone } from '@/services/pwaInstall.js'
import { useSWUpdate } from '@/services/swUpdate.js'
import { APP_VERSION, BUILD_NUMBER, BUILD_TIME } from '@/services/version.js'
import { getUserBands, saveUserBands, getDefaultBands } from '@/services/timeBands.js'
import AppIcon from '@/components/AppIcon.vue'
import TimeBandSlider from '@/components/TimeBandSlider.vue'
import CollapsibleSection from '@/components/CollapsibleSection.vue'

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
const profileFirstName = ref('')
const profileLastName = ref('')
const profileFiscalCode = ref('')
const profilePhone = ref('')
const profileStreet = ref('')
const profileStreetNumber = ref('')
const profileCity = ref('')
const profilePostalCode = ref('')
const profileMessage = ref('')
const savedBirthDate = ref('')
const savedGender = ref('')
const savedFirstName = ref('')
const savedLastName = ref('')
const savedFiscalCode = ref('')
const savedPhone = ref('')
const savedStreet = ref('')
const savedStreetNumber = ref('')
const savedCity = ref('')
const savedPostalCode = ref('')
const importCsvInput = ref(null)
const importMode = ref('add')
const pendingImportFile = ref(null)

const profileDirty = computed(() => {
  return profileBirthDate.value !== savedBirthDate.value ||
    profileGender.value !== savedGender.value ||
    profileFirstName.value !== savedFirstName.value ||
    profileLastName.value !== savedLastName.value ||
    profileFiscalCode.value !== savedFiscalCode.value ||
    profilePhone.value !== savedPhone.value ||
    profileStreet.value !== savedStreet.value ||
    profileStreetNumber.value !== savedStreetNumber.value ||
    profileCity.value !== savedCity.value ||
    profilePostalCode.value !== savedPostalCode.value
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
  profileFirstName.value = user.value?.firstName || ''
  profileLastName.value = user.value?.lastName || ''
  profileFiscalCode.value = user.value?.fiscalCode || ''
  profilePhone.value = user.value?.phone || ''
  profileStreet.value = user.value?.street || ''
  profileStreetNumber.value = user.value?.streetNumber || ''
  profileCity.value = user.value?.city || ''
  profilePostalCode.value = user.value?.postalCode || ''
  savedBirthDate.value = profileBirthDate.value
  savedGender.value = profileGender.value
  savedFirstName.value = profileFirstName.value
  savedLastName.value = profileLastName.value
  savedFiscalCode.value = profileFiscalCode.value
  savedPhone.value = profilePhone.value
  savedStreet.value = profileStreet.value
  savedStreetNumber.value = profileStreetNumber.value
  savedCity.value = profileCity.value
  savedPostalCode.value = profilePostalCode.value
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
    const count = await generateTestData(user.value.username, 30)
    message.value = `Generati ${count} dati di test con distribuzione realistica.`
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

function handleImportCsvFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  pendingImportFile.value = file
  importMode.value = 'add'
  message.value = ''
  e.target.value = ''
}

async function executeImport() {
  if (!pendingImportFile.value) return
  const file = pendingImportFile.value
  try {
    const result = await importCSV(user.value.username, file, importMode.value)
    await refreshFromServer(user.value.username)
    message.value = `Importate ${result.imported} misurazioni`
    if (result.skipped > 0) message.value += `, ${result.skipped} duplicate saltate`
    if (result.overwritten > 0) message.value += `, ${result.overwritten} sovrascritte`
    if (result.errors.length > 0) message.value += ` (${result.errors.length} errori)`
  } catch (err) { message.value = err.message }
  pendingImportFile.value = null
}

function cancelImport() {
  pendingImportFile.value = null
  importMode.value = 'add'
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
      profileCompleted: true,
      firstName: profileFirstName.value.trim() || null,
      lastName: profileLastName.value.trim() || null,
      fiscalCode: profileFiscalCode.value.trim() || null,
      phone: profilePhone.value.trim() || null,
      street: profileStreet.value.trim() || null,
      streetNumber: profileStreetNumber.value.trim() || null,
      city: profileCity.value.trim() || null,
      postalCode: profilePostalCode.value.trim() || null
    })
    savedBirthDate.value = profileBirthDate.value
    savedGender.value = profileGender.value
    savedFirstName.value = profileFirstName.value
    savedLastName.value = profileLastName.value
    savedFiscalCode.value = profileFiscalCode.value
    savedPhone.value = profilePhone.value
    savedStreet.value = profileStreet.value
    savedStreetNumber.value = profileStreetNumber.value
    savedCity.value = profileCity.value
    savedPostalCode.value = profilePostalCode.value
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
const diagMessage = ref('')
const syncInProgress = ref(false)
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

async function handleShareDiagnostics() {
  diagMessage.value = ''
  try {
    // Test Supabase connectivity directly
    let supabaseStatus = 'not checked'
    let supabaseReadings = 'not checked'
    let syncQueueStatus = 'not checked'
    let supabaseConfigured = false
    let supabaseWriteTest = 'not checked'

    try {
      const { isSupabaseConfigured: configured } = await import('@/services/supabaseClient.js')
      supabaseConfigured = configured
      if (configured) {
        const { supabase } = await import('@/services/supabaseClient.js')
        const { data, error } = await supabase.from('readings').select('id').eq('username', user.value?.username)
        supabaseStatus = error ? 'error: ' + error.message : 'ok'
        supabaseReadings = data ? `${data.length} readings` : 'null'
        // Check sync queue
        const { db } = await import('@/db/index.js')
        const pending = await db.syncQueue.where('username').equals(user.value?.username).toArray()
        syncQueueStatus = `${pending.length} pending`
        // Test write capability
        try {
          const testId = '00000000-0000-0000-0000-000000000000'
          const { error: wErr } = await supabase.from('readings').upsert({
            id: testId, username: user.value?.username, systolic: 1, diastolic: 1,
            heart_rate: 1, timestamp: new Date().toISOString(), notes: '_diag_test_',
            created_at: new Date().toISOString(), updated_at: new Date().toISOString()
          })
          if (!wErr) {
            await supabase.from('readings').delete().eq('id', testId)
            supabaseWriteTest = 'ok'
          } else {
            supabaseWriteTest = 'error: ' + wErr.message
          }
        } catch (we) { supabaseWriteTest = 'exception: ' + we.message }
      } else {
        supabaseStatus = 'not configured'
      }
    } catch (e) {
      supabaseStatus = 'exception: ' + e.message
    }

    const info = {
      version: APP_VERSION,
      build: BUILD_NUMBER,
      buildTime: BUILD_TIME,
      userAgent: navigator.userAgent,
      standalone: isStandalone(),
      isIOS: isIOS(),
      online: navigator.onLine,
      theme: document.documentElement.getAttribute('data-theme') || 'system',
      username: user.value?.username || 'N/D',
      supabaseConfigured,
      supabaseStatus,
      supabaseReadings,
      supabaseWriteTest,
      syncQueue: syncQueueStatus,
      indexedDB: 'pending...',
      localStorageReadings: 'N/D'
    }

    // Check IndexedDB
    try {
      const readings = await getReadings(user.value?.username)
      info.indexedDB = `${readings.length} readings`
    } catch { info.indexedDB = 'error' }

    // Check localStorage bridge
    try {
      const key = `pressione_readings_${user.value?.username}`
      const raw = localStorage.getItem(key)
      info.localStorageReadings = raw ? `${JSON.parse(raw).length} readings (${raw.length} bytes)` : 'empty'
    } catch { info.localStorageReadings = 'error' }

    const text = Object.entries(info).map(([k, v]) => `${k}: ${v}`).join('\n')
    const fullText = `IperTeso Diagnostics\n${new Date().toISOString()}\n\n${text}`

    if (navigator.share) {
      await navigator.share({ title: 'IperTeso Diagnostica', text: fullText })
    } else {
      await navigator.clipboard.writeText(fullText)
      diagMessage.value = 'Diagnostica copiata negli appunti!'
      setTimeout(() => diagMessage.value = '', 3000)
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      diagMessage.value = 'Errore: ' + e.message
    }
  }
}

async function handleForceSyncToSupabase() {
  syncInProgress.value = true
  diagMessage.value = ''
  try {
    const { supabase } = await import('@/services/supabaseClient.js')
    const readings = await getReadings(user.value?.username)
    let synced = 0
    for (const r of readings) {
      const { error } = await supabase.from('readings').upsert({
        id: r.id, username: r.username, systolic: r.systolic, diastolic: r.diastolic,
        heart_rate: r.heartRate, timestamp: r.timestamp, notes: r.notes || '',
        created_at: r.updatedAt || r.timestamp, updated_at: new Date().toISOString()
      })
      if (!error) synced++
    }
    diagMessage.value = `Sincronizzate ${synced}/${readings.length} letture su Supabase`
  } catch (e) {
    diagMessage.value = 'Errore: ' + e.message
  } finally {
    syncInProgress.value = false
  }
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

    <!-- Account (always visible) -->
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
    </div>

    <!-- Anagrafica -->
    <CollapsibleSection title="📋 Anagrafica" class="mb-md">
      <p class="text-secondary mb-sm" style="font-size:0.8125rem">Data di nascita e genere aiutano a personalizzare il report.</p>
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
      <p class="text-secondary mb-sm mt-md" style="font-size:0.75rem">Dati anagrafici per il report (opzionali).</p>
      <div class="profile-row mb-sm">
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Nome</label>
          <input v-model="profileFirstName" type="text" class="form-input" placeholder="Mario" />
        </div>
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Cognome</label>
          <input v-model="profileLastName" type="text" class="form-input" placeholder="Rossi" />
        </div>
      </div>
      <div class="profile-row mb-sm">
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Codice Fiscale</label>
          <input v-model="profileFiscalCode" type="text" class="form-input" placeholder="RSSMRA80A01H501U" maxlength="16" style="text-transform:uppercase" />
        </div>
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Telefono</label>
          <input v-model="profilePhone" type="tel" class="form-input" placeholder="+39 333 1234567" />
        </div>
      </div>
      <div class="profile-row mb-sm">
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Indirizzo</label>
          <input v-model="profileStreet" type="text" class="form-input" placeholder="Via Roma" />
        </div>
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">N. civico</label>
          <input v-model="profileStreetNumber" type="text" class="form-input" placeholder="42" style="max-width:100px" />
        </div>
      </div>
      <div class="profile-row mb-sm">
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">Città</label>
          <input v-model="profileCity" type="text" class="form-input" placeholder="Milano" />
        </div>
        <div class="form-group" style="margin-bottom:0">
          <label class="form-label">CAP</label>
          <input v-model="profilePostalCode" type="text" class="form-input" placeholder="20100" maxlength="5" style="max-width:100px" inputmode="numeric" />
        </div>
      </div>
      <button class="btn btn-sm btn-primary" @click="handleSaveProfile" :disabled="!profileDirty">Salva profilo</button>
      <div v-if="profileMessage" class="form-success mt-sm">{{ profileMessage }}</div>
    </CollapsibleSection>

    <!-- Password -->
    <CollapsibleSection :title="'🔒 ' + t('change_password')" class="mb-md">
      <div class="form-group"><label class="form-label">{{ t('current_password') }}</label><input v-model="currentPassword" type="password" class="form-input" /></div>
      <div class="form-group"><label class="form-label">{{ t('new_password') }}</label><input v-model="newPassword" type="password" class="form-input" placeholder="Minimo 8 caratteri" /></div>
      <div v-if="passwordError" class="form-error mb-sm">{{ passwordError }}</div>
      <div v-if="passwordSuccess" class="form-success mb-sm">{{ passwordSuccess }}</div>
      <button class="btn btn-primary btn-sm" @click="handlePasswordChange">{{ t('update_password') }}</button>
    </CollapsibleSection>

    <!-- Reminders -->
    <CollapsibleSection :title="'🔔 ' + t('reminders')" class="mb-md">
      <div class="flex justify-end mb-sm">
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
          <button class="btn btn-sm btn-ghost-error btn-icon" @click="removeReminder(r)" :title="t('remove')">
            <AppIcon name="trash" :size="16" color="currentColor" />
          </button>
        </div>
        <hr v-if="i < reminders.length - 1" style="margin:var(--space-md) 0;border-color:var(--color-surface-overlay)" />
      </div>
    </CollapsibleSection>

    <!-- Time Bands Configuration -->
    <CollapsibleSection title="⏰ Fasce Orarie" class="mb-md">
      <p class="text-secondary mb-sm mt-sm" style="font-size:0.8125rem">
        Trascina i separatori per regolare le fasce. Le fasce non possono sovrapporsi.
      </p>
      <TimeBandSlider :bands="timeBands" @update:bands="timeBands = $event" />
      <div class="flex gap-sm">
        <button class="btn btn-sm btn-primary" @click="handleSaveBands" :disabled="!bandsDirty">Salva fasce</button>
        <button class="btn btn-sm btn-ghost" @click="resetBands">Ripristina default</button>
      </div>
      <div v-if="timeBandsMessage" class="form-success mt-sm">{{ timeBandsMessage }}</div>
    </CollapsibleSection>

    <!-- Import / Export -->
    <CollapsibleSection title="📥 Importa / Esporta" class="mb-md">
      <div class="flex flex-col gap-sm mt-sm">
        <button class="btn btn-sm btn-secondary" @click="handleExportCSV"><AppIcon name="download" :size="16" /> {{ t('export_csv') }}</button>
        <button class="btn btn-sm btn-secondary" @click="handleBackup"><AppIcon name="download" :size="16" /> Backup (JSON)</button>
        <button class="btn btn-sm btn-secondary" @click="triggerRestore"><AppIcon name="upload" :size="16" /> Ripristina Backup</button>
        <input ref="restoreInput" type="file" accept=".json" style="display:none" @change="handleRestore" />
        <button class="btn btn-sm btn-secondary" @click="triggerImportCsv"><AppIcon name="upload" :size="16" /> Importa CSV (bp-tracker)</button>
        <input ref="importCsvInput" type="file" accept=".csv" style="display:none" @change="handleImportCsvFile" />
        <button class="btn btn-sm btn-secondary" @click="handleGenerateTestData"><AppIcon name="robot" :size="16" /> {{ t('generate_test_data') }}</button>
      </div>

      <!-- Import options (shown after file selection) -->
      <div v-if="pendingImportFile" class="import-options mt-sm">
        <p class="import-options__title">
          📄 {{ pendingImportFile.name }} — Come gestire i duplicati?
        </p>
        <div class="flex flex-col gap-sm mb-sm">
          <label class="import-options__label">
            <input type="radio" v-model="importMode" value="add" />
            <span><strong>Importa tutto (anche duplicati)</strong> — non controllare letture già presenti</span>
          </label>
          <label class="import-options__label">
            <input type="radio" v-model="importMode" value="skip" />
            <span><strong>Salta duplicati</strong> — non importare letture con stesso orario</span>
          </label>
          <label class="import-options__label">
            <input type="radio" v-model="importMode" value="overwrite" />
            <span><strong>Sovrascrivi duplicati</strong> — aggiorna letture con stesso orario</span>
          </label>
        </div>
        <div class="flex gap-sm">
          <button class="btn btn-sm btn-primary" @click="executeImport">
            <AppIcon name="upload" :size="14" /> Importa
          </button>
          <button class="btn btn-sm btn-ghost" @click="cancelImport">Annulla</button>
        </div>
      </div>

      <div v-if="message" class="form-success mt-sm">{{ message }}</div>
    </CollapsibleSection>

    <CollapsibleSection title="🛠️ Strumenti avanzati" class="mb-md">

      <!-- Cache & Updates -->
      <div class="mb-md">
        <h4 class="mb-sm"><AppIcon name="refresh" :size="16" /> Cache & Aggiornamenti</h4>
        <p class="text-secondary mb-sm" style="font-size:0.8125rem">
          Se l'app mostra una versione vecchia, svuota la cache per forzare il caricamento dell'ultima versione.
        </p>
        <button class="btn btn-sm btn-secondary" @click="handleForceClearCache" :disabled="cacheClearing">
          {{ cacheClearing ? 'Aggiornamento...' : 'Forza aggiornamento' }}
        </button>
      </div>

      <!-- Diagnostica -->
      <div class="mb-md">
        <h4 class="mb-sm"><AppIcon name="settings" :size="16" /> Diagnostica</h4>
        <p class="text-secondary mb-sm" style="font-size:0.8125rem">
          Condividi le informazioni di diagnostica per aiutare a risolvere i problemi.
        </p>
        <button class="btn btn-sm btn-secondary" @click="handleShareDiagnostics">
          <AppIcon name="share" :size="16" /> Condividi diagnostica
        </button>
        <button class="btn btn-sm btn-secondary mt-sm" @click="handleForceSyncToSupabase" :disabled="syncInProgress">
          <AppIcon name="refresh" :size="16" /> {{ syncInProgress ? 'Sincronizzazione...' : 'Forza sync a Supabase' }}
        </button>
        <div v-if="diagMessage" class="form-success mt-sm">{{ diagMessage }}</div>
      </div>

      <!-- Danger Zone -->
      <div>
        <h4 class="mb-sm" style="color:var(--color-error)">{{ t('danger_zone') }}</h4>
        <button class="btn btn-error btn-sm" @click="handleDeleteAll">{{ t('delete_all_data') }}</button>
      </div>
    </CollapsibleSection>

    <!-- About -->
    <CollapsibleSection title="ℹ️ Informazioni su IperTeso" class="mb-md">
      <p style="font-size:0.875rem;line-height:1.6;color:var(--color-text-secondary)">
        <strong>Ti senti una pentola a pressione pronta a fischiare? Il medico ti ha detto di "stare tranquillo", ma vivi in Italia nel 2026? Benvenuto su IperTeso!</strong>
      </p>
      <p style="font-size:0.8125rem;line-height:1.6;color:var(--color-text-secondary);margin-top:var(--space-sm)">
        <strong>IperTeso</strong> è l'unica app di monitoraggio cardiaco che non ti giudica se hai appena urlato nel traffico o se hai mangiato un etto di bresaola salatissima a pranzo. Noi non ti diamo consigli noiosi sulla meditazione: ti aiutiamo solo a capire se è il caso di sederti un attimo o di cambiare direttamente pianeta.
      </p>
      <p style="font-size:0.8125rem;line-height:1.6;color:var(--color-text-secondary);margin-top:var(--space-sm)"><strong>Cosa puoi fare con IperTeso:</strong></p>
      <ul style="font-size:0.8125rem;line-height:1.6;color:var(--color-text-secondary);padding-left:1.25rem">
        <li><strong>Registrazione dei picchi di rabbia:</strong> Segna i tuoi valori subito dopo aver letto le email del capo o aver parlato con i parenti.</li>
        <li><strong>Grafici della disperazione:</strong> Guarda la tua pressione salire e scendere in base ai giorni della settimana (spoiler: il lunedì è tutto rosso).</li>
        <li><strong>Modalità "Ansia da Camice Bianco":</strong> Un algoritmo speciale che sottrae automaticamente 20 punti alla massima se scopre che ti sei spaventato guardando lo schermo.</li>
      </ul>
      <p style="font-size:0.75rem;line-height:1.6;color:var(--color-text-tertiary);margin-top:var(--space-md);font-style:italic">
        Nota medica (seria ma non troppo): IperTeso non sostituisce un medico vero. Se vedi i numeri della tua pressione superare i chilometri orari consentiti in autostrada, posa lo smartphone e chiama un dottore!
      </p>
    </CollapsibleSection>

    <p class="text-center" style="color:var(--color-text-secondary);font-size:0.75rem;padding:var(--space-lg)">
      IperTeso v{{ APP_VERSION }} — build {{ BUILD_NUMBER }} — {{ new Date(BUILD_TIME).toLocaleString('it-IT') }}
    </p>
    <p class="text-center" style="color:var(--color-text-secondary);font-size:0.8rem;padding-bottom:var(--space-lg)">
      fatto con ❤️ per i miei amici
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
  color: var(--color-accent);
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

.import-options {
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  color: var(--color-text-primary);
}
.import-options input[type="radio"] { accent-color: var(--color-accent); }
.import-options__title {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
}
.import-options__label {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 0.8125rem;
  cursor: pointer;
  color: var(--color-text-secondary);
}
.import-options__label strong {
  color: var(--color-text-primary);
}
</style>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'

const { completePasswordRecovery } = useAuth()
const router = useRouter()
const route = useRoute()

const newPassword = ref('')
const confirmPassword = ref('')
const busy = ref(false)
const message = ref('')
const error = ref('')

function extractRecoveryToken() {
  // Try route query params (Vue Router)
  const routeToken = String(route.query.token || '').trim()
  if (routeToken) return routeToken

  // Try parsing from the full URL (handles hash-based routing)
  try {
    const href = String(window?.location?.href || '')
    if (!href) return ''

    const hashIndex = href.indexOf('#')
    if (hashIndex >= 0) {
      const hashValue = href.slice(hashIndex + 1)
      const queryIndex = hashValue.indexOf('?')
      if (queryIndex >= 0) {
        const params = new URLSearchParams(hashValue.slice(queryIndex + 1))
        const hashToken = String(params.get('token') || '').trim()
        if (hashToken) return hashToken
      }
    }
  } catch {
    // Fall through
  }

  return ''
}

async function submitRecovery() {
  error.value = ''
  message.value = ''

  if (!newPassword.value || !confirmPassword.value) {
    error.value = 'Compila tutti i campi'
    return
  }

  const token = extractRecoveryToken()
  if (!token) {
    error.value = 'Token di reset non trovato nell\'URL'
    return
  }

  busy.value = true
  try {
    await completePasswordRecovery({
      token,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    })

    message.value = 'Password aggiornata con successo! Ora puoi accedere.'
    newPassword.value = ''
    confirmPassword.value = ''
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (err) {
    error.value = err.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-header text-center">
        <img src="/logo.png" alt="Pressione" class="login-logo" />
        <h1>Reset Password</h1>
        <p>Imposta una nuova password per il tuo account</p>
      </div>

      <div class="form-group">
        <label class="form-label" for="new-password">Nuova password</label>
        <input
          id="new-password"
          v-model="newPassword"
          type="password"
          class="form-input"
          autocomplete="new-password"
          placeholder="Minimo 8 caratteri"
          :disabled="busy"
        />
      </div>

      <div class="form-group">
        <label class="form-label" for="confirm-password">Conferma password</label>
        <input
          id="confirm-password"
          v-model="confirmPassword"
          type="password"
          class="form-input"
          autocomplete="new-password"
          placeholder="Ripeti la nuova password"
          :disabled="busy"
          @keyup.enter="submitRecovery"
        />
      </div>

      <div v-if="error" class="form-error mb-sm">{{ error }}</div>
      <div v-if="message" class="form-success mb-sm">{{ message }}</div>

      <button
        class="btn btn-primary btn-block"
        :disabled="busy || !newPassword || !confirmPassword"
        @click="submitRecovery"
      >
        {{ busy ? 'Aggiornamento in corso...' : 'Aggiorna password' }}
      </button>

      <p class="text-center mt-md">
        <router-link to="/login" class="btn btn-sm btn-ghost">← Torna al login</router-link>
      </p>
    </div>
  </div>
</template>

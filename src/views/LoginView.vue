<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import { APP_VERSION, BUILD_TIME, BUILD_NUMBER } from '@/services/version.js'

const router = useRouter()
const { login, requestPasswordResetByEmail, supportsEmailReset } = useAuth()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const showRecovery = ref(false)
const forgotEmail = ref('')
const forgotMessage = ref('')
const forgotBusy = ref(false)
const forgotResetUrl = ref('')

async function handleSubmit() {
  errorMessage.value = ''
  if (!username.value || !password.value) {
    errorMessage.value = 'Inserisci username e password'
    return
  }

  isLoading.value = true
  try {
    await login(username.value, password.value)
    router.push('/')
  } catch (e) {
    errorMessage.value = e.message || 'Errore di accesso'
  } finally {
    isLoading.value = false
  }
}

async function handleForgotPassword() {
  forgotMessage.value = ''
  forgotResetUrl.value = ''

  const email = forgotEmail.value.trim()
  if (!email || !email.includes('@')) {
    forgotMessage.value = 'Inserisci un indirizzo email valido'
    return
  }

  forgotBusy.value = true
  try {
    const result = await requestPasswordResetByEmail(email)
    if (result.resetUrl) {
      forgotResetUrl.value = result.resetUrl
      forgotMessage.value = 'Usa il link qui sotto per reimpostare la password (valido 30 minuti):'
    } else {
      forgotMessage.value = '✅ Se l\'indirizzo è registrato, riceverai un\'email con il link per reimpostare la password.'
      forgotEmail.value = ''
    }
  } catch (err) {
    forgotMessage.value = err.message
  } finally {
    forgotBusy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-header text-center">
        <img src="/logo.png" alt="Pressione" class="login-logo" />
        <h1>Pressione</h1>
        <p>Monitoraggio pressione arteriosa</p>
      </div>

      <template v-if="!showRecovery">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label" for="username">Username</label>
            <input id="username" v-model="username" type="text" class="form-input"
              autocomplete="username" placeholder="Il tuo username" :disabled="isLoading" />
          </div>

          <div class="form-group">
            <label class="form-label" for="password">Password</label>
            <input id="password" v-model="password" type="password" class="form-input"
              autocomplete="current-password" placeholder="La tua password" :disabled="isLoading" />
          </div>

          <div v-if="errorMessage" class="form-error mb-md">{{ errorMessage }}</div>

          <button type="submit" class="btn btn-primary btn-block" :disabled="isLoading">
            {{ isLoading ? 'Accesso in corso...' : 'Accedi' }}
          </button>
        </form>

        <p class="text-center mt-md">
          <button class="btn btn-sm btn-ghost" @click="showRecovery = true">Password dimenticata?</button>
        </p>
      </template>

      <!-- Recovery Flow (email-based) -->
      <template v-else>
        <p class="mb-md text-secondary">Inserisci l'email del tuo account per ricevere un link di reset password.</p>
        <div class="form-group">
          <label class="form-label" for="forgot-email">Email</label>
          <input
            id="forgot-email"
            v-model="forgotEmail"
            type="email"
            class="form-input"
            autocomplete="email"
            placeholder="La tua email"
            :disabled="forgotBusy"
            @keyup.enter="handleForgotPassword"
          />
        </div>

        <div v-if="forgotMessage" class="mb-sm" :class="forgotResetUrl ? 'form-success' : 'form-error'" style="word-break:break-all">{{ forgotMessage }}</div>

        <div v-if="forgotResetUrl" class="mb-md">
          <a :href="forgotResetUrl" class="btn btn-primary btn-block" style="font-size:0.8125rem;word-break:break-all">
            {{ forgotResetUrl }}
          </a>
          <p class="text-secondary mt-sm" style="font-size:0.75rem">Clicca il link sopra per impostare una nuova password.</p>
        </div>

        <button v-if="!forgotResetUrl" class="btn btn-primary btn-block" :disabled="forgotBusy || !forgotEmail.trim()" @click="handleForgotPassword">
          {{ forgotBusy ? 'Invio in corso...' : 'Invia richiesta' }}
        </button>

        <p class="text-center mt-md">
          <button class="btn btn-sm btn-ghost" @click="showRecovery = false; forgotEmail = ''; forgotMessage = ''; forgotResetUrl = ''">← Torna al login</button>
        </p>
      </template>
    </div>

    <p class="version-info">v{{ APP_VERSION }} — build {{ BUILD_NUMBER }} — {{ new Date(BUILD_TIME).toLocaleString('it-IT') }}</p>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--space-lg);
  background: linear-gradient(135deg, #E8F5E9 0%, #E3F2FD 100%);
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: var(--space-xl);
}

.login-header {
  margin-bottom: var(--space-xl);
}

.login-logo {
  display: block;
  width: 64px;
  height: 64px;
  margin: 0 auto var(--space-sm);
  border-radius: 12px;
}

.login-header h1 {
  color: var(--color-accent);
  margin-bottom: var(--space-xs);
}

.login-header p {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.version-info {
  text-align: center;
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
  margin-top: var(--space-lg);
  opacity: 0.7;
}
</style>

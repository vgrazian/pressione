<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'

const router = useRouter()
const { login, requestPasswordReset, completePasswordReset } = useAuth()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const showRecovery = ref(false)
const recoveryStep = ref('request') // 'request' | 'token'
const recoveryUsername = ref('')
const recoveryToken = ref('')
const recoveryNewPassword = ref('')
const recoveryError = ref('')
const recoverySuccess = ref('')

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

async function handleRequestRecovery() {
  recoveryError.value = ''
  recoverySuccess.value = ''
  if (!recoveryUsername.value) {
    recoveryError.value = 'Inserisci il tuo username'
    return
  }
  try {
    const token = await requestPasswordRecovery(recoveryUsername.value)
    recoveryToken.value = token
    recoverySuccess.value = 'Token generato. Copialo e incollalo qui sotto per reimpostare la password.'
    recoveryStep.value = 'token'
  } catch (e) {
    recoveryError.value = e.message || 'Errore nella richiesta'
  }
}

async function handleCompleteRecovery() {
  recoveryError.value = ''
  recoverySuccess.value = ''
  if (!recoveryToken.value || !recoveryNewPassword.value) {
    recoveryError.value = 'Inserisci token e nuova password'
    return
  }
  if (recoveryNewPassword.value.length < 8) {
    recoveryError.value = 'La password deve essere di almeno 8 caratteri'
    return
  }
  try {
    await completePasswordReset(recoveryToken.value, recoveryNewPassword.value)
    recoverySuccess.value = 'Password reimpostata! Ora puoi accedere.'
    setTimeout(() => {
      showRecovery.value = false
      recoveryStep.value = 'request'
      recoveryUsername.value = ''
      recoveryToken.value = ''
      recoveryNewPassword.value = ''
      recoveryError.value = ''
      recoverySuccess.value = ''
      username.value = recoveryUsername.value
    }, 2000)
  } catch (e) {
    recoveryError.value = e.message || 'Errore nel reset'
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

      <!-- Recovery Flow -->
      <template v-else>
        <div v-if="recoveryStep === 'request'">
          <p class="mb-md text-secondary">Inserisci il tuo username per ricevere un token di recupero.</p>
          <div class="form-group">
            <label class="form-label" for="rec-username">Username</label>
            <input id="rec-username" v-model="recoveryUsername" type="text" class="form-input" placeholder="Il tuo username" />
          </div>
          <div v-if="recoveryError" class="form-error mb-sm">{{ recoveryError }}</div>
          <div v-if="recoverySuccess" class="form-success mb-sm">{{ recoverySuccess }}</div>
          <button class="btn btn-primary btn-block" @click="handleRequestRecovery">Invia Richiesta</button>
        </div>

        <div v-if="recoveryStep === 'token'">
          <p class="mb-md text-secondary">Inserisci il token ricevuto e la nuova password.</p>
          <div class="form-group">
            <label class="form-label" for="rec-token">Token di recupero</label>
            <input id="rec-token" v-model="recoveryToken" type="text" class="form-input" placeholder="Incolla il token" />
          </div>
          <div class="form-group">
            <label class="form-label" for="rec-password">Nuova password</label>
            <input id="rec-password" v-model="recoveryNewPassword" type="password" class="form-input" placeholder="Minimo 8 caratteri" />
          </div>
          <div v-if="recoveryError" class="form-error mb-sm">{{ recoveryError }}</div>
          <div v-if="recoverySuccess" class="form-success mb-sm">{{ recoverySuccess }}</div>
          <button class="btn btn-primary btn-block mb-sm" @click="handleCompleteRecovery">Reimposta Password</button>
        </div>

        <p class="text-center mt-md">
          <button class="btn btn-sm btn-ghost" @click="showRecovery = false; recoveryStep = 'request'">← Torna al login</button>
        </p>
      </template>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
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
</style>

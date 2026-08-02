<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'

const router = useRouter()
const { login } = useAuth()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(false)

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
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-header text-center">
        <span class="login-icon">❤️</span>
        <h1>Pressione</h1>
        <p>Monitoraggio pressione arteriosa</p>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="form-label" for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            class="form-input"
            autocomplete="username"
            placeholder="Il tuo username"
            :disabled="isLoading"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="form-input"
            autocomplete="current-password"
            placeholder="La tua password"
            :disabled="isLoading"
          />
        </div>

        <div v-if="errorMessage" class="form-error mb-md">
          {{ errorMessage }}
        </div>

        <button type="submit" class="btn btn-primary btn-block" :disabled="isLoading">
          {{ isLoading ? 'Accesso in corso...' : 'Accedi' }}
        </button>
      </form>
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

.login-icon {
  font-size: 2.5rem;
  display: block;
  margin-bottom: var(--space-sm);
}

.login-header h1 {
  color: var(--color-primary);
  margin-bottom: var(--space-xs);
}

.login-header p {
  color: var(--color-on-surface-variant);
  font-size: 0.9375rem;
}
</style>

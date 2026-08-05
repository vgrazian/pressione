<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/services/auth.js'
import AppIcon from './AppIcon.vue'

const route = useRoute()
const router = useRouter()
const { user, logout } = useAuth()

const navItems = [
  { name: 'home', label: 'Home', icon: 'home', path: '/' },
  { name: 'readingList', label: 'Lista', icon: 'list', path: '/list' },
  { name: 'analisi', label: 'Analisi', icon: 'chart', path: '/analisi' },
  { name: 'settings', label: 'Altro', icon: 'settings', path: '/settings' },
]

function isActive(item) {
  return route.path === item.path
}

async function handleLogout() {
  await logout()
  router.push('/login')
}
</script>

<template>
  <nav class="app-nav">
    <router-link
      v-for="item in navItems"
      :key="item.name"
      :to="item.path"
      class="nav-item"
      :class="{ active: isActive(item) }"
    >
      <AppIcon :name="item.icon" :size="22" />
      <span class="nav-label">{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.app-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  background: var(--color-surface-raised);
  border-top: 1px solid var(--color-border);
  padding: 0.5rem 0 env(safe-area-inset-bottom, 0.5rem);
  z-index: 100;
  box-shadow: 0 -1px 6px rgba(0, 0, 0, 0.05);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 0.25rem 0.375rem;
  text-decoration: none;
  color: var(--color-text-tertiary);
  font-size: 0.625rem;
  font-weight: 500;
  transition: color 0.15s;
  min-width: 52px;
}

.nav-item.active,
.nav-item.router-link-exact-active {
  color: var(--color-accent);
}

.nav-item:active { opacity: 0.7; }
</style>

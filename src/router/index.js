import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuth } from '@/services/auth.js'

const routes = [
    {
        path: '/',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/LoginView.vue')
    },
    {
        path: '/add',
        name: 'addReading',
        component: () => import('@/views/AddEditReadingView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/edit/:id',
        name: 'editReading',
        component: () => import('@/views/AddEditReadingView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/list',
        name: 'readingList',
        component: () => import('@/views/ReadingListView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/analisi',
        name: 'analisi',
        component: () => import('@/views/AnalisiView.vue'),
        meta: { requiresAuth: true }
    },
    // Legacy redirects
    { path: '/statistics', redirect: '/analisi' },
    { path: '/report', redirect: '/analisi' },
    {
        path: '/settings',
        name: 'settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/operators',
        name: 'operators',
        component: () => import('@/views/OperatoriView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
        path: '/share/:token',
        name: 'sharedReport',
        component: () => import('@/views/SharedReportView.vue')
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

router.beforeEach(async (to) => {
    const { isAuthenticated, isAuthReady, user } = useAuth()

    // Wait for auth to initialize
    if (!isAuthReady.value) {
        return new Promise((resolve) => {
            const check = setInterval(() => {
                if (isAuthReady.value) {
                    clearInterval(check)
                    resolve(handleRoute())
                }
            }, 100)
        })
    }

    return handleRoute()

    function handleRoute() {
        if (to.meta.requiresAuth && !isAuthenticated.value) {
            return { name: 'login' }
        }
        if (to.meta.requiresAdmin && user.value?.role !== 'admin') {
            return { name: 'home' }
        }
        if (to.name === 'login' && isAuthenticated.value) {
            return { name: 'home' }
        }
        return true
    }
})

export default router

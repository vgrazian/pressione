import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

// Initialize SW update detection (side-effect import)
import '@/services/swUpdate.js'

const app = createApp(App)
app.use(router)
app.mount('#app')

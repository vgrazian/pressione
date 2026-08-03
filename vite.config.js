import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import { resolve } from 'path'
import { execSync } from 'child_process'

// Build info — version from package.json, git hash for build number
const pkg = JSON.parse(require('fs').readFileSync(resolve(__dirname, 'package.json'), 'utf-8'))
const appVersion = pkg.version
const buildTime = new Date().toISOString()
const buildNumber = (() => {
    try { return execSync('git rev-parse --short HEAD').toString().trim() }
    catch { return 'dev' }
})()

export default defineConfig({
    base: '/pressione/',
    define: {
        __APP_VERSION__: JSON.stringify(appVersion),
        __BUILD_TIME__: JSON.stringify(buildTime),
        __BUILD_NUMBER__: JSON.stringify(buildNumber)
    },
    plugins: [
        vue(),
        VitePWA({
            registerType: 'autoUpdate',
            includeAssets: ['logo.png', 'icon-180.png', 'icon-192.png', 'icon-512.png'],
            manifest: {
                name: 'Pressione - Monitoraggio Pressione',
                short_name: 'Pressione',
                description: 'Monitoraggio pressione arteriosa e pulsazioni',
                theme_color: '#006C4C',
                background_color: '#ffffff',
                display: 'standalone',
                orientation: 'portrait',
                icons: [
                    { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
                    { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
                    { src: 'icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
                    { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
                ]
            },
            workbox: {
                globPatterns: ['**/*.{js,css,html,svg,png,woff2}']
            }
        })
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src')
        }
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        sourcemap: false
    }
})

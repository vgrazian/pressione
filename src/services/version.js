/**
 * Build version info — injected at build time by Vite plugin.
 * Provides app version, build timestamp, and build number for display on login page.
 */

// These are replaced at build time by the versionPlugin in vite.config.js
export const APP_VERSION = __APP_VERSION__
export const BUILD_TIME = __BUILD_TIME__
export const BUILD_NUMBER = __BUILD_NUMBER__

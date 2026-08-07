<!--
  Pressione — Icon component
  Minimal inline SVG icons. No dependencies.
  Stroke-width: 1.5, size: 24px default
-->
<script setup>
const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 24 },
  color: { type: String, default: 'currentColor' }
})
</script>

<template>
  <svg
    v-if="name === 'heart'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
  </svg>

  <svg
    v-else-if="name === 'home'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
    <polyline points="9 22 9 12 15 12 15 22"/>
  </svg>

  <svg
    v-else-if="name === 'list'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <line x1="8" y1="6" x2="21" y2="6"/>
    <line x1="8" y1="12" x2="21" y2="12"/>
    <line x1="8" y1="18" x2="21" y2="18"/>
    <line x1="3" y1="6" x2="3.01" y2="6"/>
    <line x1="3" y1="12" x2="3.01" y2="12"/>
    <line x1="3" y1="18" x2="3.01" y2="18"/>
  </svg>

  <svg
    v-else-if="name === 'chart'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <line x1="18" y1="20" x2="18" y2="10"/>
    <line x1="12" y1="20" x2="12" y2="4"/>
    <line x1="6" y1="20" x2="6" y2="14"/>
  </svg>

  <svg
    v-else-if="name === 'settings'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <circle cx="12" cy="12" r="3"/>
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
  </svg>

  <svg
    v-else-if="name === 'plus'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <line x1="12" y1="5" x2="12" y2="19"/>
    <line x1="5" y1="12" x2="19" y2="12"/>
  </svg>

  <svg
    v-else-if="name === 'chevron-left'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <polyline points="15 18 9 12 15 6"/>
  </svg>

  <svg
    v-else-if="name === 'search'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <circle cx="11" cy="11" r="8"/>
    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
  </svg>

  <svg
    v-else-if="name === 'trash'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <polyline points="3 6 5 6 21 6"/>
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
  </svg>

  <svg
    v-else-if="name === 'edit'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
  </svg>

  <svg
    v-else-if="name === 'users'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
    <circle cx="9" cy="7" r="4"/>
    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
  </svg>

  <svg
    v-else-if="name === 'logout'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
    <polyline points="16 17 21 12 16 7"/>
    <line x1="21" y1="12" x2="9" y2="12"/>
  </svg>

  <svg
    v-else-if="name === 'clock'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <circle cx="12" cy="12" r="10"/>
    <polyline points="12 6 12 12 16 14"/>
  </svg>

  <svg
    v-else-if="name === 'sun'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <circle cx="12" cy="12" r="5"/>
    <line x1="12" y1="1" x2="12" y2="3"/>
    <line x1="12" y1="21" x2="12" y2="23"/>
    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
    <line x1="1" y1="12" x2="3" y2="12"/>
    <line x1="21" y1="12" x2="23" y2="12"/>
    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
  </svg>

  <svg
    v-else-if="name === 'moon'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
  </svg>

  <svg
    v-else-if="name === 'copy'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
  </svg>

  <svg
    v-else-if="name === 'share'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <circle cx="18" cy="5" r="3"/>
    <circle cx="6" cy="12" r="3"/>
    <circle cx="18" cy="19" r="3"/>
    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
  </svg>

  <svg
    v-else-if="name === 'alert'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
    <line x1="12" y1="9" x2="12" y2="13"/>
    <line x1="12" y1="17" x2="12.01" y2="17"/>
  </svg>

  <svg
    v-else-if="name === 'bell'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
  </svg>

  <svg
    v-else-if="name === 'robot'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <rect x="3" y="7" width="18" height="13" rx="2"/>
    <line x1="8" y1="7" x2="8" y2="4"/>
    <line x1="16" y1="7" x2="16" y2="4"/>
    <circle cx="9" cy="12" r="1.5"/>
    <circle cx="15" cy="12" r="1.5"/>
    <line x1="9" y1="16" x2="15" y2="16"/>
  </svg>

  <svg
    v-else-if="name === 'download'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
    <polyline points="7 10 12 15 17 10"/>
    <line x1="12" y1="15" x2="12" y2="3"/>
  </svg>

  <svg
    v-else-if="name === 'upload'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
    <polyline points="17 8 12 3 7 8"/>
    <line x1="12" y1="3" x2="12" y2="15"/>
  </svg>

  <svg
    v-else-if="name === 'refresh'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <polyline points="23 4 23 10 17 10"/>
    <polyline points="1 20 1 14 7 14"/>
    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
  </svg>

  <svg
    v-else-if="name === 'x'"
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" :stroke="color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
  >
    <line x1="18" y1="6" x2="6" y2="18"/>
    <line x1="6" y1="6" x2="18" y2="18"/>
  </svg>

  <span v-else class="icon-fallback">●</span>
</template>

<style scoped>
svg {
  display: block;
  flex-shrink: 0;
}

.icon-fallback {
  display: inline-block;
  opacity: 0.3;
  font-size: 0.5em;
}
</style>

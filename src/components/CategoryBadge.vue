<script setup>
import { computed } from 'vue'
import { getCategoryColor, getCategoryBgColor, getCategoryLabel } from '@/services/categories.js'

const props = defineProps({
  category: { type: String, required: true },
  small: { type: Boolean, default: false }
})

const color = computed(() => getCategoryColor(props.category))
const bgColor = computed(() => getCategoryBgColor(props.category))
const label = computed(() => getCategoryLabel(props.category))
</script>

<template>
  <span
    class="badge-category"
    :class="{ 'badge-category--small': small }"
    :style="{ color, backgroundColor: bgColor }"
  >
    {{ label }}
  </span>
</template>

<style scoped>
.badge-category--small {
  padding: 0.125rem 0.5rem;
  font-size: 0.6875rem;
}
</style>

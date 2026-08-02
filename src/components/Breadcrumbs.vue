<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
    items: {
        type: Array,
        required: true
        // [{ label: 'Home', to: '/' }, { label: 'Nuova Misurazione' }]
    }
})

const router = useRouter()

function navigate(to) {
    if (to) router.push(to)
}
</script>

<template>
    <nav class="breadcrumbs" aria-label="Breadcrumb">
        <template v-for="(item, i) in items" :key="i">
            <span v-if="i > 0" class="breadcrumbs__sep">›</span>
            <button
                v-if="item.to"
                class="breadcrumbs__link"
                @click="navigate(item.to)"
            >{{ item.label }}</button>
            <span v-else class="breadcrumbs__current">{{ item.label }}</span>
        </template>
    </nav>
</template>

<style scoped>
.breadcrumbs {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 0.8125rem;
    padding: var(--space-sm) 0;
    flex-wrap: wrap;
}

.breadcrumbs__link {
    color: var(--color-accent);
    background: none;
    border: none;
    cursor: pointer;
    font-size: inherit;
    padding: 0;
    text-decoration: none;
}

.breadcrumbs__link:hover {
    text-decoration: underline;
}

.breadcrumbs__sep {
    color: var(--color-text-tertiary);
    font-size: 1rem;
    line-height: 1;
}

.breadcrumbs__current {
    color: var(--color-text-secondary);
    font-weight: 500;
}
</style>

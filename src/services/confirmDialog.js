import { ref } from 'vue'

/**
 * Shared confirm-dialog state (module-level singleton).
 *
 * The dialog is rendered by <ConfirmDialog /> but its `confirm` function must
 * be injected into the whole app tree. Since <ConfirmDialog /> is a sibling of
 * <router-view> (not an ancestor), a module-level store is used so App.vue can
 * provide it to every view.
 */
const visible = ref(false)
const title = ref('')
const message = ref('')
const confirmText = ref('Conferma')
const cancelText = ref('Annulla')
const variant = ref('default')
let resolvePromise = null

export function showConfirm(options = {}) {
    title.value = options.title || 'Conferma'
    message.value = options.message || 'Sei sicuro?'
    confirmText.value = options.confirmText || 'Conferma'
    cancelText.value = options.cancelText || 'Annulla'
    variant.value = options.variant || 'default'
    visible.value = true

    return new Promise((resolve) => {
        resolvePromise = resolve
    })
}

export function resolveConfirm(value) {
    visible.value = false
    if (resolvePromise) {
        resolvePromise(value)
        resolvePromise = null
    }
}

export function useConfirmDialogState() {
    return { visible, title, message, confirmText, cancelText, variant }
}

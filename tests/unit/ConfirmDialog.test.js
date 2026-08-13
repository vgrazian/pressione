import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { showConfirm, resolveConfirm, useConfirmDialogState } from '@/services/confirmDialog.js'

describe('ConfirmDialog', () => {
    beforeEach(() => {
        // Reset the shared store between tests
        const state = useConfirmDialogState()
        state.visible.value = false
        state.title.value = ''
        state.message.value = ''
    })

    it('is not visible by default', () => {
        const wrapper = mount(ConfirmDialog, { attachTo: document.body })
        // Teleport renders nothing when v-if is false
        expect(document.body.querySelector('.dialog-overlay')).toBeNull()
        wrapper.unmount()
    })

    it('renders the dialog when showConfirm is called', async () => {
        const wrapper = mount(ConfirmDialog, { attachTo: document.body })
        showConfirm({ title: 'Test', message: 'Are you sure?' })
        await wrapper.vm.$nextTick()

        const overlay = document.body.querySelector('.dialog-overlay')
        expect(overlay).not.toBeNull()
        expect(overlay.textContent).toContain('Test')
        expect(overlay.textContent).toContain('Are you sure?')

        resolveConfirm(false)
        await wrapper.vm.$nextTick()
        wrapper.unmount()
    })

    it('showConfirm returns a promise that resolves true on confirm', async () => {
        const promise = showConfirm({ title: 'Test', message: 'Msg' })
        resolveConfirm(true)
        await expect(promise).resolves.toBe(true)
    })

    it('showConfirm returns a promise that resolves false on cancel', async () => {
        const promise = showConfirm({ title: 'Test', message: 'Msg' })
        resolveConfirm(false)
        await expect(promise).resolves.toBe(false)
    })

    it('confirm button resolves true and hides the dialog', async () => {
        const wrapper = mount(ConfirmDialog, { attachTo: document.body })
        const promise = showConfirm({ title: 'Test', message: 'Msg', confirmText: 'Sì', cancelText: 'No' })
        await wrapper.vm.$nextTick()

        const confirmBtn = [...document.body.querySelectorAll('.dialog-overlay button')]
            .find((b) => b.textContent.trim() === 'Sì')
        expect(confirmBtn).toBeTruthy()
        confirmBtn.click()
        await wrapper.vm.$nextTick()

        await expect(promise).resolves.toBe(true)
        expect(document.body.querySelector('.dialog-overlay')).toBeNull()
        wrapper.unmount()
    })
})


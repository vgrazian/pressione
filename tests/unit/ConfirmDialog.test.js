import { describe, it, expect, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

describe('ConfirmDialog', () => {
    it('is not visible by default', () => {
        const wrapper = shallowMount(ConfirmDialog)
        // Teleport renders nothing when v-if is false
        expect(wrapper.find('.dialog-overlay').exists()).toBe(false)
    })

    it('show() does not throw and returns a promise', () => {
        const wrapper = shallowMount(ConfirmDialog)
        const promise = wrapper.vm.show({ title: 'Test', message: 'Are you sure?' })
        expect(promise).toBeInstanceOf(Promise)
    })

    it('exposes show method', () => {
        const wrapper = shallowMount(ConfirmDialog)
        expect(typeof wrapper.vm.show).toBe('function')
    })

    it('confirm resolves promise with true', async () => {
        const wrapper = shallowMount(ConfirmDialog)
        const vm = wrapper.vm
        const promise = vm.show({ title: 'Test', message: 'Msg' })
        vm.confirm()
        const result = await promise
        expect(result).toBe(true)
    })

    it('cancel resolves promise with false', async () => {
        const wrapper = shallowMount(ConfirmDialog)
        const vm = wrapper.vm
        const promise = vm.show({ title: 'Test', message: 'Msg' })
        vm.cancel()
        const result = await promise
        expect(result).toBe(false)
    })
})

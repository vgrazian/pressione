import { describe, it, expect, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import ProfilePrompt from '@/components/ProfilePrompt.vue'

const mockUpdateProfile = vi.fn(() => Promise.resolve())
vi.mock('@/services/auth.js', () => ({
    useAuth: () => ({
        user: { value: { username: 'test', birthDate: null, gender: null } },
        updateUserProfile: mockUpdateProfile
    })
}))

describe('ProfilePrompt', () => {
    it('renders heading', () => {
        const wrapper = shallowMount(ProfilePrompt)
        expect(wrapper.find('h2').text()).toBe('Completa il tuo profilo')
    })

    it('renders date of birth input', () => {
        const wrapper = shallowMount(ProfilePrompt)
        expect(wrapper.find('#pp-birthdate').exists()).toBe(true)
    })

    it('renders gender select', () => {
        const wrapper = shallowMount(ProfilePrompt)
        expect(wrapper.find('#pp-gender').exists()).toBe(true)
    })

    it('renders a save button', () => {
        const wrapper = shallowMount(ProfilePrompt)
        const buttons = wrapper.findAll('button')
        const saveBtn = buttons.find(b => b.text().includes('Salva'))
        expect(saveBtn).toBeTruthy()
    })

    it('renders a skip button', () => {
        const wrapper = shallowMount(ProfilePrompt)
        const buttons = wrapper.findAll('button')
        const skipBtn = buttons.find(b => b.text().includes('Salta'))
        expect(skipBtn).toBeTruthy()
    })

    it('renders checkbox', () => {
        const wrapper = shallowMount(ProfilePrompt)
        expect(wrapper.find('#pp-skip').exists()).toBe(true)
    })

    it('emits close on skip click', async () => {
        const wrapper = shallowMount(ProfilePrompt)
        const buttons = wrapper.findAll('button')
        const skipBtn = buttons.find(b => b.text().includes('Salta'))
        await skipBtn.trigger('click')
        await wrapper.vm.$nextTick()
        expect(wrapper.emitted('close')).toBeTruthy()
    })

    it('emits close on overlay click', async () => {
        const wrapper = shallowMount(ProfilePrompt)
        await wrapper.find('.profile-prompt-overlay').trigger('click')
        expect(wrapper.emitted('close')).toBeTruthy()
    })

    it('calls updateUserProfile on save with gender', async () => {
        const wrapper = shallowMount(ProfilePrompt)
        const select = wrapper.find('#pp-gender')
        await select.setValue('male')
        const buttons = wrapper.findAll('button')
        const saveBtn = buttons.find(b => b.text().includes('Salva'))
        await saveBtn.trigger('click')
        await wrapper.vm.$nextTick()
        await new Promise(r => setTimeout(r, 50))
        expect(mockUpdateProfile).toHaveBeenCalled()
    })
})

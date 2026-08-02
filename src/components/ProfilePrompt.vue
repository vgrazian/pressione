<script setup>
import { ref } from 'vue'
import { useAuth } from '@/services/auth.js'

const emit = defineEmits(['close'])

const { user, updateUserProfile } = useAuth()

const age = ref(user.value?.age || '')
const gender = ref(user.value?.gender || '')
const skipForever = ref(false)
const error = ref('')
const saving = ref(false)

const genderOptions = [
    { value: '', label: 'Preferisco non specificare' },
    { value: 'male', label: 'Maschio' },
    { value: 'female', label: 'Femmina' },
    { value: 'other', label: 'Altro' }
]

async function handleSave() {
    error.value = ''
    saving.value = true
    try {
        const ageNum = age.value ? parseInt(age.value) : null
        if (ageNum !== null && (ageNum < 1 || ageNum > 120)) {
            error.value = "L'età deve essere tra 1 e 120 anni"
            saving.value = false
            return
        }

        await updateUserProfile({
            age: ageNum,
            gender: gender.value || null,
            profileCompleted: true,
            skipProfilePrompt: skipForever.value
        })
        emit('close')
    } catch (e) {
        error.value = e.message
    } finally {
        saving.value = false
    }
}

async function handleSkip() {
    await updateUserProfile({
        skipProfilePrompt: true
    })
    emit('close')
}
</script>

<template>
    <div class="profile-prompt-overlay" @click.self="emit('close')">
        <div class="profile-prompt card">
            <h2>Completa il tuo profilo</h2>
            <p class="text-secondary mb-md" style="font-size:0.875rem">
                Età e genere (opzionali) aiutano a personalizzare il report con riferimenti clinici adeguati.
                Puoi modificarli in qualsiasi momento dalle Impostazioni.
            </p>

            <div class="form-group">
                <label class="form-label" for="pp-age">Età (anni)</label>
                <input id="pp-age" v-model="age" type="number" class="form-input"
                    placeholder="Es. 45" min="1" max="120" inputmode="numeric" />
            </div>

            <div class="form-group">
                <label class="form-label" for="pp-gender">Genere</label>
                <select id="pp-gender" v-model="gender" class="form-input">
                    <option v-for="g in genderOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
                </select>
            </div>

            <div v-if="error" class="form-error mb-sm">{{ error }}</div>

            <div class="flex items-center gap-sm mb-md" style="font-size:0.8125rem">
                <input type="checkbox" id="pp-skip" v-model="skipForever" />
                <label for="pp-skip" style="cursor:pointer;color:var(--color-text-tertiary)">
                    Non chiedermelo più
                </label>
            </div>

            <div class="flex gap-sm">
                <button class="btn btn-primary" @click="handleSave" :disabled="saving">
                    {{ saving ? 'Salvataggio...' : 'Salva' }}
                </button>
                <button class="btn btn-ghost btn-sm" @click="handleSkip">
                    Salta
                </button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.profile-prompt-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 200;
    padding: 2rem;
}

.profile-prompt {
    max-width: 380px;
    width: 100%;
    padding: var(--space-xl);
}

.profile-prompt h2 {
    margin-bottom: var(--space-sm);
    color: var(--color-accent);
}
</style>

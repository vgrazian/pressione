<script setup>
import { ref } from 'vue'
import { useAuth } from '@/services/auth.js'

const emit = defineEmits(['close'])

const { user, updateUserProfile } = useAuth()

const birthDate = ref(user.value?.birthDate || '')
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

function computeAge(birthDateStr) {
    if (!birthDateStr) return null
    const today = new Date()
    const birth = new Date(birthDateStr)
    let age = today.getFullYear() - birth.getFullYear()
    const m = today.getMonth() - birth.getMonth()
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
        age--
    }
    return age
}

async function handleSave() {
    error.value = ''
    saving.value = true
    try {
        const bd = birthDate.value || null
        if (bd) {
            const age = computeAge(bd)
            if (age !== null && (age < 1 || age > 120)) {
                error.value = 'Data di nascita non valida'
                saving.value = false
                return
            }
            // Don't allow future dates
            if (new Date(bd) > new Date()) {
                error.value = 'La data di nascita non può essere futura'
                saving.value = false
                return
            }
        }

        await updateUserProfile({
            birthDate: bd,
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
                Data di nascita e genere (opzionali) aiutano a personalizzare il report con riferimenti clinici adeguati.
                Puoi modificarli in qualsiasi momento dalle Impostazioni.
            </p>

            <div class="form-group">
                <label class="form-label" for="pp-birthdate">Data di nascita</label>
                <input id="pp-birthdate" v-model="birthDate" type="date" class="form-input" />
                <span v-if="birthDate" class="text-secondary" style="font-size:0.75rem">
                    Età calcolata: {{ computeAge(birthDate) }} anni
                </span>
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

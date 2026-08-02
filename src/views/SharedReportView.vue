<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { supabase } from '@/services/supabaseClient.js'

const route = useRoute()
const report = ref(null)
const error = ref('')
const isLoading = ref(true)
const needsPin = ref(false)
const pinInput = ref('')
const pinError = ref('')

async function hashPin(pin) {
  const encoder = new TextEncoder()
  const data = encoder.encode(pin)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('')
}

async function loadReport(pinHash = null) {
  isLoading.value = true; error.value = ''
  try {
    const token = route.params.token
    const { data, error: err } = await supabase.from('settings')
      .select('value').eq('username', '_share_' + token).single()

    if (err || !data) throw new Error('not found')
    const stored = JSON.parse(data.value)
    if (stored.revoked || new Date(stored.expiresAt) < new Date()) throw new Error('expired')

    if (stored.pinHash) {
      if (!pinHash) { needsPin.value = true; isLoading.value = false; return }
      if (pinHash !== stored.pinHash) throw new Error('wrong pin')
    }
    report.value = stored.reportData
  } catch (e) {
    error.value = 'Link non valido, scaduto o revocato.'
  } finally { isLoading.value = false }
}

async function submitPin() {
  pinError.value = ''
  if (pinInput.value.length !== 4) { pinError.value = 'Inserisci 4 cifre'; return }
  const h = await hashPin(pinInput.value)
  await loadReport(h)
}

onMounted(() => loadReport())
</script>

<template>
  <div style="max-width:800px;margin:0 auto;padding:2rem;font-family:Inter,sans-serif;">
    <div v-if="isLoading" style="text-align:center;padding:4rem;"><p>Caricamento...</p></div>

    <!-- PIN Gate -->
    <div v-else-if="needsPin" style="text-align:center;padding:4rem;">
      <h2 style="color:#006C4C;margin-bottom:1rem">🔒 Report Protetto</h2>
      <p style="color:#666;margin-bottom:1.5rem">Inserisci il PIN di 4 cifre fornito dal paziente.</p>
      <input v-model="pinInput" type="text" inputmode="numeric" maxlength="4" placeholder="1234"
        style="font-size:2rem;text-align:center;width:120px;padding:8px;border:2px solid #006C4C;border-radius:8px;letter-spacing:8px" />
      <div v-if="pinError" style="color:#BA1A1A;margin-top:8px;font-size:0.875rem">{{ pinError }}</div>
      <button @click="submitPin" style="margin-top:1rem;padding:8px 24px;background:#006C4C;color:white;border:none;border-radius:8px;font-size:1rem;cursor:pointer">Sblocca</button>
    </div>

    <div v-else-if="error" style="text-align:center;padding:4rem;">
      <h2 style="color:#BA1A1A">⚠️ {{ error }}</h2>
    </div>

    <template v-else-if="report">
      <h1 style="color:#006C4C;margin-bottom:0.5rem">Report Pressione Arteriosa</h1>
      <p style="color:#666;margin-bottom:1.5rem">{{ report.readings?.length || 0 }} misurazioni</p>
      <div v-if="report.readings?.length" style="overflow-x:auto">
        <table style="width:100%;border-collapse:collapse;font-size:0.875rem">
          <thead><tr style="background:#E8F5E9">
            <th style="padding:8px;text-align:left">Data</th><th style="padding:8px;text-align:right">Sistolica</th><th style="padding:8px;text-align:right">Diastolica</th><th style="padding:8px;text-align:right">BPM</th><th style="padding:8px;text-align:left">Categoria</th><th style="padding:8px;text-align:left">Note</th>
          </tr></thead>
          <tbody>
            <tr v-for="(r, i) in report.readings" :key="i" style="border-bottom:1px solid #E0E0E0">
              <td style="padding:6px 8px">{{ new Date(r.timestamp).toLocaleDateString('it-IT') }}</td>
              <td style="padding:6px 8px;text-align:right;font-weight:600">{{ r.systolic }}</td>
              <td style="padding:6px 8px;text-align:right;font-weight:600">{{ r.diastolic }}</td>
              <td style="padding:6px 8px;text-align:right">{{ r.heartRate }}</td>
              <td style="padding:6px 8px">{{ r.category }}</td>
              <td style="padding:6px 8px;color:#666;font-size:0.8125rem">{{ r.notes }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p style="color:#999;font-size:0.75rem;margin-top:2rem;text-align:center">Report generato da Pressione App — Autodistruzione dopo 48 ore</p>
    </template>
  </div>
</template>

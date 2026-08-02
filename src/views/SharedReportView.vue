<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { supabase } from '@/services/supabaseClient.js'

const route = useRoute()
const report = ref(null)
const error = ref('')
const isLoading = ref(true)

onMounted(async () => {
  const token = route.params.token
  const pin = route.query.pin || null
  try {
    const { data, error: err } = await supabase.rpc('get_shared_report', {
      p_token: token, p_pin: pin || null
    })
    if (err) throw err
    report.value = data.report_data
  } catch (e) {
    error.value = 'Link non valido o scaduto.'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div style="max-width:800px;margin:0 auto;padding:2rem;font-family:Inter,sans-serif;">
    <div v-if="isLoading" style="text-align:center;padding:4rem;">
      <p>Caricamento report...</p>
    </div>

    <div v-else-if="error" style="text-align:center;padding:4rem;">
      <h2 style="color:#BA1A1A">⚠️ {{ error }}</h2>
      <p style="color:#666">Il link potrebbe essere scaduto o revocato.</p>
    </div>

    <template v-else-if="report">
      <h1 style="color:#006C4C;margin-bottom:0.5rem">Report Pressione Arteriosa</h1>
      <p style="color:#666;margin-bottom:1.5rem">
        {{ report.readings?.length || 0 }} misurazioni — Generato il {{ new Date().toLocaleDateString('it-IT') }}
      </p>

      <div v-if="report.readings?.length" style="overflow-x:auto">
        <table style="width:100%;border-collapse:collapse;font-size:0.875rem">
          <thead>
            <tr style="background:#E8F5E9">
              <th style="padding:8px;text-align:left">Data</th>
              <th style="padding:8px;text-align:right">Sistolica</th>
              <th style="padding:8px;text-align:right">Diastolica</th>
              <th style="padding:8px;text-align:right">BPM</th>
              <th style="padding:8px;text-align:left">Categoria</th>
              <th style="padding:8px;text-align:left">Note</th>
            </tr>
          </thead>
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

      <p style="color:#999;font-size:0.75rem;margin-top:2rem;text-align:center">
        Report generato da Pressione App — Questo link si autodistruggerà dopo 48 ore
      </p>
    </template>
  </div>
</template>

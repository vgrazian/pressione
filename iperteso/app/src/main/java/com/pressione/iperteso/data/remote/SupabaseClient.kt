package com.pressione.iperteso.data.remote

import android.util.Log
import com.pressione.iperteso.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    private const val TAG = "IperTeso/Supabase"

    val client: SupabaseClient by lazy {
        Log.d(TAG, "Creating Supabase client: URL=${BuildConfig.SUPABASE_URL}")
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Postgrest)
        }.also {
            Log.d(TAG, "Supabase client created successfully")
        }
    }
}

package com.baltajmn.aptracker.core.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

fun createSupabaseClient() = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.ANON_KEY
) {
    install(Auth) {
        autoSaveToStorage = true
        autoLoadFromStorage = true
    }
    install(Postgrest)
    install(Realtime)
}

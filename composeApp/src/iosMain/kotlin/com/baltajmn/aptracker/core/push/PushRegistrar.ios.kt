package com.baltajmn.aptracker.core.push

import io.github.jan.supabase.SupabaseClient

// iOS receives notifications via ntfy, so there is nothing to register here.
actual fun createPushRegistrar(supabase: SupabaseClient): PushRegistrar =
    object : PushRegistrar {
        override suspend fun register(userId: String) {}
    }

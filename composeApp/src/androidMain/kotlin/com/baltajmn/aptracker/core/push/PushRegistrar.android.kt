package com.baltajmn.aptracker.core.push

import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createPushRegistrar(supabase: SupabaseClient): PushRegistrar =
    AndroidPushRegistrar(supabase)

private class AndroidPushRegistrar(private val supabase: SupabaseClient) : PushRegistrar {

    override suspend fun register(userId: String) {
        val token = currentToken() ?: return
        runCatching {
            supabase.from("push_tokens").upsert(
                PushTokenDto(token = token, platform = "android", userId = userId)
            ) { onConflict = "token" }
        }
    }

    private suspend fun currentToken(): String? {
        // getInstance() throws if the app was built without google-services.json
        // (Firebase not initialized) — treat that as "FCM unavailable, use ntfy".
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return null
        return suspendCancellableCoroutine { cont ->
            messaging.token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }
}

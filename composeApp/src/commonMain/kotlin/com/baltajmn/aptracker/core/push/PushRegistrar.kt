package com.baltajmn.aptracker.core.push

import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registers this device to receive native push notifications.
 * Android: obtains the FCM token and stores it in Supabase `push_tokens`.
 * iOS: no-op — iOS receives notifications via ntfy instead.
 */
interface PushRegistrar {
    suspend fun register(userId: String)
}

expect fun createPushRegistrar(supabase: SupabaseClient): PushRegistrar

@Serializable
data class PushTokenDto(
    val token: String,
    val platform: String,
    @SerialName("user_id") val userId: String
)

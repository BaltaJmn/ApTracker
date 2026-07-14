package com.baltajmn.aptracker.core.data.repository

import com.baltajmn.aptracker.core.domain.model.ActivityEvent
import com.baltajmn.aptracker.core.domain.repository.ActivityRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class SupabaseActivityRepository(private val supabase: SupabaseClient) : ActivityRepository {

    override suspend fun getActivityForRoom(roomId: String): Result<List<ActivityEvent>> = runCatching {
        supabase.from("activity")
            .select { filter { eq("room_id", roomId) } }
            .decodeList()
    }

    override suspend fun saveEvent(event: ActivityEvent): Result<Unit> = runCatching {
        supabase.from("activity").insert(event)
    }

    override fun observeActivityForRoom(roomId: String): Flow<ActivityEvent> = callbackFlow {
        val realtimeChannel = supabase.realtime.channel("activity-$roomId")

        // Collect postgres change events — filter client-side since server-side filter
        // DSL changed in supabase-kt 3.x (PostgresChangeFilter.filter is private)
        val collectionJob = launch {
            realtimeChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "activity"
            }.collect { action ->
                val event = action.decodeRecord<ActivityEvent>()
                if (event.roomId == roomId) {
                    channel.trySend(event)
                }
            }
        }

        supabase.realtime.connect()
        realtimeChannel.subscribe()

        awaitClose {
            collectionJob.cancel()
            launch { realtimeChannel.unsubscribe() }
        }
    }
}

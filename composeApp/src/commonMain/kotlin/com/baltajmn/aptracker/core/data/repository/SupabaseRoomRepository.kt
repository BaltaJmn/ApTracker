package com.baltajmn.aptracker.core.data.repository

import com.baltajmn.aptracker.core.domain.model.Room
import com.baltajmn.aptracker.core.domain.repository.RoomRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class SupabaseRoomRepository(private val supabase: SupabaseClient) : RoomRepository {

    override suspend fun getRooms(): Result<List<Room>> = runCatching {
        supabase.from("rooms").select().decodeList()
    }

    override suspend fun getRoom(roomId: String): Result<Room> = runCatching {
        supabase.from("rooms")
            .select { filter { eq("id", roomId) } }
            .decodeSingle()
    }

    override suspend fun addRoom(room: Room): Result<Room> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: supabase.auth.currentSessionOrNull()?.user?.id
            ?: error("Not authenticated")
        supabase.from("rooms").insert(room.copy(userId = userId)) { select() }.decodeSingle()
    }

    override suspend fun updateRoom(room: Room): Result<Unit> = runCatching {
        supabase.from("rooms").update(room) { filter { eq("id", room.id) } }
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> = runCatching {
        supabase.from("rooms").delete { filter { eq("id", roomId) } }
    }
}

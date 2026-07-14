package com.baltajmn.aptracker.core.data.repository

import com.baltajmn.aptracker.core.domain.model.Slot
import com.baltajmn.aptracker.core.domain.repository.SlotRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class SupabaseSlotRepository(private val supabase: SupabaseClient) : SlotRepository {

    override suspend fun getSlotsForRoom(roomId: String): Result<List<Slot>> = runCatching {
        supabase.from("slots")
            .select { filter { eq("room_id", roomId) } }
            .decodeList()
    }

    override suspend fun addSlot(slot: Slot): Result<Slot> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: supabase.auth.currentSessionOrNull()?.user?.id
            ?: error("Not authenticated")
        supabase.from("slots").insert(slot.copy(userId = userId)) { select() }.decodeSingle()
    }

    override suspend fun updateSlot(slot: Slot): Result<Unit> = runCatching {
        supabase.from("slots").update(slot) { filter { eq("id", slot.id) } }
    }

    override suspend fun deleteSlot(slotId: String): Result<Unit> = runCatching {
        supabase.from("slots").delete { filter { eq("id", slotId) } }
    }
}

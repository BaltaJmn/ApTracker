package com.baltajmn.aptracker.core.domain.repository

import com.baltajmn.aptracker.core.domain.model.Slot

interface SlotRepository {
    suspend fun getSlotsForRoom(roomId: String): Result<List<Slot>>
    suspend fun addSlot(slot: Slot): Result<Slot>
    suspend fun updateSlot(slot: Slot): Result<Unit>
    suspend fun deleteSlot(slotId: String): Result<Unit>
}

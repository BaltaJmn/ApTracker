package com.baltajmn.aptracker.core.domain.repository

import com.baltajmn.aptracker.core.domain.model.Room

interface RoomRepository {
    suspend fun getRooms(): Result<List<Room>>
    suspend fun getRoom(roomId: String): Result<Room>
    suspend fun addRoom(room: Room): Result<Room>
    suspend fun updateRoom(room: Room): Result<Unit>
    suspend fun deleteRoom(roomId: String): Result<Unit>
}

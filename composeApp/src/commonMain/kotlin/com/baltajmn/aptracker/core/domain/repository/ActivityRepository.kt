package com.baltajmn.aptracker.core.domain.repository

import com.baltajmn.aptracker.core.domain.model.ActivityEvent
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    suspend fun getActivityForRoom(roomId: String): Result<List<ActivityEvent>>
    suspend fun saveEvent(event: ActivityEvent): Result<Unit>
    fun observeActivityForRoom(roomId: String): Flow<ActivityEvent>
}

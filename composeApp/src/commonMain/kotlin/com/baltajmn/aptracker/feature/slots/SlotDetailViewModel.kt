package com.baltajmn.aptracker.feature.slots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.baltajmn.aptracker.core.domain.model.ActivityEvent
import com.baltajmn.aptracker.core.domain.model.Slot
import com.baltajmn.aptracker.core.domain.repository.ActivityRepository
import com.baltajmn.aptracker.core.domain.repository.SlotRepository
import com.baltajmn.aptracker.core.navigation.SlotDetailRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SlotDetailUiState(
    val slot: Slot? = null,
    val activity: List<ActivityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Read-only view over a slot's activity feed. The live tracking + notifications
 * are handled by the Raspberry Pi bridge, which writes to Supabase; here we just
 * load the history and subscribe to Realtime inserts for new events.
 */
class SlotDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SlotDetailRoute>()
    private val slotId = route.slotId
    private val slotName = route.slotName
    private val roomId = route.roomId

    private val _state = MutableStateFlow(SlotDetailUiState())
    val state: StateFlow<SlotDetailUiState> = _state.asStateFlow()

    init {
        loadData()
        observeLiveActivity()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val slotsDeferred = async { slotRepository.getSlotsForRoom(roomId) }
            val activityDeferred = async { activityRepository.getActivityForRoom(roomId) }
            val slotsResult = slotsDeferred.await()
            val activityResult = activityDeferred.await()
            val slot = slotsResult.getOrNull()?.firstOrNull { it.id == slotId }
            _state.update {
                it.copy(
                    slot = slot,
                    activity = activityResult.getOrDefault(emptyList())
                        .filter { e -> e.slotName == slotName }
                        .sortedByDescending { e -> e.timestamp },
                    isLoading = false,
                    error = slotsResult.exceptionOrNull()?.message
                        ?: activityResult.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun observeLiveActivity() {
        viewModelScope.launch {
            activityRepository.observeActivityForRoom(roomId)
                .filter { it.slotName == slotName }
                .collect { event ->
                    _state.update { s ->
                        val alreadyPresent = event.id.isNotEmpty() && s.activity.any { it.id == event.id }
                        if (alreadyPresent) s else s.copy(activity = listOf(event) + s.activity)
                    }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

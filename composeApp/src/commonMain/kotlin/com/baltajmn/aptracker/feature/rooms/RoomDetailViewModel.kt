package com.baltajmn.aptracker.feature.rooms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.baltajmn.aptracker.core.domain.model.Room
import com.baltajmn.aptracker.core.domain.model.Slot
import com.baltajmn.aptracker.core.domain.repository.RoomRepository
import com.baltajmn.aptracker.core.domain.repository.SlotRepository
import com.baltajmn.aptracker.core.navigation.RoomDetailRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomDetailUiState(
    val room: Room? = null,
    val slots: List<Slot> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSlotDialog: Boolean = false
)

class RoomDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val slotRepository: SlotRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RoomDetailRoute>()
    private val roomId = route.roomId

    private val _state = MutableStateFlow(RoomDetailUiState())
    val state: StateFlow<RoomDetailUiState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val roomDeferred = async { roomRepository.getRoom(roomId) }
            val slotsDeferred = async { slotRepository.getSlotsForRoom(roomId) }
            val roomResult = roomDeferred.await()
            val slotsResult = slotsDeferred.await()
            _state.update {
                it.copy(
                    room = roomResult.getOrNull(),
                    slots = slotsResult.getOrDefault(emptyList()),
                    isLoading = false,
                    error = roomResult.exceptionOrNull()?.message
                        ?: slotsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun addSlot(slotName: String, gameName: String?) {
        viewModelScope.launch {
            val slot = Slot(
                roomId = roomId,
                slotName = slotName.trim(),
                gameName = gameName?.ifBlank { null }
            )
            slotRepository.addSlot(slot).fold(
                onSuccess = { saved -> _state.update { it.copy(slots = it.slots + saved) } },
                onFailure = { e -> _state.update { it.copy(error = e.message) } }
            )
            hideAddSlot()
        }
    }

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            slotRepository.deleteSlot(slotId)
                .onSuccess { _state.update { it.copy(slots = it.slots.filter { s -> s.id != slotId }) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun showAddSlot() = _state.update { it.copy(showAddSlotDialog = true) }
    fun hideAddSlot() = _state.update { it.copy(showAddSlotDialog = false) }
    fun clearError() = _state.update { it.copy(error = null) }
}

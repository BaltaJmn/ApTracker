package com.baltajmn.aptracker.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.aptracker.core.domain.model.Slot
import com.baltajmn.aptracker.core.domain.repository.RoomRepository
import com.baltajmn.aptracker.core.domain.repository.SlotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationPrefsUiState(
    val slots: List<Slot> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationPrefsViewModel(
    private val roomRepository: RoomRepository,
    private val slotRepository: SlotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationPrefsUiState())
    val state: StateFlow<NotificationPrefsUiState> = _state.asStateFlow()

    init {
        loadAllSlots()
    }

    fun loadAllSlots() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val rooms = roomRepository.getRooms().getOrDefault(emptyList())
            val allSlots = rooms.map { room ->
                async { slotRepository.getSlotsForRoom(room.id).getOrDefault(emptyList()) }
            }.awaitAll().flatten()
            _state.update { it.copy(slots = allSlots, isLoading = false) }
        }
    }

    fun updateSlotNotifications(slot: Slot) {
        viewModelScope.launch {
            slotRepository.updateSlot(slot).fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(slots = s.slots.map { if (it.id == slot.id) slot else it })
                    }
                },
                onFailure = { e -> _state.update { it.copy(error = e.message) } }
            )
        }
    }
}

package com.baltajmn.aptracker.feature.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.aptracker.core.domain.model.Room
import com.baltajmn.aptracker.core.domain.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomsUiState(
    val rooms: List<Room> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RoomsViewModel(private val repository: RoomRepository) : ViewModel() {

    private val _state = MutableStateFlow(RoomsUiState())
    val state: StateFlow<RoomsUiState> = _state.asStateFlow()

    fun loadRooms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getRooms().fold(
                onSuccess = { rooms -> _state.update { it.copy(rooms = rooms, isLoading = false) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            repository.deleteRoom(roomId).fold(
                onSuccess = { _state.update { it.copy(rooms = it.rooms.filter { r -> r.id != roomId }) } },
                onFailure = { e -> _state.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

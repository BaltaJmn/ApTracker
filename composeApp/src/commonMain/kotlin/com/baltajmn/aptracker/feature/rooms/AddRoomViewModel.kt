package com.baltajmn.aptracker.feature.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.aptracker.core.domain.model.Room
import com.baltajmn.aptracker.core.domain.repository.RoomRepository
import com.baltajmn.aptracker.core.network.sanitizeHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AddRoomUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AddRoomViewModel(private val repository: RoomRepository) : ViewModel() {

    private val _state = MutableStateFlow(AddRoomUiState())
    val state: StateFlow<AddRoomUiState> = _state.asStateFlow()

    fun saveRoom(name: String, host: String, port: Int, password: String?, ntfyTopic: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val sanitizedHost = sanitizeHost(host)
            val room = Room(
                name = name,
                host = sanitizedHost,
                port = port,
                password = password?.ifBlank { null },
                ntfyTopic = ntfyTopic?.ifBlank { null } ?: generateNtfyTopic()
            )
            repository.addRoom(room).fold(
                onSuccess = { _state.update { it.copy(isLoading = false, isSaved = true) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** A random, hard-to-guess ntfy topic — the bridge pushes here and the phone subscribes to it. */
    private fun generateNtfyTopic(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        val suffix = buildString { repeat(10) { append(alphabet[Random.nextInt(alphabet.length)]) } }
        return "aptracker-$suffix"
    }
}

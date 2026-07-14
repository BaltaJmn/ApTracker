package com.baltajmn.aptracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val user = supabase.auth.currentUserOrNull()
        _state.update { it.copy(email = user?.email ?: "") }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { supabase.auth.signOut() }.fold(
                onSuccess = { _state.update { it.copy(isLoading = false, isLoggedOut = true) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }
}

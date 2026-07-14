package com.baltajmn.aptracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.aptracker.core.push.PushRegistrar
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    private val supabase: SupabaseClient,
    private val pushRegistrar: PushRegistrar
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        observeSessionStatus()
    }

    private fun observeSessionStatus() {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _state.update { it.copy(isAuthenticated = true, isCheckingSession = false, isLoading = false) }
                        // Register this device for native push (no-op on iOS).
                        supabase.auth.currentUserOrNull()?.id?.let { userId ->
                            viewModelScope.launch { runCatching { pushRegistrar.register(userId) } }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _state.update { it.copy(isAuthenticated = false, isCheckingSession = false) }
                    }
                    is SessionStatus.Initializing -> {
                        _state.update { it.copy(isCheckingSession = true) }
                    }
                    else -> {
                        _state.update { it.copy(isCheckingSession = false) }
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
            // isAuthenticated is driven exclusively by observeSessionStatus so that
            // navigation only happens after currentUserOrNull() is ready
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.fold(
                onSuccess = {
                    // With email confirmation enabled, no session is created yet — stop the
                    // spinner and tell the user, instead of hanging forever.
                    val needsConfirmation = supabase.auth.currentSessionOrNull() == null
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (needsConfirmation)
                                "Cuenta creada. Revisa tu email para confirmarla y luego inicia sesión."
                            else null
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

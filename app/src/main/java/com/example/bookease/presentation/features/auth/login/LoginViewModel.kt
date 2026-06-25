package com.example.bookease.presentation.features.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    
    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _eventFlow = MutableSharedFlow<LoginEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEmailChange(newValue: String) {
        email = newValue
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(LoginEvent.Error("Please fill in all fields"))
            }
            return
        }

        viewModelScope.launch {
            isLoading = true
            authRepository.login(email, password).collect { result ->
                isLoading = false
                result.onSuccess {
                    _eventFlow.emit(LoginEvent.Success)
                }.onFailure { e ->
                    _eventFlow.emit(LoginEvent.Error(e.message ?: "Login failed"))
                }
            }
        }
    }

    sealed class LoginEvent {
        data object Success : LoginEvent()
        data class Error(val message: String) : LoginEvent()
    }
}

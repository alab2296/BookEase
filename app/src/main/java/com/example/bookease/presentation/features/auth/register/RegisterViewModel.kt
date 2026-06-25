package com.example.bookease.presentation.features.auth.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val db: FirebaseFirestore
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    
    var password by mutableStateOf("")
        private set
        
    var confirmPassword by mutableStateOf("")
        private set

    var userRole by mutableStateOf("Customer")
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _eventFlow = MutableSharedFlow<RegisterEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEmailChange(newValue: String) {
        email = newValue
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
    }

    fun onConfirmPasswordChange(newValue: String) {
        confirmPassword = newValue
    }

    fun onRoleChange(newValue: String) {
        userRole = newValue
    }

    fun register() {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(RegisterEvent.Error("Please fill in all fields"))
            }
            return
        }

        if (password != confirmPassword) {
            viewModelScope.launch {
                _eventFlow.emit(RegisterEvent.Error("Passwords do not match"))
            }
            return
        }

        viewModelScope.launch {
            isLoading = true
            authRepository.register(email, password).collect { result ->
                result.onSuccess { user ->
                    val userMap = hashMapOf(
                        "uid" to user.uid,
                        "email" to email,
                        "role" to userRole
                    )
                    db.collection("users").document(user.uid).set(userMap)
                        .addOnSuccessListener {
                            viewModelScope.launch {
                                isLoading = false
                                _eventFlow.emit(RegisterEvent.Success(userRole))
                            }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            viewModelScope.launch {
                                _eventFlow.emit(RegisterEvent.Error(e.message ?: "Failed to save user info"))
                            }
                        }
                }.onFailure { e ->
                    isLoading = false
                    _eventFlow.emit(RegisterEvent.Error(e.message ?: "Registration failed"))
                }
            }
        }
    }

    sealed class RegisterEvent {
        data class Success(val role: String) : RegisterEvent()
        data class Error(val message: String) : RegisterEvent()
    }
}

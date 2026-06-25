package com.example.bookease.presentation.features.customer.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.model.User
import com.example.bookease.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var email by mutableStateOf("")
    var profileImageUrl by mutableStateOf("")
    
    var isLoading by mutableStateOf(false)
        private set

    fun onImageSelected(uri: android.net.Uri) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            val result = authRepository.uploadProfileImage(uid, uri)
            if (result.isSuccess) {
                profileImageUrl = result.getOrNull() ?: ""
            }
            isLoading = false
        }
    }

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            val user = authRepository.getUserData(uid)
            if (user != null) {
                name = user.name
                phone = user.phone
                email = user.email
                profileImageUrl = user.profileImageUrl
            } else {
                // Fallback to auth email if Firestore doc doesn't exist yet
                email = authRepository.currentUser?.email ?: ""
            }
            isLoading = false
        }
    }

    fun saveProfile(onComplete: (Boolean) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            val user = User(
                id = uid,
                name = name,
                phone = phone,
                email = email,
                profileImageUrl = profileImageUrl
            )
            val result = authRepository.saveUserData(user)
            isLoading = false
            onComplete(result.isSuccess)
        }
    }
}

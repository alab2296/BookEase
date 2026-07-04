package se.w3footprint.bookease.presentation.features.customer.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.User
import se.w3footprint.bookease.domain.repository.AuthRepository
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

    var localImageUri by mutableStateOf<Uri?>(null)
        private set

    var uploadErrorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var currentRole = ""

    init {
        loadProfile()
    }

    fun onImageSelected(uri: Uri) {
        val uid = authRepository.currentUser?.uid ?: return
        localImageUri = uri
        uploadErrorMessage = null
        viewModelScope.launch {
            isLoading = true
            val result = authRepository.uploadProfileImage(uid, uri)
            isLoading = false
            if (result.isSuccess) {
                profileImageUrl = result.getOrNull() ?: ""
                localImageUri = null
            } else {
                localImageUri = null
                uploadErrorMessage = result.exceptionOrNull()?.message ?: "Upload failed"
            }
        }
    }

    fun clearUploadError() {
        uploadErrorMessage = null
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
                currentRole = user.role
            } else {
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
                profileImageUrl = profileImageUrl,
                role = currentRole
            )
            val result = authRepository.saveUserData(user)
            isLoading = false
            onComplete(result.isSuccess)
        }
    }
}

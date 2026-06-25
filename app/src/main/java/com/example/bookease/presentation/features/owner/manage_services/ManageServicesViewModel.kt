package com.example.bookease.presentation.features.owner.manage_services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.model.Service
import com.example.bookease.domain.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageServicesViewModel @Inject constructor(
    private val repository: BusinessRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var servicesList by mutableStateOf<List<Service>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        loadServices()
    }

    private fun loadServices() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            repository.getServices(uid).collect { services ->
                servicesList = services
                isLoading = false
            }
        }
    }

    fun addService(name: String, price: String, duration: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val service = Service(
                businessId = uid,
                name = name,
                price = price.toDoubleOrNull() ?: 0.0,
                durationMinutes = duration.toIntOrNull() ?: 0
            )
            val result = repository.addService(service)
            onComplete(result.isSuccess)
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }
}

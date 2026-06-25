package com.example.bookease.presentation.features.customer.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.model.Business
import com.example.bookease.domain.model.Service
import com.example.bookease.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessDetailViewModel @Inject constructor(
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BusinessDetailUiState>(BusinessDetailUiState.Loading)
    val uiState: StateFlow<BusinessDetailUiState> = _uiState.asStateFlow()

    fun loadBusinessDetail(ownerId: String) {
        viewModelScope.launch {
            val business = businessRepository.getBusinessById(ownerId)
            if (business != null) {
                businessRepository.getServices(ownerId).collect { services ->
                    _uiState.value = BusinessDetailUiState.Success(business, services)
                }
            } else {
                _uiState.value = BusinessDetailUiState.Error("Business not found")
            }
        }
    }

    sealed class BusinessDetailUiState {
        data object Loading : BusinessDetailUiState()
        data class Success(val business: Business, val services: List<Service>) : BusinessDetailUiState()
        data class Error(val message: String) : BusinessDetailUiState()
    }
}

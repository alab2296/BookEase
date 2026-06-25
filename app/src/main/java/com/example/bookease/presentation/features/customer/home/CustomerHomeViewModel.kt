package com.example.bookease.presentation.features.customer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.model.Business
import com.example.bookease.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerHomeUiState>(CustomerHomeUiState.Loading)
    val uiState: StateFlow<CustomerHomeUiState> = _uiState.asStateFlow()

    init {
        getBusinesses()
    }

    private fun getBusinesses() {
        viewModelScope.launch {
            businessRepository.getBusinesses().collect { businesses ->
                _uiState.value = CustomerHomeUiState.Success(businesses)
            }
        }
    }

    sealed class CustomerHomeUiState {
        data object Loading : CustomerHomeUiState()
        data class Success(val businesses: List<Business>) : CustomerHomeUiState()
        data class Error(val message: String) : CustomerHomeUiState()
    }
}

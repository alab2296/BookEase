package se.w3footprint.bookease.presentation.features.customer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerHomeUiState>(CustomerHomeUiState.Loading)
    val uiState: StateFlow<CustomerHomeUiState> = _uiState.asStateFlow()

    init {
        loadBusinesses()
    }

    fun reload() {
        _uiState.value = CustomerHomeUiState.Loading
        loadBusinesses()
    }

    private fun loadBusinesses() {
        viewModelScope.launch {
            businessRepository.getBusinesses()
                .catch { e ->
                    _uiState.value = CustomerHomeUiState.Error(e.message ?: "Failed to load businesses")
                }
                .collect { businesses ->
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

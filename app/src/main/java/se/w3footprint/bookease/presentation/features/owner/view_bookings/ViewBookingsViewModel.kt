package se.w3footprint.bookease.presentation.features.owner.view_bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.AppointmentStatus
import se.w3footprint.bookease.domain.model.User
import se.w3footprint.bookease.domain.use_case.GetBookingsUseCase
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewBookingsViewModel @Inject constructor(
    private val getBookingsUseCase: GetBookingsUseCase,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewBookingsUiState>(ViewBookingsUiState.Loading)
    val uiState: StateFlow<ViewBookingsUiState> = _uiState.asStateFlow()

    private val _customerDetails = MutableStateFlow<Map<String, User>>(emptyMap())
    val customerDetails: StateFlow<Map<String, User>> = _customerDetails.asStateFlow()

    init {
        loadBookings()
    }

    private fun loadBookings() {
        val userId = auth.currentUser?.uid ?: return
        getBookingsUseCase(userId, true)
            .onEach { bookings ->
                _uiState.value = ViewBookingsUiState.Success(bookings)
                loadCustomerDetails(bookings)
            }
            .catch { e ->
                _uiState.value = ViewBookingsUiState.Error(e.message ?: "Unknown error")
            }
            .launchIn(viewModelScope)
    }

    private fun loadCustomerDetails(bookings: List<Appointment>) {
        val customerIds = bookings.map { it.customerId }.distinct()
        viewModelScope.launch {
            val details = mutableMapOf<String, User>()
            customerIds.forEach { id ->
                val user = authRepository.getUserData(id)
                if (user != null) {
                    details[id] = user
                }
            }
            _customerDetails.value = details
        }
    }

    fun updateStatus(bookingId: String, status: AppointmentStatus) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, status.name)
        }
    }
}

sealed interface ViewBookingsUiState {
    data object Loading : ViewBookingsUiState
    data class Success(val bookings: List<Appointment>) : ViewBookingsUiState
    data class Error(val message: String) : ViewBookingsUiState
}

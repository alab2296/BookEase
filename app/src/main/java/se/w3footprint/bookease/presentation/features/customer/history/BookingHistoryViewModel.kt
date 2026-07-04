package se.w3footprint.bookease.presentation.features.customer.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.Review
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingHistoryUiState>(BookingHistoryUiState.Loading)
    val uiState: StateFlow<BookingHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return
        bookingRepository.getBookingsForCustomer(userId)
            .onEach { bookings ->
                _uiState.value = BookingHistoryUiState.Success(bookings)
            }
            .catch { e ->
                _uiState.value = BookingHistoryUiState.Error(e.message ?: "Unknown error")
            }
            .launchIn(viewModelScope)
    }

    fun submitReview(businessId: String, rating: Float, comment: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val review = Review(
                customerId = userId,
                businessId = businessId,
                rating = rating,
                comment = comment
            )
            reviewRepository.addReview(review)
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId)
        }
    }

    fun deleteBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.deleteBooking(bookingId)
        }
    }
}

sealed interface BookingHistoryUiState {
    data object Loading : BookingHistoryUiState
    data class Success(val bookings: List<Appointment>) : BookingHistoryUiState
    data class Error(val message: String) : BookingHistoryUiState
}

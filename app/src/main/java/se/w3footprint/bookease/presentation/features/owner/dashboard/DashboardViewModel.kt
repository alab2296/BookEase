package se.w3footprint.bookease.presentation.features.owner.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.AppointmentStatus
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val bookingRepository: BookingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _totalBookings = MutableStateFlow(0)
    val totalBookings: StateFlow<Int> = _totalBookings.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val business = businessRepository.getBusinessById(uid)
            _businessName.value = business?.name ?: ""
        }
        viewModelScope.launch {
            bookingRepository.getBookingsForOwner(uid)
                .catch { }
                .collect { bookings ->
                    _pendingCount.value = bookings.count { it.statusEnum == AppointmentStatus.PENDING }
                    _totalBookings.value = bookings.size
                }
        }
    }

    fun logout() {
        auth.signOut()
    }
}

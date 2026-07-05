package se.w3footprint.bookease.presentation.features.customer.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.model.TimeSlot
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val businessRepository: BusinessRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var timeSlotsList by mutableStateOf<List<TimeSlot>>(emptyList())
        private set

    var selectedSlot by mutableStateOf<TimeSlot?>(null)
    
    var selectedDate by mutableStateOf("")

    var business by mutableStateOf<Business?>(null)
        private set

    internal var clock: () -> Long = System::currentTimeMillis

    var isLoading by mutableStateOf(false)
        private set

    var isBooking by mutableStateOf(false)
        private set

    private val _eventFlow = MutableSharedFlow<BookingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun loadBusiness(ownerId: String) {
        viewModelScope.launch {
            isLoading = true
            business = businessRepository.getBusinessById(ownerId)
            isLoading = false
        }
    }

    fun onDateSelected(ownerId: String, date: String, duration: Int) {
        selectedDate = date
        selectedSlot = null
        generateAvailableSlots(ownerId, date, duration)
    }

    private fun generateAvailableSlots(ownerId: String, date: String, duration: Int) {
        val currentBusiness = business ?: return
        val openingHour = currentBusiness.openingHours.find { it.day.equals(date, ignoreCase = true) }
        
        if (openingHour == null || openingHour.isClosed || openingHour.openTime.isEmpty()) {
            timeSlotsList = emptyList()
            return
        }

        viewModelScope.launch {
            isLoading = true
            bookingRepository.getBookingsForOwnerByDay(ownerId, date).collect { bookings ->
                val slots = mutableListOf<TimeSlot>()
                var currentTime = openingHour.openTime
                val endTime = openingHour.closeTime
                val bufferTime = 15

                val now = Calendar.getInstance().apply { timeInMillis = clock() }
                val currentDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(now.time)
                val isToday = date.equals(currentDayName, ignoreCase = true)
                val currentTimeOfDay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

                while (isBefore(currentTime, endTime)) {
                    val slotEndTime = addMinutes(currentTime, duration)
                    if (!isBefore(slotEndTime, endTime) && slotEndTime != endTime) break
                    
                    val startTimeStr = currentTime
                    val endTimeStr = slotEndTime

                    if (isToday && isBefore(startTimeStr, currentTimeOfDay)) {
                        currentTime = addMinutes(currentTime, duration + bufferTime)
                        continue
                    }

                    val isBooked = bookings.any { booking ->
                        val isActive = booking.status.uppercase() in listOf("PENDING", "ACCEPTED", "COMPLETED")
                        isActive && isOverlapping(startTimeStr, endTimeStr, booking.startTime, addMinutes(booking.startTime, booking.duration.toIntOrNull() ?: duration))
                    }
                    
                    if (!isBooked) {
                        slots.add(TimeSlot(
                            businessId = ownerId,
                            day = date,
                            startTime = startTimeStr,
                            endTime = endTimeStr,
                            isAvailable = true
                        ))
                    }
                    currentTime = addMinutes(currentTime, duration + bufferTime)
                }
                
                timeSlotsList = slots
                isLoading = false
            }
        }
    }

    private fun isBefore(time1: String, time2: String): Boolean {
        val t1 = time1.split(":")
        val t2 = time2.split(":")
        if (t1.size < 2 || t2.size < 2) return false
        val h1 = t1[0].toIntOrNull() ?: return false
        val m1 = t1[1].toIntOrNull() ?: return false
        val h2 = t2[0].toIntOrNull() ?: return false
        val m2 = t2[1].toIntOrNull() ?: return false
        return h1 < h2 || (h1 == h2 && m1 < m2)
    }

    private fun addMinutes(time: String, minutes: Int): String {
        val parts = time.split(":")
        if (parts.size < 2) return time
        var h = parts[0].toIntOrNull() ?: return time
        var m = (parts[1].toIntOrNull() ?: return time) + minutes
        h += m / 60
        m %= 60
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }

    private fun isOverlapping(s1: String, e1: String, s2: String, e2: String): Boolean {
        return isBefore(s1, e2) && isBefore(s2, e1)
    }

    fun confirmBooking(ownerId: String, serviceName: String, price: String, duration: String) {
        val slot = selectedSlot ?: return
        val customerId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            isBooking = true
            val appointment = Appointment(
                customerId = customerId,
                ownerId = ownerId,
                serviceName = serviceName,
                price = price,
                duration = duration,
                day = slot.day,
                startTime = slot.startTime,
                status = "PENDING",
                timestamp = System.currentTimeMillis()
            )

            val result = bookingRepository.createBooking(appointment)
            isBooking = false
            if (result.isSuccess) {
                _eventFlow.emit(BookingEvent.Success)
            } else {
                _eventFlow.emit(BookingEvent.Error(result.exceptionOrNull()?.message ?: "Booking failed"))
            }
        }
    }

    sealed class BookingEvent {
        data object Success : BookingEvent()
        data class Error(val message: String) : BookingEvent()
    }
}

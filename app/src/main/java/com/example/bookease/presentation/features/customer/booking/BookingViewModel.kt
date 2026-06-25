package com.example.bookease.presentation.features.customer.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookease.domain.model.Appointment
import com.example.bookease.domain.model.TimeSlot
import com.example.bookease.domain.repository.BookingRepository
import com.example.bookease.domain.repository.BusinessRepository
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
    
    var selectedDate by mutableStateOf("") // format: "yyyy-MM-dd" or "Monday" as per current project
    
    var business by mutableStateOf<com.example.bookease.domain.model.Business?>(null)
        private set
    
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
        
        // 1. Get opening hours for this day
        // For simplicity, let's assume 'date' is a day name for now, 
        // but in production it would be a full date and we'd find the day of week.
        val dayOfWeek = date // Simplified for now
        val openingHour = currentBusiness.openingHours.find { it.day.equals(dayOfWeek, ignoreCase = true) }
        
        if (openingHour == null || openingHour.isClosed || openingHour.openTime.isEmpty()) {
            timeSlotsList = emptyList()
            return
        }

        viewModelScope.launch {
            isLoading = true
            // 2. Get existing bookings for this day
            bookingRepository.getBookingsForOwnerByDay(ownerId, date).collect { bookings ->
                val slots = mutableListOf<TimeSlot>()
                
                var currentTime = openingHour.openTime // e.g., "09:00"
                val endTime = openingHour.closeTime // e.g., "17:00"
                
                val bufferTime = 15 // minutes

                val now = Calendar.getInstance()
                val currentDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(now.time)
                val isToday = date.equals(currentDayName, ignoreCase = true)
                val currentTimeOfDay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

                while (isBefore(currentTime, endTime)) {
                    val slotEndTime = addMinutes(currentTime, duration)
                    if (!isBefore(slotEndTime, endTime) && slotEndTime != endTime) break
                    
                    val startTimeStr = currentTime
                    val endTimeStr = slotEndTime
                    
                    // Prevent booking in the past
                    if (isToday && isBefore(startTimeStr, currentTimeOfDay)) {
                        currentTime = addMinutes(currentTime, duration + bufferTime)
                        continue
                    }

                    // Check if this slot overlaps with any existing booking
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
                    
                    // Move to next possible slot (with buffer)
                    currentTime = addMinutes(currentTime, duration + bufferTime)
                }
                
                timeSlotsList = slots
                isLoading = false
            }
        }
    }

    // Helper functions for time manipulation (simplified)
    private fun isBefore(time1: String, time2: String): Boolean {
        if (time1.isEmpty() || time2.isEmpty()) return false
        val t1 = time1.split(":").map { it.toInt() }
        val t2 = time2.split(":").map { it.toInt() }
        return t1[0] < t2[0] || (t1[0] == t2[0] && t1[1] < t2[1])
    }

    private fun addMinutes(time: String, minutes: Int): String {
        val t = time.split(":").map { it.toInt() }
        var h = t[0]
        var m = t[1] + minutes
        h += m / 60
        m %= 60
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }

    private fun isOverlapping(s1: String, e1: String, s2: String, e2: String): Boolean {
        return isBefore(s1, e2) && isBefore(s2, e1)
    }

    fun loadSlots(ownerId: String) {
        // Deprecated or redirect to generate if date is set
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

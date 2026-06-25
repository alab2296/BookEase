package com.example.bookease.domain.repository

import com.example.bookease.domain.model.Appointment
import com.example.bookease.domain.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun getBookingsForOwner(ownerId: String): Flow<List<Appointment>>
    fun getBookingsForOwnerByDay(ownerId: String, day: String): Flow<List<Appointment>>
    fun getBookingsForCustomer(customerId: String): Flow<List<Appointment>>
    suspend fun createBooking(appointment: Appointment): Result<Unit>
    suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit>
    suspend fun cancelBooking(bookingId: String): Result<Unit>
    suspend fun deleteBooking(bookingId: String): Result<Unit>
    suspend fun isSlotAvailable(ownerId: String, day: String, startTime: String): Boolean
}

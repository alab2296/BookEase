package com.example.bookease.domain.use_case

import com.example.bookease.domain.model.Appointment
import com.example.bookease.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookingsUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    operator fun invoke(userId: String, isOwner: Boolean): Flow<List<Appointment>> {
        return if (isOwner) {
            repository.getBookingsForOwner(userId)
        } else {
            repository.getBookingsForCustomer(userId)
        }
    }
}

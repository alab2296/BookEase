package se.w3footprint.bookease.domain.repository

import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.model.Service
import se.w3footprint.bookease.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow

interface BusinessRepository {
    fun getBusinesses(): Flow<List<Business>>
    suspend fun getBusinessById(ownerId: String): Business?
    fun getServices(ownerId: String): Flow<List<Service>>
    fun getTimeSlots(ownerId: String): Flow<List<TimeSlot>>
    suspend fun saveBusinessProfile(business: Business): Result<Unit>
    suspend fun addService(service: Service): Result<Unit>
    suspend fun deleteService(ownerId: String, serviceId: String): Result<Unit>
    suspend fun addTimeSlot(timeSlot: TimeSlot): Result<Unit>
    suspend fun deleteTimeSlot(ownerId: String, timeSlotId: String): Result<Unit>
    suspend fun uploadBusinessImage(ownerId: String, imageUri: android.net.Uri): Result<String>
}

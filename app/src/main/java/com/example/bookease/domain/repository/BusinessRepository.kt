package com.example.bookease.domain.repository

import com.example.bookease.domain.model.Business
import com.example.bookease.domain.model.Service
import com.example.bookease.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow

interface BusinessRepository {
    fun getBusinesses(): Flow<List<Business>>
    suspend fun getBusinessById(ownerId: String): Business?
    fun getServices(ownerId: String): Flow<List<Service>>
    fun getTimeSlots(ownerId: String): Flow<List<TimeSlot>>
    suspend fun saveBusinessProfile(business: Business): Result<Unit>
    suspend fun addService(service: Service): Result<Unit>
    suspend fun deleteService(serviceId: String): Result<Unit>
    suspend fun addTimeSlot(timeSlot: TimeSlot): Result<Unit>
    suspend fun deleteTimeSlot(timeSlotId: String): Result<Unit>
    suspend fun uploadBusinessImage(ownerId: String, imageUri: android.net.Uri): Result<String>
}

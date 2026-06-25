package com.example.bookease.data.repository

import com.example.bookease.domain.model.Appointment
import com.example.bookease.domain.model.AppointmentStatus
import com.example.bookease.domain.repository.BookingRepository
import com.example.bookease.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationHelper: NotificationHelper
) : BookingRepository {

    override fun getBookingsForOwner(ownerId: String): Flow<List<Appointment>> = callbackFlow {
        val listener = firestore.collection("bookings")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    override fun getBookingsForOwnerByDay(ownerId: String, day: String): Flow<List<Appointment>> = callbackFlow {
        val listener = firestore.collection("bookings")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("day", day)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    override fun getBookingsForCustomer(customerId: String): Flow<List<Appointment>> = callbackFlow {
        val listener = firestore.collection("bookings")
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createBooking(appointment: Appointment): Result<Unit> = try {
        val query = firestore.collection("bookings")
            .whereEqualTo("ownerId", appointment.ownerId)
            .whereEqualTo("day", appointment.day)
            .whereEqualTo("startTime", appointment.startTime)
            .whereIn("status", listOf("PENDING", "ACCEPTED", "COMPLETED"))
            .get()
            .await()

        if (!query.isEmpty) {
            Result.failure(Exception("This slot is already booked or pending approval."))
        } else {
            firestore.collection("bookings").add(appointment).await()
            // Notify the business owner
            notificationHelper.showNotification(
                "New Booking Request",
                "You have a new booking request for ${appointment.serviceName} on ${appointment.day} at ${appointment.startTime}"
            )
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun isSlotAvailable(ownerId: String, day: String, startTime: String): Boolean {
        return try {
            val query = firestore.collection("bookings")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("day", day)
                .whereEqualTo("startTime", startTime)
                .whereIn("status", listOf("PENDING", "ACCEPTED", "COMPLETED"))
                .get()
                .await()
            query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit> = try {
        firestore.collection("bookings").document(bookingId)
            .update("status", status)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> = try {
        firestore.collection("bookings").document(bookingId)
            .update("status", "CANCELLED")
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteBooking(bookingId: String): Result<Unit> = try {
        firestore.collection("bookings").document(bookingId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

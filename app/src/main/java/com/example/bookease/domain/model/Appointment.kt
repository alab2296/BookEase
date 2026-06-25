package com.example.bookease.domain.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

enum class AppointmentStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}

data class Appointment(
    val id: String = "",
    val customerId: String = "",
    val ownerId: String = "",
    val serviceName: String = "",
    val price: String = "", // Keeping as String for now as it comes from UI, but could be Double
    val duration: String = "", // Keeping as String for now as it comes from UI, but could be Int
    val day: String = "",
    val startTime: String = "",
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val statusEnum: AppointmentStatus
        get() = try {
            AppointmentStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            AppointmentStatus.PENDING
        }
}

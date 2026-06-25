package com.example.bookease.domain.model

import java.util.Date

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.INFO,
    val relatedId: String? = null // e.g., bookingId
)

enum class NotificationType {
    BOOKING_SUBMITTED,
    BOOKING_ACCEPTED,
    BOOKING_REJECTED,
    REMINDER,
    NEW_BOOKING_REQUEST,
    BOOKING_CANCELLED,
    INFO
}

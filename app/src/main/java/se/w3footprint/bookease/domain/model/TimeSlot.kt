package se.w3footprint.bookease.domain.model

data class TimeSlot(
    val id: String = "",
    val businessId: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val isAvailable: Boolean = true
)

package com.example.bookease.domain.model

data class TimeSlot(
    val id: String = "",
    val businessId: String = "",
    val day: String = "", // e.g., "Monday", "2023-10-27"
    val startTime: String = "",
    val endTime: String = "",
    val isAvailable: Boolean = true
)

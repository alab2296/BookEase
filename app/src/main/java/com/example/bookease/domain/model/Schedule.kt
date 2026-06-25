package com.example.bookease.domain.model

data class Schedule(
    val id: String = "",
    val businessId: String = "",
    val dayOfWeek: Int = 1, // 1 for Monday, etc.
    val startTime: String = "",
    val endTime: String = ""
)

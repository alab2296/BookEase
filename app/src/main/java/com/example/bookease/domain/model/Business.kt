package com.example.bookease.domain.model

data class Business(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val logoUrl: String = "",
    val coverUrl: String = "",
    val gallery: List<String> = emptyList(),
    val openingHours: List<OpeningHour> = emptyList(),
    val rating: Float = 0.0f,
    val reviewCount: Int = 0
)

data class OpeningHour(
    val day: String = "", // e.g., "Monday"
    val openTime: String = "", // e.g., "09:00"
    val closeTime: String = "", // e.g., "17:00"
    val isClosed: Boolean = false
)

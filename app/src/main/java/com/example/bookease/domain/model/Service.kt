package com.example.bookease.domain.model

data class Service(
    val id: String = "",
    val businessId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val durationMinutes: Int = 0,
    val description: String = ""
)

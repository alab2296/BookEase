package com.example.bookease.domain.model

data class Review(
    val id: String = "",
    val customerId: String = "",
    val businessId: String = "",
    val rating: Float = 0.0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

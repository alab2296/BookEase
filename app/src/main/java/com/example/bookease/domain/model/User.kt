package com.example.bookease.domain.model

enum class UserRole {
    CUSTOMER,
    BUSINESS_OWNER
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val profileImageUrl: String = "",
    val favoriteBusinessIds: List<String> = emptyList(),
    val reviewIds: List<String> = emptyList()
)

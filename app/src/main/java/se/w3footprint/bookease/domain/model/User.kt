package se.w3footprint.bookease.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val profileImageUrl: String = "",
    val favoriteBusinessIds: List<String> = emptyList(),
    val reviewIds: List<String> = emptyList()
)

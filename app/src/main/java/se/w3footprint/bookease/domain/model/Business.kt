package se.w3footprint.bookease.domain.model

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
    val day: String = "",
    val openTime: String = "",
    val closeTime: String = "",
    val isClosed: Boolean = false
)

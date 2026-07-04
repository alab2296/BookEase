package se.w3footprint.bookease.domain.model

data class DashboardStats(
    val totalBookings: Int = 0,
    val completedBookings: Int = 0,
    val cancelledBookings: Int = 0,
    val pendingBookings: Int = 0,
    val totalRevenue: Double = 0.0,
    val popularServices: List<PopularService> = emptyList()
)

data class PopularService(
    val serviceName: String,
    val bookingCount: Int,
    val revenue: Double
)

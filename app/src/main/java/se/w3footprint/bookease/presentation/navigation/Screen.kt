package se.w3footprint.bookease.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object BusinessProfile : Screen("businessProfile")
    data object Dashboard : Screen("dashboard")
    data object Services : Screen("services")
    data object TimeSlots : Screen("timeSlots")
    data object ViewBookings : Screen("viewBookings")
    data object CustomerHome : Screen("customerHome")
    data object History : Screen("history")
    data object Profile : Screen("profile")
    data object BusinessDetail : Screen("businessDetail/{ownerId}") {
        fun createRoute(ownerId: String) = "businessDetail/$ownerId"
    }
    data object Booking : Screen("booking/{ownerId}/{serviceName}/{price}/{duration}") {
        fun createRoute(ownerId: String, serviceName: String, price: String, duration: String) =
            "booking/$ownerId/$serviceName/$price/$duration"
    }
}

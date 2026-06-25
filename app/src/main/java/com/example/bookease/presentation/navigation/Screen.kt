package com.example.bookease.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Dashboard : Screen("dashboard")
    data object Services : Screen("services")
    data object TimeSlots : Screen("timeSlots")
    data object ViewBookings : Screen("viewBookings")
    data object CustomerHome : Screen("customerHome")
    data object Profile : Screen("profile")
    data object BusinessDetail : Screen("businessDetail/{ownerId}") {
        fun createRoute(ownerId: String) = "businessDetail/$ownerId"
    }
    data object Booking : Screen("booking/{ownerId}/{serviceName}/{price}/{duration}") {
        fun createRoute(ownerId: String, serviceName: String, price: String, duration: String) =
            "booking/$ownerId/$serviceName/$price/$duration"
    }
}

package com.example.bookease.presentation.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bookease.presentation.features.auth.login.LoginScreen
import com.example.bookease.presentation.features.auth.register.RegisterScreen
import com.example.bookease.presentation.features.auth.splash.SplashScreen
import com.example.bookease.presentation.features.customer.booking.BookingScreen
import com.example.bookease.presentation.features.customer.detail.BusinessDetailScreen
import com.example.bookease.presentation.features.customer.history.BookingHistoryScreen
import com.example.bookease.presentation.features.customer.home.CustomerHomeScreen
import com.example.bookease.presentation.features.customer.profile.CustomerProfileScreen
import com.example.bookease.presentation.features.owner.dashboard.DashboardScreen
import com.example.bookease.presentation.features.owner.manage_schedule.ManageTimeSlotsScreen
import com.example.bookease.presentation.features.owner.manage_services.ManageServicesScreen
import com.example.bookease.presentation.features.owner.profile.BusinessProfileScreen
import com.example.bookease.presentation.features.owner.view_bookings.ViewBookingsScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen { role ->
                navigateToRoleHome(navController, role, auth, db, "splash")
            }
        }

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role") ?: "Customer"
                                navigateToRoleHome(navController, role, auth, db, "login")
                            }
                    }
                },
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistrationSuccess = { role ->
                    navigateToRoleHome(navController, role, auth, db, "register")
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("businessProfile") {
            BusinessProfileScreen(
                onContinue = {
                    navController.navigate("dashboard") {
                        popUpTo("businessProfile") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onManageServices = { navController.navigate("services") },
                onManageTimeSlots = { navController.navigate("timeSlots") },
                onViewBookings = { navController.navigate("viewBookings") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        
        composable("services") {
            ManageServicesScreen(onBack = { navController.popBackStack() })
        }
        composable("timeSlots") {
            ManageTimeSlotsScreen(onBack = { navController.popBackStack() })
        }
        composable("viewBookings") {
            ViewBookingsScreen(onBack = { navController.popBackStack() })
        }
        
        composable("customerHome") {
            CustomerHomeScreen(
                onBusinessClick = { ownerId ->
                    navController.navigate("businessDetail/$ownerId")
                },
                onViewHistory = { navController.navigate("history") },
                onProfileClick = { navController.navigate("profile") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("customerHome") { inclusive = true }
                    }
                }
            )
        }
        
        composable("history") {
            BookingHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable("profile") {
            CustomerProfileScreen(onBack = { navController.popBackStack() })
        }

        composable("businessDetail/{ownerId}") { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
            BusinessDetailScreen(
                ownerId = ownerId,
                onBookService = { serviceName, price, duration ->
                    navController.navigate("booking/$ownerId/$serviceName/$price/$duration")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("booking/{ownerId}/{serviceName}/{price}/{duration}") { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""
            val price = backStackEntry.arguments?.getString("price") ?: ""
            val duration = backStackEntry.arguments?.getString("duration") ?: ""
            BookingScreen(
                ownerId = ownerId,
                serviceName = serviceName,
                price = price,
                duration = duration,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun navigateToRoleHome(
    navController: NavController,
    role: String?,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    popUpTo: String
) {
    if (role == null) {
        navController.navigate("login") {
            popUpTo(popUpTo) { inclusive = true }
        }
        return
    }

    Toast.makeText(navController.context, "Detected role: $role", Toast.LENGTH_SHORT).show()

    if (role == "Business") {
        val uid = auth.currentUser?.uid ?: ""
        db.collection("businesses").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    navController.navigate("dashboard") {
                        popUpTo(popUpTo) { inclusive = true }
                    }
                } else {
                    navController.navigate("businessProfile") {
                        popUpTo(popUpTo) { inclusive = true }
                    }
                }
            }
            .addOnFailureListener {
                navController.navigate("businessProfile") {
                    popUpTo(popUpTo) { inclusive = true }
                }
            }
    } else {
        navController.navigate("customerHome") {
            popUpTo(popUpTo) { inclusive = true }
        }
    }
}

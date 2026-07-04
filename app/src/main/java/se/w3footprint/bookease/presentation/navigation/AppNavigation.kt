package se.w3footprint.bookease.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import se.w3footprint.bookease.presentation.features.auth.login.LoginScreen
import se.w3footprint.bookease.presentation.features.auth.register.RegisterScreen
import se.w3footprint.bookease.presentation.features.auth.splash.SplashScreen
import se.w3footprint.bookease.presentation.features.customer.booking.BookingScreen
import se.w3footprint.bookease.presentation.features.customer.detail.BusinessDetailScreen
import se.w3footprint.bookease.presentation.features.customer.history.BookingHistoryScreen
import se.w3footprint.bookease.presentation.features.customer.home.CustomerHomeScreen
import se.w3footprint.bookease.presentation.features.customer.profile.CustomerProfileScreen
import se.w3footprint.bookease.presentation.features.owner.dashboard.DashboardScreen
import se.w3footprint.bookease.presentation.features.owner.manage_schedule.ManageTimeSlotsScreen
import se.w3footprint.bookease.presentation.features.owner.manage_services.ManageServicesScreen
import se.w3footprint.bookease.presentation.features.owner.profile.BusinessProfileScreen
import se.w3footprint.bookease.presentation.features.owner.view_bookings.ViewBookingsScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen { role ->
                navigateToRoleHome(navController, role, auth, db, Screen.Splash.route)
            }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role") ?: "Customer"
                                navigateToRoleHome(navController, role, auth, db, Screen.Login.route)
                            }
                    }
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistrationSuccess = { role ->
                    navigateToRoleHome(navController, role, auth, db, Screen.Register.route)
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.BusinessProfile.route) {
            BusinessProfileScreen(
                onContinue = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.BusinessProfile.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onManageServices = { navController.navigate(Screen.Services.route) },
                onManageTimeSlots = { navController.navigate(Screen.TimeSlots.route) },
                onViewBookings = { navController.navigate(Screen.ViewBookings.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Services.route) {
            ManageServicesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TimeSlots.route) {
            ManageTimeSlotsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ViewBookings.route) {
            ViewBookingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CustomerHome.route) {
            CustomerHomeScreen(
                onBusinessClick = { ownerId ->
                    navController.navigate(Screen.BusinessDetail.createRoute(ownerId))
                },
                onLogout = {
                    auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.CustomerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.History.route) {
            BookingHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            CustomerProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.BusinessDetail.route) { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
            BusinessDetailScreen(
                ownerId = ownerId,
                onBookService = { serviceName, price, duration ->
                    navController.navigate(Screen.Booking.createRoute(ownerId, serviceName, price, duration))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Booking.route) { backStackEntry ->
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
        navController.navigate(Screen.Login.route) {
            popUpTo(popUpTo) { inclusive = true }
        }
        return
    }

    if (role == "Business") {
        val uid = auth.currentUser?.uid ?: ""
        db.collection("businesses").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(popUpTo) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.BusinessProfile.route) {
                        popUpTo(popUpTo) { inclusive = true }
                    }
                }
            }
            .addOnFailureListener {
                navController.navigate(Screen.BusinessProfile.route) {
                    popUpTo(popUpTo) { inclusive = true }
                }
            }
    } else {
        navController.navigate(Screen.CustomerHome.route) {
            popUpTo(popUpTo) { inclusive = true }
        }
    }
}

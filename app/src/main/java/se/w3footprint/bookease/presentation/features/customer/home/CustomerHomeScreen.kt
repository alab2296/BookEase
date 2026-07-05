package se.w3footprint.bookease.presentation.features.customer.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.AppointmentStatus
import se.w3footprint.bookease.presentation.features.customer.history.BookingHistoryViewModel
import se.w3footprint.bookease.presentation.features.customer.history.BookingHistoryUiState
import se.w3footprint.bookease.presentation.features.customer.history.HistoryItem
import se.w3footprint.bookease.presentation.features.customer.history.ReviewDialog
import se.w3footprint.bookease.presentation.features.customer.profile.CustomerProfileViewModel
import se.w3footprint.bookease.ui.components.BusinessCard
import se.w3footprint.bookease.ui.components.EmptyStateView
import se.w3footprint.bookease.ui.components.ErrorView
import se.w3footprint.bookease.ui.components.LoadingView

private val CATEGORIES = listOf("All", "Salon", "Barber", "Gym", "Spa", "Clinic", "Dentist", "Physiotherapy", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    onBusinessClick: (String) -> Unit,
    onLogout: () -> Unit,
    homeViewModel: CustomerHomeViewModel = hiltViewModel(),
    historyViewModel: BookingHistoryViewModel = hiltViewModel(),
    profileViewModel: CustomerProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val uiState by homeViewModel.uiState.collectAsState()

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { profileViewModel.onImageSelected(it) }
    }

    Scaffold(
        topBar = {
            when (selectedTab) {
                0 -> TopAppBar(
                    title = {
                        Text(
                            "BookEase",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                1 -> TopAppBar(
                    title = { Text("My Bookings", fontWeight = FontWeight.Bold) }
                )
                else -> CenterAlignedTopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold) }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Home else Icons.Default.Home,
                            contentDescription = "Explore"
                        )
                    },
                    label = { Text("Explore") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.CalendarMonth else Icons.Default.CalendarMonth,
                            contentDescription = "Bookings"
                        )
                    },
                    label = { Text("Bookings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Person else Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> ExploreTab(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    onBusinessClick = onBusinessClick,
                    onRetry = homeViewModel::reload
                )
                1 -> BookingsTab(viewModel = historyViewModel)
                2 -> ProfileTab(
                    viewModel = profileViewModel,
                    onLogout = onLogout,
                    onPickImage = { imageLauncher.launch("image/*") },
                    context = context
                )
            }
        }
    }
}

@Composable
private fun ExploreTab(
    uiState: CustomerHomeViewModel.CustomerHomeUiState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onBusinessClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search businesses or services...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CATEGORIES) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        when (val state = uiState) {
            is CustomerHomeViewModel.CustomerHomeUiState.Loading -> LoadingView()
            is CustomerHomeViewModel.CustomerHomeUiState.Success -> {
                val filtered = state.businesses.filter { business ->
                    (selectedCategory == "All" || business.category.equals(selectedCategory, ignoreCase = true)) &&
                    (searchQuery.isEmpty() || business.name.contains(searchQuery, ignoreCase = true) ||
                            business.category.contains(searchQuery, ignoreCase = true))
                }

                if (filtered.isEmpty()) {
                    EmptyStateView(
                        title = if (searchQuery.isNotEmpty()) "No results found" else "No businesses yet",
                        description = if (searchQuery.isNotEmpty())
                            "Try a different search or category"
                        else
                            "Check back soon for new businesses",
                        icon = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.Store
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Search Results" else "Services near you",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(filtered) { business ->
                            BusinessCard(business) { onBusinessClick(business.ownerId) }
                        }
                    }
                }
            }
            is CustomerHomeViewModel.CustomerHomeUiState.Error -> {
                ErrorView(message = state.message, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun BookingsTab(viewModel: BookingHistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showReviewDialog by remember { mutableStateOf<Appointment?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Appointment?>(null) }
    val context = LocalContext.current

    LaunchedEffect(viewModel.reviewMessage) {
        viewModel.reviewMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearReviewMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is BookingHistoryUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is BookingHistoryUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is BookingHistoryUiState.Success -> {
                if (state.bookings.isEmpty()) {
                    EmptyStateView(
                        title = "No bookings yet",
                        description = "Your booking history will appear here",
                        icon = Icons.Default.CalendarMonth
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.bookings) { booking ->
                            HistoryItem(
                                booking = booking,
                                onRate = { showReviewDialog = booking },
                                onCancel = { viewModel.cancelBooking(booking.id) },
                                onDelete = { showDeleteDialog = booking }
                            )
                        }
                    }
                }
            }
        }

        showDeleteDialog?.let { appointment ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Booking") },
                text = { Text("Remove this booking from your history?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBooking(appointment.id)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }

        showReviewDialog?.let { appointment ->
            ReviewDialog(
                onDismiss = { showReviewDialog = null },
                onSubmit = { rating, comment ->
                    viewModel.submitReview(appointment.id, appointment.ownerId, rating, comment)
                    showReviewDialog = null
                }
            )
        }
    }
}

@Composable
private fun ProfileTab(
    viewModel: CustomerProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onPickImage: () -> Unit,
    context: android.content.Context
) {
    LaunchedEffect(viewModel.uploadErrorMessage) {
        viewModel.uploadErrorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearUploadError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val imageModel: Any? = when {
            viewModel.localImageUri != null -> viewModel.localImageUri
            viewModel.profileImageUrl.startsWith("http") -> viewModel.profileImageUrl
            viewModel.profileImageUrl.isNotEmpty() ->
                android.util.Base64.decode(viewModel.profileImageUrl, android.util.Base64.DEFAULT)
            else -> null
        }

        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (viewModel.isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = {},
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            enabled = false,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = viewModel.phone,
            onValueChange = { viewModel.phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                viewModel.saveProfile { success ->
                    if (success) {
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                width = 1.dp
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

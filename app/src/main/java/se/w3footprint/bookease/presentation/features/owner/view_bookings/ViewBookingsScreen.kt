package se.w3footprint.bookease.presentation.features.owner.view_bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.AppointmentStatus
import se.w3footprint.bookease.domain.model.User
import se.w3footprint.bookease.ui.components.EmptyStateView
import se.w3footprint.bookease.ui.components.InfoRow
import se.w3footprint.bookease.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBookingsScreen(
    onBack: () -> Unit = {},
    viewModel: ViewBookingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val customerDetails by viewModel.customerDetails.collectAsState()
    var showCustomerDialog by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ViewBookingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ViewBookingsUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                is ViewBookingsUiState.Success -> {
                    if (state.bookings.isEmpty()) {
                        EmptyStateView(
                            title = "No bookings yet",
                            description = "They will appear here once customers book.",
                            icon = Icons.Default.EventBusy
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.bookings) { booking ->
                                val customer = customerDetails[booking.customerId]
                                BookingItem(
                                    booking = booking,
                                    customer = customer,
                                    onAccept = { viewModel.updateStatus(booking.id, AppointmentStatus.ACCEPTED) },
                                    onReject = { viewModel.updateStatus(booking.id, AppointmentStatus.REJECTED) },
                                    onViewCustomer = { showCustomerDialog = customer }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showCustomerDialog?.let { customer ->
        AlertDialog(
            onDismissRequest = { showCustomerDialog = null },
            title = { Text("Customer Details") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (customer.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = customer.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = customer.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (customer.phone.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = customer.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BookingItem(
    booking: Appointment,
    customer: User?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onViewCustomer: () -> Unit
) {
    val statusColor = when (booking.statusEnum) {
        AppointmentStatus.ACCEPTED -> Color(0xFF4CAF50)
        AppointmentStatus.REJECTED -> Color(0xFFF44336)
        AppointmentStatus.COMPLETED -> Color(0xFF2196F3)
        else -> Color(0xFFFFC107)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.serviceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusBadge(booking.statusEnum.name, statusColor)
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewCustomer() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = customer?.name ?: "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(View Details)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(icon = Icons.Default.CalendarToday, text = booking.day)
            InfoRow(icon = Icons.Default.AccessTime, text = booking.startTime)
            InfoRow(
                icon = Icons.Default.Payments,
                text = "${booking.price} SEK · ${booking.duration} mins"
            )

            if (booking.statusEnum == AppointmentStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}


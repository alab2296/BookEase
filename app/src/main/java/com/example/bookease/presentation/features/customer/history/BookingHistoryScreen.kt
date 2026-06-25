package com.example.bookease.presentation.features.customer.history

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.bookease.domain.model.Appointment
import com.example.bookease.domain.model.AppointmentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    onBack: () -> Unit = {},
    viewModel: BookingHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showReviewDialog by remember { mutableStateOf<Appointment?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Appointment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings", fontWeight = FontWeight.Bold) },
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
                is BookingHistoryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is BookingHistoryUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                is BookingHistoryUiState.Success -> {
                    if (state.bookings.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No bookings found")
                        }
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
                                    onDelete = { showDeleteConfirmDialog = booking }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        showDeleteConfirmDialog?.let { appointment ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Delete Booking History") },
                text = { Text("Are you sure you want to delete this booking from your history?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBooking(appointment.id)
                            showDeleteConfirmDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        showReviewDialog?.let { appointment ->
            ReviewDialog(
                onDismiss = { showReviewDialog = null },
                onSubmit = { rating, comment ->
                    viewModel.submitReview(appointment.ownerId, rating, comment)
                    showReviewDialog = null
                }
            )
        }
    }
}

@Composable
fun ReviewDialog(onDismiss: () -> Unit, onSubmit: (Float, String) -> Unit) {
    var rating by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your experience") },
        text = {
            Column {
                Text("How was the service?")
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Rating: ${rating.toInt()} Stars")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HistoryItem(
    booking: Appointment,
    onRate: () -> Unit = {},
    onCancel: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val statusColor = when (booking.statusEnum) {
        AppointmentStatus.ACCEPTED -> Color(0xFF4CAF50)
        AppointmentStatus.REJECTED -> Color(0xFFF44336)
        AppointmentStatus.COMPLETED -> Color(0xFF2196F3)
        AppointmentStatus.CANCELLED -> Color(0xFF9E9E9E)
        else -> Color(0xFFFFC107)
    }

    val canDelete = booking.statusEnum == AppointmentStatus.CANCELLED || booking.statusEnum == AppointmentStatus.REJECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (canDelete) onDelete()
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(booking.statusEnum.name, statusColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(icon = Icons.Default.CalendarToday, text = booking.day)
            InfoRow(icon = Icons.Default.AccessTime, text = booking.startTime)
            InfoRow(
                icon = Icons.Default.Payments,
                text = "${booking.price} SEK · ${booking.duration} mins"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (booking.statusEnum == AppointmentStatus.PENDING || booking.statusEnum == AppointmentStatus.ACCEPTED) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel")
                    }
                }

                if (booking.statusEnum == AppointmentStatus.COMPLETED) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRate,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Rate & Review")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusBadge(status: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
    ) {
        Text(
            text = status.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

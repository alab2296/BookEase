package se.w3footprint.bookease.presentation.features.customer.booking

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import se.w3footprint.bookease.domain.model.TimeSlot
import se.w3footprint.bookease.ui.components.LoadingView
import se.w3footprint.bookease.ui.components.PrimaryButton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    ownerId: String,
    serviceName: String,
    price: String,
    duration: String,
    onBack: () -> Unit = {},
    viewModel: BookingViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(ownerId) {
        viewModel.loadBusiness(ownerId)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BookingViewModel.BookingEvent.Success -> {
                    Toast.makeText(context, "Booking confirmed!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is BookingViewModel.BookingEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                PrimaryButton(
                    text = "Confirm Booking",
                    onClick = {
                        if (viewModel.selectedSlot == null) {
                            Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.confirmBooking(ownerId, serviceName, price, duration)
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                    enabled = !viewModel.isBooking && viewModel.selectedSlot != null,
                    isLoading = viewModel.isBooking
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Selected Service", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = serviceName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryInfoItem(label = "Duration", value = "$duration mins")
                        SummaryInfoItem(label = "Price", value = "$price SEK")
                    }
                }
            }

            Text(
                text = "Select Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDateSelector(
                selectedDate = viewModel.selectedDate,
                onDateSelected = { date ->
                    viewModel.onDateSelected(ownerId, date, duration.toIntOrNull() ?: 30)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Available Slots",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (viewModel.isLoading) {
                LoadingView()
            } else if (viewModel.selectedDate.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Please select a date first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (viewModel.timeSlotsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No available slots for this day.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val chunkedSlots = viewModel.timeSlotsList.chunked(3)
                    items(chunkedSlots) { row ->
                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             row.forEach { slot ->
                                 TimeSlotCard(
                                     slot = slot,
                                     isSelected = viewModel.selectedSlot == slot,
                                     modifier = Modifier.weight(1f)
                                 ) { viewModel.selectedSlot = slot }
                             }
                             repeat(3 - row.size) {
                                 Spacer(modifier = Modifier.weight(1f))
                             }
                         }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalDateSelector(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val dates = remember {
        val list = mutableListOf<Pair<String, String>>()
        val dateFmt = SimpleDateFormat("dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        for (i in 0 until 14) {
            val dayOfMonth = dateFmt.format(cal.time)
            val fullDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
            list.add(fullDayName to dayOfMonth)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(dates) { (dayName, dayOfMonth) ->
            val isSelected = selectedDate == dayName
            Surface(
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onDateSelected(dayName) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayOfMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryInfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimeSlotCard(slot: TimeSlot, isSelected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    Surface(
        modifier = modifier
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${slot.startTime} - ${slot.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

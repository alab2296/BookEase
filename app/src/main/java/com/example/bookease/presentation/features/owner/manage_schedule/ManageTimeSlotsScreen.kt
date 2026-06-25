package com.example.bookease.presentation.features.owner.manage_schedule

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.bookease.domain.model.TimeSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTimeSlotsScreen(
    onBack: () -> Unit = {},
    viewModel: ManageScheduleViewModel = hiltViewModel()
) {
    var showAddSheet by remember { mutableStateOf(value = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Slot")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isLoading && viewModel.slotsList.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.slotsList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No time slots scheduled.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Add your availability to start receiving bookings.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val groupedSlots = viewModel.slotsList.groupBy { it.day.ifEmpty { "Other" } }
                    groupedSlots.forEach { (day, daySlots) ->
                        item {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(daySlots) { slot ->
                            SlotItem(
                                startTime = slot.startTime,
                                endTime = slot.endTime
                            ) {
                                viewModel.deleteSlot(slot.id)
                            }
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            AddSlotSheet(
                onDismiss = { showAddSheet = false },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun SlotItem(startTime: String, endTime: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule, 
                contentDescription = null, 
                modifier = Modifier.size(20.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$startTime - $endTime",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Delete", 
                    tint = MaterialTheme.colorScheme.error, 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotSheet(
    onDismiss: () -> Unit, 
    viewModel: ManageScheduleViewModel
) {
    val modalSheetState = rememberModalBottomSheetState()
    var day by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Add Time Slot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = day,
                onValueChange = { day = it },
                label = { Text("Day (e.g., Monday)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start Time") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("09:00") }
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End Time") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("10:00") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (day.isEmpty() || start.isEmpty() || end.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    viewModel.addSlot(day, start, end) { success ->
                        isSaving = false
                        if (success) {
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Error saving slot", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Slot", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

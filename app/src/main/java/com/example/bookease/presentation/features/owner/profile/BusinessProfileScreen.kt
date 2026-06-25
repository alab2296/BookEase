package com.example.bookease.presentation.features.owner.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.bookease.domain.model.OpeningHour

@Composable
fun BusinessProfileScreen(
    onContinue: () -> Unit,
    viewModel: BusinessProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set Up Your Business",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.businessName,
            onValueChange = { viewModel.businessName = it },
            label = { Text("Business Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.description,
            onValueChange = { viewModel.description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.category,
            onValueChange = { viewModel.category = it },
            label = { Text("Category (e.g., Salon, Gym)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.address,
            onValueChange = { viewModel.address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.phone,
            onValueChange = { viewModel.phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.website,
            onValueChange = { viewModel.website = it },
            label = { Text("Website") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.imageUrls,
            onValueChange = { viewModel.imageUrls = it },
            label = { Text("Image URLs (comma separated)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("https://example.com/img1.jpg, ...") },
            trailingIcon = {
                IconButton(onClick = { launcher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo")
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Opening Hours",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        viewModel.openingHours.forEachIndexed { index, hour ->
            OpeningHourItem(
                hour = hour,
                onUpdate = { updated -> viewModel.updateOpeningHour(index, updated) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (viewModel.businessName.isEmpty() || viewModel.category.isEmpty() || viewModel.address.isEmpty()) {
                    Toast.makeText(context, "Please fill in required fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.saveProfile { success ->
                    if (success) {
                        Toast.makeText(context, "Business saved!", Toast.LENGTH_SHORT).show()
                        onContinue()
                    } else {
                        Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = "Save and Continue", fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OpeningHourItem(
    hour: OpeningHour,
    onUpdate: (OpeningHour) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = hour.day, modifier = Modifier.width(100.dp))
        
        Checkbox(
            checked = !hour.isClosed,
            onCheckedChange = { onUpdate(hour.copy(isClosed = !it)) }
        )
        
        if (!hour.isClosed) {
            OutlinedTextField(
                value = hour.openTime,
                onValueChange = { onUpdate(hour.copy(openTime = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("09:00") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = hour.closeTime,
                onValueChange = { onUpdate(hour.copy(closeTime = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("17:00") },
                singleLine = true
            )
        } else {
            Text(
                text = "Closed",
                modifier = Modifier.weight(2f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

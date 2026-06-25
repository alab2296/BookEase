package com.example.bookease.presentation.features.customer.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.bookease.domain.model.Business
import com.example.bookease.ui.components.BusinessCard
import com.example.bookease.ui.components.EmptyStateView
import com.example.bookease.ui.components.ErrorView
import com.example.bookease.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    onBusinessClick: (String) -> Unit,
    onViewHistory: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CustomerHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Salon", "Barber", "Gym", "Spa", "Clinic")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("BookEase", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                },
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search services or businesses...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Categories
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            when (val state = uiState) {
                is CustomerHomeViewModel.CustomerHomeUiState.Loading -> {
                    LoadingView()
                }
                is CustomerHomeViewModel.CustomerHomeUiState.Success -> {
                    val filteredBusinesses = state.businesses.filter {
                        ((selectedCategory == "All") || (it.category.equals(selectedCategory, ignoreCase = true))) &&
                        (it.name.contains(searchQuery, ignoreCase = true))
                    }

                    if (filteredBusinesses.isEmpty()) {
                        EmptyStateView(
                            title = "No results found",
                            description = "Try searching for something else or change the category.",
                            icon = Icons.Default.SearchOff
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = if (searchQuery.isEmpty()) "Featured Businesses" else "Search Results",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(filteredBusinesses) { business ->
                                BusinessCard(business) { onBusinessClick(business.ownerId) }
                            }
                        }
                    }
                }
                is CustomerHomeViewModel.CustomerHomeUiState.Error -> {
                    ErrorView(message = state.message, onRetry = { /* reload logic */ })
                }
            }
        }
    }
}


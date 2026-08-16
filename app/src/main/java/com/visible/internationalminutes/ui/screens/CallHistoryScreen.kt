package com.visible.internationalminutes.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visible.internationalminutes.model.CallRecord
import com.visible.internationalminutes.model.QuotaCategory
import com.visible.internationalminutes.ui.MainUiState
import com.visible.internationalminutes.ui.components.CallRecordItem
import com.visible.internationalminutes.ui.theme.VisibleBlue
import com.visible.internationalminutes.ui.theme.VisibleMint

enum class CallFilter {
    ALL_INTERNATIONAL,
    COUNTED_500M,
    UNLIMITED_PRO,
    ALL_CALLS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL_INTERNATIONAL) }

    val filteredCalls = remember(uiState.callRecords, searchQuery, selectedFilter) {
        uiState.callRecords.filter { call ->
            // Filter by category
            val matchesFilter = when (selectedFilter) {
                CallFilter.ALL_INTERNATIONAL -> call.isInternational
                CallFilter.COUNTED_500M -> call.quotaCategory == QuotaCategory.COUNTED_500_MIN
                CallFilter.UNLIMITED_PRO -> call.quotaCategory == QuotaCategory.UNLIMITED_PRO
                CallFilter.ALL_CALLS -> true
            }

            // Filter by search query
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                call.formattedNumber.lowercase().contains(q) ||
                        call.rawNumber.contains(q) ||
                        (call.contactName?.lowercase()?.contains(q) == true) ||
                        (call.country?.name?.lowercase()?.contains(q) == true)
            }

            matchesFilter && matchesSearch
        }
    }

    val totalBilledMinutes = remember(filteredCalls) {
        filteredCalls.sumOf { it.billedMinutes }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Call History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.billingCycle.formattedDateRange}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = VisibleBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search contact, number, country...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VisibleBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == CallFilter.ALL_INTERNATIONAL,
                    onClick = { selectedFilter = CallFilter.ALL_INTERNATIONAL },
                    label = { Text("All International") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VisibleBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
                FilterChip(
                    selected = selectedFilter == CallFilter.COUNTED_500M,
                    onClick = { selectedFilter = CallFilter.COUNTED_500M },
                    label = { Text("500m Quota Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VisibleBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
                FilterChip(
                    selected = selectedFilter == CallFilter.UNLIMITED_PRO,
                    onClick = { selectedFilter = CallFilter.UNLIMITED_PRO },
                    label = { Text("Unlimited Pro Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VisibleMint,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
                FilterChip(
                    selected = selectedFilter == CallFilter.ALL_CALLS,
                    onClick = { selectedFilter = CallFilter.ALL_CALLS },
                    label = { Text("All Calls (incl. Domestic)") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary Subheader
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredCalls.size} calls found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Total: $totalBilledMinutes min billed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredCalls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching calls found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search filter or switching SIM filter in settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCalls, key = { it.id }) { call ->
                        CallRecordItem(call = call)
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

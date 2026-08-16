package com.visible.internationalminutes.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visible.internationalminutes.ui.MainUiState
import com.visible.internationalminutes.ui.components.BillingCycleBanner
import com.visible.internationalminutes.ui.components.CountryUsageCard
import com.visible.internationalminutes.ui.components.QuotaGaugeCard
import com.visible.internationalminutes.ui.components.SimStatusCard
import com.visible.internationalminutes.ui.components.UsageHistogramCard
import com.visible.internationalminutes.ui.theme.VisibleAmber
import com.visible.internationalminutes.ui.theme.VisibleBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSimClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "International Call Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "For the Visible+ Pro plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.callRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VisibleBlue)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner (if not granted and not in demo mode)
            if (!uiState.hasCallLogPermission && !uiState.userSettings.isDemoDataEnabled) {
                item {
                    PermissionRequestBanner(onRequestPermissions = onRequestPermissions)
                }
            }

            // SIM Status Card
            item {
                SimStatusCard(
                    activeSim = uiState.activeSim,
                    isAutoDetected = uiState.isAutoDetectedSim,
                    onSimClick = onSimClick
                )
            }

            // Hero Quota Gauge Card
            item {
                QuotaGaugeCard(quota = uiState.quotaSummary)
            }

            // Billing Cycle Progress Banner
            item {
                BillingCycleBanner(billingCycle = uiState.billingCycle)
            }

            // Interactive Histogram (Daily Cycle Slider & Monthly History)
            item {
                UsageHistogramCard(
                    dailyUsages = uiState.dailyUsages,
                    cycleDailyUsages = uiState.cycleDailyUsages,
                    monthlyUsages = uiState.monthlyUsages
                )
            }

            // Top Countries Breakdown
            if (uiState.countryUsages.isNotEmpty()) {
                item {
                    CountryUsageCard(countryUsages = uiState.countryUsages)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PermissionRequestBanner(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = VisibleAmber.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = VisibleAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Call Log Permission Needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grant call log access so the app can automatically calculate your 500 monthly international minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = VisibleAmber),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Permission", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

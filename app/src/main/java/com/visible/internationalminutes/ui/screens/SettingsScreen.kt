package com.visible.internationalminutes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visible.internationalminutes.data.UserSettings
import com.visible.internationalminutes.ui.MainUiState
import com.visible.internationalminutes.ui.theme.VisibleAmber
import com.visible.internationalminutes.ui.theme.VisibleBlue
import com.visible.internationalminutes.ui.theme.VisibleMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onBillingDayChange: (Int) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onSimSelectionChange: (Int) -> Unit,
    onPersistentNotificationToggle: (Boolean) -> Unit,
    onDemoDataToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDayDialog by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var showSimDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Billing Cycle Section
            item {
                SectionTitle(text = "Billing & Reset Cycle")
            }

            item {
                SettingsClickableCard(
                    icon = Icons.Default.CalendarToday,
                    title = "Billing Cycle Reset Day",
                    subtitle = "Day ${uiState.userSettings.billingDayOfMonth} of each month",
                    onClick = { showDayDialog = true }
                )
            }

            item {
                SettingsClickableCard(
                    icon = Icons.Default.Schedule,
                    title = "Cycle Reset Timezone",
                    subtitle = when (uiState.userSettings.billingTimezoneId) {
                        "America/New_York" -> "Eastern Time (EST/EDT) • Default"
                        "America/Chicago" -> "Central Time (CST/CDT)"
                        "America/Denver" -> "Mountain Time (MST/MDT)"
                        "America/Los_Angeles" -> "Pacific Time (PST/PDT)"
                        else -> uiState.userSettings.billingTimezoneId
                    },
                    onClick = { showTimezoneDialog = true }
                )
            }

            // SIM Selection Section
            item {
                SectionTitle(text = "Cellular SIM Card")
            }

            item {
                SettingsClickableCard(
                    icon = Icons.Default.SimCard,
                    title = "Selected Calling SIM",
                    subtitle = when (uiState.userSettings.selectedSimSubscriptionId) {
                        UserSettings.SIM_AUTO_VISIBLE -> "Auto-Detect Visible SIM (${uiState.activeSim?.displayTitle ?: "None detected"})"
                        UserSettings.SIM_ALL -> "All SIMs on Device"
                        else -> uiState.activeSim?.displayTitle ?: "Custom SIM (ID ${uiState.userSettings.selectedSimSubscriptionId})"
                    },
                    onClick = { showSimDialog = true }
                )
            }

            // Notifications & Background Sync Section
            item {
                SectionTitle(text = "Status & Background Sync")
            }

            item {
                SettingsClickableCard(
                    icon = if (uiState.isBatteryUnrestricted) Icons.Default.CheckCircle else Icons.Default.BatteryChargingFull,
                    title = "Battery & Background Sync",
                    subtitle = if (uiState.isBatteryUnrestricted) {
                        "Unrestricted • App is exempt from Android sleep restrictions"
                    } else {
                        "Optimized • Tap to allow unrestricted background activity"
                    },
                    tint = if (uiState.isBatteryUnrestricted) VisibleMint else VisibleAmber,
                    onClick = {
                        try {
                            val intent = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                val intent = Intent(AndroidSettings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Default.Notifications,
                    title = "Persistent Status Notification",
                    subtitle = "Shows ongoing remaining minutes (${uiState.quotaSummary.remainingMinutes} min) in the notification bar",
                    checked = uiState.userSettings.showPersistentNotification,
                    onCheckedChange = onPersistentNotificationToggle
                )
            }

            // Testing / Simulation Section
            item {
                SectionTitle(text = "Testing & Diagnostics")
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Default.Science,
                    title = "Demo Mock Call Data",
                    subtitle = "Populates realistic international test calls (UK, France, India, Mexico, etc.) for testing the UI",
                    checked = uiState.userSettings.isDemoDataEnabled,
                    onCheckedChange = onDemoDataToggle
                )
            }

            // Plan Info Card
            item {
                PlanInformationCard()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Billing Day Dialog
    if (showDayDialog) {
        BillingDayPickerDialog(
            currentDay = uiState.userSettings.billingDayOfMonth,
            onDismiss = { showDayDialog = false },
            onConfirm = { day ->
                onBillingDayChange(day)
                showDayDialog = false
            }
        )
    }

    // Timezone Dialog
    if (showTimezoneDialog) {
        TimezonePickerDialog(
            currentTimezone = uiState.userSettings.billingTimezoneId,
            onDismiss = { showTimezoneDialog = false },
            onSelect = { tz ->
                onTimezoneChange(tz)
                showTimezoneDialog = false
            }
        )
    }

    // SIM Selection Dialog
    if (showSimDialog) {
        SimSelectionDialog(
            currentPref = uiState.userSettings.selectedSimSubscriptionId,
            availableSims = uiState.availableSims,
            onDismiss = { showSimDialog = false },
            onSelect = { subId ->
                onSimSelectionChange(subId)
                showSimDialog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = VisibleBlue,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsClickableCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color = VisibleBlue,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VisibleBlue,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = VisibleBlue
                )
            )
        }
    }
}

@Composable
private fun PlanInformationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VisibleBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Visible+ Pro Plan Calling Rules",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• 500 minutes per month pooled across 85+ supported international countries.\n" +
                        "• Unlimited calls to Canada, Mexico, Puerto Rico, US Virgin Islands, Guam, and Saipan.\n" +
                        "• Each call is billed rounded up to the nearest whole minute as per standard carrier metering.\n" +
                        "• Usage resets each month at 00:00:00 in your configured billing timezone (Default: Eastern Time).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BillingDayPickerDialog(
    currentDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDay by remember { mutableFloatStateOf(currentDay.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Select Billing Cycle Day")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Day ${selectedDay.toInt()}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = VisibleBlue
                )
                Text(
                    text = "Your billing cycle will reset on the ${selectedDay.toInt()}th of each month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = selectedDay,
                    onValueChange = { selectedDay = it },
                    valueRange = 1f..31f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = VisibleBlue,
                        activeTrackColor = VisibleBlue
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDay.toInt()) }) {
                Text("Save", fontWeight = FontWeight.Bold, color = VisibleBlue)
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
private fun TimezonePickerDialog(
    currentTimezone: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val timezones = listOf(
        "America/New_York" to "Eastern Time (EST/EDT) - Default",
        "America/Chicago" to "Central Time (CST/CDT)",
        "America/Denver" to "Mountain Time (MST/MDT)",
        "America/Los_Angeles" to "Pacific Time (PST/PDT)",
        "America/Anchorage" to "Alaska Time (AKST/AKDT)",
        "Pacific/Honolulu" to "Hawaii Time (HST)",
        "UTC" to "Coordinated Universal Time (UTC)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Select Reset Timezone")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                timezones.forEach { (tzId, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tzId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTimezone == tzId,
                            onClick = { onSelect(tzId) },
                            colors = RadioButtonDefaults.colors(selectedColor = VisibleBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SimSelectionDialog(
    currentPref: Int,
    availableSims: List<com.visible.internationalminutes.model.SimInfo>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Select SIM Card")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Auto Detect Visible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(UserSettings.SIM_AUTO_VISIBLE) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentPref == UserSettings.SIM_AUTO_VISIBLE,
                        onClick = { onSelect(UserSettings.SIM_AUTO_VISIBLE) },
                        colors = RadioButtonDefaults.colors(selectedColor = VisibleBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Auto-Detect Visible SIM",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically finds Visible or Verizon SIM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // All SIMs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(UserSettings.SIM_ALL) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentPref == UserSettings.SIM_ALL,
                        onClick = { onSelect(UserSettings.SIM_ALL) },
                        colors = RadioButtonDefaults.colors(selectedColor = VisibleBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All Device SIMs (No SIM filter)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Specific detected SIMs
                availableSims.forEach { sim ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sim.subscriptionId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentPref == sim.subscriptionId,
                            onClick = { onSelect(sim.subscriptionId) },
                            colors = RadioButtonDefaults.colors(selectedColor = VisibleBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = sim.displayTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (sim.isVisibleOrVerizon) {
                                Text(
                                    text = "Visible / Verizon Carrier",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VisibleMint
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


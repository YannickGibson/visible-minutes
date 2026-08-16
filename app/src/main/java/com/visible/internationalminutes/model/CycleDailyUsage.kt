package com.visible.internationalminutes.model

/**
 * Encapsulates the daily histogram data and summary for a single monthly billing cycle slide.
 */
data class CycleDailyUsage(
    val billingCycle: BillingCycle,
    val cycleLabel: String,         // e.g. "Aug 2026 Cycle" or "Current Cycle"
    val formattedDateRange: String, // e.g. "Aug 15 – Sep 14, 2026"
    val dailyUsages: List<DailyUsage>,
    val totalCountedMinutes: Int,
    val totalUnlimitedMinutes: Int,
    val totalCallsCount: Int,
    val isCurrentCycle: Boolean = false
)

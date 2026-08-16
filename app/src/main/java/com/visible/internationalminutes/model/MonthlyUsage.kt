package com.visible.internationalminutes.model

/**
 * Historical billing cycle bucket for monthly histogram rendering.
 */
data class MonthlyUsage(
    val billingCycle: BillingCycle,
    val cycleLabel: String,      // e.g. "Aug '26", "Jul '26"
    val shortDateRange: String,  // e.g. "Aug 15 - Sep 14"
    val countedMinutes: Int,
    val unlimitedMinutes: Int,
    val callsCount: Int,
    val isCurrentCycle: Boolean = false
)

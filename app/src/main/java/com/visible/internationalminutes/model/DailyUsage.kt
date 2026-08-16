package com.visible.internationalminutes.model

/**
 * Daily bucket for histogram rendering.
 */
data class DailyUsage(
    val dayStartEpochMillis: Long,
    val dayLabel: String,       // e.g. "Aug 15"
    val dayOfWeekLabel: String,  // e.g. "Sat"
    val countedMinutes: Int,
    val unlimitedMinutes: Int,
    val callsCount: Int,
    val isToday: Boolean = false
)

/**
 * Breakdown of minutes grouped by destination country.
 */
data class CountryUsage(
    val country: SupportedCountry,
    val billedMinutes: Int,
    val callsCount: Int,
    val isUnlimited: Boolean
)

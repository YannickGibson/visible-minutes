package com.visible.internationalminutes.model

import kotlin.math.max

enum class QuotaHealth {
    HEALTHY,   // < 70% used
    WARNING,   // 70% - 90% used
    CRITICAL,  // > 90% used
    EXHAUSTED  // >= 100% used
}

/**
 * Aggregated summary of international calling usage for a billing cycle.
 */
data class QuotaSummary(
    val monthlyLimitMinutes: Int = 500,
    val billedCountedMinutes: Int = 0,
    val exactCountedSeconds: Long = 0,
    val unlimitedBilledMinutes: Int = 0,
    val totalOutgoingCallsCount: Int = 0,
    val countedCallsCount: Int = 0,
    val unlimitedCallsCount: Int = 0,
    val billingCycle: BillingCycle
) {
    val remainingMinutes: Int
        get() = max(0, monthlyLimitMinutes - billedCountedMinutes)

    val usedFraction: Float
        get() {
            if (monthlyLimitMinutes <= 0) return 0f
            return (billedCountedMinutes.toFloat() / monthlyLimitMinutes.toFloat()).coerceIn(0f, 1f)
        }

    val usedPercentage: Int
        get() = (usedFraction * 100).toInt()

    val totalDaysInCycle: Int
        get() = billingCycle.totalDaysInCycle

    val daysElapsed: Int
        get() = billingCycle.daysElapsedInCycle

    /**
     * Current average counted minutes used per elapsed day in this cycle.
     */
    val currentDailyAverageMinutes: Float
        get() = (billedCountedMinutes.toFloat() / daysElapsed.toFloat()).coerceAtLeast(0f)

    /**
     * Optimal target daily usage rate to evenly distribute 500 minutes over the billing period.
     */
    val optimalDailyMinutes: Float
        get() = (monthlyLimitMinutes.toFloat() / totalDaysInCycle.toFloat()).coerceAtLeast(0f)

    val formattedCurrentDailyAvg: String
        get() = String.format(java.util.Locale.US, "%.1f", currentDailyAverageMinutes)

    val formattedOptimalDaily: String
        get() = String.format(java.util.Locale.US, "%.1f", optimalDailyMinutes)

    val health: QuotaHealth
        get() = when {
            billedCountedMinutes >= monthlyLimitMinutes -> QuotaHealth.EXHAUSTED
            usedFraction >= 0.90f -> QuotaHealth.CRITICAL
            usedFraction >= 0.70f -> QuotaHealth.WARNING
            else -> QuotaHealth.HEALTHY
        }
}

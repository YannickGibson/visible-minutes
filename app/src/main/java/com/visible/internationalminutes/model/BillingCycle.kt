package com.visible.internationalminutes.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Encapsulates the user's monthly billing cycle calculation according to American timezone rules (e.g. Eastern Time).
 */
data class BillingCycle(
    val billingDayOfMonth: Int,
    val zoneId: ZoneId = DEFAULT_ZONE,
    val startInstant: Instant,
    val endInstant: Instant,
    val nowInstant: Instant = Instant.now()
) {
    val startMillis: Long get() = startInstant.toEpochMilli()
    val endMillis: Long get() = endInstant.toEpochMilli()
    val nowMillis: Long get() = nowInstant.toEpochMilli()

    /**
     * Percentage through the current billing period (0.0 to 1.0).
     */
    val progressFraction: Float
        get() {
            val total = endMillis - startMillis
            if (total <= 0) return 0f
            val elapsed = (nowMillis - startMillis).coerceIn(0L, total)
            return elapsed.toFloat() / total.toFloat()
        }

    val daysRemaining: Long
        get() = ChronoUnit.DAYS.between(
            nowInstant.atZone(zoneId).toLocalDate(),
            endInstant.atZone(zoneId).toLocalDate()
        ).coerceAtLeast(0)

    val hoursRemaining: Long
        get() = ChronoUnit.HOURS.between(nowInstant, endInstant).coerceAtLeast(0)

    val totalDaysInCycle: Int
        get() {
            val start = startInstant.atZone(zoneId).toLocalDate()
            val end = endInstant.atZone(zoneId).toLocalDate()
            return ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
        }

    val daysElapsedInCycle: Int
        get() {
            val start = startInstant.atZone(zoneId).toLocalDate()
            val now = nowInstant.atZone(zoneId).toLocalDate()
            val diff = ChronoUnit.DAYS.between(start, now).toInt() + 1
            return diff.coerceIn(1, totalDaysInCycle)
        }

    val formattedCountdown: String
        get() {
            val days = daysRemaining
            val hours = (hoursRemaining % 24)
            return when {
                days > 1 -> "$days days remaining"
                days == 1L -> "1 day, $hours hrs remaining"
                hours > 0 -> "$hours hours remaining"
                else -> "Resets today!"
            }
        }

    val formattedDateRange: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
            val yearFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
            val startZdt = startInstant.atZone(zoneId)
            val endZdt = endInstant.atZone(zoneId).minusSeconds(1) // Show inclusive last day

            return if (startZdt.year == endZdt.year) {
                "${formatter.format(startZdt)} – ${yearFormatter.format(endZdt)}"
            } else {
                "${yearFormatter.format(startZdt)} – ${yearFormatter.format(endZdt)}"
            }
        }

    val formattedResetDate: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy (z)", Locale.US)
            return formatter.format(endInstant.atZone(zoneId))
        }

    companion object {
        val DEFAULT_ZONE: ZoneId = ZoneId.of("America/New_York")

        /**
         * Calculates the active billing cycle for a given point in time, billing day, and timezone.
         */
        fun calculate(
            billingDayOfMonth: Int,
            zoneId: ZoneId = DEFAULT_ZONE,
            referenceInstant: Instant = Instant.now()
        ): BillingCycle {
            val clampedDay = billingDayOfMonth.coerceIn(1, 31)
            val zonedDateTime = referenceInstant.atZone(zoneId)
            val localDate = zonedDateTime.toLocalDate()

            // Compare against this month's actual reset date. A day-31 cycle,
            // for example, resets on September 30, not October 1.
            val resetThisMonth = localDate.withDayOfMonth(
                clampedDay.coerceAtMost(localDate.lengthOfMonth())
            )
            val startLocalDate = if (!localDate.isBefore(resetThisMonth)) {
                resetThisMonth
            } else {
                val prevMonth = localDate.minusMonths(1)
                val safeDay = clampedDay.coerceAtMost(prevMonth.lengthOfMonth())
                LocalDate.of(prevMonth.year, prevMonth.month, safeDay)
            }

            val nextMonth = startLocalDate.plusMonths(1)
            val endLocalDate = LocalDate.of(
                nextMonth.year,
                nextMonth.month,
                clampedDay.coerceAtMost(nextMonth.lengthOfMonth())
            )

            val startZdt = ZonedDateTime.of(startLocalDate, LocalTime.MIN, zoneId)
            val endZdt = ZonedDateTime.of(endLocalDate, LocalTime.MIN, zoneId)

            return BillingCycle(
                billingDayOfMonth = clampedDay,
                zoneId = zoneId,
                startInstant = startZdt.toInstant(),
                endInstant = endZdt.toInstant(),
                nowInstant = referenceInstant
            )
        }

        /**
         * Generates past N billing cycles for historical analysis and histograms.
         */
        fun generatePastCycles(
            count: Int,
            billingDayOfMonth: Int,
            zoneId: ZoneId = DEFAULT_ZONE,
            referenceInstant: Instant = Instant.now()
        ): List<BillingCycle> {
            val cycles = mutableListOf<BillingCycle>()
            var currentRef = referenceInstant
            for (i in 0 until count) {
                val cycle = calculate(billingDayOfMonth, zoneId, currentRef)
                cycles.add(cycle)
                currentRef = cycle.startInstant.minus(1, ChronoUnit.DAYS)
            }
            return cycles
        }
    }
}

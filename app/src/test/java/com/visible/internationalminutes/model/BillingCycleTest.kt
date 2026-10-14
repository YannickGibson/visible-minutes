package com.visible.internationalminutes.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BillingCycleTest {
    private val zone = BillingCycle.DEFAULT_ZONE

    @Test
    fun `day 31 resets at midnight on the last day of a short month`() {
        val reset = midnight("2026-09-30")
        val before = BillingCycle.calculate(31, zone, reset.minusMillis(1))
        val after = BillingCycle.calculate(31, zone, reset)

        assertEquals(midnight("2026-08-31"), before.startInstant)
        assertEquals(reset, before.endInstant)
        assertEquals(reset, after.startInstant)
        assertEquals(midnight("2026-10-31"), after.endInstant)
        assertEquals(0f, after.progressFraction, 0f)
        assertEquals(1, after.daysElapsedInCycle)
    }

    @Test
    fun `days 29 through 31 reset on February 28 in a non leap year`() {
        for (billingDay in 29..31) {
            val cycle = BillingCycle.calculate(billingDay, zone, midnight("2026-02-28"))

            assertEquals("Billing day $billingDay", midnight("2026-02-28"), cycle.startInstant)
            assertEquals(midnight("2026-03-$billingDay"), cycle.endInstant)
            assertEquals(0f, cycle.progressFraction, 0f)
        }
    }

    @Test
    fun `February uses leap day and restores the requested day in March`() {
        val cycle = BillingCycle.calculate(31, zone, midnight("2028-02-29"))

        assertEquals(midnight("2028-02-29"), cycle.startInstant)
        assertEquals(midnight("2028-03-31"), cycle.endInstant)
    }

    @Test
    fun `regular billing day rolls over across multiple months`() {
        for (month in 8..10) {
            val reset = LocalDate.of(2026, month, 7).atStartOfDay(zone).toInstant()
            val before = BillingCycle.calculate(7, zone, reset.minusMillis(1))
            val after = BillingCycle.calculate(7, zone, reset)

            assertEquals(reset, before.endInstant)
            assertEquals(reset, after.startInstant)
            assertEquals(0f, after.progressFraction, 0f)
            assertEquals(1, after.daysElapsedInCycle)
        }
    }

    @Test
    fun `active cycle always contains the reference time for every billing day`() {
        val zones = listOf(zone, ZoneId.of("UTC"), ZoneId.of("Asia/Tokyo"))
        for (testZone in zones) {
            for (year in listOf(2026, 2028)) {
                var date = LocalDate.of(year, 1, 1)
                while (date.year == year) {
                    for (billingDay in 1..31) {
                        for (hour in listOf(0L, 12L)) {
                            val reference = date.atStartOfDay(testZone).plusHours(hour).toInstant()
                            val cycle = BillingCycle.calculate(billingDay, testZone, reference)
                            val message = "$date, hour $hour, billing day $billingDay, $testZone"

                            assertFalse(message, cycle.startInstant.isAfter(reference))
                            assertTrue(message, cycle.endInstant.isAfter(reference))
                            assertTrue(message, cycle.progressFraction in 0f..1f)
                            assertTrue(message, cycle.totalDaysInCycle in 28..31)
                        }
                    }
                    date = date.plusDays(1)
                }
            }
        }
    }

    @Test
    fun `history is contiguous across short months and year boundaries`() {
        val reference = midnight("2026-05-31")
        val cycles = BillingCycle.generatePastCycles(6, 31, zone, reference)

        assertEquals(6, cycles.size)
        assertEquals(reference, cycles.first().startInstant)
        assertEquals(
            listOf("2026-05-31", "2026-04-30", "2026-03-31", "2026-02-28", "2026-01-31", "2025-12-31")
                .map(::midnight),
            cycles.map { it.startInstant }
        )
        cycles.zipWithNext().forEach { (newer, older) ->
            assertEquals(newer.startInstant, older.endInstant)
        }
    }

    @Test
    fun `reset follows the billing timezone instead of UTC midnight`() {
        val before = BillingCycle.calculate(7, zone, Instant.parse("2026-10-07T03:59:59Z"))
        val after = BillingCycle.calculate(7, zone, Instant.parse("2026-10-07T04:00:00Z"))

        assertEquals(midnight("2026-09-07"), before.startInstant)
        assertEquals(midnight("2026-10-07"), after.startInstant)
        assertEquals(0f, after.progressFraction, 0f)
    }

    @Test
    fun `cycle progress uses actual elapsed time across daylight saving changes`() {
        for ((startDate, expectedHours) in listOf("2026-03-01" to 743L, "2026-11-01" to 721L)) {
            val start = midnight(startDate)
            val initial = BillingCycle.calculate(1, zone, start)
            val duration = Duration.between(initial.startInstant, initial.endInstant)
            val halfway = BillingCycle.calculate(1, zone, start.plus(duration.dividedBy(2)))

            assertEquals(expectedHours, duration.toHours())
            assertEquals(0.5f, halfway.progressFraction, 0.0001f)
        }
    }

    private fun midnight(date: String): Instant = LocalDate.parse(date).atStartOfDay(zone).toInstant()
}

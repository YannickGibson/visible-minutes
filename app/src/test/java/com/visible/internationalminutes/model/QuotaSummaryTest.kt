package com.visible.internationalminutes.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class QuotaSummaryTest {
    private val cycle = BillingCycle.calculate(7, referenceInstant = Instant.parse("2026-10-14T16:00:00Z"))

    @Test
    fun `used fraction increases from unused through exhausted`() {
        for ((used, expectedFraction) in listOf(0 to 0f, 125 to 0.25f, 250 to 0.5f, 500 to 1f, 650 to 1f)) {
            val quota = QuotaSummary(billedCountedMinutes = used, billingCycle = cycle)

            assertEquals("$used minutes used", expectedFraction, quota.usedFraction, 0.0001f)
            assertEquals((500 - used).coerceAtLeast(0), quota.remainingMinutes)
        }
    }

    @Test
    fun `used fraction uses the configured quota and clamps invalid fractions`() {
        assertEquals(0.25f, QuotaSummary(1000, 250, billingCycle = cycle).usedFraction, 0f)
        assertEquals(0f, QuotaSummary(0, 250, billingCycle = cycle).usedFraction, 0f)
        assertEquals(0f, QuotaSummary(-1, 250, billingCycle = cycle).usedFraction, 0f)
        assertEquals(0f, QuotaSummary(500, -1, billingCycle = cycle).usedFraction, 0f)
    }

    @Test
    fun `380 minutes left fills 76 percent of the gauge`() {
        val quota = QuotaSummary(billedCountedMinutes = 120, billingCycle = cycle)

        assertEquals(380, quota.remainingMinutes)
        assertEquals(0.76f, quota.remainingFraction, 0.0001f)
        assertEquals(0.24f, quota.usedFraction, 0.0001f)
    }

    @Test
    fun `remaining gauge drains as minutes are used and empties at the limit`() {
        for ((used, expectedFraction) in listOf(0 to 1f, 125 to 0.75f, 250 to 0.5f, 500 to 0f, 650 to 0f)) {
            val quota = QuotaSummary(billedCountedMinutes = used, billingCycle = cycle)

            assertEquals("$used minutes used", expectedFraction, quota.remainingFraction, 0.0001f)
        }
    }

    @Test
    fun `remaining gauge handles custom and invalid limits without overflowing`() {
        assertEquals(0.75f, QuotaSummary(1000, 250, billingCycle = cycle).remainingFraction, 0f)
        assertEquals(0f, QuotaSummary(0, 0, billingCycle = cycle).remainingFraction, 0f)
        assertEquals(0f, QuotaSummary(-1, 0, billingCycle = cycle).remainingFraction, 0f)
        assertEquals(1f, QuotaSummary(500, -1, billingCycle = cycle).remainingFraction, 0f)
    }

    @Test
    fun `warning colors follow usage thresholds`() {
        for ((used, expectedHealth) in listOf(
            0 to QuotaHealth.HEALTHY,
            349 to QuotaHealth.HEALTHY,
            350 to QuotaHealth.WARNING,
            449 to QuotaHealth.WARNING,
            450 to QuotaHealth.CRITICAL,
            499 to QuotaHealth.CRITICAL,
            500 to QuotaHealth.EXHAUSTED,
            650 to QuotaHealth.EXHAUSTED
        )) {
            assertEquals(expectedHealth, QuotaSummary(billedCountedMinutes = used, billingCycle = cycle).health)
        }
    }
}

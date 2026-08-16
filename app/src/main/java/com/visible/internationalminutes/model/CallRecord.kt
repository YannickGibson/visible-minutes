package com.visible.internationalminutes.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Model representing an outgoing call log entry.
 */
data class CallRecord(
    val id: Long,
    val rawNumber: String,
    val formattedNumber: String,
    val contactName: String?,
    val timestampMillis: Long,
    val durationSeconds: Long,
    val country: SupportedCountry?,
    val quotaCategory: QuotaCategory,
    val simSubscriptionId: Int? = null,
    val simDisplayName: String? = null
) {
    /**
     * Cellular carriers (including Verizon/Visible) bill voice calls rounded up to the nearest whole minute.
     * E.g. a 5-second call = 1 billed minute; a 61-second call = 2 billed minutes.
     */
    val billedMinutes: Int
        get() {
            if (durationSeconds <= 0) return 0
            return ceil(durationSeconds / 60.0).toInt()
        }

    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format(Locale.US, "%d:%02d (%d min billed)", minutes, seconds, billedMinutes)
        }

    val formattedShortDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format(Locale.US, "%dm %02ds", minutes, seconds)
        }

    val formattedDateTime: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
            return sdf.format(Date(timestampMillis))
        }

    val formattedShortDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US)
            return sdf.format(Date(timestampMillis))
        }

    val isInternational: Boolean
        get() = quotaCategory == QuotaCategory.COUNTED_500_MIN ||
                quotaCategory == QuotaCategory.UNLIMITED_PRO ||
                quotaCategory == QuotaCategory.UNSUPPORTED_DESTINATION

    val countsTowardsQuota: Boolean
        get() = quotaCategory == QuotaCategory.COUNTED_500_MIN
}

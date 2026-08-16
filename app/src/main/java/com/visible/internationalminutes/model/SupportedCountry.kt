package com.visible.internationalminutes.model

/**
 * Quota category for Visible+ Pro plan calls.
 */
enum class QuotaCategory {
    /**
     * Counts against the 500 minutes monthly allotment.
     */
    COUNTED_500_MIN,

    /**
     * Unlimited international destinations included in Visible+ Pro (Mexico, Canada, Puerto Rico, USVI, Guam, Saipan).
     */
    UNLIMITED_PRO,

    /**
     * Domestic US call (does not count toward international quota).
     */
    DOMESTIC_US,

    /**
     * Call to an unsupported international destination (may incur extra charges or be blocked).
     */
    UNSUPPORTED_DESTINATION
}

/**
 * Country metadata and calling rules under Visible+ Pro plan.
 */
data class SupportedCountry(
    val name: String,
    val isoCode: String,
    val dialCode: String,
    val quotaCategory: QuotaCategory,
    val flagEmoji: String = isoCodeToEmoji(isoCode),
    val alternateNames: List<String> = emptyList()
) {
    val isUnlimited: Boolean
        get() = quotaCategory == QuotaCategory.UNLIMITED_PRO

    val is500MinQuota: Boolean
        get() = quotaCategory == QuotaCategory.COUNTED_500_MIN
}

/**
 * Helper to convert ISO 3166-1 alpha-2 country code to emoji flag.
 */
fun isoCodeToEmoji(isoCode: String): String {
    if (isoCode.length != 2) return "🌐"
    val firstChar = Character.codePointAt(isoCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(isoCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

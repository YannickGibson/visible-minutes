package com.visible.internationalminutes.data

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.visible.internationalminutes.model.QuotaCategory
import com.visible.internationalminutes.model.SupportedCountry
import java.util.Locale

/**
 * Repository containing the complete Visible+ Pro international calling destinations list
 * and phone number classification algorithms using Google's libphonenumber.
 */
object SupportedCountriesRepository {

    private val phoneUtil: PhoneNumberUtil by lazy {
        PhoneNumberUtil.getInstance()
    }

    /**
     * Complete list of all supported countries & territories under Visible+ Pro plan.
     */
    val allSupportedCountries: List<SupportedCountry> = listOf(
        // Unlimited Destinations
        SupportedCountry("Canada", "CA", "+1", QuotaCategory.UNLIMITED_PRO),
        SupportedCountry("Mexico", "MX", "+52", QuotaCategory.UNLIMITED_PRO),
        SupportedCountry("Puerto Rico", "PR", "+1-787", QuotaCategory.UNLIMITED_PRO, alternateNames = listOf("+1-939")),
        SupportedCountry("US Virgin Islands", "VI", "+1-340", QuotaCategory.UNLIMITED_PRO),
        SupportedCountry("Guam", "GU", "+1-671", QuotaCategory.UNLIMITED_PRO),
        SupportedCountry("Northern Mariana Islands / Saipan", "MP", "+1-670", QuotaCategory.UNLIMITED_PRO, alternateNames = listOf("Saipan", "Mariana Islands")),

        // 500 Minutes Monthly Allotment Destinations
        SupportedCountry("American Samoa", "AS", "+1-684", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Andorra", "AD", "+376", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Anguilla", "AI", "+1-264", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Argentina", "AR", "+54", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Australia", "AU", "+61", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Austria", "AT", "+43", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Bahamas", "BS", "+1-242", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Belarus", "BY", "+375", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Belgium", "BE", "+32", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Bermuda", "BM", "+1-441", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Bosnia and Herzegovina", "BA", "+387", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Bosnia")),
        SupportedCountry("Brazil", "BR", "+55", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Brunei", "BN", "+673", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Bulgaria", "BG", "+359", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Cambodia", "KH", "+855", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Cayman Islands", "KY", "+1-345", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Chile", "CL", "+56", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("China", "CN", "+86", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Christmas and Cocos Island", "CX", "+61", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Cocos Islands", "CC")),
        SupportedCountry("Colombia", "CO", "+57", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Costa Rica", "CR", "+506", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Czech Republic", "CZ", "+420", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Czech Rep")),
        SupportedCountry("Denmark", "DK", "+45", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Dominican Republic", "DO", "+1-809", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("+1-829", "+1-849")),
        SupportedCountry("El Salvador", "SV", "+503", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Faroe Islands", "FO", "+298", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Fiji", "FJ", "+679", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Finland", "FI", "+358", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("France", "FR", "+33", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("French Guiana", "GF", "+594", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Germany", "DE", "+49", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Gibraltar", "GI", "+350", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Greece", "GR", "+30", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Guadeloupe", "GP", "+590", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Hong Kong", "HK", "+852", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Hungary", "HU", "+36", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Iceland", "IS", "+354", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("India", "IN", "+91", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Indonesia", "ID", "+62", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Ireland", "IE", "+353", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Israel", "IL", "+972", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Italy", "IT", "+39", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Japan", "JP", "+81", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Kazakhstan", "KZ", "+7", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Kuwait", "KW", "+965", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Kyrgyzstan", "KG", "+996", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Liechtenstein", "LI", "+423", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Luxembourg", "LU", "+352", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Macau", "MO", "+853", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Malaysia", "MY", "+60", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Malta", "MT", "+356", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Marshall Islands", "MH", "+692", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Martinique", "MQ", "+596", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Monaco", "MC", "+377", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Mongolia", "MN", "+976", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Montenegro", "ME", "+382", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Montserrat", "MS", "+1-664", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Nepal", "NP", "+977", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Netherlands", "NL", "+31", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("New Zealand", "NZ", "+64", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Norway", "NO", "+47", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Pakistan", "PK", "+92", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Palau", "PW", "+680", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Panama", "PA", "+507", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Paraguay", "PY", "+595", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Peru", "PE", "+51", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Poland", "PL", "+48", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Portugal", "PT", "+351", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Romania", "RO", "+40", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Russian Federation", "RU", "+7", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Russia")),
        SupportedCountry("Saudi Arabia", "SA", "+966", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Singapore", "SG", "+65", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Slovak Republic", "SK", "+421", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Slovak Rep", "Slovakia")),
        SupportedCountry("Slovenia", "SI", "+386", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("South Africa", "ZA", "+27", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("South Korea", "KR", "+82", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Korea")),
        SupportedCountry("Spain", "ES", "+34", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Sri Lanka", "LK", "+94", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Swaziland", "SZ", "+268", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Eswatini")),
        SupportedCountry("Sweden", "SE", "+46", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Switzerland", "CH", "+41", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Taiwan", "TW", "+886", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Thailand", "TH", "+66", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Turkey", "TR", "+90", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("Türkiye")),
        SupportedCountry("United Arab Emirates", "AE", "+971", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("UAE")),
        SupportedCountry("United Kingdom", "GB", "+44", QuotaCategory.COUNTED_500_MIN, alternateNames = listOf("UK", "Great Britain")),
        SupportedCountry("Uruguay", "UY", "+598", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Venezuela", "VE", "+58", QuotaCategory.COUNTED_500_MIN),
        SupportedCountry("Vietnam", "VN", "+84", QuotaCategory.COUNTED_500_MIN)
    ).sortedBy { it.name }

    private val countryByIso: Map<String, SupportedCountry> by lazy {
        allSupportedCountries.associateBy { it.isoCode.uppercase(Locale.US) }
    }

    data class ParsedCallInfo(
        val rawNumber: String,
        val formattedNumber: String,
        val country: SupportedCountry?,
        val quotaCategory: QuotaCategory,
        val regionCode: String?
    )

    /**
     * Determines whether a dialed number from the call log is domestic US, unlimited Visible+ Pro,
     * counted toward the 500-minute pool, or unsupported.
     */
    fun classifyNumber(rawNumber: String): ParsedCallInfo {
        val cleanNumber = rawNumber.trim().replace(" ", "").replace("-", "")
        if (cleanNumber.isBlank()) {
            return ParsedCallInfo(rawNumber, rawNumber, null, QuotaCategory.DOMESTIC_US, null)
        }

        // Check if number starts with standard US 011 international dialing prefix (e.g. 0114420...)
        val standardizedNumber = if (cleanNumber.startsWith("011") && cleanNumber.length > 3) {
            "+" + cleanNumber.substring(3)
        } else if (!cleanNumber.startsWith("+") && cleanNumber.length > 10 && !cleanNumber.startsWith("1")) {
            // E.g. raw dialed digits like 447911123456
            "+$cleanNumber"
        } else {
            cleanNumber
        }

        try {
            val protoNumber = phoneUtil.parse(standardizedNumber, "US")
            val regionCode = phoneUtil.getRegionCodeForNumber(protoNumber)?.uppercase(Locale.US)
            val formatted = phoneUtil.format(protoNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)

            if (regionCode == null || regionCode == "ZZ") {
                return ParsedCallInfo(rawNumber, rawNumber, null, QuotaCategory.UNSUPPORTED_DESTINATION, null)
            }

            // Domestic US check (excluding US territories that are handled separately)
            if (regionCode == "US") {
                val usCountry = SupportedCountry("United States", "US", "+1", QuotaCategory.DOMESTIC_US)
                return ParsedCallInfo(rawNumber, formatted, usCountry, QuotaCategory.DOMESTIC_US, regionCode)
            }

            // Check if it's one of the supported countries
            val supported = countryByIso[regionCode]
            if (supported != null) {
                return ParsedCallInfo(
                    rawNumber = rawNumber,
                    formattedNumber = formatted,
                    country = supported,
                    quotaCategory = supported.quotaCategory,
                    regionCode = regionCode
                )
            }

            // Unrecognized/unsupported foreign country
            val foreignCountry = SupportedCountry(
                name = Locale("", regionCode).displayCountry.ifBlank { regionCode },
                isoCode = regionCode,
                dialCode = "+${protoNumber.countryCode}",
                quotaCategory = QuotaCategory.UNSUPPORTED_DESTINATION
            )
            return ParsedCallInfo(
                rawNumber = rawNumber,
                formattedNumber = formatted,
                country = foreignCountry,
                quotaCategory = QuotaCategory.UNSUPPORTED_DESTINATION,
                regionCode = regionCode
            )
        } catch (e: NumberParseException) {
            // If libphonenumber could not parse, try fallback prefix matching
            return fallbackPrefixMatch(rawNumber)
        }
    }

    private fun fallbackPrefixMatch(rawNumber: String): ParsedCallInfo {
        val digitsOnly = rawNumber.filter { it.isDigit() || it == '+' }
        for (country in allSupportedCountries) {
            val cleanDial = country.dialCode.replace("-", "").replace(" ", "")
            if (digitsOnly.startsWith(cleanDial) || (cleanDial.startsWith("+") && digitsOnly.startsWith("011" + cleanDial.substring(1)))) {
                return ParsedCallInfo(
                    rawNumber = rawNumber,
                    formattedNumber = rawNumber,
                    country = country,
                    quotaCategory = country.quotaCategory,
                    regionCode = country.isoCode
                )
            }
        }
        return ParsedCallInfo(rawNumber, rawNumber, null, QuotaCategory.DOMESTIC_US, null)
    }
}

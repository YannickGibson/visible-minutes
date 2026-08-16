package com.visible.internationalminutes.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.visible.internationalminutes.model.BillingCycle
import com.visible.internationalminutes.model.CallRecord
import com.visible.internationalminutes.model.CountryUsage
import com.visible.internationalminutes.model.DailyUsage
import com.visible.internationalminutes.model.QuotaCategory
import com.visible.internationalminutes.model.QuotaSummary
import com.visible.internationalminutes.model.SimInfo
import com.visible.internationalminutes.model.SupportedCountry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Repository responsible for reading Android system call logs, filtering international calls,
 * auto-detecting SIM cards, and computing billing cycle statistics.
 */
class CallLogRepository(private val context: Context) {

    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retrieves active SIM subscriptions on the device.
     */
    fun getAvailableSims(): List<SimInfo> {
        val result = mutableListOf<SimInfo>()
        if (!hasPhoneStatePermission()) {
            return result
        }

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
            subList?.forEach { info ->
                val carrier = info.carrierName?.toString() ?: ""
                val displayName = info.displayName?.toString() ?: ""
                val isVisible = carrier.contains("visible", ignoreCase = true) ||
                        displayName.contains("visible", ignoreCase = true) ||
                        carrier.contains("verizon", ignoreCase = true)

                result.add(
                    SimInfo(
                        subscriptionId = info.subscriptionId,
                        displayName = displayName,
                        carrierName = carrier,
                        slotIndex = info.simSlotIndex,
                        isVisibleOrVerizon = isVisible
                    )
                )
            }
        } catch (e: SecurityException) {
            // Permission denied or restricted
        }
        return result
    }

    /**
     * Resolves the target SIM subscription ID.
     */
    fun resolveTargetSimId(selectedPref: Int, availableSims: List<SimInfo>): Int? {
        return when (selectedPref) {
            UserSettings.SIM_ALL -> null // Query all SIMs
            UserSettings.SIM_AUTO_VISIBLE -> {
                val visibleSim = availableSims.firstOrNull { it.isVisibleOrVerizon }
                visibleSim?.subscriptionId ?: availableSims.firstOrNull()?.subscriptionId
            }
            else -> selectedPref // Explicit sub ID
        }
    }

    /**
     * Loads outgoing calls for the specified billing cycle.
     */
    suspend fun getCallsForBillingCycle(
        billingCycle: BillingCycle,
        targetSimSubId: Int?,
        useDemoData: Boolean
    ): List<CallRecord> = withContext(Dispatchers.IO) {
        if (useDemoData) {
            return@withContext generateDemoCalls(billingCycle)
        }

        if (!hasCallLogPermission()) {
            return@withContext emptyList()
        }

        val calls = mutableListOf<CallRecord>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        val selection = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} < ?"
        val selectionArgs = arrayOf(
            CallLog.Calls.OUTGOING_TYPE.toString(),
            billingCycle.startMillis.toString(),
            billingCycle.endMillis.toString()
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                val phoneAccountIdIndex = it.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getLong(idIndex) else 0L
                    val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    val cachedName = if (nameIndex >= 0) it.getString(nameIndex) else null
                    val date = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                    val durationSec = if (durationIndex >= 0) it.getLong(durationIndex) else 0L
                    val phoneAccountId = if (phoneAccountIdIndex >= 0) it.getString(phoneAccountIdIndex) else null

                    // Filter by target SIM if required
                    if (targetSimSubId != null && phoneAccountId != null) {
                        val subIdFromAccount = phoneAccountId.toIntOrNull()
                        if (subIdFromAccount != null && subIdFromAccount != targetSimSubId) {
                            continue
                        }
                    }

                    val parsed = SupportedCountriesRepository.classifyNumber(rawNumber)

                    calls.add(
                        CallRecord(
                            id = id,
                            rawNumber = rawNumber,
                            formattedNumber = parsed.formattedNumber,
                            contactName = cachedName,
                            timestampMillis = date,
                            durationSeconds = durationSec,
                            country = parsed.country,
                            quotaCategory = parsed.quotaCategory,
                            simSubscriptionId = targetSimSubId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext calls
    }

    /**
     * Computes the aggregated quota summary for the billing cycle.
     */
    fun computeQuotaSummary(
        calls: List<CallRecord>,
        billingCycle: BillingCycle,
        monthlyQuotaMinutes: Int = 500
    ): QuotaSummary {
        var billedCountedMinutes = 0
        var exactCountedSeconds = 0L
        var unlimitedBilledMinutes = 0
        var countedCallsCount = 0
        var unlimitedCallsCount = 0

        calls.forEach { call ->
            when (call.quotaCategory) {
                QuotaCategory.COUNTED_500_MIN -> {
                    billedCountedMinutes += call.billedMinutes
                    exactCountedSeconds += call.durationSeconds
                    countedCallsCount++
                }
                QuotaCategory.UNLIMITED_PRO, QuotaCategory.DOMESTIC_US -> {
                    unlimitedBilledMinutes += call.billedMinutes
                    unlimitedCallsCount++
                }
                else -> {
                    // Other
                }
            }
        }

        return QuotaSummary(
            monthlyLimitMinutes = monthlyQuotaMinutes,
            billedCountedMinutes = billedCountedMinutes,
            exactCountedSeconds = exactCountedSeconds,
            unlimitedBilledMinutes = unlimitedBilledMinutes,
            totalOutgoingCallsCount = calls.size,
            countedCallsCount = countedCallsCount,
            unlimitedCallsCount = unlimitedCallsCount,
            billingCycle = billingCycle
        )
    }

    /**
     * Computes daily usage data for the histogram.
     */
    fun computeDailyUsage(
        calls: List<CallRecord>,
        billingCycle: BillingCycle,
        zoneId: ZoneId = billingCycle.zoneId
    ): List<DailyUsage> {
        val startZdt = billingCycle.startInstant.atZone(zoneId)
        val nowZdt = billingCycle.nowInstant.atZone(zoneId)
        val endZdt = billingCycle.endInstant.atZone(zoneId)

        // Generate daily buckets for every day in the billing cycle (full month)
        val dailyMap = mutableMapOf<LocalDate, MutableList<CallRecord>>()
        var currDate = startZdt.toLocalDate()
        val targetEndDate = endZdt.toLocalDate().minusDays(1)

        while (!currDate.isAfter(targetEndDate)) {
            dailyMap[currDate] = mutableListOf()
            currDate = currDate.plusDays(1)
        }

        calls.forEach { call ->
            val callDate = Instant.ofEpochMilli(call.timestampMillis).atZone(zoneId).toLocalDate()
            dailyMap[callDate]?.add(call)
        }

        val dayLabelFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

        return dailyMap.map { (date, dayCalls) ->
            var countedMins = 0
            var unlimitedMins = 0
            var count = 0

            dayCalls.forEach { call ->
                when (call.quotaCategory) {
                    QuotaCategory.COUNTED_500_MIN -> {
                        countedMins += call.billedMinutes
                        count++
                    }
                    QuotaCategory.UNLIMITED_PRO, QuotaCategory.DOMESTIC_US -> {
                        unlimitedMins += call.billedMinutes
                        count++
                    }
                    else -> {}
                }
            }

            val epochMillis = ZonedDateTime.of(date, LocalTime.MIN, zoneId).toInstant().toEpochMilli()
            val isToday = date == nowZdt.toLocalDate()
            DailyUsage(
                dayStartEpochMillis = epochMillis,
                dayLabel = dayLabelFormatter.format(date),
                dayOfWeekLabel = dayOfWeekFormatter.format(date),
                countedMinutes = countedMins,
                unlimitedMinutes = unlimitedMins,
                callsCount = count,
                isToday = isToday
            )
        }.sortedBy { it.dayStartEpochMillis }
    }

    /**
     * Computes usage aggregated by country. Filters out countries with 0 billed minutes.
     */
    fun computeCountryUsage(calls: List<CallRecord>): List<CountryUsage> {
        val grouped = calls.filter { it.country != null }.groupBy { it.country!! }
        return grouped.map { (country, countryCalls) ->
            val totalMins = countryCalls.sumOf { it.billedMinutes }
            CountryUsage(
                country = country,
                billedMinutes = totalMins,
                callsCount = countryCalls.size,
                isUnlimited = country.isUnlimited || country.quotaCategory == QuotaCategory.DOMESTIC_US
            )
        }.filter { it.billedMinutes > 0 }.sortedByDescending { it.billedMinutes }
    }

    /**
     * Generates historical monthly usage for the past N billing cycles.
     */
    suspend fun getHistoricalMonthlyUsage(
        cycleCount: Int = 6,
        billingDayOfMonth: Int,
        zoneId: ZoneId,
        targetSimSubId: Int?,
        useDemoData: Boolean
    ): List<com.visible.internationalminutes.model.MonthlyUsage> = withContext(Dispatchers.IO) {
        val cycles = BillingCycle.generatePastCycles(cycleCount, billingDayOfMonth, zoneId).reversed()
        val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM ''yy", Locale.US)
        val shortRangeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

        return@withContext cycles.mapIndexed { index, cycle ->
            val isCurrent = index == cycles.size - 1
            val calls = if (useDemoData) {
                if (isCurrent) {
                    generateDemoCalls(cycle)
                } else {
                    generatePastDemoCalls(cycle, index)
                }
            } else {
                getCallsForBillingCycle(cycle, targetSimSubId, useDemoData = false)
            }

            var countedMins = 0
            var unlimitedMins = 0
            var callCount = 0

            calls.forEach { call ->
                when (call.quotaCategory) {
                    QuotaCategory.COUNTED_500_MIN -> {
                        countedMins += call.billedMinutes
                        callCount++
                    }
                    QuotaCategory.UNLIMITED_PRO, QuotaCategory.DOMESTIC_US -> {
                        unlimitedMins += call.billedMinutes
                        callCount++
                    }
                    else -> {}
                }
            }

            val startZdt = cycle.startInstant.atZone(zoneId)
            val endZdt = cycle.endInstant.atZone(zoneId).minusSeconds(1)

            com.visible.internationalminutes.model.MonthlyUsage(
                billingCycle = cycle,
                cycleLabel = monthLabelFormatter.format(startZdt),
                shortDateRange = "${shortRangeFormatter.format(startZdt)} - ${shortRangeFormatter.format(endZdt)}",
                countedMinutes = countedMins,
                unlimitedMinutes = unlimitedMins,
                callsCount = callCount,
                isCurrentCycle = isCurrent
            )
        }
    }

    /**
     * Generates swipeable cycle buckets containing the full daily usage for each of the past N billing cycles.
     */
    suspend fun getDailyUsageForPastCycles(
        cycleCount: Int = 6,
        billingDayOfMonth: Int,
        zoneId: ZoneId,
        targetSimSubId: Int?,
        useDemoData: Boolean
    ): List<com.visible.internationalminutes.model.CycleDailyUsage> = withContext(Dispatchers.IO) {
        val cycles = BillingCycle.generatePastCycles(cycleCount, billingDayOfMonth, zoneId).reversed()
        val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

        return@withContext cycles.mapIndexed { index, cycle ->
            val isCurrent = index == cycles.size - 1
            val calls = if (useDemoData) {
                if (isCurrent) {
                    generateDemoCalls(cycle)
                } else {
                    generatePastDemoCalls(cycle, index)
                }
            } else {
                getCallsForBillingCycle(cycle, targetSimSubId, useDemoData = false)
            }

            val daily = computeDailyUsage(calls, cycle, zoneId)
            val countedMins = calls.filter { it.quotaCategory == QuotaCategory.COUNTED_500_MIN }.sumOf { it.billedMinutes }
            val unlimitedMins = calls.filter { it.quotaCategory == QuotaCategory.UNLIMITED_PRO || it.quotaCategory == QuotaCategory.DOMESTIC_US }.sumOf { it.billedMinutes }
            val totalCalls = calls.size

            val startZdt = cycle.startInstant.atZone(zoneId)

            com.visible.internationalminutes.model.CycleDailyUsage(
                billingCycle = cycle,
                cycleLabel = if (isCurrent) "Current Cycle (${monthLabelFormatter.format(startZdt)})" else monthLabelFormatter.format(startZdt),
                formattedDateRange = cycle.formattedDateRange,
                dailyUsages = daily,
                totalCountedMinutes = countedMins,
                totalUnlimitedMinutes = unlimitedMins,
                totalCallsCount = totalCalls,
                isCurrentCycle = isCurrent
            )
        }
    }

    private fun generatePastDemoCalls(cycle: BillingCycle, cycleIndex: Int): List<CallRecord> {
        val list = mutableListOf<CallRecord>()
        val startMillis = cycle.startMillis
        val durationMillis = (cycle.endMillis - cycle.startMillis).coerceAtLeast(86400000L)

        // Seed variance across past months
        val mockSets = listOf(
            listOf(Triple("+44 20 7946 0912", "UK", 1800L), Triple("+91 98765 43210", "IN", 3600L), Triple("+52 55 1234", "MX", 2400L)),
            listOf(Triple("+49 30 123456", "DE", 2100L), Triple("+33 1 4268", "FR", 1500L), Triple("+1 416 555", "CA", 4200L)),
            listOf(Triple("+81 3 1234", "JP", 1200L), Triple("+44 20 7946", "UK", 2700L), Triple("+57 1 2345", "CO", 1800L)),
            listOf(Triple("+91 98765", "IN", 4500L), Triple("+55 11 9876", "BR", 1600L), Triple("+52 55 1234", "MX", 3100L)),
            listOf(Triple("+61 2 9876", "AU", 1900L), Triple("+34 91 123", "ES", 2200L), Triple("+1 787 555", "PR", 1800L))
        )

        val set = mockSets.getOrElse(cycleIndex % mockSets.size) { mockSets.first() }
        var idSeq = 5000L + (cycleIndex * 100)

        set.forEachIndexed { i, (num, name, dur) ->
            val time = startMillis + ((durationMillis * (i + 1)) / (set.size + 1))
            val parsed = SupportedCountriesRepository.classifyNumber(num)
            list.add(
                CallRecord(
                    id = idSeq++,
                    rawNumber = num,
                    formattedNumber = parsed.formattedNumber,
                    contactName = name,
                    timestampMillis = time,
                    durationSeconds = dur,
                    country = parsed.country,
                    quotaCategory = parsed.quotaCategory,
                    simSubscriptionId = 1
                )
            )
        }
        return list
    }

    /**
     * Demo data generator to test in emulator or before making real international calls.
     */
    private fun generateDemoCalls(billingCycle: BillingCycle): List<CallRecord> {
        val list = mutableListOf<CallRecord>()
        val startMillis = billingCycle.startMillis
        val nowMillis = billingCycle.nowMillis
        val diff = (nowMillis - startMillis).coerceAtLeast(86400000L)

        val demoItems = listOf(
            Triple("+44 20 7946 0912", "Oliver Smith (UK)", 480L),     // 8 min (UK - 500m)
            Triple("+49 30 123456", "Claudia Weber (DE)", 750L),       // 13 min (Germany - 500m)
            Triple("+91 98765 43210", "Rajesh Kumar (IN)", 1420L),    // 24 min (India - 500m)
            Triple("+52 55 1234 5678", "Carlos Morales (MX)", 1850L),  // 31 min (Mexico - Unlimited)
            Triple("+81 3 1234 5678", "Kenji Sato (JP)", 360L),        // 6 min (Japan - 500m)
            Triple("+33 1 42 68 55 00", "Amélie Laurent (FR)", 610L),  // 11 min (France - 500m)
            Triple("+1 416 555 0199", "Sarah Jenkins (CA)", 2100L),    // 35 min (Canada - Unlimited)
            Triple("+57 1 2345678", "Mateo Gómez (CO)", 940L),         // 16 min (Colombia - 500m)
            Triple("+61 2 9876 5432", "Liam O'Connor (AU)", 520L),     // 9 min (Australia - 500m)
            Triple("+55 11 98765 4321", "Beatriz Silva (BR)", 1100L),  // 19 min (Brazil - 500m)
            Triple("+39 06 69874", "Marco Rossi (IT)", 430L),          // 8 min (Italy - 500m)
            Triple("+86 10 6552 9988", "Wei Zhang (CN)", 1250L),       // 21 min (China - 500m)
            Triple("+1 787 555 0123", "Sofia Rivera (PR)", 800L),      // 14 min (Puerto Rico - Unlimited)
            Triple("+34 91 123 4567", "Javier Alvarez (ES)", 580L)     // 10 min (Spain - 500m)
        )

        var idSeq = 1000L
        demoItems.forEachIndexed { index, (num, name, dur) ->
            val callTime = startMillis + ((diff * (index + 1)) / (demoItems.size + 1))
            val parsed = SupportedCountriesRepository.classifyNumber(num)
            list.add(
                CallRecord(
                    id = idSeq++,
                    rawNumber = num,
                    formattedNumber = parsed.formattedNumber,
                    contactName = name,
                    timestampMillis = callTime,
                    durationSeconds = dur,
                    country = parsed.country,
                    quotaCategory = parsed.quotaCategory,
                    simSubscriptionId = 1,
                    simDisplayName = "Visible eSIM"
                )
            )
        }

        return list.sortedByDescending { it.timestampMillis }
    }
}

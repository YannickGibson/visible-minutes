package com.visible.internationalminutes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visible.internationalminutes.data.CallLogRepository
import com.visible.internationalminutes.data.SupportedCountriesRepository
import com.visible.internationalminutes.data.UserSettings
import com.visible.internationalminutes.data.UserSettingsRepository
import com.visible.internationalminutes.model.BillingCycle
import com.visible.internationalminutes.model.CallRecord
import com.visible.internationalminutes.model.CountryUsage
import com.visible.internationalminutes.model.CycleDailyUsage
import com.visible.internationalminutes.model.DailyUsage
import com.visible.internationalminutes.model.MonthlyUsage
import com.visible.internationalminutes.model.QuotaSummary
import com.visible.internationalminutes.model.SimInfo
import com.visible.internationalminutes.model.SupportedCountry
import com.visible.internationalminutes.notification.QuotaNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

data class MainUiState(
    val isLoading: Boolean = true,
    val userSettings: UserSettings = UserSettings(),
    val billingCycle: BillingCycle = BillingCycle.calculate(1),
    val quotaSummary: QuotaSummary = QuotaSummary(billingCycle = BillingCycle.calculate(1)),
    val callRecords: List<CallRecord> = emptyList(),
    val dailyUsages: List<DailyUsage> = emptyList(),
    val cycleDailyUsages: List<CycleDailyUsage> = emptyList(),
    val monthlyUsages: List<MonthlyUsage> = emptyList(),
    val countryUsages: List<CountryUsage> = emptyList(),
    val availableSims: List<SimInfo> = emptyList(),
    val activeSim: SimInfo? = null,
    val isAutoDetectedSim: Boolean = false,
    val hasCallLogPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val searchQuery: String = "",
    val allSupportedCountries: List<SupportedCountry> = SupportedCountriesRepository.allSupportedCountries
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userSettingsRepo = UserSettingsRepository(application)
    private val callLogRepo = CallLogRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkBatteryOptimization()
        // Observe settings changes continuously as single source of truth
        viewModelScope.launch {
            userSettingsRepo.settingsFlow.collectLatest { settings ->
                loadDataFor(settings)
            }
        }
    }

    private suspend fun loadDataFor(settings: UserSettings) {
        _uiState.update { it.copy(isLoading = true, userSettings = settings) }

        val zoneId = try {
            ZoneId.of(settings.billingTimezoneId)
        } catch (e: Exception) {
            BillingCycle.DEFAULT_ZONE
        }

        val cycle = BillingCycle.calculate(settings.billingDayOfMonth, zoneId)
        val availableSims = callLogRepo.getAvailableSims()
        val targetSubId = callLogRepo.resolveTargetSimId(settings.selectedSimSubscriptionId, availableSims)

        val activeSim = availableSims.firstOrNull { it.subscriptionId == targetSubId }
        val isAuto = settings.selectedSimSubscriptionId == UserSettings.SIM_AUTO_VISIBLE

        val hasPermission = callLogRepo.hasCallLogPermission()
        val useDemo = settings.isDemoDataEnabled || (!hasPermission && settings.isDemoDataEnabled)

        val calls = callLogRepo.getCallsForBillingCycle(
            billingCycle = cycle,
            targetSimSubId = targetSubId,
            useDemoData = useDemo
        )

        val summary = callLogRepo.computeQuotaSummary(calls, cycle, settings.monthlyQuotaMinutes)
        val daily = callLogRepo.computeDailyUsage(calls, cycle, zoneId)
        val cycleDailies = callLogRepo.getDailyUsageForPastCycles(
            cycleCount = 6,
            billingDayOfMonth = settings.billingDayOfMonth,
            zoneId = zoneId,
            targetSimSubId = targetSubId,
            useDemoData = useDemo
        )
        val monthly = callLogRepo.getHistoricalMonthlyUsage(
            cycleCount = 6,
            billingDayOfMonth = settings.billingDayOfMonth,
            zoneId = zoneId,
            targetSimSubId = targetSubId,
            useDemoData = useDemo
        )
        val countries = callLogRepo.computeCountryUsage(calls)

        _uiState.update {
            it.copy(
                isLoading = false,
                userSettings = settings,
                billingCycle = cycle,
                quotaSummary = summary,
                callRecords = calls,
                dailyUsages = daily,
                cycleDailyUsages = cycleDailies,
                monthlyUsages = monthly,
                countryUsages = countries,
                availableSims = availableSims,
                activeSim = activeSim,
                isAutoDetectedSim = isAuto,
                hasCallLogPermission = hasPermission
            )
        }

        // Refresh ongoing notification
        QuotaNotificationManager.updatePersistentNotification(
            getApplication(),
            summary,
            settings.showPersistentNotification
        )
    }

    fun refreshData() {
        viewModelScope.launch {
            val settings = try {
                userSettingsRepo.settingsFlow.first()
            } catch (e: Exception) {
                _uiState.value.userSettings
            }
            loadDataFor(settings)
        }
    }

    fun setBillingDay(day: Int) {
        viewModelScope.launch {
            userSettingsRepo.setBillingDayOfMonth(day)
        }
    }

    fun setBillingTimezone(timezoneId: String) {
        viewModelScope.launch {
            userSettingsRepo.setBillingTimezone(timezoneId)
        }
    }

    fun setSelectedSim(subscriptionId: Int) {
        viewModelScope.launch {
            userSettingsRepo.setSelectedSimSubscriptionId(subscriptionId)
        }
    }

    fun setPersistentNotification(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepo.setPersistentNotificationEnabled(enabled)
        }
    }

    fun setDemoDataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepo.setDemoDataEnabled(enabled)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun checkBatteryOptimization() {
        val context = getApplication<Application>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isUnrestricted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        _uiState.update { it.copy(isBatteryUnrestricted = isUnrestricted) }
    }

    fun updatePermissions(hasCallLog: Boolean, hasNotification: Boolean) {
        _uiState.update {
            it.copy(
                hasCallLogPermission = hasCallLog,
                hasNotificationPermission = hasNotification
            )
        }
        refreshData()
    }
}

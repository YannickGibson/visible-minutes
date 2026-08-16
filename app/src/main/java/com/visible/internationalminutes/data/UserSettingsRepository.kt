package com.visible.internationalminutes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Settings configuration data class.
 */
data class UserSettings(
    val billingDayOfMonth: Int = 1,
    val billingTimezoneId: String = "America/New_York",
    val selectedSimSubscriptionId: Int = SIM_AUTO_VISIBLE, // -1: Auto-Detect Visible, -2: All SIMs, >=0: Specific SubId
    val showPersistentNotification: Boolean = true,
    val isDemoDataEnabled: Boolean = false,
    val monthlyQuotaMinutes: Int = 500
) {
    companion object {
        const val SIM_AUTO_VISIBLE = -1
        const val SIM_ALL = -2
    }
}

/**
 * Repository to persist user preferences using Jetpack DataStore.
 */
class UserSettingsRepository(private val context: Context) {

    private val KEY_BILLING_DAY = intPreferencesKey("billing_day_of_month")
    private val KEY_TIMEZONE = stringPreferencesKey("billing_timezone_id")
    private val KEY_SELECTED_SIM = intPreferencesKey("selected_sim_id")
    private val KEY_PERSISTENT_NOTIF = booleanPreferencesKey("show_persistent_notification")
    private val KEY_DEMO_DATA = booleanPreferencesKey("is_demo_data_enabled")
    private val KEY_MONTHLY_LIMIT = intPreferencesKey("monthly_quota_minutes")

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            billingDayOfMonth = prefs[KEY_BILLING_DAY] ?: 1,
            billingTimezoneId = prefs[KEY_TIMEZONE] ?: "America/New_York",
            selectedSimSubscriptionId = prefs[KEY_SELECTED_SIM] ?: UserSettings.SIM_AUTO_VISIBLE,
            showPersistentNotification = prefs[KEY_PERSISTENT_NOTIF] ?: true,
            isDemoDataEnabled = prefs[KEY_DEMO_DATA] ?: false,
            monthlyQuotaMinutes = prefs[KEY_MONTHLY_LIMIT] ?: 500
        )
    }

    suspend fun setBillingDayOfMonth(day: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BILLING_DAY] = day.coerceIn(1, 31)
        }
    }

    suspend fun setBillingTimezone(timezoneId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TIMEZONE] = timezoneId
        }
    }

    suspend fun setSelectedSimSubscriptionId(subId: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_SIM] = subId
        }
    }

    suspend fun setPersistentNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSISTENT_NOTIF] = enabled
        }
    }

    suspend fun setDemoDataEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEMO_DATA] = enabled
        }
    }
}

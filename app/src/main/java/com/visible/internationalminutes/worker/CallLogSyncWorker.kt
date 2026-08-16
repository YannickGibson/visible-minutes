package com.visible.internationalminutes.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.visible.internationalminutes.data.CallLogRepository
import com.visible.internationalminutes.data.UserSettingsRepository
import com.visible.internationalminutes.model.BillingCycle
import com.visible.internationalminutes.notification.QuotaNotificationManager
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Background worker to periodically sync call logs and refresh the ongoing quota notification.
 */
class CallLogSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userSettingsRepo = UserSettingsRepository(context)
        val callLogRepo = CallLogRepository(context)

        val settings = userSettingsRepo.settingsFlow.first()
        val zoneId = try {
            ZoneId.of(settings.billingTimezoneId)
        } catch (e: Exception) {
            BillingCycle.DEFAULT_ZONE
        }

        val cycle = BillingCycle.calculate(settings.billingDayOfMonth, zoneId)
        val availableSims = callLogRepo.getAvailableSims()
        val targetSim = callLogRepo.resolveTargetSimId(settings.selectedSimSubscriptionId, availableSims)

        val calls = callLogRepo.getCallsForBillingCycle(cycle, targetSim, settings.isDemoDataEnabled)
        val summary = callLogRepo.computeQuotaSummary(calls, cycle, settings.monthlyQuotaMinutes)

        QuotaNotificationManager.updatePersistentNotification(
            context,
            summary,
            settings.showPersistentNotification
        )

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "visible_call_log_sync_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<CallLogSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}

package com.visible.internationalminutes

import android.app.Application
import com.visible.internationalminutes.notification.QuotaNotificationManager
import com.visible.internationalminutes.worker.CallLogSyncWorker

class VisibleMinutesApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        QuotaNotificationManager.createNotificationChannel(this)

        // Schedule periodic background sync
        CallLogSyncWorker.schedule(this)
    }
}

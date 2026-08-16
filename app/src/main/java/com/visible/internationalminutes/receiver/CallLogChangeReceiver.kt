package com.visible.internationalminutes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.visible.internationalminutes.worker.CallLogSyncWorker

/**
 * BroadcastReceiver triggered after device boot or package update to schedule sync worker.
 */
class CallLogChangeReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        CallLogSyncWorker.schedule(context)
    }
}

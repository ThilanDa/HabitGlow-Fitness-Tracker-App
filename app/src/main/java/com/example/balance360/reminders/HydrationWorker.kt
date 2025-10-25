package com.example.balance360.reminders

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.balance360.util.NotificationHelper

class HydrationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        NotificationHelper.showHydrationReminder(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "hydration_reminders"
    }
}

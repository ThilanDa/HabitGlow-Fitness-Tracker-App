package com.example.balance360.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.balance360.data.Prefs
import java.util.Calendar

class HydrationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val s = prefs.getSettings()

        val now = Calendar.getInstance()
        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMins = s.startHour * 60 + s.startMinute
        val endMins = s.endHour * 60 + s.endMinute

        val withinWindow = if (endMins >= startMins) {
            // Same-day window
            nowMins in startMins..endMins
        } else {
            // Overnight window (e.g., 22:00 to 06:00)
            nowMins >= startMins || nowMins <= endMins
        }

        if (withinWindow) {
            NotificationHelper.showHydrationNotification(applicationContext)
        }
        return Result.success()
    }
}

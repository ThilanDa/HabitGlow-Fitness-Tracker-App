package com.example.balance360.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.balance360.R

object NotificationHelper {
    const val CHANNEL_ID = "hydration_reminders"
    private const val CHANNEL_NAME = "Hydration Reminders"
    private const val CHANNEL_DESC = "Drink water reminder notifications"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = CHANNEL_DESC
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun showHydrationNotification(context: Context, title: String = "Time to drink water", text: String = "Stay hydrated and healthy") {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(1001, builder.build())
        // Log the notification for in-app viewing
        try {
            com.example.balance360.data.Prefs(context).addNotification(
                com.example.balance360.data.Prefs.AppNotification(title = title, text = text, ts = System.currentTimeMillis())
            )
        } catch (_: Exception) { /* ignore */ }
    }
}

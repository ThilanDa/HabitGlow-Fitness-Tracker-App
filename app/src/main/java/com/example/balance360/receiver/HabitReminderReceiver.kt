package com.example.balance360.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.balance360.R
import com.example.balance360.data.Prefs
import java.util.Calendar

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: context.getString(R.string.app_name)

        // Check schedule: if habit has specific days and today isn't one of them, skip
        if (habitId != null) {
            val prefs = Prefs(context)
            val h = prefs.getHabits().firstOrNull { it.id == habitId }
            val scheduled = h?.daysOfWeek
            if (!scheduled.isNullOrEmpty()) {
                val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                if (!scheduled.contains(todayDow)) return
            }
        }

        ensureChannel(context)

        val pendingIntent = runCatching {
            NavDeepLinkBuilder(context)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.notificationsFragment)
                .createPendingIntent()
        }.getOrNull()

        val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text, habitName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (pendingIntent != null) {
            notifBuilder.setContentIntent(pendingIntent)
        }
        val notif = notifBuilder.build()

        with(NotificationManagerCompat.from(context)) {
            notify(habitName.hashCode(), notif)
        }

        // Log to in-app notifications
        runCatching {
            val title = context.getString(R.string.reminder_title)
            val text = context.getString(R.string.reminder_text, habitName)
            Prefs(context).addNotification(Prefs.AppNotification(title = title, text = text, ts = System.currentTimeMillis()))
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_habits_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.channel_habits_desc) }
                nm.createNotificationChannel(ch)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "habits_reminders"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
    }
}

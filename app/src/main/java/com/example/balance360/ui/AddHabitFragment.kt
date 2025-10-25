package com.example.balance360.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.balance360.R
import com.example.balance360.data.Habit
import com.example.balance360.data.Prefs
import com.google.android.material.tabs.TabLayout
import com.google.android.material.button.MaterialButton
import java.util.UUID
import android.app.TimePickerDialog
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.balance360.receiver.HabitReminderReceiver
import android.widget.LinearLayout
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class AddHabitFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_habit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etName = view.findViewById<EditText>(R.id.etHabitName)
        val etDesc = view.findViewById<EditText>(R.id.etHabitDesc)

        // Icon selection
        val iconViews = listOf(
            view.findViewById<TextView>(R.id.iconWater),
            view.findViewById<TextView>(R.id.iconRun),
            view.findViewById<TextView>(R.id.iconBooks),
            view.findViewById<TextView>(R.id.iconMeditate),
            view.findViewById<TextView>(R.id.iconSalad),
            view.findViewById<TextView>(R.id.iconSleep),
            view.findViewById<TextView>(R.id.iconMuscle),
            view.findViewById<TextView>(R.id.iconTarget),
        )
        var selectedIcon: String? = null

        fun selectIcon(v: TextView) {
            iconViews.forEach { it.isSelected = false }
            v.isSelected = true
            selectedIcon = v.tag as? String
        }
        // Preselect last icon (target) visually
        selectIcon(iconViews.last())

        iconViews.forEach { tv ->
            tv.setOnClickListener { selectIcon(tv) }
        }

        // Timer tab + Set Reminder button
        val tabTimer = view.findViewById<TabLayout>(R.id.tabTimer)
        val btnSetReminder = view.findViewById<MaterialButton>(R.id.btnSetReminder)
        val llTimerDuration = view.findViewById<LinearLayout>(R.id.llTimerDuration)
        val etTimerMinutes = view.findViewById<EditText>(R.id.etTimerMinutes)
        val tvReminderList = view.findViewById<TextView>(R.id.tvReminderList)
        val weekdayChecks = listOf(
            view.findViewById<android.widget.CheckBox>(R.id.cbSun),
            view.findViewById<android.widget.CheckBox>(R.id.cbMon),
            view.findViewById<android.widget.CheckBox>(R.id.cbTue),
            view.findViewById<android.widget.CheckBox>(R.id.cbWed),
            view.findViewById<android.widget.CheckBox>(R.id.cbThu),
            view.findViewById<android.widget.CheckBox>(R.id.cbFri),
            view.findViewById<android.widget.CheckBox>(R.id.cbSat)
        )
        var reminderHour: Int? = null
        var reminderMinute: Int? = null
        val reminderTimes = mutableListOf<String>() // formatted HH:mm

        fun updateReminderButton() {
            val isTimer = tabTimer.selectedTabPosition == 1
            btnSetReminder.isEnabled = isTimer
            llTimerDuration.visibility = if (isTimer) View.VISIBLE else View.GONE
            if (!isTimer) {
                btnSetReminder.text = getString(R.string.set_reminder)
                reminderTimes.clear()
                tvReminderList.text = ""
            } else if (reminderHour != null && reminderMinute != null) {
                val hh = reminderHour!!
                val mm = reminderMinute!!
                val label = String.format("%02d:%02d", hh, mm)
                btnSetReminder.text = getString(R.string.set_reminder_time, label)
            }
        }

        // Initialize Tab selection to None (index 0)
        tabTimer.getTabAt(0)?.select()
        updateReminderButton()

        tabTimer.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { updateReminderButton() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) { updateReminderButton() }
        })

        btnSetReminder.setOnClickListener {
            val now = Calendar.getInstance()
            val hour = reminderHour ?: now.get(Calendar.HOUR_OF_DAY)
            val minute = reminderMinute ?: now.get(Calendar.MINUTE)
            TimePickerDialog(requireContext(), { _, h, m ->
                reminderHour = h
                reminderMinute = m
                updateReminderButton()
                val label = String.format("%02d:%02d", h, m)
                if (!reminderTimes.contains(label)) {
                    if (reminderTimes.size < 3) {
                        reminderTimes.add(label)
                    } else {
                        // replace the last one (simple UX)
                        reminderTimes[reminderTimes.lastIndex] = label
                    }
                }
                tvReminderList.text = if (reminderTimes.isEmpty()) "" else reminderTimes.joinToString(
                    prefix = getString(R.string.reminders_prefix, ""), separator = "  •  "
                )
            }, hour, minute, true).show()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            findNavController().popBackStack()
        }
        view.findViewById<View>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val prefs = Prefs(requireContext())
            val list = prefs.getHabits().toMutableList()
            val isTimer = tabTimer.selectedTabPosition == 1
            val timerMinutes = etTimerMinutes.text?.toString()?.toIntOrNull()
            val selectedDays = weekdayChecks.mapNotNull { v -> if (v.isChecked) v.tag.toString().toIntOrNull() else null }
            val habit = Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                description = etDesc.text?.toString()?.takeIf { it.isNotBlank() },
                icon = selectedIcon,
                reminderHour = if (isTimer) reminderHour else null,
                reminderMinute = if (isTimer) reminderMinute else null,
                reminderTimes = if (isTimer && reminderTimes.isNotEmpty()) reminderTimes.toList() else null,
                timerMinutes = if (isTimer) timerMinutes else null,
                daysOfWeek = selectedDays.ifEmpty { null }
            )
            // If you later extend Habit with description/icon/color, set them here using etDesc and selections
            list.add(0, habit)
            prefs.setHabits(list)

            // Schedule notification if a reminder time was set
            if (isTimer) {
                val times = (habit.reminderTimes ?: emptyList()).ifEmpty {
                    val hh = habit.reminderHour
                    val mm = habit.reminderMinute
                    if (hh != null && mm != null) listOf(String.format("%02d:%02d", hh, mm)) else emptyList()
                }
                if (times.isNotEmpty()) {
                    times.forEach { t ->
                        val parts = t.split(":")
                        if (parts.size == 2) scheduleHabitReminder(requireContext(), habit.id, habit.name, parts[0].toInt(), parts[1].toInt())
                    }
                    // Android 13+ needs POST_NOTIFICATIONS runtime permission
                    val canNotify = if (Build.VERSION.SDK_INT >= 33) {
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true
                    if (!canNotify && Build.VERSION.SDK_INT >= 33) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2002)
                        Toast.makeText(requireContext(), "Please allow notification permission to receive habit reminders", Toast.LENGTH_LONG).show()
                    } else {
                        triggerImmediateReminder(requireContext(), habit.id, habit.name)
                    }
                }
            }
            findNavController().popBackStack()
        }
    }

    private fun scheduleHabitReminder(context: Context, habitId: String, habitName: String, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habitName)
        }
        // Use unique request codes per habit/time slot
        val baseCode = (habitId + String.format("%02d%02d", hour, minute)).hashCode()
        val piRepeating = PendingIntent.getBroadcast(
            context,
            baseCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 1) Fire an exact alarm for the very next occurrence to ensure it triggers even in Doze
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                val piExact = PendingIntent.getBroadcast(
                    context,
                    baseCode + 1, // distinct from repeating
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, piExact)
            }
        }

        // 2) Also set a daily repeating alarm as a fallback for subsequent days
        am.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            piRepeating
        )
    }

    private fun triggerImmediateReminder(context: Context, habitId: String, habitName: String) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habitName)
        }
        context.sendBroadcast(intent)
    }
}


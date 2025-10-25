package com.example.balance360.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.balance360.R
import com.example.balance360.data.Habit
import com.example.balance360.data.Prefs
import com.example.balance360.receiver.HabitReminderReceiver
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

class EditHabitFragment : Fragment() {
    private var habit: Habit? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_habit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val habitId = arguments?.getString("habitId")
        val prefs = Prefs(requireContext())
        habit = prefs.getHabits().firstOrNull { it.id == habitId }
        if (habit == null) {
            Toast.makeText(requireContext(), "Habit not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        view.findViewById<TextView>(R.id.tvTitle)?.text = getString(R.string.edit_habit)

        val etName = view.findViewById<EditText>(R.id.etHabitName)
        val etDesc = view.findViewById<EditText>(R.id.etHabitDesc)
        etName.setText(habit!!.name)
        etDesc.setText(habit!!.description ?: "")

        // Icon selection same as Add
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
        var selectedIcon: String? = habit!!.icon
        fun selectIcon(v: TextView) {
            iconViews.forEach { it.isSelected = false }
            v.isSelected = true
            selectedIcon = v.tag as? String
        }
        val pre = iconViews.firstOrNull { it.tag == selectedIcon } ?: iconViews.last()
        selectIcon(pre)
        iconViews.forEach { tv -> tv.setOnClickListener { selectIcon(tv) } }

        // Timer
        val tabTimer = view.findViewById<TabLayout>(R.id.tabTimer)
        val btnSetReminder = view.findViewById<MaterialButton>(R.id.btnSetReminder)
        var reminderHour: Int? = habit!!.reminderHour
        var reminderMinute: Int? = habit!!.reminderMinute

        fun updateReminderButton() {
            val isTimer = tabTimer.selectedTabPosition == 1
            btnSetReminder.isEnabled = isTimer
            if (!isTimer) {
                btnSetReminder.text = getString(R.string.set_reminder)
            } else if (reminderHour != null && reminderMinute != null) {
                val label = String.format("%02d:%02d", reminderHour!!, reminderMinute!!)
                btnSetReminder.text = getString(R.string.set_reminder_time, label)
            }
        }
        // Set initial tab
        if (reminderHour != null && reminderMinute != null) tabTimer.getTabAt(1)?.select() else tabTimer.getTabAt(0)?.select()
        updateReminderButton()

        tabTimer.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { updateReminderButton() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) { updateReminderButton() }
        })

        btnSetReminder.setOnClickListener {
            val now = Calendar.getInstance()
            val hour = reminderHour ?: now.get(Calendar.HOUR_OF_DAY)
            val minute = reminderMinute ?: now.get(Calendar.MINUTE)
            TimePickerDialog(requireContext(), { _, h, m ->
                reminderHour = h
                reminderMinute = m
                updateReminderButton()
            }, hour, minute, true).show()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val list = prefs.getHabits().toMutableList()
            val idx = list.indexOfFirst { it.id == habit!!.id }
            if (idx >= 0) {
                val isTimer = tabTimer.selectedTabPosition == 1
                val updated = habit!!.copy(
                    name = name,
                    description = etDesc.text?.toString()?.takeIf { it.isNotBlank() },
                    icon = selectedIcon,
                    reminderHour = if (isTimer) reminderHour else null,
                    reminderMinute = if (isTimer) reminderMinute else null
                )
                list[idx] = updated
                prefs.setHabits(list)
                scheduleHabitReminder(requireContext(), updated.name, updated.reminderHour ?: return@setOnClickListener, updated.reminderMinute ?: return@setOnClickListener)
            }
            findNavController().popBackStack()
        }
    }

    private fun scheduleHabitReminder(context: Context, habitName: String, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habitName)
        }
        val requestCode = habitName.hashCode()
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }
}

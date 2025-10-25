package com.example.balance360.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import android.app.TimePickerDialog
import android.widget.ProgressBar
import android.text.InputFilter
import android.text.Spanned
import android.widget.Toast
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.fragment.app.Fragment
import com.example.balance360.R
import com.example.balance360.data.Prefs
import com.example.balance360.notifications.HydrationReminderWorker
import com.example.balance360.notifications.NotificationHelper
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydrationFragment : Fragment() {
    // Activity Result launcher for notifications permission (Android 13+)
    private val notifPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* granted: Boolean -> we don't need to handle here; action continues */ }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_hydration, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = Prefs(requireContext())
        val settings = prefs.getSettings()

        val tvTodayIntake = view.findViewById<TextView>(R.id.tvTodayIntake)
        val tvTodayPct = view.findViewById<TextView>(R.id.tvTodayPct)
        val tvRemainingMl = view.findViewById<TextView>(R.id.tvRemainingMl)
        val gauge = view.findViewById<ProgressBar>(R.id.gaugeProgress)
        val tvGaugePct = view.findViewById<TextView>(R.id.tvGaugePct)
        val tvGaugeLabel = view.findViewById<TextView>(R.id.tvGaugeLabel)
        val tvGaugeSub = view.findViewById<TextView>(R.id.tvGaugeSub)

        val tvAddAmount = view.findViewById<TextView>(R.id.tvAddAmount)
        val btnMinus = view.findViewById<View>(R.id.btnMinus)
        val btnPlus = view.findViewById<View>(R.id.btnPlus)
        val btnQuick250 = view.findViewById<View>(R.id.btnQuick250)
        val btnQuick500 = view.findViewById<View>(R.id.btnQuick500)
        val btnQuick750 = view.findViewById<View>(R.id.btnQuick750)
        val chipInterval = view.findViewById<ChipGroup>(R.id.chipInterval)
        val btnAddAmount = view.findViewById<View>(R.id.btnAddAmount)
        val btnRemoveAmount = view.findViewById<View>(R.id.btnQuickMinus300)
        val etStartTime = view.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<EditText>(R.id.etEndTime)
        val etDailyGoal = view.findViewById<EditText>(R.id.etDailyGoal)
        
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = dayFmt.format(Date())

        fun updateUI() {
            val intakeMl = prefs.getWaterMl(todayKey).coerceIn(0, 2000)
            val sNow = prefs.getSettings()
            val goal = sNow.dailyWaterGoalMl.coerceAtLeast(1)
            val pct = ((intakeMl * 100) / goal).coerceAtMost(100)
            val remaining = (goal - intakeMl).coerceAtLeast(0)

            tvTodayIntake.text = "${intakeMl}ml"
            tvTodayPct.text = "${pct}% of goal"
            tvRemainingMl.text = "${remaining}ml"
            // Removed progressToday/tvTodayGoal (not in current layout)

            // Update vertical gauge if present
            gauge?.progress = pct
            tvGaugePct?.text = "$pct%"
            tvGaugeLabel?.text = "${intakeMl}ml / ${goal}ml"
            tvGaugeSub?.text = if (remaining > 0) "${remaining}ml to go!" else "Goal reached!"

            // Tank views removed from layout; nothing else to update here
        }

        fun addWater(ml: Int) {
            val current = prefs.getWaterMl(todayKey)
            val next = (current + ml).coerceIn(0, 2000)
            prefs.setWaterMl(todayKey, next)
            updateUI()
        }

        fun fmt(h: Int, m: Int): String = String.format(Locale.getDefault(), "%02d:%02d", h, m)

        // Helper to update the adjustable amount and button label
        fun setAmount(newVal: Int) {
            val clamped = newVal.coerceIn(50, 1000) // bounds for single add step
            tvAddAmount.text = clamped.toString()
            (btnAddAmount as? android.widget.Button)?.text = "Add ${clamped}ml"
            (btnRemoveAmount as? android.widget.Button)?.text = "Remove ${clamped}ml"
        }

        // Initialize times from settings
        etStartTime?.setText(fmt(settings.startHour, settings.startMinute))
        etEndTime?.setText(fmt(settings.endHour, settings.endMinute))

        // Initialize and limit Daily Goal (ml) to 1..2000
        etDailyGoal?.setText(settings.dailyWaterGoalMl.toString())
        val maxGoal = 2000
        etDailyGoal?.filters = arrayOf(InputFilter { source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int ->
            val newVal = StringBuilder(dest).replace(dstart, dend, source.substring(start, end)).toString()
            if (newVal.isEmpty()) return@InputFilter null
            val num = newVal.toIntOrNull() ?: return@InputFilter ""
            if (num in 1..maxGoal) null else ""
        })

        etStartTime?.setOnClickListener {
            val s = prefs.getSettings()
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                etStartTime.setText(fmt(hourOfDay, minute))
                val copy = prefs.getSettings()
                copy.startHour = hourOfDay
                copy.startMinute = minute
                prefs.setSettings(copy)
            }, s.startHour, s.startMinute, true).show()
        }

        etEndTime?.setOnClickListener {
            val s = prefs.getSettings()
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                etEndTime.setText(fmt(hourOfDay, minute))
                val copy = prefs.getSettings()
                copy.endHour = hourOfDay
                copy.endMinute = minute
                prefs.setSettings(copy)
            }, s.endHour, s.endMinute, true).show()
        }

        // Initialize amount label (default 350ml)
        setAmount(tvAddAmount.text.toString().toIntOrNull() ?: 350)

        // Minus/Plus now adjust the number shown between the buttons
        btnMinus?.setOnClickListener {
            val cur = tvAddAmount.text.toString().toIntOrNull() ?: 350
            setAmount(cur - 50)
        }
        btnPlus?.setOnClickListener {
            val cur = tvAddAmount.text.toString().toIntOrNull() ?: 350
            setAmount(cur + 50)
        }

        (btnAddAmount as? android.widget.Button)?.setOnClickListener {
            val amt = tvAddAmount.text.toString().toIntOrNull() ?: 350
            addWater(amt)
        }
        (btnRemoveAmount as? android.widget.Button)?.setOnClickListener {
            val amt = tvAddAmount.text.toString().toIntOrNull() ?: 350
            addWater(-amt)
        }
        btnQuick250?.setOnClickListener { addWater(250) }
        btnQuick500?.setOnClickListener { addWater(500) }
        btnQuick750?.setOnClickListener { addWater(750) }
        // Removed fixed 300ml buttons; dynamic remove handled above


        // Preselect interval based on saved settings (ChipGroup)
        val selectedChipId = when (settings.hydrationIntervalMinutes) {
            30 -> R.id.chipInt30
            45 -> R.id.chipInt45
            60 -> R.id.chipInt60
            90 -> R.id.chipInt90
            120 -> R.id.chipInt120
            else -> R.id.chipInt60
        }
        chipInterval?.check(selectedChipId)

        // Schedule periodic notifications using WorkManager
        view.findViewById<View>(R.id.btnSetReminder)?.setOnClickListener {
            // Request POST_NOTIFICATIONS permission on Android 13+ using Activity Result API
            if (Build.VERSION.SDK_INT >= 33) {
                val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val checkedId = chipInterval?.checkedChipId ?: R.id.chipInt60
            val minutes = when (checkedId) {
                R.id.chipInt30 -> 30
                R.id.chipInt45 -> 45
                R.id.chipInt60 -> 60
                R.id.chipInt90 -> 90
                R.id.chipInt120 -> 120
                else -> 60
            }

            // Persist chosen interval and goal (previously done by Save Reminder button)
            run {
                val s = prefs.getSettings()
                s.hydrationIntervalMinutes = minutes
                val goal = (etDailyGoal?.text?.toString()?.toIntOrNull() ?: s.dailyWaterGoalMl).coerceIn(1, maxGoal)
                s.dailyWaterGoalMl = goal
                prefs.setSettings(s)
                updateUI()
            }

            val work = PeriodicWorkRequestBuilder<HydrationReminderWorker>(minutes.toLong(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork("hydration_reminder", ExistingPeriodicWorkPolicy.UPDATE, work)

            // Immediate feedback notification
            NotificationHelper.showHydrationNotification(requireContext(), title = getString(R.string.app_name), text = "Hydration reminder scheduled")
            Toast.makeText(requireContext(), "Settings saved. Reminder scheduled (every $minutes min)", Toast.LENGTH_SHORT).show()
        }

        updateUI()
    }
}

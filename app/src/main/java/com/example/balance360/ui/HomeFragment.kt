package com.example.balance360.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.balance360.R
import android.widget.TextView
import com.example.balance360.data.Prefs
import com.example.balance360.util.DateUtils
import android.util.Log
import android.widget.Toast
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.components.XAxis
import com.example.balance360.data.MoodEntry
import androidx.appcompat.app.AlertDialog

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val prefs = Prefs(requireContext())

            // Header greeting and date
            val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
            val tvDate = view.findViewById<TextView>(R.id.tvDate)
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 5..11 -> "Good Morning!"
                in 12..16 -> "Good Afternoon!"
                else -> "Good Evening!"
            }
            tvGreeting.text = greeting
            val df = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            tvDate.text = df.format(Date())

            // Today's Mood card
            fun updateTodayMood() {
                val card = view.findViewById<View>(R.id.cardTodayMood)
                val tvEmoji = view.findViewById<TextView>(R.id.tvTodayMoodEmoji)
                val tvLabel = view.findViewById<TextView>(R.id.tvTodayMoodLabel)
                val tvPct = view.findViewById<TextView>(R.id.tvTodayMoodPct)
                if (card == null || tvEmoji == null || tvLabel == null || tvPct == null) return
                val startOfDay = java.util.Calendar.getInstance().apply {
                    time = Date(); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endOfDay = startOfDay + 24L*60*60*1000 - 1
                val todayMoods = prefs.getMoods().filter { it.timestamp in startOfDay..endOfDay }
                if (todayMoods.isEmpty()) {
                    card.visibility = View.GONE
                    return
                }
                val counts = todayMoods.groupingBy { it.emoji }.eachCount()
                val topEntry = counts.maxByOrNull { it.value }
                val topEmoji = if (topEntry != null) topEntry.key else "🙂"
                val topCount = if (topEntry != null) topEntry.value else 0
                val pct = (topCount * 100 / todayMoods.size).coerceIn(0, 100)
                tvEmoji?.text = topEmoji
                tvLabel?.text = "OTHER"
                tvPct?.text = "$pct%"
                card.visibility = View.VISIBLE
            }

            // Render today's mood card now
            updateTodayMood()

            // Settings button in header navigates to Settings
            view.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
                findNavController().navigate(R.id.settingsFragment)
            }

            // Habits progress
            val tvHabitsCount = view.findViewById<TextView>(R.id.tvHabitsCount)
            val pbHabits = view.findViewById<ProgressBar>(R.id.pbHabits)
            val tvHabitsPct = view.findViewById<TextView>(R.id.tvHabitsPct)
            val habits = prefs.getHabits()
            val today = DateUtils.todayKey()
            val completed = prefs.getCompletions(today)
            val done = completed.size
            val total = habits.size
            val pct = if (total == 0) 0 else (done * 100 / total)
            tvHabitsCount.text = "$done/$total"
            pbHabits.progress = pct
            tvHabitsPct.text = "$pct% Complete"

            fun updateHydrationCard() {
                val tvHydrationCount = view.findViewById<TextView>(R.id.tvHydrationCount)
                val pbHydration = view.findViewById<ProgressBar>(R.id.pbHydration)
                val tvHydrationPct = view.findViewById<TextView>(R.id.tvHydrationPct)
                val settings = prefs.getSettings()
                val todayKey = DateUtils.todayKey()
                val intakeMl = prefs.getWaterMl(todayKey).coerceIn(0, 2000)
                val goalMl = settings.dailyWaterGoalMl.coerceAtLeast(1)
                val pctHyd = ((intakeMl * 100) / goalMl).coerceAtMost(100)
                tvHydrationCount.text = "${intakeMl}ml/${goalMl}ml"
                pbHydration.progress = pctHyd
                tvHydrationPct.text = "$pctHyd% of goal"
            }
            updateHydrationCard()

            // Charts (last 7 days for pie)
            fun setupMoodPie() {
                val pie = view.findViewById<PieChart>(R.id.pieMood) ?: return
                pie.setNoDataText("No chart data available.")
                // Use last 7 days (including today)
                val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val cal = Calendar.getInstance().apply { time = Date(); add(Calendar.DAY_OF_YEAR, -6) }
                val keys = mutableSetOf<String>()
                for (i in 0..6) { keys.add(dayFmt.format(cal.time)); cal.add(Calendar.DAY_OF_YEAR, 1) }
                val moods = prefs.getMoods().filter { keys.contains(dayFmt.format(Date(it.timestamp))) }
                val counts = mutableMapOf<String, Int>()
                moods.forEach { me -> counts[me.emoji] = (counts[me.emoji] ?: 0) + 1 }
                if (counts.isEmpty()) {
                    pie.data = null
                    pie.invalidate()
                    return
                }
                val entries = counts.entries
                    .sortedByDescending { it.value }
                    .map { (emoji, c) -> PieEntry(c.toFloat(), emoji) }
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = listOf(
                    0xFF42A5F5.toInt(), 0xFF66BB6A.toInt(), 0xFFFFA726.toInt(), 0xFFAB47BC.toInt(), 0xFFEF5350.toInt()
                )
                dataSet.valueTextSize = 10f
                val data = PieData(dataSet).apply { setValueTextColor(android.graphics.Color.WHITE) }
                pie.data = data
                pie.description.isEnabled = false
                pie.legend.isEnabled = false
                pie.setUsePercentValues(false)
                pie.setEntryLabelColor(android.graphics.Color.BLACK)
                pie.setHoleColor(android.graphics.Color.TRANSPARENT)
                pie.notifyDataSetChanged()
                pie.invalidate()
            }

            fun setupWaterBar() {
                val bar = view.findViewById<BarChart>(R.id.barWater) ?: return
                bar.setNoDataText("No chart data available.")
                val cal = Calendar.getInstance()
                cal.time = Date()
                val labels = mutableListOf<String>()
                val entries = mutableListOf<BarEntry>()
                val goal = prefs.getSettings().dailyWaterGoalMl.coerceAtLeast(1)
                // last 7 days, oldest to newest
                cal.add(Calendar.DAY_OF_YEAR, -6)
                for (i in 0..6) {
                    val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                    val ml = prefs.getWaterMl(key).coerceAtLeast(0)
                    entries.add(BarEntry(i.toFloat(), ml.toFloat()))
                    labels.add(SimpleDateFormat("MM/dd", Locale.getDefault()).format(cal.time))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                val dataSet = BarDataSet(entries, "ml")
                dataSet.color = 0xFF42A5F5.toInt()
                val data = BarData(dataSet)
                data.barWidth = 0.6f
                bar.data = data
                bar.description.isEnabled = false
                bar.legend.isEnabled = false
                bar.axisRight.isEnabled = false
                bar.axisLeft.axisMinimum = 0f
                bar.axisLeft.axisMaximum = goal.toFloat()
                bar.xAxis.position = XAxis.XAxisPosition.BOTTOM
                bar.xAxis.setDrawGridLines(false)
                bar.xAxis.granularity = 1f
                bar.xAxis.labelCount = labels.size
                bar.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                bar.notifyDataSetChanged()
                bar.invalidate()
            }

            setupMoodPie()
            setupWaterBar()

            // Quick actions (only Tips retained)
            view.findViewById<View>(R.id.qaViewTrends).setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_tips, null, false)
                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                // Wire tabs
                val tabs = dialogView.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
                val tabTips = dialogView.findViewById<View>(R.id.tabTips)
                val tabNotifications = dialogView.findViewById<View>(R.id.tabNotifications)
                fun selectTab(idx: Int) {
                    if (idx == 0) { tabTips.visibility = View.VISIBLE; tabNotifications.visibility = View.GONE }
                    else { tabTips.visibility = View.GONE; tabNotifications.visibility = View.VISIBLE }
                }
                selectTab(0)
                tabs?.addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) { selectTab(tab.position) }
                    override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                    override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                })

                // Populate notifications list
                runCatching {
                    val list = dialogView.findViewById<android.widget.LinearLayout>(R.id.listNotifications)
                    val empty = dialogView.findViewById<android.widget.TextView>(R.id.emptyNotifications)
                    val items = Prefs(requireContext()).getNotifications()
                    if (items.isEmpty()) { empty?.visibility = View.VISIBLE } else { empty?.visibility = View.GONE }
                    items.forEach { n ->
                        val row = android.widget.LinearLayout(requireContext()).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setPadding(0, 8, 0, 8)
                        }
                        val title = android.widget.TextView(requireContext()).apply {
                            text = n.title
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }
                        val body = android.widget.TextView(requireContext()).apply {
                            text = n.text
                        }
                        val time = android.widget.TextView(requireContext()).apply {
                            text = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(n.ts))
                            setTextColor(0xFF7A7A7A.toInt())
                            textSize = 12f
                        }
                        row.addView(title)
                        row.addView(body)
                        row.addView(time)
                        list?.addView(row)
                    }
                }

                dialogView.findViewById<View>(R.id.btnOk)?.setOnClickListener { dialog.dismiss() }
                dialog.show()
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to load home overview", e)
            Toast.makeText(requireContext(), "Failed to load home: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { v ->
            val prefs = Prefs(requireContext())
            val tvHydrationCount = v.findViewById<TextView>(R.id.tvHydrationCount)
            val pbHydration = v.findViewById<ProgressBar>(R.id.pbHydration)
            val tvHydrationPct = v.findViewById<TextView>(R.id.tvHydrationPct)
            if (tvHydrationCount != null && pbHydration != null && tvHydrationPct != null) {
                val settings = prefs.getSettings()
                val todayKey = DateUtils.todayKey()
                val intakeMl = prefs.getWaterMl(todayKey).coerceIn(0, 2000)
                val goalMl = settings.dailyWaterGoalMl.coerceAtLeast(1)
                val pctHyd = ((intakeMl * 100) / goalMl).coerceAtMost(100)
                tvHydrationCount.text = "${intakeMl}ml/${goalMl}ml"
                pbHydration.progress = pctHyd
                tvHydrationPct.text = "$pctHyd% of goal"
            }
            // Refresh charts when returning to home (last 7 days for pie)
            runCatching {
                val pie = v.findViewById<PieChart>(R.id.pieMood)
                val bar = v.findViewById<BarChart>(R.id.barWater)
                if (pie != null || bar != null) {
                    // Reuse local functions by reconstructing minimal logic
                    // Pie (last 7 days real data)
                    pie?.let { p ->
                        p.setNoDataText("No chart data available.")
                        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance().apply { time = Date(); add(Calendar.DAY_OF_YEAR, -6) }
                        val keys = mutableSetOf<String>()
                        for (i in 0..6) { keys.add(dayFmt.format(cal.time)); cal.add(Calendar.DAY_OF_YEAR, 1) }
                        val counts = mutableMapOf<String, Int>()
                        Prefs(requireContext()).getMoods()
                            .filter { keys.contains(dayFmt.format(Date(it.timestamp))) }
                            .forEach { me -> counts[me.emoji] = (counts[me.emoji] ?: 0) + 1 }
                        if (counts.isEmpty()) { p.data = null; p.invalidate() } else {
                            val entries = counts.entries.sortedByDescending { it.value }.map { PieEntry(it.value.toFloat(), it.key) }
                            val ds = PieDataSet(entries, "").apply { colors = listOf(0xFF42A5F5.toInt(), 0xFF66BB6A.toInt(), 0xFFFFA726.toInt(), 0xFFAB47BC.toInt(), 0xFFEF5350.toInt()); valueTextColor = android.graphics.Color.WHITE }
                            p.data = PieData(ds)
                            p.description.isEnabled = false; p.legend.isEnabled = false
                            p.notifyDataSetChanged(); p.invalidate()
                        }
                    }
                    // Bar
                    bar?.let { b ->
                        b.setNoDataText("No chart data available.")
                        val cal = Calendar.getInstance(); cal.time = Date(); cal.add(Calendar.DAY_OF_YEAR, -6)
                        val labels = mutableListOf<String>(); val entries = mutableListOf<BarEntry>()
                        for (i in 0..6) {
                            val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                            val ml = Prefs(requireContext()).getWaterMl(key).coerceIn(0, 2000)
                            entries.add(BarEntry(i.toFloat(), ml.toFloat()))
                            labels.add(SimpleDateFormat("MM/dd", Locale.getDefault()).format(cal.time))
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        val ds = BarDataSet(entries, "ml"); ds.color = 0xFF42A5F5.toInt(); val d = BarData(ds); d.barWidth = 0.6f
                        b.data = d; b.description.isEnabled = false; b.legend.isEnabled = false; b.axisRight.isEnabled = false; b.axisLeft.axisMinimum = 0f; b.axisLeft.axisMaximum = 2000f
                        b.xAxis.position = XAxis.XAxisPosition.BOTTOM; b.xAxis.setDrawGridLines(false); b.xAxis.granularity = 1f; b.xAxis.labelCount = labels.size
                        b.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                        b.notifyDataSetChanged(); b.invalidate()
                    }
                }
            }
        }
    }
}

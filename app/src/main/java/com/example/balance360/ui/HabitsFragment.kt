package com.example.balance360.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.balance360.R
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AlertDialog
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balance360.data.Habit
import com.example.balance360.data.Prefs
import com.example.balance360.ui.adapters.HabitsAdapter
import com.example.balance360.util.DateUtils
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf

class HabitsFragment : Fragment() {
    private var adapter: HabitsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnAdd = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddHabit)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val rv = view.findViewById<RecyclerView>(R.id.rvHabits)
        val tvPercent = view.findViewById<TextView>(R.id.tvTodayProgressPercent)
        val tvGoalCount = view.findViewById<TextView>(R.id.tvGoalCount)
        val pbGoal = view.findViewById<android.widget.ProgressBar>(R.id.pbGoal)

        val prefs = Prefs(requireContext())
        val habits = prefs.getHabits()
        val today = DateUtils.todayKey()
        var completed = prefs.getCompletions(today)

        fun refreshEmpty() {
            val count = adapter?.itemCount ?: 0
            emptyState.visibility = if (count == 0) View.VISIBLE else View.GONE
        }

        fun refreshProgress() {
            val completedToday = prefs.getCompletions(today)
            val visible = adapter?.currentItems().orEmpty()
            val eligible = visible.size
            val done = visible.count { h -> completedToday.contains(h.id) }
            val percent = if (eligible == 0) 0 else (done * 100 / eligible)
            tvPercent.text = "$percent%"
            tvGoalCount.text = "$done/$eligible"
            pbGoal.progress = percent
        }

        fun streakFor(h: Habit): Int {
            var days = 0
            var idx = 0
            while (true) {
                val key = DateUtils.dateKeyDaysAgo(idx)
                val set = prefs.getCompletions(key)
                val done = set.contains(h.id)
                val dow = DateUtils.dayOfWeekDaysAgo(idx)
                val scheduled = h.daysOfWeek
                val isScheduledDay = scheduled.isNullOrEmpty() || scheduled.contains(dow)
                if (!isScheduledDay) {
                    idx++
                    continue
                }
                if (!done) break
                days++
                idx++
                if (idx > 365) break
            }
            return days
        }

        val localAdapter = HabitsAdapter(
            items = habits,
            isCompleted = { h -> completed.contains(h.id) },
            onToggle = { h, checked ->
                if (checked) completed.add(h.id) else completed.remove(h.id)
                prefs.setCompletions(today, completed)
                rv.adapter?.notifyDataSetChanged()
                refreshEmpty()
                refreshProgress()
            },
            onDelete = { h ->
                (rv.adapter as? HabitsAdapter)?.remove(h)
                val newList = prefs.getHabits().filter { it.id != h.id }
                prefs.setHabits(newList)
                completed.remove(h.id)
                prefs.setCompletions(today, completed)
                habits.removeAll { it.id == h.id }
                rv.adapter?.notifyDataSetChanged()
                refreshEmpty()
                refreshProgress()
            },
            onEdit = { h ->
                val args = bundleOf("habitId" to h.id)
                findNavController().navigate(R.id.action_habitsFragment_to_editHabitFragment, args)
            },
            getStreak = { h -> streakFor(h) }
        )
        adapter = localAdapter
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = localAdapter

        btnAdd.setOnClickListener { findNavController().navigate(R.id.action_habitsFragment_to_addHabitFragment) }

        refreshEmpty()
        refreshProgress()
    }

    override fun onResume() {
        super.onResume()
        // Reload data to reflect any changes from Add/Edit screens
        view?.let { v ->
            val prefs = Prefs(requireContext())
            val today = DateUtils.todayKey()
            val rv = v.findViewById<RecyclerView>(R.id.rvHabits)
            val currentList = prefs.getHabits()
            (rv.adapter as? HabitsAdapter)?.setItems(currentList)
            // Ensure empty/progress refresh
            val emptyState = v.findViewById<LinearLayout>(R.id.emptyState)
            val tvPercent = v.findViewById<TextView>(R.id.tvTodayProgressPercent)
            val tvGoalCount = v.findViewById<TextView>(R.id.tvGoalCount)
            val pbGoal = v.findViewById<android.widget.ProgressBar>(R.id.pbGoal)
            // Use existing helpers by recomputing similarly
            val adapterRef = rv.adapter as? HabitsAdapter
            val visible = adapterRef?.currentItems().orEmpty()
            emptyState.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
            val completedToday = prefs.getCompletions(today)
            val eligible = visible.size
            val done = visible.count { completedToday.contains(it.id) }
            val percent = if (eligible == 0) 0 else (done * 100 / eligible)
            tvPercent.text = "$percent%"
            tvGoalCount.text = "$done/$eligible"
            pbGoal.progress = percent
        }
    }
}

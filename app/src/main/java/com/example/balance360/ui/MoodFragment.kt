package com.example.balance360.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balance360.R
import com.example.balance360.data.MoodEntry
import com.example.balance360.data.Prefs
import com.example.balance360.ui.adapters.MoodAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MoodFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_mood, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = Prefs(requireContext())
        val etNote = view.findViewById<EditText>(R.id.etNote)
        // Journal list setup
        val rv = view.findViewById<RecyclerView>(R.id.rvMoodHistory)
        val sv = view.findViewById<android.widget.ScrollView>(R.id.svMood)
        val historyAdapter = MoodAdapter(prefs.getMoods())
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = historyAdapter

        // Map mood containers to emojis (10 moods as per layout)
        val pairs: List<Pair<Int, String>> = listOf(
            R.id.moodHappy to "😊",
            R.id.moodExcited to "😄",
            R.id.moodCalm to "😌",
            R.id.moodLoved to "🥰",
            R.id.moodConfident to "😎",
            R.id.moodThoughtful to "🤔",
            R.id.moodSad to "😔",
            R.id.moodAnxious to "😰",
            R.id.moodTired to "😴",
            R.id.moodAngry to "😠",
        )
        val views: List<Pair<LinearLayout, String>> = pairs.map { (id, emoji) ->
            view.findViewById<LinearLayout>(id) to emoji
        }
        // Label map to show in preview
        val labelById: Map<Int, String> = mapOf(
            R.id.moodHappy to "Happy",
            R.id.moodExcited to "Excited",
            R.id.moodCalm to "Calm",
            R.id.moodLoved to "Loved",
            R.id.moodConfident to "Confident",
            R.id.moodThoughtful to "Thoughtful",
            R.id.moodSad to "Sad",
            R.id.moodAnxious to "Anxious",
            R.id.moodTired to "Tired",
            R.id.moodAngry to "Angry",
        )
        val previewEmoji = view.findViewById<TextView>(R.id.tvPreviewEmoji)
        val previewText = view.findViewById<TextView>(R.id.tvPreviewText)

        var selectedEmoji: String = views.first().second
        fun select(v: LinearLayout) {
            views.forEach { it.first.isSelected = false }
            v.isSelected = true
            // Update preview banner
            val id = v.id
            val label = labelById[id] ?: ""
            val emoji = views.firstOrNull { it.first.id == id }?.second ?: selectedEmoji
            previewEmoji?.text = emoji
            previewText?.text = if (label.isNotEmpty()) "Feeling $label" else ""
        }
        // Preselect first
        select(views.first().first)
        views.forEach { (ll, emoji) -> ll.setOnClickListener { selectedEmoji = emoji; select(ll) } }

        // Intensity controls removed from UI; store as null
        var intensity: Int? = null

        // Calendar filtering
        val calendarView = view.findViewById<android.widget.CalendarView>(R.id.calendarMood)
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fun filterByDay(date: Date) {
            val key = dayFmt.format(date)
            val filtered = prefs.getMoods().filter { dayFmt.format(Date(it.timestamp)) == key }
            historyAdapter.replace(filtered)
        }
        // Default to today
        filterByDay(Date())
        // Scroll a bit down so the lower content is visible by default
        val header = view.findViewById<TextView>(R.id.tvJournalHeader)
        sv?.post {
            val offset = (8 * resources.displayMetrics.density).toInt()
            val targetY = (header?.top ?: 0) - offset
            sv.smoothScrollTo(0, if (targetY < 0) 0 else targetY)
        }
        calendarView?.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            filterByDay(cal.time)
        }

        // Cancel/Save from both top and bottom rows if present
        fun saveMood() {
            val note = etNote.text?.toString()?.takeIf { it.isNotBlank() }
            val entry = MoodEntry(System.currentTimeMillis(), selectedEmoji, note, intensity)
            val list = prefs.getMoods().toMutableList().apply { add(0, entry) }
            prefs.setMoods(list)
            // re-filter based on selected calendar day
            val selectedDate = calendarView?.date ?: System.currentTimeMillis()
            filterByDay(Date(selectedDate))
            rv?.scrollToPosition(0)
            sv?.post {
                val y = (rv?.top ?: 0) - 24
                sv.smoothScrollTo(0, if (y < 0) 0 else y)
            }
        }
        view.findViewById<TextView>(R.id.btnCancel)?.setOnClickListener { findNavController().popBackStack() }
        view.findViewById<TextView>(R.id.btnCancelBottom)?.setOnClickListener { findNavController().popBackStack() }
        view.findViewById<TextView>(R.id.btnSaveMoodBottom)?.setOnClickListener { saveMood() }
    }
}

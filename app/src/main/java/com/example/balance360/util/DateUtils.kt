package com.example.balance360.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

object DateUtils {
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    fun todayKey(): String = dayFormat.format(Date())
    fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dayFormat.format(cal.time)
    }
    fun dayOfWeekDaysAgo(daysAgo: Int): Int {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    }
}

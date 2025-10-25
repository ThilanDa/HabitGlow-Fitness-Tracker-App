package com.example.balance360.data

data class Habit(
    val id: String,
    var name: String,
    var description: String? = null,
    var icon: String? = null,       // emoji like "🎯" or "💧"
    var colorHex: String? = null,   // optional accent color e.g. "#66BB6A"
    var reminderHour: Int? = null,
    var reminderMinute: Int? = null,
    // New: support multiple reminder times stored as "HH:mm" strings (24h). Keep old hour/min for compatibility.
    var reminderTimes: List<String>? = null,
    // New: optional timer duration in minutes when Timer mode is selected
    var timerMinutes: Int? = null,
    // New: scheduled days of week using Calendar.DAY_OF_WEEK values (1=Sun..7=Sat)
    var daysOfWeek: List<Int>? = null
)

data class MoodEntry(
    val timestamp: Long,
    val emoji: String,
    val note: String? = null,
    val intensity: Int? = null // 1..10
)

data class Settings(
    var hydrationIntervalMinutes: Int = 120,
    var dailyWaterGoalMl: Int = 2000,
    var darkTheme: Boolean = false,
    var notificationsEnabled: Boolean = true,
    var soundEffects: Boolean = true,
    var startHour: Int = 8,
    var startMinute: Int = 0,
    var endHour: Int = 22,
    var endMinute: Int = 0
)

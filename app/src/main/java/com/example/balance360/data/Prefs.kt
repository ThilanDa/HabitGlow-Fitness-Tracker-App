package com.example.balance360.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("balance_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Habits
    fun getHabits(): MutableList<Habit> {
        val json = sp.getString(KEY_HABITS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            gson.fromJson<MutableList<Habit>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun setHabits(list: List<Habit>) {
        sp.edit().putString(KEY_HABITS, gson.toJson(list)).apply()
    }

    // Daily completion map: date (yyyy-MM-dd) -> set of habit ids
    fun getCompletions(dateKey: String): MutableSet<String> {
        val json = sp.getString("$KEY_COMPLETIONS:$dateKey", null) ?: return mutableSetOf()
        return try {
            val type = object : TypeToken<MutableSet<String>>() {}.type
            gson.fromJson<MutableSet<String>>(json, type) ?: mutableSetOf()
        } catch (_: Exception) {
            mutableSetOf()
        }
    }

    fun setCompletions(dateKey: String, ids: Set<String>) {
        sp.edit().putString("$KEY_COMPLETIONS:$dateKey", gson.toJson(ids)).apply()
    }

    // Moods
    fun getMoods(): MutableList<MoodEntry> {
        val json = sp.getString(KEY_MOODS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            gson.fromJson<MutableList<MoodEntry>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun setMoods(list: List<MoodEntry>) {
        sp.edit().putString(KEY_MOODS, gson.toJson(list)).apply()
    }

    // Settings
    fun getSettings(): Settings {
        val json = sp.getString(KEY_SETTINGS, null) ?: return Settings()
        return try { gson.fromJson(json, Settings::class.java) ?: Settings() } catch (_: Exception) { Settings() }
    }

    fun setSettings(s: Settings) {
        sp.edit().putString(KEY_SETTINGS, gson.toJson(s)).apply()
    }

    // Water intake per day
    fun getWaterMl(dateKey: String): Int = sp.getInt("$KEY_WATER:$dateKey", 0)
    fun setWaterMl(dateKey: String, ml: Int) {
        sp.edit().putInt("$KEY_WATER:$dateKey", ml).apply()
    }

    // --- App PIN (hashed) ---
    fun isPinSet(): Boolean = getPinHash().isNotEmpty()

    fun setPin(pin: String) {
        val hash = sha256(pin)
        sp.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saved = getPinHash()
        return saved.isNotEmpty() && saved == sha256(pin)
    }

    private fun getPinHash(): String = sp.getString(KEY_PIN_HASH, "") ?: ""

    private fun sha256(s: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(s.toByteArray())
            bytes.joinToString("") { b -> "%02x".format(b) }
        } catch (_: Exception) { "" }
    }

    // --- Backup/Restore ---
    data class BackupEntry(val k: String, val t: String, val v: Any?)

    // --- In-app notification log ---
    data class AppNotification(val title: String, val text: String, val ts: Long)

    fun getNotifications(): MutableList<AppNotification> {
        val json = sp.getString(KEY_NOTIFICATIONS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<AppNotification>>() {}.type
            gson.fromJson<MutableList<AppNotification>>(json, type) ?: mutableListOf()
        } catch (_: Exception) { mutableListOf() }
    }

    fun addNotification(n: AppNotification) {
        val list = getNotifications()
        list.add(0, n) // most recent first
        // Keep last 100
        val trimmed = if (list.size > 100) list.subList(0, 100) else list
        sp.edit().putString(KEY_NOTIFICATIONS, gson.toJson(trimmed)).apply()
    }

    // --- Simple local auth (demo) ---
    fun isLoggedIn(): Boolean = (sp.getString(KEY_USER_EMAIL, null)?.isNotEmpty() == true)

    fun getUserEmail(): String? = sp.getString(KEY_USER_EMAIL, null)

    fun signIn(email: String, password: String): Boolean {
        val savedEmail = sp.getString(KEY_USER_EMAIL, null)
        val savedHash = sp.getString(KEY_USER_PASS, null)
        return if (savedEmail != null && savedHash != null && savedEmail.equals(email, ignoreCase = true) && savedHash == sha256(password)) {
            true
        } else false
    }

    fun signUp(email: String, password: String) {
        val norm = email.trim().lowercase()
        sp.edit()
            .putString(KEY_USER_EMAIL, norm)
            .putString(KEY_USER_PASS, sha256(password))
            .apply()
    }

    fun signOut() {
        sp.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PASS)
            .apply()
    }

    /** Serialize all SharedPreferences entries to a JSON string with explicit types. */
    fun generateBackup(): String {
        val list = mutableListOf<BackupEntry>()
        for ((k, v) in sp.all) {
            when (v) {
                is String -> list.add(BackupEntry(k, "string", v))
                is Int -> list.add(BackupEntry(k, "int", v))
                is Long -> list.add(BackupEntry(k, "long", v))
                is Float -> list.add(BackupEntry(k, "float", v))
                is Boolean -> list.add(BackupEntry(k, "bool", v))
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val set = v as? Set<String>
                    list.add(BackupEntry(k, "string_set", set?.toList()))
                }
                else -> list.add(BackupEntry(k, "unknown", v?.toString()))
            }
        }
        return gson.toJson(list)
    }

    /** Apply a previously exported JSON backup. Returns true if applied. */
    fun applyBackup(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<BackupEntry>>() {}.type
            val list: List<BackupEntry> = gson.fromJson(json, type) ?: return false
            val editor = sp.edit().clear()
            list.forEach { e ->
                when (e.t) {
                    "string" -> editor.putString(e.k, e.v as? String)
                    "int" -> editor.putInt(e.k, (e.v as Number).toInt())
                    "long" -> editor.putLong(e.k, (e.v as Number).toLong())
                    "float" -> editor.putFloat(e.k, (e.v as Number).toFloat())
                    "bool" -> editor.putBoolean(e.k, e.v as Boolean)
                    "string_set" -> {
                        val listAny = e.v as? List<*>
                        val set = listAny?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                        editor.putStringSet(e.k, set)
                    }
                    else -> { /* skip */ }
                }
            }
            editor.apply()
            true
        } catch (_: Exception) { false }
    }

    companion object {
        private const val KEY_HABITS = "habits"
        private const val KEY_COMPLETIONS = "completions"
        private const val KEY_MOODS = "moods"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_WATER = "water"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_NOTIFICATIONS = "notifications_log"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASS = "user_pass_hash"
    }
}

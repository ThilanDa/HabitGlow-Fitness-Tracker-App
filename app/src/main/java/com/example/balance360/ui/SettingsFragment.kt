package com.example.balance360.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.balance360.R
import com.example.balance360.data.Prefs
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SettingsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private var pendingExportJson: String? = null

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val json = pendingExportJson ?: return@registerForActivityResult
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { it.write(json) }
            }
            Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pendingExportJson = null
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            val json = requireContext().contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins, Charsets.UTF_8)).readText()
            } ?: ""
            val ok = prefs.applyBackup(json)
            if (ok) {
                val ns = prefs.getSettings()
                view?.findViewById<Switch>(R.id.swNotifications)?.isChecked = ns.notificationsEnabled
                view?.findViewById<Switch>(R.id.swSounds)?.isChecked = ns.soundEffects
                view?.findViewById<Spinner>(R.id.spTheme)?.setSelection(if (ns.darkTheme) 1 else 0)
                AppCompatDelegate.setDefaultNightMode(if (ns.darkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(requireContext(), "Import successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Import failed (invalid file)", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = Prefs(requireContext())
        val s = prefs.getSettings()

        // Back button in the top bar
        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            findNavController().popBackStack()
        }

        // Theme spinner (Light/Dark)
        val spTheme = view.findViewById<Spinner>(R.id.spTheme)
        val themeItems = listOf("Light", "Dark")
        spTheme.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, themeItems)
        spTheme.setSelection(if (s.darkTheme) 1 else 0)

        // Notifications toggle + badge
        val swNotifications = view.findViewById<Switch>(R.id.swNotifications)
        val tvNotifBadge = view.findViewById<TextView>(R.id.tvNotifBadge)
        swNotifications.isChecked = s.notificationsEnabled

        fun refreshNotifBadge() {
            val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            tvNotifBadge.visibility = if (swNotifications.isChecked && needsPermission) View.VISIBLE else View.GONE
        }
        refreshNotifBadge()

        swNotifications.setOnCheckedChangeListener { _, isChecked ->
            val copy = prefs.getSettings()
            copy.notificationsEnabled = isChecked
            prefs.setSettings(copy)
            refreshNotifBadge()
        }

        tvNotifBadge.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
        }

        // Sound effects
        val swSounds = view.findViewById<Switch>(R.id.swSounds)
        swSounds.isChecked = s.soundEffects
        swSounds.setOnCheckedChangeListener { _, isChecked ->
            val copy = prefs.getSettings()
            copy.soundEffects = isChecked
            prefs.setSettings(copy)
        }

        // Apply selected theme on spinner change
        spTheme.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: View?, position: Int, id: Long) {
                val dark = position == 1
                val copy = prefs.getSettings()
                if (copy.darkTheme != dark) {
                    copy.darkTheme = dark
                    prefs.setSettings(copy)
                    AppCompatDelegate.setDefaultNightMode(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        // Export via system file picker (choose folder/name on phone)
        view.findViewById<Button>(R.id.btnExport).setOnClickListener {
            pendingExportJson = prefs.generateBackup()
            val name = "balance360_backup_${System.currentTimeMillis()}.json"
            exportLauncher.launch(name)
        }
        // Import via system file picker (choose the backup JSON on phone)
        view.findViewById<Button>(R.id.btnImport).setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/*"))
        }

        // About info
        val versionName: String = try {
            val pm = requireContext().packageManager
            val pkg = requireContext().packageName
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName ?: "1.0"
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).versionName ?: "1.0"
            }
        } catch (_: Exception) { "1.0" }
        view.findViewById<TextView>(R.id.tvVersion).text = versionName
        view.findViewById<TextView>(R.id.tvBuildDate).text = "September 2024" // update if you have a real build date
        view.findViewById<TextView>(R.id.tvDeveloper).text = "Balance360 Team"
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.tvNotifBadge)?.let { badge ->
            val sw = view?.findViewById<Switch>(R.id.swNotifications)
            val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            badge.visibility = if (sw?.isChecked == true && needsPermission) View.VISIBLE else View.GONE
        }
    }
}

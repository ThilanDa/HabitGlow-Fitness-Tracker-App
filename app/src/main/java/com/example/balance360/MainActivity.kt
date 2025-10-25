package com.example.balance360

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.example.balance360.data.Prefs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved theme before inflating views
        try {
            val dark = Prefs(this).getSettings().darkTheme
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        } catch (_: Exception) { }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.setupWithNavController(navController)

            // Ensure tapping items always navigates to the root destination and fixes cases
            // where Home appears unresponsive after deep navigation.
            bottomNav.setOnItemSelectedListener { item ->
                val options = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    // popUpTo the graph so switching tabs doesn't build a long back stack
                    .setPopUpTo(navController.graph.startDestinationId, false)
                    .build()
                return@setOnItemSelectedListener try {
                    if (item.itemId == navController.currentDestination?.id) {
                        true
                    } else {
                        navController.navigate(item.itemId, null, options)
                        true
                    }
                } catch (_: Exception) { false }
            }

            // Reselecting a tab pops to its start destination (e.g., Home scroll to top behavior)
            bottomNav.setOnItemReselectedListener { menuItem ->
                try {
                    navController.popBackStack(menuItem.itemId, false)
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation setup failed", e)
            Toast.makeText(this, "Navigation setup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        try {
            requestNotificationPermissionIfNeeded()
        } catch (e: Exception) {
            Log.w("MainActivity", "Permission request failed", e)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
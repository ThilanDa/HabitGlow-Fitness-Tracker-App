package com.example.balance360.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.balance360.R
import com.example.balance360.data.Prefs

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Short delay for splash; always show Sign In on launch
        window.decorView.postDelayed({
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }, 600)
    }
}

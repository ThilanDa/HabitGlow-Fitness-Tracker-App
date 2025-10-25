package com.example.balance360.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.balance360.MainActivity
import com.example.balance360.R
import com.example.balance360.data.Prefs

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val etEmail = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val etConfirm = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirm)
        val btnSignUp = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignUp)
        val tvGoSignIn = findViewById<TextView>(R.id.tvGoSignIn)

        val prefs = Prefs(this)

        btnSignUp.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val pass = etPassword.text?.toString() ?: ""
            val confirm = etConfirm.text?.toString() ?: ""
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (pass.length < 4) {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val existing = prefs.getUserEmail()
            if (!existing.isNullOrEmpty()) {
                // If account exists on device
                if (existing.equals(email, ignoreCase = true)) {
                    // If credentials match, treat as sign-in and continue
                    if (prefs.signIn(email, pass)) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Account already exists. Please sign in with the correct password.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Another account exists on this device. Please sign out first (Settings).", Toast.LENGTH_LONG).show()
                }
            } else {
                // First-time sign up
                prefs.signUp(email, pass)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        tvGoSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}

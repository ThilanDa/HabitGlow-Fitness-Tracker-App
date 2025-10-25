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

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val etEmail = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val btnSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn)
        val tvGoSignUp = findViewById<TextView>(R.id.tvGoSignUp)

        val prefs = Prefs(this)

        btnSignIn.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val pass = etPassword.text?.toString() ?: ""
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (pass.length < 4) {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (prefs.signIn(email, pass)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid credentials. Try Sign up.", Toast.LENGTH_LONG).show()
            }
        }

        tvGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}

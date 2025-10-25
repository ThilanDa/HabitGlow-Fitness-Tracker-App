package com.example.balance360.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.balance360.MainActivity
import com.example.balance360.R
import com.example.balance360.data.Prefs

class PinActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE_SET = "mode_set" // if true -> set new PIN, else -> verify existing PIN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val prefs = Prefs(this)
        val isSetMode = intent.getBooleanExtra(EXTRA_MODE_SET, !prefs.isPinSet())

        val title = findViewById<TextView>(R.id.tvTitle)
        val etPin = findViewById<EditText>(R.id.etPin)
        val etConfirm = findViewById<EditText>(R.id.etConfirm)
        val tilPin = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPin)
        val tilConfirm = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilConfirm)
        val btn = findViewById<Button>(R.id.btnPrimary)
        val hint = findViewById<TextView>(R.id.tvHint)

        fun setupField(et: EditText) {
            et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            et.filters = arrayOf(InputFilter.LengthFilter(4))
        }
        setupField(etPin)
        setupField(etConfirm)

        if (isSetMode) {
            title.text = getString(R.string.pin_set_title)
            btn.text = getString(R.string.action_set_pin)
            hint.text = getString(R.string.pin_set_hint)
            etConfirm.visibility = View.VISIBLE
            tilConfirm.visibility = View.VISIBLE
        } else {
            title.text = getString(R.string.pin_enter_title)
            btn.text = getString(R.string.action_unlock)
            hint.text = getString(R.string.pin_enter_hint)
            etConfirm.visibility = View.GONE
            tilConfirm.visibility = View.GONE
        }

        btn.setOnClickListener {
            tilPin.error = null
            tilConfirm.error = null
            val p1 = etPin.text?.toString()?.trim() ?: ""
            if (p1.length != 4) {
                tilPin.error = getString(R.string.pin_4_digits)
                return@setOnClickListener
            }
            if (isSetMode) {
                val p2 = etConfirm.text?.toString()?.trim() ?: ""
                if (p1 != p2) {
                    tilConfirm.error = getString(R.string.pin_mismatch)
                    return@setOnClickListener
                }
                prefs.setPin(p1)
                Toast.makeText(this, getString(R.string.pin_set_success), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                if (prefs.verifyPin(p1)) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    tilPin.error = getString(R.string.pin_incorrect)
                }
            }
        }
    }
}

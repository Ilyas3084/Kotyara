package com.example.popitkakursacha

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var sharedPrefs: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }

            findPreference<Preference>("pref_enable_vibration")?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (requireContext().checkSelfPermission(android.Manifest.permission.VIBRATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(requireContext(), "Требуется разрешение на вибрацию", Toast.LENGTH_SHORT).show()
                        return@setOnPreferenceChangeListener false
                    }
                }
                true
            }

            findPreference<Preference>("pref_theme")?.setOnPreferenceChangeListener { _, newValue ->
                val themeValue = newValue as String
                when (themeValue) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                true
            }
        }

        private fun showAboutDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("О приложении")
                .setMessage("Kotyara Scanner v1.0\n\nУправление товарами и ячейками с помощью QR-кодов.\n\n© 2025")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
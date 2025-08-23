package com.example.popitkakursacha

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var sharedPrefs: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            // Обработка "О приложении"
            findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }

            // Проверка вибрации
            findPreference<Preference>("pref_enable_vibration")?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (requireContext().checkSelfPermission(android.Manifest.permission.VIBRATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(requireContext(), "Требуется разрешение на вибрацию", Toast.LENGTH_SHORT).show()
                        return@setOnPreferenceChangeListener false
                    }
                }
                true
            }
        }

        private fun showAboutDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("О приложении")
                .setMessage("SmartShelf v1.0\n\nУправление товарами и ячейками с помощью QR-кодов.\n\n© 2025")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
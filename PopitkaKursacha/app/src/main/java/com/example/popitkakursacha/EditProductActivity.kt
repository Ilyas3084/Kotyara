package com.example.popitkakursacha

import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar

class EditProductActivity : AppCompatActivity() {
    private lateinit var productDao: ProductDao
    private lateinit var product: Product
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        productDao = AppDatabase.getDatabase(this).productDao()
        product = intent.getSerializableExtra("product") as Product
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val nameInput = findViewById<TextInputEditText>(R.id.productNameInput)
        val descInput = findViewById<TextInputEditText>(R.id.productDescInput)
        val priceInput = findViewById<TextInputEditText>(R.id.productPriceInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        val scanQrButton = findViewById<Button>(R.id.scanQrButton)

        nameInput.setText(product.name)
        descInput.setText(product.description)
        priceInput.setText(product.price.toString())

        saveButton.setOnClickListener {
            saveProductChanges()
        }

        deleteButton.setOnClickListener {
            deleteProduct()
        }

        scanQrButton.setOnClickListener {
            scanQrCode()
        }
    }

    private fun saveProductChanges() {
        val nameInput = findViewById<TextInputEditText>(R.id.productNameInput)
        val descInput = findViewById<TextInputEditText>(R.id.productDescInput)
        val priceInput = findViewById<TextInputEditText>(R.id.productPriceInput)

        val newName = nameInput.text.toString()
        val newDesc = descInput.text.toString()
        val newPrice = priceInput.text.toString().toDoubleOrNull() ?: 0.0

        if (newName.isBlank() || newDesc.isBlank()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedProduct = product.copy(
            name = newName,
            description = newDesc,
            price = newPrice
        )

        lifecycleScope.launch(Dispatchers.IO) {
            productDao.insert(updatedProduct)
            launch(Dispatchers.Main) {
                Toast.makeText(this@EditProductActivity, "Товар обновлен", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun deleteProduct() {
        lifecycleScope.launch(Dispatchers.IO) {
            productDao.delete(product)
            launch(Dispatchers.Main) {
                Toast.makeText(this@EditProductActivity, "Товар удален", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun scanQrCode() {
        try {
            val integrator = IntentIntegrator(this)
                .setPrompt("Сканируйте QR-код ячейки")
                .setOrientationLocked(true)
                .setBeepEnabled(true)
                .setCaptureActivity(PortraitScannerActivity::class.java)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setCameraId(0)
                .setBarcodeImageEnabled(false)

            integrator.initiateScan()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска камеры: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show()
            } else {
                processScannedQrCode(result.contents)
            }
        }
    }

    private fun processScannedQrCode(qrCode: String) {
        triggerVibration() // Добавляем вибрацию при успешном сканировании
        val updatedProduct = product.copy(cellQrCode = qrCode)

        lifecycleScope.launch(Dispatchers.IO) {
            productDao.insert(updatedProduct)
            launch(Dispatchers.Main) {
                Toast.makeText(
                    this@EditProductActivity,
                    "Товар привязан к ячейке: $qrCode",
                    Toast.LENGTH_SHORT
                ).show()
                product = updatedProduct
            }
        }
    }

    private fun triggerVibration() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val enableVibration = sharedPrefs.getBoolean("pref_enable_vibration", true)
        if (enableVibration && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }
}
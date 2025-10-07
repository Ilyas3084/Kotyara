package com.example.popitkakursacha

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var productDao: ProductDao
    private lateinit var cellDao: CellDao
    private val gson = Gson()
    private var lastScannedBarcode: String? = null
    private var lastScannedQrCode: String? = null
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var vibrator: Vibrator

    // Лаунчеры для файловых операций
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importDatabase(it) }
    }
    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportDatabase(it) }
    }

    // Лаунчер для сканирования
    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
            if (scanResult?.contents != null) {
                processScannedCode(scanResult.contents)
            } else {
                Toast.makeText(this, "Сканирование не удалось", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show()
        }
    }

    // Лаунчер для разрешения камеры
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startScan()
        } else {
            Toast.makeText(this, "Разрешение на камеру не предоставлено", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация базы данных
        val db = AppDatabase.getDatabase(this)
        productDao = db.productDao()
        cellDao = db.cellDao()

        // Инициализация SharedPreferences для настроек
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Инициализация вибратора
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Инициализация темы
        initializeTheme()

        initButtons()
    }

    private fun initButtons() {
        // Кнопка настроек
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Экспорт базы данных
        findViewById<Button>(R.id.exportDbButton).setOnClickListener {
            exportFileLauncher.launch("products_backup.json")
        }

        // Импорт базы данных
        findViewById<Button>(R.id.importDbButton).setOnClickListener {
            importFileLauncher.launch("application/json")
        }

        // Сканирование кода
        findViewById<Button>(R.id.scanButton).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Просмотр списка товаров
        findViewById<Button>(R.id.viewProductsButton).setOnClickListener {
            startActivity(Intent(this, ProductsListActivity::class.java))
        }

        // Копирование результата
        findViewById<Button>(R.id.copyButton).setOnClickListener {
            val text = lastScannedBarcode ?: lastScannedQrCode
            if (text.isNullOrEmpty()) {
                Toast.makeText(this, "Нет данных для копирования", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Scan Result", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }

        // Очистка результата
        findViewById<Button>(R.id.clearButton).setOnClickListener {
            clearScanResult()
        }

        // Добавление товара
        findViewById<Button>(R.id.addProductButton).setOnClickListener {
            showAddProductDialog(lastScannedBarcode)
        }

        // Просмотр списка ячеек
        findViewById<Button>(R.id.viewCellsButton).setOnClickListener {
            startActivity(Intent(this, CellListActivity::class.java))
        }
    }

    private fun clearScanResult() {
        clearProductInfo()
        lastScannedBarcode = null
        lastScannedQrCode = null
        findViewById<Button>(R.id.copyButton).visibility = View.GONE
        findViewById<Button>(R.id.clearButton).visibility = View.GONE
        findViewById<Button>(R.id.addProductButton).visibility = View.GONE
        findViewById<androidx.cardview.widget.CardView>(R.id.productCard).visibility = View.GONE
    }

    private fun startScan() {
        try {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val enableSound = sharedPrefs.getBoolean("pref_enable_sound", true)

            val integrator = IntentIntegrator(this).apply {
                setPrompt("Сканируйте штрих-код товара или QR-код ячейки")
                setOrientationLocked(true)
                setBeepEnabled(enableSound)
                captureActivity = PortraitScannerActivity::class.java
            }
            scanLauncher.launch(integrator.createScanIntent())
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска камеры: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeTheme() {
        val themePreference = sharedPrefs.getString("pref_theme", "system")
        when (themePreference) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun triggerVibration() {
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

    private fun processScannedCode(code: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (code.matches(Regex("\\d+"))) {
                processBarcode(code)
            } else {
                processQrCode(code)
            }
        }
    }

    private suspend fun processBarcode(barcode: String) {
        val product = productDao.getProductByCode(barcode)
        withContext(Dispatchers.Main) {
            triggerVibration() // Добавляем вибрацию при успешном сканировании
            lastScannedBarcode = barcode
            findViewById<Button>(R.id.copyButton).visibility = View.VISIBLE
            findViewById<Button>(R.id.clearButton).visibility = View.VISIBLE
            if (product == null) {
                Toast.makeText(this@MainActivity, "Товар не найден", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.addProductButton).visibility = View.VISIBLE
                findViewById<androidx.cardview.widget.CardView>(R.id.productCard).visibility = View.GONE
                clearProductInfo()
            } else {
                displayProductInfo(product)
                findViewById<Button>(R.id.addProductButton).visibility = View.GONE
                findViewById<androidx.cardview.widget.CardView>(R.id.productCard).visibility = View.VISIBLE
            }
        }
    }

    private suspend fun processQrCode(qrCode: String) {
        lastScannedQrCode = qrCode
        val cell = cellDao.getCell(qrCode)
        withContext(Dispatchers.Main) {
            triggerVibration() // Добавляем вибрацию при успешном сканировании
            
            findViewById<Button>(R.id.copyButton).visibility = View.VISIBLE
            findViewById<Button>(R.id.clearButton).visibility = View.VISIBLE
            findViewById<androidx.cardview.widget.CardView>(R.id.productCard).visibility = View.GONE
            clearProductInfo()
            if (cell == null) {
                showAddCellDialog(qrCode)
            } else {
                showCellOptionsDialog(cell)
            }
        }
    }

    private fun showAddCellDialog(qrCode: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_cell, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.cellNameInput)
        AlertDialog.Builder(this)
            .setTitle("Новая ячейка")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val cellName = nameInput.text.toString()
                if (cellName.isBlank()) {
                    Toast.makeText(this, "Введите название ячейки", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val cell = Cell(
                            qrCode = qrCode,
                            name = cellName
                        )
                        withContext(Dispatchers.IO) {
                            cellDao.insert(cell)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Ячейка сохранена", Toast.LENGTH_SHORT).show()
                        }
                        linkProductToCellIfNeeded(qrCode)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCellOptionsDialog(cell: Cell) {
        val options = arrayOf("Изменить название", "Привязать товар", "Отмена")
        AlertDialog.Builder(this)
            .setTitle(cell.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditCellDialog(cell)
                    1 -> linkProductToCellIfNeeded(cell.qrCode)
                }
            }
            .show()
    }

    private fun showEditCellDialog(cell: Cell) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_cell, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.cellNameInput)
        nameInput.setText(cell.name)
        AlertDialog.Builder(this)
            .setTitle("Редактировать ячейку")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedCell = cell.copy(name = nameInput.text.toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    cellDao.insert(updatedCell)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun linkProductToCellIfNeeded(qrCode: String) {
        lastScannedBarcode?.let { barcode ->
            lifecycleScope.launch {
                try {
                    val product = withContext(Dispatchers.IO) {
                        productDao.getProductByCode(barcode)
                    }
                    if (product != null) {
                        val updatedProduct = product.copy(cellQrCode = qrCode)
                        withContext(Dispatchers.IO) {
                            productDao.insert(updatedProduct)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Товар привязан к ячейке", Toast.LENGTH_SHORT).show()
                            displayProductInfo(updatedProduct)
                            findViewById<androidx.cardview.widget.CardView>(R.id.productCard).visibility = View.VISIBLE
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Товар не найден", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "Сначала отсканируйте штрих-код товара", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun displayProductInfo(product: Product) {
        val cellName = product.cellQrCode?.let { qrCode ->
            cellDao.getCell(qrCode)?.name ?: "Неизвестная ячейка"
        } ?: "Ячейка не указана"

        withContext(Dispatchers.Main) {
            findViewById<TextView>(R.id.productName).text = product.name
            findViewById<TextView>(R.id.productDescription).text = product.description
            findViewById<TextView>(R.id.productPrice).text = "Цена: ${product.price} руб."
            findViewById<TextView>(R.id.productCell).text = "Ячейка: $cellName"
        }
    }

    private fun clearProductInfo() {
        findViewById<TextView>(R.id.productName).text = ""
        findViewById<TextView>(R.id.productDescription).text = ""
        findViewById<TextView>(R.id.productPrice).text = ""
        findViewById<TextView>(R.id.productCell).text = ""
    }

    private fun showAddProductDialog(barcode: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.productNameInput)
        val descInput = dialogView.findViewById<EditText>(R.id.productDescInput)
        val priceInput = dialogView.findViewById<EditText>(R.id.productPriceInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить товар")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = nameInput.text.toString().trim()
                val description = descInput.text.toString().trim()
                val priceText = priceInput.text.toString().trim()

                if (name.isEmpty() || priceText.isEmpty() || barcode == null) {
                    Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val price = priceText.toDoubleOrNull()
                if (price == null || price < 0) {
                    Toast.makeText(this, "Введите корректную цену", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val product = Product(
                    barcode = barcode,
                    name = name,
                    description = description,
                    price = price
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        productDao.insert(product)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Товар добавлен", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            displayProductInfo(product)
                            findViewById<Button>(R.id.addProductButton).visibility = View.GONE
                            findViewById<androidx.cardview.widget.CardView>(R.id.productCard).visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun exportDatabase(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val products = productDao.getAllProducts()
                val cells = cellDao.getAllCells()
                val data = mapOf("products" to products, "cells" to cells)
                val json = gson.toJson(data)

                contentResolver.openFileDescriptor(uri, "w")?.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor).use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Экспорт завершен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun importDatabase(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = InputStreamReader(inputStream).readText()
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    val data: Map<String, Any> = gson.fromJson(json, type)

                    val productsType = object : TypeToken<List<Product>>() {}.type
                    val products: List<Product> = gson.fromJson(gson.toJson(data["products"]), productsType)

                    val cellsType = object : TypeToken<List<Cell>>() {}.type
                    val cells: List<Cell> = gson.fromJson(gson.toJson(data["cells"]), cellsType)

                    productDao.insertAll(products)
                    cellDao.insertAll(cells)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Импорт завершен", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
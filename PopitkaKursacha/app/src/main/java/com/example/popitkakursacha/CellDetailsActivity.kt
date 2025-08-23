package com.example.popitkakursacha

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar

class CellDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cell_details)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        val cell = intent.getSerializableExtra("cell") as? Cell
        if (cell == null) {
            Toast.makeText(this, "Ошибка загрузки ячейки", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayCellInfo(cell)
        loadProductsInCell(cell.qrCode)
    }

    private fun displayCellInfo(cell: Cell) {
        findViewById<TextView>(R.id.cellName).text = cell.name
        findViewById<TextView>(R.id.cellQrCode).text = "QR-код: ${cell.qrCode}"
        findViewById<TextView>(R.id.cellDescription).text = cell.description
    }

    private fun loadProductsInCell(qrCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val products = AppDatabase.getDatabase(this@CellDetailsActivity)
                .productDao()
                .getProductsByCell(qrCode)

            launch(Dispatchers.Main) {
                val productsText = if (products.isEmpty()) {
                    "В ячейке нет товаров"
                } else {
                    products.joinToString("\n") { it.name }
                }

                findViewById<TextView>(R.id.productsInCell).text = productsText
            }
        }
    }
}
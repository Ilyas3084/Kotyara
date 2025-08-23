package com.example.popitkakursacha

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.google.android.material.appbar.MaterialToolbar

class ProductDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        val product = intent.getSerializableExtra("product") as? Product
        val cell = intent.getSerializableExtra("cell") as? Cell

        product?.let {
            findViewById<TextView>(R.id.productName).text = it.name
            findViewById<TextView>(R.id.productBarcode).text = "Штрих-код: ${it.barcode}"
            findViewById<TextView>(R.id.productDescription).text = it.description
            findViewById<TextView>(R.id.productPrice).text = "Цена: ${it.price} руб."

            cell?.let { c ->
                findViewById<TextView>(R.id.productCell).text = """
                    Ячейка: ${c.name}
                    Описание: ${c.description}
                    Код: ${c.qrCode}
                """.trimIndent()
                findViewById<TextView>(R.id.productCell).visibility = View.VISIBLE
            } ?: run {
                findViewById<TextView>(R.id.productCell).visibility = View.GONE
            }
        } ?: run {
            finish()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
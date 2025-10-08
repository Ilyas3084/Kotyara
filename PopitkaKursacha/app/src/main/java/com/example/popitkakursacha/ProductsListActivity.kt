package com.example.popitkakursacha

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.withContext
import java.io.Serializable
import com.google.android.material.appbar.MaterialToolbar

class ProductsListActivity : AppCompatActivity() {
    private lateinit var adapter: ProductsAdapter
    private lateinit var allProducts: List<Product>
    private lateinit var filteredProducts: List<Product>
    private lateinit var cellsMap: Map<String, Cell>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_list)

        val recyclerView = findViewById<RecyclerView>(R.id.productsRecyclerView)
        val searchInput = findViewById<EditText>(R.id.searchInput)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        val db = AppDatabase.getDatabase(this)
        val productDao = db.productDao()
        val cellDao = db.cellDao()

        lifecycleScope.launch(Dispatchers.IO) {
            allProducts = productDao.getAllProducts()
            cellsMap = cellDao.getAllCells().associateBy { it.qrCode }

            withContext(Dispatchers.Main) {
                filteredProducts = allProducts
                adapter = ProductsAdapter(
                    products = filteredProducts,
                    cells = cellsMap,
                    onEditClick = { product ->
                        showEditProductDialog(product)
                    }
                )
                recyclerView.adapter = adapter

                searchInput.doAfterTextChanged { editable ->
                    val query = editable.toString().trim()
                    filterProducts(query)
                }
            }
        }
    }

    private fun filterProducts(query: String) {
        filteredProducts = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { product ->
                product.barcode.contains(query, ignoreCase = true) ||
                product.name.contains(query, ignoreCase = true)
            }
        }
        adapter.updateProducts(filteredProducts)
    }

    private fun showEditProductDialog(product: Product) {
        val intent = Intent(this, EditProductActivity::class.java).apply {
            putExtra("product", product as Serializable)
        }
        startActivityForResult(intent, EDIT_PRODUCT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PRODUCT_REQUEST && resultCode == RESULT_OK) {
            recreate()
        }
    }

    companion object {
        const val EDIT_PRODUCT_REQUEST = 1001
    }
}
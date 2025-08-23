package com.example.popitkakursacha

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar

class CellListActivity : AppCompatActivity() {
    private lateinit var adapter: CellsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cell_list)

        recyclerView = findViewById(R.id.cellsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        val searchInput = findViewById<TextInputEditText>(R.id.searchInput)
        searchInput.doAfterTextChanged { editable ->
            val query = editable.toString().trim()
            filterCells(query)
        }

        loadCells()
    }

    private fun loadCells() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cells = AppDatabase.getDatabase(this@CellListActivity).cellDao().getAllCells()
            launch(Dispatchers.Main) {
                adapter = CellsAdapter(cells) { cell ->
                    showCellDetails(cell)
                }
                recyclerView.adapter = adapter
            }
        }
    }

    private fun filterCells(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val allCells = AppDatabase.getDatabase(this@CellListActivity).cellDao().getAllCells()
            val filteredCells = if (query.isEmpty()) {
                allCells
            } else {
                allCells.filter { it.qrCode.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) }
            }
            launch(Dispatchers.Main) {
                adapter.updateCells(filteredCells)
            }
        }
    }

    private fun showCellDetails(cell: Cell) {
        val intent = Intent(this, CellDetailsActivity::class.java).apply {
            putExtra("cell", cell)
        }
        startActivity(intent)
    }
}
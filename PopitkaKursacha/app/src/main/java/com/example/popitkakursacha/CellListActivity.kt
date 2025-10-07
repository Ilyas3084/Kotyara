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
import androidx.appcompat.app.AlertDialog

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
                adapter = CellsAdapter(cells, 
                    onCellClick = { cell -> showCellDetails(cell) },
                    onCellDelete = { cell -> showDeleteConfirmation(cell) }
                )
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

    private fun showDeleteConfirmation(cell: Cell) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_cell_confirm_title))
            .setMessage(getString(R.string.delete_cell_confirm_message, cell.name))
            .setPositiveButton(getString(R.string.delete_button)) { _, _ ->
                deleteCell(cell)
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun deleteCell(cell: Cell) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(this@CellListActivity).cellDao().delete(cell)
                launch(Dispatchers.Main) {
                    // Обновляем список ячеек после удаления
                    loadCells()
                    // Показываем сообщение об успехе
                    android.widget.Toast.makeText(
                        this@CellListActivity,
                        getString(R.string.delete_success),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@CellListActivity,
                        getString(R.string.delete_error, e.message),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
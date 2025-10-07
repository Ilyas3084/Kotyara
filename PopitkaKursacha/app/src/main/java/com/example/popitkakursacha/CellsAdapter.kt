package com.example.popitkakursacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

class CellsAdapter(
    private var cells: List<Cell>,
    private val onCellClick: (Cell) -> Unit,
    private val onCellDelete: (Cell) -> Unit
) : RecyclerView.Adapter<CellsAdapter.CellViewHolder>() {

    inner class CellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.cellName)
        val qrCode: TextView = itemView.findViewById(R.id.cellQrCode)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cell, parent, false)
        return CellViewHolder(view)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val cell = cells[position]
        holder.name.text = cell.name
        holder.qrCode.text = "QR: ${cell.qrCode}"

        holder.itemView.setOnClickListener { onCellClick(cell) }
        holder.deleteButton.setOnClickListener { onCellDelete(cell) }
    }

    override fun getItemCount(): Int = cells.size

    fun updateCells(newList: List<Cell>) {
        this.cells = newList
        notifyDataSetChanged()
    }
}
package com.example.popitkakursacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductsAdapter(
    private var products: List<Product>,
    private val cells: Map<String, Cell>,
    private val onEditClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.productName)
        val barcode: TextView = itemView.findViewById(R.id.productCode)
        val description: TextView = itemView.findViewById(R.id.productDescription)
        val price: TextView = itemView.findViewById(R.id.productPrice)
        val cellName: TextView = itemView.findViewById(R.id.productCell)
        val editButton: Button = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.barcode.text = product.barcode
        holder.description.text = product.description
        holder.price.text = "${product.price} руб."

        product.cellQrCode?.let { qrCode ->
            cells[qrCode]?.let { cell ->
                holder.cellName.text = cell.name
                holder.cellName.visibility = View.VISIBLE
            } ?: run {
                holder.cellName.visibility = View.GONE
            }
        } ?: run {
            holder.cellName.visibility = View.GONE
        }

        holder.editButton.setOnClickListener { onEditClick(product) }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newList: List<Product>) {
        this.products = newList
        notifyDataSetChanged()
    }
}
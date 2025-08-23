package com.example.popitkakursacha

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val barcode: String,
    val name: String,
    val description: String,
    val price: Double,
    val cellQrCode: String? = null,
    val imagePath: String? = null
) : Serializable
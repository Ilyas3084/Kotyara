package com.example.popitkakursacha

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cells")
data class Cell(
    @PrimaryKey val qrCode: String,
    val name: String,
    val description: String = ""
) : Serializable
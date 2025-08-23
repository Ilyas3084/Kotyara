package com.example.popitkakursacha

import androidx.room.*

@Dao
interface CellDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cell: Cell)
    @Query("SELECT * FROM cells WHERE qrCode = :qrCode")
    suspend fun getCell(qrCode: String): Cell?
    @Query("SELECT * FROM cells")
    suspend fun getAllCells(): List<Cell>
    @Delete
    suspend fun delete(cell: Cell)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cells: List<Cell>)
}
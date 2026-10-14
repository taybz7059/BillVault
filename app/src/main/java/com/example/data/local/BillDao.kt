package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE userEmail = :email ORDER BY expiryDate ASC")
    fun getBillsForUser(email: String): Flow<List<BillItem>>

    @Query("SELECT * FROM bills WHERE userEmail = :email AND id = :id")
    suspend fun getBillById(email: String, id: Long): BillItem?

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): BillItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillItem>)

    @Update
    suspend fun updateBill(bill: BillItem)

    @Delete
    suspend fun deleteBill(bill: BillItem)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("SELECT COUNT(*) FROM bills WHERE userEmail = :email")
    suspend fun getBillCount(email: String): Int
}

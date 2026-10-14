package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class WarrantyStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    LIFETIME
}

@Entity(tableName = "bills")
data class BillItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userEmail: String,
    val productName: String,
    val brand: String = "",
    val storeOrSeller: String = "",
    val category: String = "Electronics",
    val purchaseDate: String, // YYYY-MM-DD
    val warrantyPeriodMonths: Int = 12,
    val expiryDate: String, // YYYY-MM-DD
    val price: Double = 0.0,
    val currency: String = "₹",
    val invoiceNumber: String = "",
    val serialNumber: String = "",
    val warrantyType: String = "Standard Warranty",
    val coverageDetails: String = "",
    val supportContact: String = "",
    val notes: String = "",
    val imageUri: String = "", // Local file URI or path
    val secondaryImageUri: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    fun getWarrantyStatus(): WarrantyStatus {
        if (warrantyPeriodMonths >= 1200) return WarrantyStatus.LIFETIME
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val expiry = sdf.parse(expiryDate) ?: return WarrantyStatus.ACTIVE
            val now = Date()
            val diffMillis = expiry.time - now.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            when {
                diffMillis < 0 -> WarrantyStatus.EXPIRED
                diffDays <= 30 -> WarrantyStatus.EXPIRING_SOON
                else -> WarrantyStatus.ACTIVE
            }
        } catch (e: Exception) {
            WarrantyStatus.ACTIVE
        }
    }

    fun getDaysRemaining(): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val expiry = sdf.parse(expiryDate) ?: return 0
            val now = Date()
            val diffMillis = expiry.time - now.time
            TimeUnit.MILLISECONDS.toDays(diffMillis)
        } catch (e: Exception) {
            0
        }
    }

    fun getWarrantyProgress(): Float {
        // Progress between 0.0 (just bought) and 1.0 (fully expired)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val start = sdf.parse(purchaseDate)?.time ?: return 0.5f
            val end = sdf.parse(expiryDate)?.time ?: return 0.5f
            val now = System.currentTimeMillis()
            if (end <= start) return 1.0f
            val progress = (now - start).toFloat() / (end - start).toFloat()
            progress.coerceIn(0.0f, 1.0f)
        } catch (e: Exception) {
            0.5f
        }
    }
}

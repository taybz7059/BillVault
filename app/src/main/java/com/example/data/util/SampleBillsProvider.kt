package com.example.data.util

import com.example.data.local.BillItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SampleBillsProvider {

    fun getSampleBills(userEmail: String): List<BillItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Bill 1: Active long-term warranty (Sony Bravia 55" 4K Google TV) - expires in ~18 months
        cal.time = Date()
        cal.add(Calendar.MONTH, -6)
        val tvPurchase = sdf.format(cal.time)
        cal.add(Calendar.MONTH, 24)
        val tvExpiry = sdf.format(cal.time)

        // Bill 2: Expiring Soon (< 25 days left!) - LG Smart Refrigerator 260L
        cal.time = Date()
        cal.add(Calendar.YEAR, -1)
        cal.add(Calendar.DAY_OF_YEAR, 20)
        val fridgePurchase = sdf.format(cal.time)
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, 20) // 20 days remaining!
        val fridgeExpiry = sdf.format(cal.time)

        // Bill 3: Expired warranty - Apple iPhone 14 Pro
        cal.time = Date()
        cal.add(Calendar.YEAR, -2)
        val phonePurchase = sdf.format(cal.time)
        cal.add(Calendar.YEAR, 1)
        val phoneExpiry = sdf.format(cal.time)

        // Bill 4: Active Gadget - Dell XPS 15 Laptop (3-year warranty)
        cal.time = Date()
        cal.add(Calendar.MONTH, -8)
        val laptopPurchase = sdf.format(cal.time)
        cal.add(Calendar.MONTH, 36)
        val laptopExpiry = sdf.format(cal.time)

        return listOf(
            BillItem(
                userEmail = userEmail,
                productName = "Sony Bravia 55' 4K OLED TV",
                brand = "Sony",
                storeOrSeller = "Reliance Digital, Ahmedabad",
                category = "Electronics",
                purchaseDate = tvPurchase,
                warrantyPeriodMonths = 24,
                expiryDate = tvExpiry,
                price = 78990.0,
                currency = "₹",
                invoiceNumber = "INV-RD-2024-8849",
                serialNumber = "SN-SONY-55A80K-9912",
                warrantyType = "Comprehensive Brand Warranty",
                coverageDetails = "2 Years comprehensive panel + motherboard replacement coverage",
                supportContact = "1800-103-7799 / sonycare@sony.co.in",
                notes = "Registered on Sony India portal. Keep original invoice for free home visit.",
                createdAtTimestamp = System.currentTimeMillis() - 86400000L * 180
            ),
            BillItem(
                userEmail = userEmail,
                productName = "LG Smart Inverter Refrigerator 260L",
                brand = "LG",
                storeOrSeller = "Vijay Sales",
                category = "Home Appliances",
                purchaseDate = fridgePurchase,
                warrantyPeriodMonths = 12,
                expiryDate = fridgeExpiry,
                price = 28490.0,
                currency = "₹",
                invoiceNumber = "VS-AHM-90214",
                serialNumber = "LG-GL-T292-883",
                warrantyType = "1 Year Machine + 10 Year Compressor",
                coverageDetails = "1 Year complete product warranty, 10 years smart inverter compressor parts",
                supportContact = "1800-315-9999 / serviceindia@lge.com",
                notes = "Free annual maintenance check voucher attached to physical card.",
                createdAtTimestamp = System.currentTimeMillis() - 86400000L * 340
            ),
            BillItem(
                userEmail = userEmail,
                productName = "Dell XPS 15 9530 Laptop",
                brand = "Dell",
                storeOrSeller = "Amazon India",
                category = "Computers & Laptops",
                purchaseDate = laptopPurchase,
                warrantyPeriodMonths = 36,
                expiryDate = laptopExpiry,
                price = 145000.0,
                currency = "₹",
                invoiceNumber = "AMZN-2024-991204-DL",
                serialNumber = "DELL-SVC-TAG-7XK9P2",
                warrantyType = "Dell ProSupport Plus with Accidental Damage",
                coverageDetails = "3 Years Next Business Day Onsite service, accidental liquid spill coverage",
                supportContact = "1800-425-4002 / support@dell.com",
                notes = "Service Tag 7XK9P2. Onsite engineer visits within 24 hours.",
                createdAtTimestamp = System.currentTimeMillis() - 86400000L * 240
            ),
            BillItem(
                userEmail = userEmail,
                productName = "Apple iPhone 14 Pro 128GB",
                brand = "Apple",
                storeOrSeller = "Apple Store India Online",
                category = "Mobile & Gadgets",
                purchaseDate = phonePurchase,
                warrantyPeriodMonths = 12,
                expiryDate = phoneExpiry,
                price = 119900.0,
                currency = "₹",
                invoiceNumber = "APL-IN-2023-77401",
                serialNumber = "F2LXV99PQ1M3",
                warrantyType = "Apple Limited Warranty (Expired)",
                coverageDetails = "1 Year hardware repair coverage through Apple Authorized Service Providers",
                supportContact = "000800 1009009 / getsupport.apple.com",
                notes = "Warranty period has ended. Keep bill for proof of ownership / resale value.",
                createdAtTimestamp = System.currentTimeMillis() - 86400000L * 700
            )
        )
    }
}
